package com.functional.spice.presentation

import android.graphics.Color
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.functional.spice.core.theme.FunctionalSpiceTheme
import com.functional.spice.domain.model.PrinterMode
import com.functional.spice.domain.repository.PrinterRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var printerRepository: PrinterRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35 is edge-to-edge: draw behind light, transparent system bars with dark icons,
        // even when the phone is in dark mode (the app is light only).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Launched because the KP206 was plugged in over USB
        handleUsbDeviceIntent()

        setContent {
            FunctionalSpiceTheme {
                SpiceApp()
            }
        }
    }

    private fun handleUsbDeviceIntent() {
        val usbDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
        if (usbDevice != null) {
            printerRepository.setPrinterMode(PrinterMode.USB)
            lifecycleScope.launch { printerRepository.connect() }
        }
    }
}
