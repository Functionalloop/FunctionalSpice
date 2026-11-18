package com.functional.spice.domain.model

enum class PrinterMode(val title: String) {
    BLUETOOTH("Bluetooth"),
    USB("USB cable")
}

sealed class PrinterStatus {
    object Disconnected : PrinterStatus()
    object Connecting : PrinterStatus()
    data class Connected(val deviceName: String, val mode: PrinterMode, val address: String? = null) : PrinterStatus()
    object PermissionRequired : PrinterStatus()
    data class Error(val message: String) : PrinterStatus()

    val isConnected: Boolean
        get() = this is Connected
}

/** A bonded Bluetooth device as listed on the Printer screen. */
data class PairedDevice(
    val name: String,
    val address: String,
    val isPrinter: Boolean
)
