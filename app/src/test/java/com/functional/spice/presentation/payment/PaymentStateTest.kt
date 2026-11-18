package com.functional.spice.presentation.payment

import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.BillItem
import com.functional.spice.domain.model.PaymentMethod
import com.functional.spice.domain.payment.ChangeCalculator
import com.functional.spice.domain.payment.UpiLink
import com.functional.spice.presentation.components.KEY_BACKSPACE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Payment shows correct results for 230/300, 230/230, 230/200 and 95/500". */
class PaymentStateTest {

    private fun state(total: Int, received: Int? = null): PaymentState {
        val bill = Bill(
            id = "b", billNumber = 1042,
            items = listOf(BillItem("i", "b", "m", "Chai", total.toDouble(), 1, total.toDouble())),
            totalAmount = total.toDouble()
        )
        return PaymentState(draft = bill, cashInput = received?.toString() ?: "")
    }

    @Test
    fun total230_received300_returns70_as50and20() {
        val s = state(230, 300)
        assertEquals(CashResult.ReturnChange(70, listOf(50 to 1, 20 to 1)), s.cashResult)
        assertNull(s.validate())
        val bill = s.finalBill(now = 1L)
        assertEquals(300, bill.cashReceived)
        assertEquals(70, bill.changeReturned)
        assertEquals(PaymentMethod.CASH, bill.paymentMethod)
        assertTrue(bill.printPending)
    }

    @Test
    fun billsUnderPrintMinimumAreSavedWithoutPrinting() {
        val small = state(40, 50).copy(minPrintAmount = 100)
        assertEquals(false, small.willPrint)
        assertEquals("Confirm (no print)", small.confirmLabel)
        assertEquals(false, small.finalBill(now = 1L).printPending)
        assertEquals("Mark paid (no print)", small.copy(method = PaymentMethod.UPI).confirmLabel)

        val atMark = state(100, 100).copy(minPrintAmount = 100)
        assertTrue(atMark.willPrint)
        assertEquals("Confirm and print", atMark.confirmLabel)
        assertTrue(atMark.finalBill(now = 1L).printPending)

        // 0 (the default) prints every bill
        assertTrue(state(10, 10).finalBill(now = 1L).printPending)
    }

    @Test
    fun total230_received230_isExact() {
        val s = state(230, 230)
        assertEquals(CashResult.Exact, s.cashResult)
        assertNull(s.validate())
        assertEquals(0, s.finalBill(1L).changeReturned)
    }

    @Test
    fun total230_received200_stillToCollect30() {
        val s = state(230, 200)
        assertEquals(CashResult.StillToCollect(30), s.cashResult)
        assertEquals("Collect ₹30 more first", s.validate())
    }

    @Test
    fun total95_received500_returns405_as200x2and5() {
        val s = state(95, 500)
        assertEquals(CashResult.ReturnChange(405, listOf(200 to 2, 5 to 1)), s.cashResult)
        assertNull(s.validate())
    }

    @Test
    fun nothingEntered() {
        val s = state(230)
        assertEquals(CashResult.Empty, s.cashResult)
        assertEquals("Enter the cash received first", s.validate())
    }

    @Test
    fun upiNeedsNoCash() {
        val s = state(230).copy(method = PaymentMethod.UPI)
        assertNull(s.validate())
        val bill = s.finalBill(1L)
        assertEquals(PaymentMethod.UPI, bill.paymentMethod)
        assertNull(bill.cashReceived)
        assertNull(bill.changeReturned)
    }

    @Test
    fun keypadLimitsToFiveDigitsAndHandles00() {
        var s = state(230)
        listOf("0", "3", "00").forEach { s = s.withKey(it) }
        assertEquals("300", s.cashInput)
        listOf("9", "9", "9").forEach { s = s.withKey(it) }
        assertEquals("30099", s.cashInput)
        s = s.withKey(KEY_BACKSPACE)
        assertEquals("3009", s.cashInput)
    }

    @Test
    fun quickAmounts() {
        assertEquals(listOf(230, 250, 300, 500), ChangeCalculator.suggestions(230))
        assertEquals(listOf(300, 350, 400, 500), ChangeCalculator.suggestions(300))
        assertEquals(listOf(95, 100, 500, 1000), ChangeCalculator.suggestions(95))
    }

    @Test
    fun upiLinkMatchesSpec() {
        assertEquals(
            "upi://pay?pa=deusteastall@upi&pn=Deus%20Tea%20Stall&am=230.00&cu=INR&tn=Bill%201042",
            UpiLink.build("deusteastall@upi", "Deu's Tea Stall", 230, 1042)
        )
    }
}
