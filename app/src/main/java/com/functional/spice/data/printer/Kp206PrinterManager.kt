package com.functional.spice.data.printer

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnections
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException
import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.PairedDevice
import com.functional.spice.domain.model.PrinterMode
import com.functional.spice.domain.model.PrinterStatus
import com.functional.spice.domain.model.ShopConfig
import com.functional.spice.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

class PrinterOfflineException(message: String) : Exception(message)

/**
 * Posiflow KP206 over Bluetooth or USB via DantSu ESC/POS:
 * EscPosPrinter(connection, 203, 48f, 32) = 203 dpi, 48mm printable on 58mm paper, 32 chars/line.
 *
 * A print opens the connection, prints and closes it. If the printer can't be reached the
 * call fails (so the bill can be queued) and, with auto-reconnect on, the manager retries
 * every 5 seconds until the printer answers again.
 */
@Singleton
class Kp206PrinterManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val footerStrip: FooterStripRenderer,
    private val cupLogo: CupLogoRenderer
) {
    companion object {
        const val DPI = 203
        const val WIDTH_MM = 48f
        const val CHARS_PER_LINE = 32
        const val RECONNECT_INTERVAL_MS = 5_000L
        private const val ACTION_USB_PERMISSION = "com.functional.spice.USB_PERMISSION"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** One connection at a time: prints, probes and reconnects never overlap. */
    private val ioLock = Mutex()

    private val _currentMode = MutableStateFlow(PrinterMode.BLUETOOTH)
    val currentMode: StateFlow<PrinterMode> = _currentMode.asStateFlow()

    private val _printerStatus = MutableStateFlow<PrinterStatus>(PrinterStatus.Disconnected)
    val printerStatus: StateFlow<PrinterStatus> = _printerStatus.asStateFlow()

    @Volatile private var preferredAddress: String? = null
    @Volatile private var autoReconnect = true
    /** Set by "Disconnect": stops auto-reconnect until the user taps Connect again. */
    @Volatile private var userDisconnected = false
    /** The USB permission dialog is shown once per Connect tap, never from the retry loop. */
    @Volatile private var usbPermissionAsked = false
    private var reconnectJob: Job? = null

    init {
        scope.launch {
            var first = true
            settings.printerPrefs.collect { prefs ->
                preferredAddress = prefs.deviceAddress
                autoReconnect = prefs.autoReconnect
                val modeChanged = _currentMode.value != prefs.mode
                _currentMode.value = prefs.mode
                if (first || modeChanged) {
                    first = false
                    checkAndConnect()
                }
                updateReconnectLoop()
            }
        }
    }

    fun setMode(mode: PrinterMode) {
        if (_currentMode.value != mode) {
            _currentMode.value = mode
            scope.launch {
                settings.setPrinterMode(mode)
                checkAndConnect(userInitiated = true)
            }
        }
    }

    suspend fun selectDevice(address: String) {
        preferredAddress = address
        settings.setPrinterAddress(address)
        checkAndConnect(userInitiated = true)
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        autoReconnect = enabled
        settings.setAutoReconnect(enabled)
        updateReconnectLoop()
    }

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            granted(Manifest.permission.BLUETOOTH_CONNECT) && granted(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            granted(Manifest.permission.BLUETOOTH)
        }
    }

    private fun granted(p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    /** Opens and closes a connection to check the printer actually answers. */
    suspend fun checkAndConnect(userInitiated: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        if (userInitiated) {
            userDisconnected = false
            usbPermissionAsked = false
        }
        ioLock.withLock {
            // Background retries stay quiet so the status pill doesn't flicker every 5 seconds
            if (userInitiated) _printerStatus.value = PrinterStatus.Connecting
            val target = when (val t = findTarget()) {
                is Target.Found -> t
                is Target.Missing -> {
                    _printerStatus.value = t.status
                    return@withContext Result.failure(PrinterOfflineException(t.reason))
                }
            }
            try {
                target.connection.connect()
                _printerStatus.value = PrinterStatus.Connected(target.name, _currentMode.value, target.address)
                Result.success(Unit)
            } catch (e: Exception) {
                _printerStatus.value = PrinterStatus.Disconnected
                Result.failure(e)
            } finally {
                runCatching { target.connection.disconnect() }
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        userDisconnected = true
        _printerStatus.value = PrinterStatus.Disconnected
    }

    private fun updateReconnectLoop() {
        if (autoReconnect) {
            if (reconnectJob?.isActive == true) return
            reconnectJob = scope.launch {
                while (isActive) {
                    delay(RECONNECT_INTERVAL_MS)
                    val s = _printerStatus.value
                    val waitingForPermission =
                        s is PrinterStatus.PermissionRequired && !hasBluetoothPermissions()
                    if (!userDisconnected && !s.isConnected && s !is PrinterStatus.Connecting && !waitingForPermission) {
                        checkAndConnect()
                    }
                }
            }
        } else {
            reconnectJob?.cancel()
            reconnectJob = null
        }
    }

    // ---- Paired devices -------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<PairedDevice> {
        if (!hasBluetoothPermissions()) return emptyList()
        return runCatching {
            val printers = BluetoothPrintersConnections().list.orEmpty().map { it.device.address }.toSet()
            BluetoothConnections().list.orEmpty().map { conn ->
                val d = conn.device
                PairedDevice(name = d.name ?: d.address, address = d.address, isPrinter = d.address in printers)
            }.sortedByDescending { it.isPrinter }
        }.getOrDefault(emptyList())
    }

    // ---- Printing -------------------------------------------------------------------------

    /**
     * Customer bill with the cup logo on top and the Functional logo strip at the foot.
     * If an image can't be printed, the bill prints again text-only (no cup, "Functional" wordmark).
     */
    suspend fun printBill(bill: Bill, shop: ShopConfig): Result<Unit> = printWith { printer ->
        val footerHex = runCatching { footerStrip.hex(printer) }.getOrNull()
        val cupHex = runCatching { cupLogo.hex(printer) }.getOrNull()
        try {
            printer.printFormattedText(ReceiptFormatter.customerBill(bill, shop, footerHex, cupHex))
        } catch (e: Exception) {
            if ((footerHex == null && cupHex == null) || !e.isFormattingError()) throw e
            printer.printFormattedText(ReceiptFormatter.customerBill(bill, shop, null, null))
        }
    }

    /** Any pre-built DantSu markup (day and month summaries). */
    suspend fun printMarkup(markup: String): Result<Unit> = printWith { printer ->
        printer.printFormattedText(markup)
    }

    suspend fun printTestReceipt(shop: ShopConfig): Result<Unit> =
        printBill(ReceiptFormatter.createSampleBill(), shop)

    private fun Exception.isFormattingError() =
        this is EscPosParserException || this is EscPosEncodingException || this is EscPosBarcodeException

    private suspend fun printWith(block: (EscPosPrinter) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        ioLock.withLock {
            val target = when (val t = findTarget()) {
                is Target.Found -> t
                is Target.Missing -> {
                    _printerStatus.value = t.status
                    return@withContext Result.failure(PrinterOfflineException(t.reason))
                }
            }
            var printer: EscPosPrinter? = null
            try {
                printer = EscPosPrinter(target.connection, DPI, WIDTH_MM, CHARS_PER_LINE)
                block(printer)
                userDisconnected = false
                _printerStatus.value = PrinterStatus.Connected(target.name, _currentMode.value, target.address)
                Result.success(Unit)
            } catch (e: EscPosConnectionException) {
                _printerStatus.value = PrinterStatus.Disconnected
                Result.failure(PrinterOfflineException("Printer is not reachable"))
            } catch (e: Exception) {
                _printerStatus.value = PrinterStatus.Error(e.message ?: "Print failed")
                Result.failure(e)
            } finally {
                runCatching { printer?.disconnectPrinter() ?: target.connection.disconnect() }
            }
        }
    }

    // ---- Finding the printer --------------------------------------------------------------

    private sealed class Target {
        class Found(val connection: DeviceConnection, val name: String, val address: String?) : Target()
        class Missing(val status: PrinterStatus, val reason: String) : Target()
    }

    @SuppressLint("MissingPermission")
    private fun findTarget(): Target = try {
        when (_currentMode.value) {
            PrinterMode.BLUETOOTH -> {
                if (!hasBluetoothPermissions()) {
                    Target.Missing(PrinterStatus.PermissionRequired, "Bluetooth permission required")
                } else {
                    val printers = BluetoothPrintersConnections().list.orEmpty()
                    val wanted = preferredAddress
                    val conn: BluetoothConnection? = if (wanted != null) {
                        printers.firstOrNull { it.device.address == wanted }
                            ?: BluetoothConnections().list.orEmpty().firstOrNull { it.device.address == wanted }
                    } else {
                        printers.firstOrNull()
                    }
                    if (conn == null) {
                        Target.Missing(PrinterStatus.Disconnected, "No paired Bluetooth printer found")
                    } else {
                        Target.Found(conn, conn.device.name ?: "Posiflow KP206", conn.device.address)
                    }
                }
            }
            PrinterMode.USB -> {
                val conn = UsbPrintersConnections.selectFirstConnected(context)
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                when {
                    conn == null -> Target.Missing(PrinterStatus.Disconnected, "No USB printer connected")
                    !usbManager.hasPermission(conn.device) -> {
                        if (!usbPermissionAsked) {
                            usbPermissionAsked = true
                            requestUsbPermission(usbManager, conn.device)
                        }
                        Target.Missing(PrinterStatus.Error("Allow USB access, then tap Connect"), "USB permission required")
                    }
                    else -> Target.Found(conn, conn.device.productName ?: "Posiflow KP206 USB", null)
                }
            }
        }
    } catch (e: Exception) {
        Target.Missing(PrinterStatus.Error(e.message ?: "Printer error"), e.message ?: "Printer error")
    }

    private fun requestUsbPermission(usbManager: UsbManager, device: android.hardware.usb.UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(context.packageName)
        usbManager.requestPermission(device, PendingIntent.getBroadcast(context, 0, intent, flags))
    }
}
