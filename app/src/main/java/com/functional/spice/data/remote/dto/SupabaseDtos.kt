package com.functional.spice.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseBillDto(
    @SerialName("id")
    val id: String,
    @SerialName("bill_number")
    val billNumber: Int,
    @SerialName("timestamp")
    val timestamp: Long,
    @SerialName("total_amount")
    val totalAmount: Double,
    /** CASH or UPI (the spec's payment_mode; this table already had payment_method). */
    @SerialName("payment_method")
    val paymentMethod: String,
    @SerialName("created_at")
    val createdAt: String,
    // v2: run supabase/migrations/2026-11-13_v2_payment_and_day_close.sql first, or inserts fail
    // (bills then simply stay PENDING on the phone until the columns exist)
    @SerialName("cash_received")
    val cashReceived: Int? = null,
    @SerialName("change_returned")
    val changeReturned: Int? = null
)

@Serializable
data class SupabaseBillItemDto(
    @SerialName("id")
    val id: String,
    @SerialName("bill_id")
    val billId: String,
    @SerialName("menu_item_id")
    val menuItemId: String,
    /** Name snapshot at sale time. */
    @SerialName("item_name")
    val itemName: String,
    /** Price snapshot at sale time. */
    @SerialName("unit_price")
    val unitPrice: Double,
    @SerialName("quantity")
    val quantity: Int,
    @SerialName("subtotal")
    val subtotal: Double,
    // v2: category snapshot
    @SerialName("category")
    val category: String? = null
)

@Serializable
data class SupabaseDayCloseDto(
    /** ISO date, e.g. "2026-11-13". */
    @SerialName("date")
    val date: String,
    @SerialName("opening_float")
    val openingFloat: Int,
    /** ISO-8601 instant, or null while the day is still open. */
    @SerialName("closed_at")
    val closedAt: String? = null
)
