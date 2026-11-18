package com.functional.spice.data.repository

import com.functional.spice.data.local.dao.BillDao
import com.functional.spice.data.local.entity.BillEntity
import com.functional.spice.data.local.entity.BillItemEntity
import com.functional.spice.data.local.entity.BillWithItems
import com.functional.spice.data.remote.SupabaseClientProvider
import com.functional.spice.data.remote.dto.SupabaseBillDto
import com.functional.spice.data.remote.dto.SupabaseBillItemDto
import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.SyncStatus
import com.functional.spice.domain.repository.BillRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillRepositoryImpl @Inject constructor(
    private val billDao: BillDao,
    private val supabaseProvider: SupabaseClientProvider
) : BillRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getAllBills(): Flow<List<Bill>> {
        return combine(billDao.getAllBills(), billDao.getAllBillItems(), ::join)
    }

    override fun getPendingBills(): Flow<List<Bill>> {
        return combine(billDao.getPendingBillsFlow(), billDao.getAllBillItems(), ::join)
    }

    override fun observeBillsBetween(start: Long, end: Long): Flow<List<Bill>> {
        return combine(billDao.getBillsBetween(start, end), billDao.getBillItemsBetween(start, end), ::join)
    }

    override fun observePendingSyncCount(): Flow<Int> = billDao.getPendingSyncCountFlow()

    override fun observeNextBillNumber(): Flow<Int> = billDao.observeMaxBillNumber().map(::nextAfter)

    override suspend fun getNextBillNumber(): Int = withContext(Dispatchers.IO) {
        nextAfter(billDao.getMaxBillNumber())
    }

    private fun nextAfter(maxNumber: Int?): Int =
        if (maxNumber == null || maxNumber < 1000) 1042 else maxNumber + 1

    override suspend fun saveBill(bill: Bill): Result<Bill> = withContext(Dispatchers.IO) {
        try {
            val billEntity = BillWithItems.toEntity(bill)
            val itemEntities = bill.items.map { BillItemEntity.fromDomain(it) }

            // 1. Save to local Room DB first (offline-first highway resilience)
            billDao.insertBillWithItems(billEntity, itemEntities)

            // 2. Try a background cloud sync if connectivity is available
            scope.launch {
                trySyncSingleBill(bill)
            }

            Result.success(bill)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun pushToSupabase(bill: Bill) {
        val client = supabaseProvider.client
        client.from("bills").insert(bill.toDto())
        val itemDtos = bill.items.map { it.toDto() }
        if (itemDtos.isNotEmpty()) {
            client.from("bill_items").insert(itemDtos)
        }
        billDao.updateSyncStatus(bill.id, SyncStatus.SYNCED.name)
    }

    private suspend fun trySyncSingleBill(bill: Bill) {
        try {
            pushToSupabase(bill)
        } catch (_: Exception) {
            // Highway network offline; bill remains safely stored as PENDING in Room DB
        }
    }

    override suspend fun syncPendingBills(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val pendingEntities = billDao.getBillsBySyncStatus(SyncStatus.PENDING.name)
            if (pendingEntities.isEmpty()) {
                return@withContext Result.success(0)
            }

            val allItems = billDao.getItemsForBills(pendingEntities.map { it.id }).groupBy { it.billId }
            var syncedCount = 0
            var lastError: Exception? = null

            for (billEntity in pendingEntities) {
                try {
                    pushToSupabase(BillWithItems(billEntity, allItems[billEntity.id] ?: emptyList()).toDomain())
                    syncedCount++
                } catch (e: Exception) {
                    // Stop on connection failure; the rest stay PENDING
                    lastError = e
                    break
                }
            }

            if (syncedCount == 0 && lastError != null) Result.failure(lastError) else Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBillById(billId: String): Bill? = withContext(Dispatchers.IO) {
        val billEntity = billDao.getBillById(billId) ?: return@withContext null
        val items = billDao.getItemsForBill(billId)
        BillWithItems(billEntity, items).toDomain()
    }

    override fun observePrintPendingCount(): Flow<Int> = billDao.getPrintPendingCountFlow()

    override suspend fun getPrintPendingBills(): List<Bill> = withContext(Dispatchers.IO) {
        val bills = billDao.getPrintPendingBills()
        if (bills.isEmpty()) return@withContext emptyList()
        val items = billDao.getItemsForBills(bills.map { it.id }).groupBy { it.billId }
        bills.map { BillWithItems(it, items[it.id] ?: emptyList()).toDomain() }
    }

    override suspend fun setPrintPending(billId: String, pending: Boolean) = withContext(Dispatchers.IO) {
        billDao.setPrintPending(billId, pending)
    }

    override suspend fun clearPrintQueue() = withContext(Dispatchers.IO) {
        billDao.clearPrintQueue()
    }

    private fun join(bills: List<BillEntity>, allItems: List<BillItemEntity>): List<Bill> {
        val itemsByBillId = allItems.groupBy { it.billId }
        return bills.map { BillWithItems(it, itemsByBillId[it.id] ?: emptyList()).toDomain() }
    }

    private fun Bill.toDto() = SupabaseBillDto(
        id = id,
        billNumber = billNumber,
        timestamp = timestamp,
        totalAmount = totalAmount,
        paymentMethod = paymentMethod.name,
        createdAt = formattedDate,
        cashReceived = cashReceived,
        changeReturned = changeReturned
    )

    private fun com.functional.spice.domain.model.BillItem.toDto() = SupabaseBillItemDto(
        id = id,
        billId = billId,
        menuItemId = menuItemId,
        itemName = itemName,
        unitPrice = unitPrice,
        quantity = quantity,
        subtotal = subtotal,
        category = category
    )
}
