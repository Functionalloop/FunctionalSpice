package com.functional.spice.domain.model

/** Quantity and amount sold for one item name (from bill line snapshots). */
data class ItemTotal(
    val name: String,
    val qty: Int,
    val amount: Int,
    /** Unit price, or null if the item sold at more than one price in the period. */
    val rate: Int? = null
)

data class CategoryTotal(val name: String, val amount: Int) {
    fun percentOf(total: Int): Int = if (total > 0) Math.round(amount * 100f / total) else 0
}

/** Input for the printed day summary slip. */
data class DaySummary(
    val date: Long,
    val printedAt: Long,
    val bills: Int,
    val firstBillNo: Int,
    val lastBillNo: Int,
    val items: List<ItemTotal>,
    val categories: List<CategoryTotal>,
    val cashSales: Int,
    val upiSales: Int,
    val openingFloat: Int
) {
    val total: Int get() = cashSales + upiSales
    val itemsSold: Int get() = items.sumOf { it.qty }
}

/** Input for the printed month summary slip. */
data class MonthSummary(
    val month: Long,
    val daysOpen: Int,
    val bills: Int,
    val bestDay: Long,
    val bestDaySales: Int,
    /** e.g. "Week 1 (1-7)" to 81800 */
    val weeks: List<Pair<String, Int>>,
    val topItems: List<ItemTotal>,
    val categories: List<CategoryTotal>,
    val cashSales: Int,
    val upiSales: Int
) {
    val total: Int get() = cashSales + upiSales
}
