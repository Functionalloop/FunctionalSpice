package com.functional.spice.domain.repository

import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.DaySummary
import com.functional.spice.domain.model.MonthSummary
import com.functional.spice.domain.model.PairedDevice
import com.functional.spice.domain.model.PrinterMode
import com.functional.spice.domain.model.PrinterStatus
import kotlinx.coroutines.flow.StateFlow

interface PrinterRepository {
    val printerStatus: StateFlow<PrinterStatus>
    val currentMode: StateFlow<PrinterMode>

    fun setPrinterMode(mode: PrinterMode)
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    fun pairedDevices(): List<PairedDevice>
    suspend fun selectDevice(address: String)
    suspend fun setAutoReconnect(enabled: Boolean)

    /** Customer bill with the Functional logo strip. Fails if the printer can't be reached. */
    suspend fun printBill(bill: Bill): Result<Unit>
    suspend fun printDaySummary(summary: DaySummary): Result<Unit>
    suspend fun printMonthSummary(summary: MonthSummary): Result<Unit>
    suspend fun printTestReceipt(): Result<Unit>
}
