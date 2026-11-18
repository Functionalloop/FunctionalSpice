package com.functional.spice.data.repository

import com.functional.spice.data.printer.Kp206PrinterManager
import com.functional.spice.data.printer.ReceiptFormatter
import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.DaySummary
import com.functional.spice.domain.model.MonthSummary
import com.functional.spice.domain.model.PairedDevice
import com.functional.spice.domain.model.PrinterMode
import com.functional.spice.domain.model.PrinterStatus
import com.functional.spice.domain.repository.PrinterRepository
import com.functional.spice.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrinterRepositoryImpl @Inject constructor(
    private val printerManager: Kp206PrinterManager,
    private val settings: SettingsRepository
) : PrinterRepository {

    override val printerStatus: StateFlow<PrinterStatus> = printerManager.printerStatus
    override val currentMode: StateFlow<PrinterMode> = printerManager.currentMode

    override fun setPrinterMode(mode: PrinterMode) {
        printerManager.setMode(mode)
    }

    override suspend fun connect(): Result<Unit> = printerManager.checkAndConnect(userInitiated = true)

    override suspend fun disconnect() {
        printerManager.disconnect()
    }

    override fun pairedDevices(): List<PairedDevice> = printerManager.pairedDevices()

    override suspend fun selectDevice(address: String) {
        printerManager.selectDevice(address)
    }

    override suspend fun setAutoReconnect(enabled: Boolean) {
        printerManager.setAutoReconnect(enabled)
    }

    override suspend fun printBill(bill: Bill): Result<Unit> =
        printerManager.printBill(bill, settings.shopConfig.first())

    override suspend fun printDaySummary(summary: DaySummary): Result<Unit> =
        printerManager.printMarkup(ReceiptFormatter.daySummary(summary, settings.shopConfig.first()))

    override suspend fun printMonthSummary(summary: MonthSummary): Result<Unit> =
        printerManager.printMarkup(ReceiptFormatter.monthSummary(summary, settings.shopConfig.first()))

    override suspend fun printTestReceipt(): Result<Unit> =
        printerManager.printTestReceipt(settings.shopConfig.first())
}
