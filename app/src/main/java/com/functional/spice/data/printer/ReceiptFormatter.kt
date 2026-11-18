package com.functional.spice.data.printer

import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.BillItem
import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.DaySummary
import com.functional.spice.domain.model.MonthSummary
import com.functional.spice.domain.model.PaymentMethod
import com.functional.spice.domain.model.ShopConfig
import com.functional.spice.domain.model.SyncStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Builds DantSu ESC/POS markup for the Posiflow KP206 (58mm):
 * EscPosPrinter(connection, 203, 48f, 32). Every line fits 32 characters.
 * "Rs." on paper: the printer has no ₹ glyph.
 */
object ReceiptFormatter {
    const val COLS = 32
    private val en = Locale.ENGLISH

    private fun fmt(pattern: String, time: Long) = SimpleDateFormat(pattern, en).format(Date(time))
    private fun clean(s: String) = s.replace(Regex("[<>\\[\\]]"), "").trim().take(COLS)
    private fun amt(r: Int) = "$r.00"
    private fun amt(d: Double) = String.format(Locale.US, "%.2f", d)
    private fun rs(r: Int) = "Rs. ${amt(r)}"
    private fun rule(c: Char) = "[C]${c.toString().repeat(COLS)}\n"
    private fun lr(left: String, right: String) = "[L]$left[R]$right\n"

    /**
     * Long names get their own line; the numbers go on the next line.
     * Also wraps whenever name + numbers would not fit 32 columns with a space between.
     */
    private fun item(name: String, right: String, maxName: Int) =
        if (name.length > maxName || name.length + 1 + right.length > COLS) "[L]$name\n[R]$right\n"
        else lr(name, right)

    /** Inverted TOTAL bar, padded by hand to exactly 32 chars. */
    private fun totalBar(total: Int): String {
        val left = " TOTAL"
        val right = "${rs(total)} "
        val gap = (COLS - left.length - right.length).coerceAtLeast(1)
        return "[L]<font size='tall' color='bg-black'><b>$left${" ".repeat(gap)}$right</b></font>\n"
    }

    private fun header(shop: ShopConfig): String {
        val name = clean(shop.printName)
        val size = if (name.length <= 16) "big" else "tall"
        return "[C]<b><font size='$size'>$name</font></b>\n" +
            "[C]${clean(shop.address)}\n" + rule('=')
    }

    private fun summaryFooter(shop: ShopConfig) =
        "[C]- - - - - - - - - - - - - - - -\n[C]Billing by Functional\n" +
            "[C]${clean("Need help? ${shop.supportPhone}")}\n[L]\n[L]\n"

    /** Rupees without ".00" for whole amounts: 230, 12.50. */
    private fun rsShort(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.2f", v)

    /** Tagline under the header; changes with the hour the bill was made. Each fits 32 columns. */
    fun greeting(hour: Int): String = when (hour) {
        in 5..10 -> "~ fresh morning chai ~"
        in 11..15 -> "~ afternoon chai break ~"
        in 16..19 -> "~ evening chai & snacks ~"
        in 20..23 -> "~ night chai, no rush ~"
        else -> "~ late night chai club ~" // 0..4
    }

    /** "TEA STALL" -> "T E A   S T A L L"; plain text when spacing would pass 32 columns. */
    fun letterSpaced(text: String): String {
        val t = clean(text).uppercase(en)
        val spaced = t.toList().joinToString(" ")
        return if (spaced.length <= COLS) spaced else t
    }

    /**
     * TOTAL row. 'wide' doubles every character's width, so it is used only while
     * "TOTAL" + amount + a 2-column gap fit 32 columns; bigger totals use 'tall'.
     */
    private fun totalRow(total: String): String {
        val amount = "Rs. $total"
        val size = if (2 * ("TOTAL".length + amount.length) + 2 <= COLS) "wide" else "tall"
        return "[L]<font size='$size'><b>TOTAL</b></font>[R]<font size='$size'><b>$amount</b></font>\n"
    }

    /** Centred text; splits at the last space that fits when it is wider than 32 columns. */
    private fun centred(text: String): String {
        val t = text.replace(Regex("[<>\\[\\]]"), "").trim()
        if (t.length <= COLS) return "[C]$t\n"
        val cut = t.lastIndexOf(' ', COLS).takeIf { it > 0 } ?: COLS
        return "[C]${t.substring(0, cut).trim()}\n" + centred(t.substring(cut))
    }

    /**
     * Customer bill (v2.2 layout): cup logo, shop name, letter-spaced subtitle, address, a tagline
     * that follows the clock, items, TOTAL, payment, "Dev borem korum!" and the Functional footer.
     * Item lines are "name xQty ... amount"; a line that would not fit 32 columns puts the name
     * on its own line and "  xQty ... amount" below it.
     *
     * @param footerStripHex from FooterStripRenderer.hex(); null prints the text "Functional" instead.
     * @param cupLogoHex from CupLogoRenderer.hex(); null skips the cup.
     */
    fun customerBill(
        bill: Bill,
        shop: ShopConfig,
        footerStripHex: String?,
        cupLogoHex: String? = null
    ): String = buildString {
        val cal = Calendar.getInstance().apply { timeInMillis = bill.timestamp }
        if (cupLogoHex != null) append("[C]<img>$cupLogoHex</img>\n")
        val name = clean(shop.printName)
        val size = if (name.length <= 16) "big" else "tall"
        append("[C]<b><font size='$size'>$name</font></b>\n")
        if (shop.subtitle.isNotBlank()) append("[C]<b>${letterSpaced(shop.subtitle)}</b>\n")
        append("[C]${clean(shop.address)}\n")
        append("[L]\n")
        append("[C]${greeting(cal.get(Calendar.HOUR_OF_DAY))}\n")
        append(rule('-'))
        append(lr("Bill #${bill.billNumber}", fmt("d MMM, h:mm a", bill.timestamp)))
        append(rule('-'))
        bill.items.forEach { l ->
            val itemName = clean(l.itemName)
            val left = "$itemName x${l.quantity}"
            val amount = rsShort(l.subtotal)
            if (left.length + amount.length + 1 <= COLS) {
                append(lr(left, amount))
            } else {
                append("[L]$itemName\n")
                append(lr("  x${l.quantity}", amount))
            }
        }
        append(rule('-'))
        append(totalRow(rsShort(bill.totalAmount)))
        append(rule('-'))
        val cash = bill.cashReceived
        when {
            bill.paymentMethod == PaymentMethod.UPI -> append("[C]Paid via UPI\n")
            cash == null -> append("[C]Paid in cash\n") // bills saved before v2 have no cash amount
            else -> {
                val change = bill.changeReturned ?: (cash - bill.totalRupees).coerceAtLeast(0)
                append(lr("Cash", "Rs. $cash"))
                append(lr("Change", "Rs. $change"))
            }
        }
        append("[L]\n")
        append("[C]<font size='tall'><b>Dev borem korum!</b></font>\n")
        append("[C]Thank you, come again\n")
        append("[L]\n")
        append("[C]- - - - - - - - - - - - - - - -\n")
        if (footerStripHex != null) append("[C]<img>$footerStripHex</img>\n") else append("[C]<b>Functional</b>\n")
        append(centred("Built for ${shop.displayName}"))
        append("[C]Yours next? functionalloop.in\n")
        append(centred(shop.supportPhone))
        append("[L]\n[L]\n")
    }

    fun daySummary(d: DaySummary, shop: ShopConfig): String = buildString {
        append(header(shop))
        append("[C]<b>DAY SUMMARY</b>\n[L]\n")
        append(lr("Date", fmt("dd-MMM-yy", d.date)))
        append(lr("Printed", fmt("HH:mm", d.printedAt)))
        append(lr("Bills", "${d.bills}"))
        append(lr("Items sold", "${d.itemsSold}"))
        append(lr("Avg bill", "Rs. " + amt(if (d.bills > 0) d.total.toDouble() / d.bills else 0.0)))
        append(lr("Bill range", String.format(en, "#%04d-#%04d", d.firstBillNo, d.lastBillNo)))
        append(rule('-'))
        append(lr("<b>Item</b>", "<b>${"Qty".padStart(5)}${"Amt".padStart(11)}</b>"))
        d.items.sortedByDescending { it.amount }.forEach {
            append(item(clean(it.name), "${it.qty}".padStart(5) + amt(it.amount).padStart(11), 15))
        }
        append(rule('-'))
        d.categories.forEach { (name, a) -> append(lr(clean(name).take(16), rs(a))) }
        append(rule('-'))
        append(lr("Cash sales", rs(d.cashSales)))
        append(lr("UPI sales", rs(d.upiSales)))
        append(lr("Opening float", rs(d.openingFloat)))
        append(lr("<b>Cash in drawer</b>", "<b>${rs(d.openingFloat + d.cashSales)}</b>"))
        append(totalBar(d.total))
        append(summaryFooter(shop))
    }

    fun monthSummary(m: MonthSummary, shop: ShopConfig): String = buildString {
        append(header(shop))
        append("[C]<b>MONTH SUMMARY</b>\n[L]\n")
        append(lr("Month", fmt("MMM-yy", m.month)))
        append(lr("Days open", "${m.daysOpen}"))
        append(lr("Bills", "${m.bills}"))
        append(lr("Daily avg", "Rs. " + amt(if (m.daysOpen > 0) m.total.toDouble() / m.daysOpen else 0.0)))
        append(lr("Best day", fmt("dd-MMM", m.bestDay)))
        append(lr("Best day sales", rs(m.bestDaySales)))
        append(rule('-'))
        m.weeks.forEach { (label, a) -> append(lr(clean(label), rs(a))) }
        append(rule('-'))
        append(lr("<b>Top items</b>", "<b>Qty</b>"))
        m.topItems.take(8).forEach { append(lr(clean(it.name).take(24), "${it.qty}")) }
        append(rule('-'))
        m.categories.forEach { (name, a) -> append(lr(clean(name).take(16), rs(a))) }
        append(rule('-'))
        append(lr("Cash sales", rs(m.cashSales)))
        append(lr("UPI sales", rs(m.upiSales)))
        append(totalBar(m.total))
        append(summaryFooter(shop))
    }

    /** Sample bill from the design (page 6), used for "Print test slip". */
    fun createSampleBill(): Bill {
        fun line(n: Int, name: String, qty: Int, rate: Int, cat: Category) = BillItem(
            id = "sample_$n",
            billId = "sample_bill",
            menuItemId = "sample_item_$n",
            itemName = name,
            unitPrice = rate.toDouble(),
            quantity = qty,
            subtotal = (qty * rate).toDouble(),
            category = cat.name
        )
        val items = listOf(
            line(1, "Special chai", 2, 15, Category.HOT_TEA),
            line(2, "Bhaji pav", 3, 40, Category.SNACKS),
            line(3, "Ros omelette", 1, 60, Category.SNACKS),
            line(4, "Cutting chai", 2, 10, Category.HOT_TEA)
        )
        return Bill(
            id = "test_sample_1042",
            billNumber = 1042,
            timestamp = System.currentTimeMillis(),
            items = items,
            totalAmount = items.sumOf { it.subtotal },
            paymentMethod = PaymentMethod.CASH,
            syncStatus = SyncStatus.SYNCED,
            cashReceived = 300,
            changeReturned = 70
        )
    }
}
