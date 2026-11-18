package com.functional.spice.data.local.entity

import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.PaymentMethod
import com.functional.spice.domain.model.SyncStatus

data class BillWithItems(
    val bill: BillEntity,
    val items: List<BillItemEntity>
) {
    fun toDomain(): Bill {
        return Bill(
            id = bill.id,
            billNumber = bill.billNumber,
            timestamp = bill.timestamp,
            items = items.map { it.toDomain() },
            totalAmount = bill.totalAmount,
            paymentMethod = runCatching { PaymentMethod.valueOf(bill.paymentMethod) }.getOrDefault(PaymentMethod.CASH),
            syncStatus = runCatching { SyncStatus.valueOf(bill.syncStatus) }.getOrDefault(SyncStatus.PENDING),
            cashReceived = bill.cashReceived,
            changeReturned = bill.changeReturned,
            printPending = bill.printPending
        )
    }

    companion object {
        fun toEntity(bill: Bill): BillEntity = BillEntity(
            id = bill.id,
            billNumber = bill.billNumber,
            timestamp = bill.timestamp,
            totalAmount = bill.totalAmount,
            paymentMethod = bill.paymentMethod.name,
            syncStatus = SyncStatus.PENDING.name,
            itemCount = bill.totalItemCount,
            cashReceived = bill.cashReceived,
            changeReturned = bill.changeReturned,
            printPending = bill.printPending
        )
    }
}
