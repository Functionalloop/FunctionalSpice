package com.functional.spice.domain.model

import java.util.Locale
import kotlin.math.roundToInt

/**
 * One bill line. [itemName] and [unitPrice] are snapshots taken at sale time, so
 * summaries stay correct after a menu item is renamed, repriced or deleted.
 */
data class BillItem(
    val id: String,
    val billId: String,
    val menuItemId: String,
    val itemName: String,
    val unitPrice: Double,
    val quantity: Int,
    val subtotal: Double,
    /** Category name ([Category.name]) at sale time; null for bills saved before v2. */
    val category: String? = null
) {
    val formattedUnitPrice: String
        get() = String.format(Locale.US, "%.0f", unitPrice)

    val formattedSubtotal: String
        get() = String.format(Locale.US, "%.2f", subtotal)

    val unitPriceRupees: Int
        get() = unitPrice.roundToInt()

    val subtotalRupees: Int
        get() = subtotal.roundToInt()
}
