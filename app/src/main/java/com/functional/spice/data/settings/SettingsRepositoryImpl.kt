package com.functional.spice.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.functional.spice.domain.model.PrinterMode
import com.functional.spice.domain.model.ShopConfig
import com.functional.spice.domain.repository.PrinterPrefs
import com.functional.spice.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val PRINT_NAME = stringPreferencesKey("shop_print_name")
        val SUBTITLE = stringPreferencesKey("shop_subtitle")
        val DISPLAY_NAME = stringPreferencesKey("shop_display_name")
        val ADDRESS = stringPreferencesKey("shop_address")
        val SLUG = stringPreferencesKey("shop_slug")
        val PHONE = stringPreferencesKey("shop_support_phone")
        val UPI_ID = stringPreferencesKey("shop_upi_id")
        val MIN_PRINT_AMOUNT = intPreferencesKey("shop_min_print_amount")

        val PRINTER_MODE = stringPreferencesKey("printer_mode")
        val PRINTER_ADDRESS = stringPreferencesKey("printer_address")
        val AUTO_RECONNECT = booleanPreferencesKey("printer_auto_reconnect")
    }

    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val shopConfig: Flow<ShopConfig> = prefs.map { p ->
        val d = ShopConfig()
        ShopConfig(
            shopName = p[Keys.SHOP_NAME] ?: d.shopName,
            printName = p[Keys.PRINT_NAME] ?: d.printName,
            subtitle = p[Keys.SUBTITLE] ?: d.subtitle,
            displayName = p[Keys.DISPLAY_NAME] ?: d.displayName,
            address = p[Keys.ADDRESS] ?: d.address,
            slug = p[Keys.SLUG] ?: d.slug,
            supportPhone = p[Keys.PHONE] ?: d.supportPhone,
            upiId = p[Keys.UPI_ID] ?: d.upiId,
            minPrintAmount = p[Keys.MIN_PRINT_AMOUNT] ?: d.minPrintAmount
        )
    }.distinctUntilChanged()

    override val printerPrefs: Flow<PrinterPrefs> = prefs.map { p ->
        PrinterPrefs(
            mode = p[Keys.PRINTER_MODE]?.let { runCatching { PrinterMode.valueOf(it) }.getOrNull() }
                ?: PrinterMode.BLUETOOTH,
            deviceAddress = p[Keys.PRINTER_ADDRESS],
            autoReconnect = p[Keys.AUTO_RECONNECT] ?: true
        )
    }.distinctUntilChanged()

    override suspend fun saveShopConfig(config: ShopConfig) {
        dataStore.edit { p ->
            p[Keys.SHOP_NAME] = config.shopName.trim()
            p[Keys.PRINT_NAME] = config.printName.trim()
            p[Keys.SUBTITLE] = config.subtitle.trim()
            p[Keys.DISPLAY_NAME] = config.displayName.trim()
            p[Keys.ADDRESS] = config.address.trim()
            p[Keys.SLUG] = config.slug.trim()
            p[Keys.PHONE] = config.supportPhone.trim()
            p[Keys.UPI_ID] = config.upiId.trim()
            p[Keys.MIN_PRINT_AMOUNT] = config.minPrintAmount.coerceAtLeast(0)
        }
    }

    override suspend fun setPrinterMode(mode: PrinterMode) {
        dataStore.edit { it[Keys.PRINTER_MODE] = mode.name }
    }

    override suspend fun setPrinterAddress(address: String?) {
        dataStore.edit { p ->
            if (address == null) p.remove(Keys.PRINTER_ADDRESS) else p[Keys.PRINTER_ADDRESS] = address
        }
    }

    override suspend fun setAutoReconnect(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_RECONNECT] = enabled }
    }
}
