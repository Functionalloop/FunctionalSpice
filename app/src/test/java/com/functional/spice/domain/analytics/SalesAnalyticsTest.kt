package com.functional.spice.domain.analytics

import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.BillItem
import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** "Summaries match the database, including deleted menu items". */
class SalesAnalyticsTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 11, 13)

    private fun at(day: Int, hour: Int) =
        LocalDate.of(2026, 11, day).atTime(hour, 10).atZone(zone).toInstant().toEpochMilli()

    private var n = 1000
    private fun bill(day: Int, hour: Int, method: PaymentMethod, vararg lines: BillItem): Bill {
        val id = "b${n++}"
        val items = lines.map { it.copy(billId = id) }
        return Bill(id, n, at(day, hour), items, items.sumOf { it.subtotal }, method)
    }

    private fun line(name: String, qty: Int, rate: Int, menuId: String, category: Category?) =
        BillItem("l_${name}_$qty", "", menuId, name, rate.toDouble(), qty, (qty * rate).toDouble(), category?.name)

    /** Only ids still on the menu; "m_old" was deleted and isn't in the lookup. */
    private val menu = mapOf("m_chai" to Category.HOT_TEA, "m_pav" to Category.SNACKS)

    private val bills = listOf(
        bill(13, 8, PaymentMethod.CASH, line("Cutting chai", 2, 10, "m_chai", Category.HOT_TEA), line("Bhaji pav", 1, 40, "m_pav", Category.SNACKS)),
        bill(13, 8, PaymentMethod.UPI, line("Cutting chai", 3, 10, "m_chai", Category.HOT_TEA)),
        // Saved before v2 (no category snapshot) for an item that was later deleted
        bill(13, 20, PaymentMethod.CASH, line("Filter coffee", 2, 20, "m_old", null)),
        // Earlier days this month
        bill(1, 9, PaymentMethod.CASH, line("Bhaji pav", 5, 40, "m_pav", Category.SNACKS)),
        bill(8, 9, PaymentMethod.UPI, line("Cutting chai", 10, 10, "m_chai", Category.HOT_TEA)),
        // Last month: ignored
        Bill("old", 1, LocalDate.of(2026, 10, 31).atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
            listOf(line("Cutting chai", 99, 10, "m_chai", Category.HOT_TEA)), 990.0)
    )

    @Test
    fun today() {
        val t = SalesAnalytics.today(bills, today, zone, menu)
        assertEquals(3, t.bills)
        assertEquals(20 + 40 + 30 + 40, t.total)
        assertEquals(8, t.itemsSold)
        assertEquals(20 + 40 + 40, t.cashSales)
        assertEquals(30, t.upiSales)
        assertEquals(8, t.busiestHour)
        assertEquals(90, t.hourly[8 - SalesAnalytics.FIRST_HOUR])
        // Deleted item still counts, under its snapshot name
        val coffee = t.items.first { it.name == "Filter coffee" }
        assertEquals(2, coffee.qty)
        assertEquals(40, coffee.amount)
        assertEquals(20, coffee.rate)
        // Deleted item with no category snapshot lands in "Other", never dropped
        assertEquals(130, t.categories.sumOf { it.amount })
        assertEquals(40, t.categories.first { it.name == SalesAnalytics.OTHER_CATEGORY }.amount)
        assertEquals(50, t.categories.first { it.name == "Hot tea" }.amount)
        assertEquals(t.total, t.cashSales + t.upiSales)
    }

    @Test
    fun month() {
        val m = SalesAnalytics.month(bills, today, zone, menu)
        assertEquals(5, m.bills)
        assertEquals(130 + 200 + 100, m.total)
        assertEquals(3, m.daysOpen)
        assertEquals(30, m.daily.size)
        assertEquals(200, m.daily[0])
        assertEquals(1, m.bestDay)
        // 430 / 3 days x 30 days = 4300, rounded to the nearest 100
        assertEquals(4300, m.onPace)
        assertEquals(listOf(1 to 7, 8 to 13), m.weeks.map { it.startDay to it.endDay })
        assertEquals(listOf(true, false), m.weeks.map { it.complete })
        assertEquals(200, m.weeks[0].amount)
        assertEquals(230, m.weeks[1].amount)
        assertEquals("Cutting chai", m.topItems.first().name)
        assertEquals(15, m.topItems.first().qty)
        assertEquals(m.total, m.cashSales + m.upiSales)
        assertEquals("Week 2 (8-13)", m.weeks[1].printLabel)
    }
}
