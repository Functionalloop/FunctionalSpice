package com.functional.spice.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.functional.spice.domain.model.PaymentMethod
import com.functional.spice.domain.model.SyncStatus

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "billNumber")
    val billNumber: Int,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "totalAmount")
    val totalAmount: Double,
    /** Payment mode: CASH or UPI. Existing v1 column, reused (the spec calls it paymentMode). */
    @ColumnInfo(name = "paymentMethod")
    val paymentMethod: String = PaymentMethod.CASH.name,
    @ColumnInfo(name = "syncStatus")
    val syncStatus: String = SyncStatus.PENDING.name,
    @ColumnInfo(name = "itemCount")
    val itemCount: Int = 0,
    // v2
    @ColumnInfo(name = "cashReceived")
    val cashReceived: Int? = null,
    @ColumnInfo(name = "changeReturned")
    val changeReturned: Int? = null,
    /** 1 while the bill is saved but not printed yet; the print queue clears it. */
    @ColumnInfo(name = "printPending", defaultValue = "0")
    val printPending: Boolean = false
)
