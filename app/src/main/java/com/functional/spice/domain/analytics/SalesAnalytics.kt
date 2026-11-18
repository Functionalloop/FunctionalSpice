package com.functional.spice.domain.analytics

import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.BillItem
import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.CategoryTotal
import com.functional.spice.domain.model.DaySummary
import com.functional.spice.domain.model.ItemTotal
import com.functional.spice.domain.model.MonthSummary
import com.functional.spice.domain.model.PaymentMethod
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class TodayStats(
    val date: LocalDate,
    val total: Int,
    val bills: Int,
    val itemsSold: Int,
    /** Average bill in whole rupees (rounded) for the screen. */
    val avgBill: Int,
    /** Sales per hour; index 0 = 6 AM to 7 AM ... index 15 = 9 PM to 10 PM. */
    val hourly: List<Int>,
    /** Hour of day (6..21) with the highest sales, or null if no sales. */
    val busiestHour: Int?,
    val categories: List<CategoryTotal>,
    val items: List<ItemTotal>,
    val cashSales: Int,
    val upiSales: Int,
    val firstBillNo: Int,
    val lastBillNo: Int,
    /** Newest first. */
    val recent: List<Bill>
)

data class WeekTotal(
    val index: Int,
    val startDay: Int,
    val endDay: Int,
    val amount: Int,
    /** False while the week is still running (it ends after today). */
    val complete: Boolean
) {
    val days: Int get() = endDay - startDay + 1
    val printLabel: String get() = "Week $index ($startDay-$endDay)"
}

data class MonthStats(
    val month: YearMonth,
    /** Day of month for today; days after it are in the future. */
    val today: Int,
    val total: Int,
    val bills: Int,
    val daysOpen: Int,
    val dailyAvg: Double,
    /** Daily average x days in month, rounded to the nearest ₹100. */
    val onPace: Int,
    /** Sales per day; index 0 = day 1. */
    val daily: List<Int>,
    val bestDay: Int?,
    val bestDaySales: Int,
    val weeks: List<WeekTotal>,
    /** Top sellers by quantity. */
    val topItems: List<ItemTotal>,
    val categories: List<CategoryTotal>,
    val cashSales: Int,
    val upiSales: Int
)

/**
 * Everything on the Bills screen and the printed summaries comes from here.
 * Numbers use the name and price stored on each bill line, never the live menu,
 * so edited or deleted menu items still count correctly.
 */
object SalesAnalytics {
    const val FIRST_HOUR = 6
    const val LAST_HOUR = 21
    const val OTHER_CATEGORY = "Other"

    /**
     * @param menuCategories current category for each menu item id, used only for
     *   bill lines saved before v2 (they have no category snapshot).
     */
    fun categoryOf(item: BillItem, menuCategories: Map<String, Category>): String {
        val snap = item.category?.let { Category.fromString(it) }?.takeIf { it != Category.ALL }
        val cat = snap ?: menuCategories[item.menuItemId]?.takeIf { it != Category.ALL }
        return cat?.displayName ?: OTHER_CATEGORY
    }

    fun today(
        bills: List<Bill>,
        date: LocalDate,
        zone: ZoneId,
        menuCategories: Map<String, Category>
    ): TodayStats {
        val day = bills.filter { it.localDate(zone) == date }
        val total = day.sumOf { it.totalRupees }
        val hourly = IntArray(LAST_HOUR - FIRST_HOUR + 1)
        day.forEach { bill ->
            val hour = Instant.ofEpochMilli(bill.timestamp).atZone(zone).hour
            hourly[hour.coerceIn(FIRST_HOUR, LAST_HOUR) - FIRST_HOUR] += bill.totalRupees
        }
        val busiest = hourly.indices.maxByOrNull { hourly[it] }
            ?.takeIf { hourly[it] > 0 }
            ?.let { it + FIRST_HOUR }

        return TodayStats(
            date = date,
            total = total,
            bills = day.size,
            itemsSold = day.sumOf { it.totalItemCount },
            avgBill = if (day.isNotEmpty()) (total.toDouble() / day.size).roundToInt() else 0,
            hourly = hourly.toList(),
            busiestHour = busiest,
            categories = categories(day, menuCategories),
            items = items(day).sortedByDescending { it.amount },
            cashSales = day.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalRupees },
            upiSales = day.filter { it.paymentMethod == PaymentMethod.UPI }.sumOf { it.totalRupees },
            firstBillNo = day.minOfOrNull { it.billNumber } ?: 0,
            lastBillNo = day.maxOfOrNull { it.billNumber } ?: 0,
            recent = day.sortedByDescending { it.timestamp }
        )
    }

    fun month(
        bills: List<Bill>,
        today: LocalDate,
        zone: ZoneId,
        menuCategories: Map<String, Category>
    ): MonthStats {
        val ym = YearMonth.from(today)
        val inMonth = bills.filter { YearMonth.from(it.localDate(zone)) == ym }
        val daily = IntArray(ym.lengthOfMonth())
        inMonth.forEach { daily[it.localDate(zone).dayOfMonth - 1] += it.totalRupees }

        val total = inMonth.sumOf { it.totalRupees }
        val daysOpen = inMonth.map { it.localDate(zone) }.distinct().size
        val dailyAvg = if (daysOpen > 0) total.toDouble() / daysOpen else 0.0
        val onPace = ((dailyAvg * ym.lengthOfMonth()) / 100.0).roundToLong().toInt() * 100
        val bestIndex = daily.indices.maxByOrNull { daily[it] }?.takeIf { daily[it] > 0 }

        val weeks = mutableListOf<WeekTotal>()
        var start = 1
        var index = 1
        while (start <= today.dayOfMonth) {
            val fullEnd = minOf(start + 6, ym.lengthOfMonth())
            val end = minOf(fullEnd, today.dayOfMonth)
            weeks += WeekTotal(
                index = index,
                startDay = start,
                endDay = end,
                amount = (start..end).sumOf { daily[it - 1] },
                complete = end == fullEnd
            )
            start += 7
            index++
        }

        return MonthStats(
            month = ym,
            today = today.dayOfMonth,
            total = total,
            bills = inMonth.size,
            daysOpen = daysOpen,
            dailyAvg = dailyAvg,
            onPace = onPace,
            daily = daily.toList(),
            bestDay = bestIndex?.plus(1),
            bestDaySales = bestIndex?.let { daily[it] } ?: 0,
            weeks = weeks,
            topItems = items(inMonth).sortedWith(compareByDescending<ItemTotal> { it.qty }.thenByDescending { it.amount }).take(5),
            categories = categories(inMonth, menuCategories),
            cashSales = inMonth.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalRupees },
            upiSales = inMonth.filter { it.paymentMethod == PaymentMethod.UPI }.sumOf { it.totalRupees }
        )
    }

    fun toDaySummary(stats: TodayStats, openingFloat: Int, printedAt: Long, zone: ZoneId) = DaySummary(
        date = stats.date.atStartOfDay(zone).toInstant().toEpochMilli(),
        printedAt = printedAt,
        bills = stats.bills,
        firstBillNo = stats.firstBillNo,
        lastBillNo = stats.lastBillNo,
        items = stats.items,
        categories = stats.categories,
        cashSales = stats.cashSales,
        upiSales = stats.upiSales,
        openingFloat = openingFloat
    )

    fun toMonthSummary(stats: MonthStats, zone: ZoneId) = MonthSummary(
        month = stats.month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        daysOpen = stats.daysOpen,
        bills = stats.bills,
        bestDay = stats.month.atDay(stats.bestDay ?: 1).atStartOfDay(zone).toInstant().toEpochMilli(),
        bestDaySales = stats.bestDaySales,
        weeks = stats.weeks.map { it.printLabel to it.amount },
        topItems = stats.topItems,
        categories = stats.categories,
        cashSales = stats.cashSales,
        upiSales = stats.upiSales
    )

    private fun items(bills: List<Bill>): List<ItemTotal> =
        bills.flatMap { it.items }
            .groupBy { it.itemName.trim() }
            .map { (name, lines) ->
                val rates = lines.map { it.unitPriceRupees }.distinct()
                ItemTotal(
                    name = name,
                    qty = lines.sumOf { it.quantity },
                    amount = lines.sumOf { it.subtotalRupees },
                    rate = rates.singleOrNull()
                )
            }

    private fun categories(bills: List<Bill>, menuCategories: Map<String, Category>): List<CategoryTotal> =
        bills.flatMap { it.items }
            .groupBy { categoryOf(it, menuCategories) }
            .map { (name, lines) -> CategoryTotal(name, lines.sumOf { it.subtotalRupees }) }
            .sortedByDescending { it.amount }

    private fun Bill.localDate(zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
}
