package com.functional.spice.presentation.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.functional.spice.data.printer.PrintQueue
import com.functional.spice.domain.model.PairedDevice
import com.functional.spice.domain.model.PrinterMode
import com.functional.spice.domain.model.PrinterStatus
import com.functional.spice.domain.model.ShopConfig
import com.functional.spice.domain.repository.PrinterRepository
import com.functional.spice.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PrinterUiState(
    val status: PrinterStatus = PrinterStatus.Disconnected,
    val mode: PrinterMode = PrinterMode.BLUETOOTH,
    val devices: List<PairedDevice> = emptyList(),
    val selectedAddress: String? = null,
    val autoReconnect: Boolean = true,
    val printQueueCount: Int = 0,
    val shop: ShopConfig = ShopConfig(),
    /** Non-null while the shop details are being edited. */
    val shopDraft: ShopConfig? = null,
    val isBusy: Boolean = false,
    val message: String? = null
)

/** Printer (design page 5, right) plus the editable shop details used on every slip. */
@HiltViewModel
class PrinterViewModel @Inject constructor(
    private val printerRepository: PrinterRepository,
    private val settingsRepository: SettingsRepository,
    private val printQueue: PrintQueue
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrinterUiState())
    val uiState: StateFlow<PrinterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            printerRepository.printerStatus.collect { s ->
                _uiState.update { it.copy(status = s) }
                if (s.isConnected) refreshDevices()
            }
        }
        viewModelScope.launch { printerRepository.currentMode.collect { m -> _uiState.update { it.copy(mode = m) } } }
        viewModelScope.launch {
            settingsRepository.printerPrefs.collect { p ->
                _uiState.update { it.copy(selectedAddress = p.deviceAddress, autoReconnect = p.autoReconnect) }
            }
        }
        viewModelScope.launch { settingsRepository.shopConfig.collect { c -> _uiState.update { it.copy(shop = c) } } }
        viewModelScope.launch { printQueue.pendingCount.collect { n -> _uiState.update { it.copy(printQueueCount = n) } } }
        refreshDevices()
    }

    fun setMode(mode: PrinterMode) = printerRepository.setPrinterMode(mode)

    fun refreshDevices() {
        viewModelScope.launch {
            val devices = withContext(Dispatchers.IO) { printerRepository.pairedDevices() }
            _uiState.update { it.copy(devices = devices) }
        }
    }

    fun selectDevice(device: PairedDevice) = busy {
        printerRepository.selectDevice(device.address)
        null
    }

    fun setAutoReconnect(enabled: Boolean) {
        viewModelScope.launch { printerRepository.setAutoReconnect(enabled) }
    }

    fun connect() = busy {
        val result = printerRepository.connect()
        refreshDevices()
        if (result.isSuccess) "Printer connected" else "Couldn't reach the printer. Check it's on and paired."
    }

    fun disconnect() = busy {
        printerRepository.disconnect()
        "Disconnected. Bills will wait in the print queue."
    }

    fun printTestSlip() = busy {
        if (printerRepository.printTestReceipt().isSuccess) "Test slip printed" else "Printer offline. Connect it and try again."
    }

    /** Drops the waiting prints; the bills themselves stay saved. */
    fun skipPrintQueue() = busy {
        printQueue.clear()
        "Print queue cleared. The bills are still saved."
    }

    // ---- Shop details ---------------------------------------------------------------------

    fun editShop() = _uiState.update { it.copy(shopDraft = it.shop) }
    fun cancelShop() = _uiState.update { it.copy(shopDraft = null) }
    fun updateShopDraft(block: (ShopConfig) -> ShopConfig) = _uiState.update { s -> s.copy(shopDraft = s.shopDraft?.let(block)) }

    fun saveShop() {
        val draft = _uiState.value.shopDraft ?: return
        viewModelScope.launch {
            settingsRepository.saveShopConfig(draft)
            _uiState.update { it.copy(shopDraft = null, message = "Shop details saved") }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun busy(block: suspend () -> String?) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val msg = block()
            _uiState.update { it.copy(isBusy = false, message = msg ?: it.message) }
        }
    }
}
