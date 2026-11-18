package com.functional.spice.domain.model

/** Stored as text in bills.paymentMethod (Room) and bills.payment_method (Supabase). */
enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI")
}
