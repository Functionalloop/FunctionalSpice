package com.functional.spice.domain.repository

import com.functional.spice.domain.model.Bill
import kotlinx.coroutines.flow.Flow

interface BillRepository {
    fun getAllBills(): Flow<List<Bill>>
    fun getPendingBills(): Flow<List<Bill>>

    /** Bills with start <= timestamp < end, newest first. */
    fun observeBillsBetween(start: Long, end: Long): Flow<List<Bill>>

    fun observePendingSyncCount(): Flow<Int>
    fun observeNextBillNumber(): Flow<Int>
    suspend fun getNextBillNumber(): Int
    suspend fun saveBill(bill: Bill): Result<Bill>
    suspend fun syncPendingBills(): Result<Int>
    suspend fun getBillById(billId: String): Bill?

    // Print queue
    fun observePrintPendingCount(): Flow<Int>
    suspend fun getPrintPendingBills(): List<Bill>
    suspend fun setPrintPending(billId: String, pending: Boolean)
    suspend fun clearPrintQueue()
}
