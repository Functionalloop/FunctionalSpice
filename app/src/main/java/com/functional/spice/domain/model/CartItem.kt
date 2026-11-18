package com.functional.spice.domain.model

data class CartItem(
    val item: MenuItem,
    val quantity: Int
) {
    val subtotal: Double
        get() = item.price * quantity

    val subtotalRupees: Int
        get() = item.priceRupees * quantity

    val formattedSubtotal: String
        get() = Money.inr(subtotalRupees)
}
