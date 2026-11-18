package com.functional.spice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.functional.spice.data.local.entity.BillEntity
import com.functional.spice.data.local.entity.BillItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY timestamp DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bill_items")
    fun getAllBillItems(): Flow<List<BillItemEntity>>

    @Query("SELECT * FROM bills WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun getBillsBetween(start: Long, end: Long): Flow<List<BillEntity>>

    @Query(
        "SELECT bill_items.* FROM bill_items INNER JOIN bills ON bills.id = bill_items.billId " +
            "WHERE bills.timestamp >= :start AND bills.timestamp < :end"
    )
    fun getBillItemsBetween(start: Long, end: Long): Flow<List<BillItemEntity>>

    @Query("SELECT * FROM bills WHERE syncStatus = :syncStatus ORDER BY timestamp ASC")
    suspend fun getBillsBySyncStatus(syncStatus: String): List<BillEntity>

    @Query("SELECT * FROM bills WHERE syncStatus = 'PENDING' ORDER BY timestamp ASC")
    fun getPendingBillsFlow(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE printPending = 1 ORDER BY billNumber ASC")
    suspend fun getPrintPendingBills(): List<BillEntity>

    @Query("SELECT COUNT(*) FROM bills WHERE printPending = 1")
    fun getPrintPendingCountFlow(): Flow<Int>

    @Query("UPDATE bills SET printPending = :pending WHERE id = :billId")
    suspend fun setPrintPending(billId: String, pending: Boolean)

    @Query("UPDATE bills SET printPending = 0 WHERE printPending = 1")
    suspend fun clearPrintQueue()

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: String): BillEntity?

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    suspend fun getItemsForBill(billId: String): List<BillItemEntity>

    @Query("SELECT * FROM bill_items WHERE billId IN (:billIds)")
    suspend fun getItemsForBills(billIds: List<String>): List<BillItemEntity>

    @Query("SELECT MAX(billNumber) FROM bills")
    suspend fun getMaxBillNumber(): Int?

    @Query("SELECT MAX(billNumber) FROM bills")
    fun observeMaxBillNumber(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM bills WHERE syncStatus = 'PENDING'")
    fun getPendingSyncCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItemEntity>)

    @Transaction
    suspend fun insertBillWithItems(bill: BillEntity, items: List<BillItemEntity>) {
        insertBill(bill)
        insertBillItems(items)
    }

    @Query("UPDATE bills SET syncStatus = :status WHERE id = :billId")
    suspend fun updateSyncStatus(billId: String, status: String)

    @Query("DELETE FROM bills WHERE id = :billId")
    suspend fun deleteBill(billId: String)
}
