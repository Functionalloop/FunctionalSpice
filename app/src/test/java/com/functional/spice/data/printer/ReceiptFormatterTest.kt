package com.functional.spice.data.printer

import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.BillItem
import com.functional.spice.domain.model.CategoryTotal
import com.functional.spice.domain.model.DaySummary
import com.functional.spice.domain.model.ItemTotal
import com.functional.spice.domain.model.MonthSummary
import com.functional.spice.domain.model.PaymentMethod
import com.functional.spice.domain.model.ShopConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptFormatterTest {

    private val shop = ShopConfig()
    private val time = SimpleDateFormat("dd-MMM-yy HH:mm", Locale.ENGLISH).parse("13-Nov-26 21:15")!!.time

    private fun line(name: String, qty: Int, rate: Int) =
        BillItem("i_$name", "b", "m_$name", name, rate.toDouble(), qty, (qty * rate).toDouble())

    private val designBill = Bill(
        id = "b",
        billNumber = 1042,
        timestamp = time,
        items = listOf(
            line("Special chai", 2, 15),
            line("Bhaji pav", 3, 40),
            line("Ros omelette", 1, 60),
            line("Cutting chai", 2, 10)
        ),
        totalAmount = 230.0,
        paymentMethod = PaymentMethod.CASH,
        cashReceived = 300,
        changeReturned = 70
    )

    /** Printed width of one markup line, or null for image / QR lines. */
    private fun printedWidth(markupLine: String): Int? {
        if ("<img>" in markupLine || "<qrcode" in markupLine) return null
        val wide = "size='big'" in markupLine || "size='wide'" in markupLine
        val text = markupLine
            .replace(Regex("\\[[LCR]]"), "")
            .replace(Regex("<[^>]*>"), "")
        return if (wide) text.length * 2 else text.length
    }

    private fun assertFits(markup: String) {
        markup.lines().filter { it.isNotEmpty() }.forEach { l ->
            val w = printedWidth(l) ?: return@forEach
            assertTrue("Line is $w chars, over 32: $l", w <= ReceiptFormatter.COLS)
        }
    }

    @Test
    fun customerBillMatchesNewLayout() {
        val out = ReceiptFormatter.customerBill(designBill, shop, footerStripHex = "ABCD", cupLogoHex = "CAFE")
        val expected = listOf(
            "[C]<img>CAFE</img>",
            "[C]<b><font size='big'>LOCAL SPICE</font></b>",
            "[C]<b>T E A   S T A L L</b>",
            "[C]Canacona, Goa",
            "[C]~ night chai, no rush ~",
            "[C]--------------------------------",
            "[L]Bill #1042[R]13 Nov, 9:15 PM",
            "[L]Special chai x2[R]30",
            "[L]Bhaji pav x3[R]120",
            "[L]Ros omelette x1[R]60",
            "[L]Cutting chai x2[R]20",
            "[L]<font size='wide'><b>TOTAL</b></font>[R]<font size='wide'><b>Rs. 230</b></font>",
            "[L]Cash[R]Rs. 300",
            "[L]Change[R]Rs. 70",
            "[C]<font size='tall'><b>Dev borem korum!</b></font>",
            "[C]Thank you, come again",
            "[C]- - - - - - - - - - - - - - - -",
            "[C]<img>ABCD</img>",
            "[C]Built for Deu",
            "[C]Yours next? functionalloop.in",
            "[C]93220 53720"
        )
        val lines = out.lines()
        expected.forEach { assertTrue("Missing: $it", it in lines) }
        // Cup comes first, the footer strip after the thank-you
        assertTrue(lines.indexOf("[C]<img>CAFE</img>") < lines.indexOf("[C]<b><font size='big'>LOCAL SPICE</font></b>"))
        // Removed from the old layout
        listOf("=====", "Qty x Rate", "Items:", "bg-black", "<qrcode", "Paid by").forEach {
            assertFalse("Should be gone: $it", out.contains(it))
        }
        assertFits(out)
    }

    @Test
    fun noCupAndBlankSubtitlePrintNothingExtra() {
        val out = ReceiptFormatter.customerBill(designBill, shop.copy(subtitle = " "), null)
        assertFalse(out.contains("CAFE"))
        assertFalse(out.contains("T E A"))
        assertTrue(out.lines().first() == "[C]<b><font size='big'>LOCAL SPICE</font></b>")
    }

    @Test
    fun letterSpacingFallsBackWhenTooWide() {
        assertEquals("T E A   S T A L L", ReceiptFormatter.letterSpaced("tea stall"))
        assertEquals("CHAI AND SNACKS CORNER", ReceiptFormatter.letterSpaced("Chai and snacks corner"))
    }

    @Test
    fun bigTotalSwitchesFromWideToTall() {
        val bill = designBill.copy(totalAmount = 1_234_567.0)
        val out = ReceiptFormatter.customerBill(bill, shop, null)
        assertTrue("[L]<font size='tall'><b>TOTAL</b></font>[R]<font size='tall'><b>Rs. 1234567</b></font>" in out.lines())
        assertFits(out)
    }

    @Test
    fun upiBillSaysPaidViaUpiAndFallsBackToTextWordmark() {
        val out = ReceiptFormatter.customerBill(
            designBill.copy(paymentMethod = PaymentMethod.UPI, cashReceived = null, changeReturned = null), shop, null
        )
        val lines = out.lines()
        assertTrue("[C]Paid via UPI" in lines)
        assertFalse(out.contains("[L]Cash["))
        assertFalse(out.contains("Change"))
        assertTrue("[C]<b>Functional</b>" in lines)
        assertFalse(out.contains("<img>"))
        assertFits(out)
    }

    @Test
    fun oldCashBillWithoutAmountsSaysPaidInCash() {
        val out = ReceiptFormatter.customerBill(designBill.copy(cashReceived = null, changeReturned = null), shop, null)
        assertTrue("[C]Paid in cash" in out.lines())
        assertFalse(out.contains("Change"))
    }

    @Test
    fun greetingFollowsTheClock() {
        assertEquals("~ fresh morning chai ~", ReceiptFormatter.greeting(7))
        assertEquals("~ afternoon chai break ~", ReceiptFormatter.greeting(13))
        assertEquals("~ evening chai & snacks ~", ReceiptFormatter.greeting(18))
        assertEquals("~ night chai, no rush ~", ReceiptFormatter.greeting(23))
        assertEquals("~ late night chai club ~", ReceiptFormatter.greeting(2))
        (0..23).forEach { assertTrue(ReceiptFormatter.greeting(it).length <= ReceiptFormatter.COLS) }
    }

    @Test
    fun longNamesAndBigNumbersWrapWithin32Columns() {
        val bill = designBill.copy(
            items = listOf(
                line("Ginger lemon tea", 2, 20),                 // fits on one line
                line("Extra large masala milk shake", 999, 9999)  // too long: name on its own line
            ),
            totalAmount = 40.0 + 999 * 9999,
            cashReceived = 9_999_999,
            changeReturned = 9_999_999 - 9_989_041
        )
        val out = ReceiptFormatter.customerBill(bill, shop, null)
        val lines = out.lines()
        assertTrue("[L]Ginger lemon tea x2[R]40" in lines)
        assertTrue("[L]Extra large masala milk shake" in lines)
        assertTrue("[L]  x999[R]9989001" in lines)
        assertFits(out)
    }

    @Test
    fun longShopDetailsStillFit() {
        val longShop = shop.copy(
            displayName = "Local Spice Tea Stall, Canacona",
            supportPhone = "+91 93220 53720",
            printName = "LOCAL SPICE CANACONA"
        )
        val out = ReceiptFormatter.customerBill(designBill, longShop, null)
        val lines = out.lines()
        assertTrue("[C]<b><font size='tall'>LOCAL SPICE CANACONA</font></b>" in lines)
        assertTrue("[C]Built for Local Spice Tea Stall," in lines)
        assertTrue("[C]Canacona" in lines)
        assertTrue("[C]+91 93220 53720" in lines)
        assertFits(out)
    }

    @Test
    fun daySummaryFitsAndMatchesDesign() {
        val d = DaySummary(
            date = time, printedAt = time, bills = 318, firstBillNo = 724, lastBillNo = 1041,
            items = listOf(
                ItemTotal("Bhaji pav", 64, 2560),
                ItemTotal("Cutting chai", 214, 2140),
                ItemTotal("Ginger lemon tea", 31, 620)
            ),
            categories = listOf(CategoryTotal("Hot tea", 5800), CategoryTotal("Snacks", 5125), CategoryTotal("Cold drinks", 1365)),
            cashSales = 9840, upiSales = 2450, openingFloat = 500
        )
        val out = ReceiptFormatter.daySummary(d, shop)
        val lines = out.lines()
        listOf(
            "[C]<b>DAY SUMMARY</b>",
            "[L]Date[R]13-Nov-26",
            "[L]Bill range[R]#0724-#1041",
            "[L]<b>Item</b>[R]<b>  Qty        Amt</b>",
            "[L]Bhaji pav[R]   64    2560.00",
            "[L]Ginger lemon tea",
            "[R]   31     620.00",
            "[L]Opening float[R]Rs. 500.00",
            "[L]<b>Cash in drawer</b>[R]<b>Rs. 10340.00</b>",
            "[L]<font size='tall' color='bg-black'><b> TOTAL             Rs. 12290.00 </b></font>",
            "[C]Billing by Functional",
            "[C]Need help? 93220 53720"
        ).forEach { assertTrue("Missing: $it", it in lines) }
        assertFalse("Summary slips have no QR", out.contains("<qrcode"))
        assertFits(out)
    }

    @Test
    fun monthSummaryFits() {
        val m = MonthSummary(
            month = time, daysOpen = 13, bills = 3968, bestDay = time, bestDaySales = 14120,
            weeks = listOf("Week 1 (1-7)" to 81800, "Week 2 (8-13)" to 70910),
            topItems = listOf(ItemTotal("Cutting chai", 2716, 27160)),
            categories = listOf(CategoryTotal("Hot tea", 71800)),
            cashSales = 122170, upiSales = 30540
        )
        val out = ReceiptFormatter.monthSummary(m, shop)
        assertTrue("[L]Daily avg[R]Rs. 11746.92" in out.lines())
        assertTrue("[L]<font size='tall' color='bg-black'><b> TOTAL            Rs. 152710.00 </b></font>" in out.lines())
        assertFalse(out.contains("<qrcode"))
        assertFits(out)
    }
}
