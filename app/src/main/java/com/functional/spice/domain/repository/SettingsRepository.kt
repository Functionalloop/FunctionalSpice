package com.functional.spice.domain.repository

import com.functional.spice.domain.model.PrinterMode
import com.functional.spice.domain.model.ShopConfig
import kotlinx.coroutines.flow.Flow

data class PrinterPrefs(
    val mode: PrinterMode = PrinterMode.BLUETOOTH,
    /** Bluetooth address of the chosen printer; null = first paired printer. */
    val deviceAddress: String? = null,
    /** Retry every 5 seconds while the printer is unreachable. */
    val autoReconnect: Boolean = true
)

interface SettingsRepository {
    val shopConfig: Flow<ShopConfig>
    val printerPrefs: Flow<PrinterPrefs>

    suspend fun saveShopConfig(config: ShopConfig)
    suspend fun setPrinterMode(mode: PrinterMode)
    suspend fun setPrinterAddress(address: String?)
    suspend fun setAutoReconnect(enabled: Boolean)
}
