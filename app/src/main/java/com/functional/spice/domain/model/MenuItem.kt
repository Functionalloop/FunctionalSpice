package com.functional.spice.domain.model

import kotlin.math.roundToInt

data class MenuItem(
    val id: String,
    val name: String,
    val konkaniSubtitle: String = "",
    val category: Category,
    val price: Double,
    /** False = sold out: stays in the menu but is hidden from the Counter. */
    val isAvailable: Boolean = true,
    val sortOrder: Int = 0
) {
    val priceRupees: Int
        get() = price.roundToInt()

    val formattedPrice: String
        get() = Money.inr(priceRupees)
}
