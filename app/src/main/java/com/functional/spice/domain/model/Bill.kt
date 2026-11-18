package com.functional.spice.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class Bill(
    val id: String,
    val billNumber: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<BillItem>,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    /** Whole rupees the customer handed over (cash bills only). */
    val cashReceived: Int? = null,
    /** Whole rupees returned to the customer (cash bills only). */
    val changeReturned: Int? = null,
    /** True while the bill is saved but not printed yet (printer offline). */
    val printPending: Boolean = false
) {
    val totalItemCount: Int
        get() = items.sumOf { it.quantity }

    /** Bill total in whole rupees. All menu prices are whole rupees. */
    val totalRupees: Int
        get() = totalAmount.roundToInt()

    val formattedBillNumber: String
        get() = "#$billNumber"

    val formattedTotal: String
        get() = String.format(Locale.US, "Rs. %.2f", totalAmount)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd-MMM-yy HH:mm", Locale.US)
            return sdf.format(Date(timestamp))
        }

    val formattedTime: String
        get() = SimpleDateFormat("HH:mm", Locale.US).format(Date(timestamp))
}
