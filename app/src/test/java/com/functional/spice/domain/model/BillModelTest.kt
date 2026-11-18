package com.functional.spice.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BillModelTest {

    @Test
    fun testBillItemCalculations() {
        val item1 = BillItem(
            id = "1", billId = "b1", menuItemId = "m1", itemName = "Special Chai",
            unitPrice = 15.0, quantity = 3, subtotal = 45.0
        )
        val item2 = BillItem(
            id = "2", billId = "b1", menuItemId = "m2", itemName = "Bun Maska",
            unitPrice = 25.0, quantity = 2, subtotal = 50.0
        )
        val bill = Bill(
            id = "b1", billNumber = 1043, items = listOf(item1, item2),
            totalAmount = 95.0, paymentMethod = PaymentMethod.UPI
        )

        assertEquals(5, bill.totalItemCount)
        assertEquals(95, bill.totalRupees)
        assertEquals("#1043", bill.formattedBillNumber)
        assertEquals("Rs. 95.00", bill.formattedTotal)
    }

    @Test
    fun testCartItemSubtotal() {
        val menuItem = MenuItem(id = "m1", name = "Kokum Soda", category = Category.COLD_DRINKS, price = 25.0)
        val cartItem = CartItem(item = menuItem, quantity = 4)
        assertEquals(100.0, cartItem.subtotal, 0.001)
        assertEquals("₹100", cartItem.formattedSubtotal)
    }

    @Test
    fun indianGrouping() {
        assertEquals("₹0", Money.inr(0))
        assertEquals("₹230", Money.inr(230))
        assertEquals("₹9,840", Money.inr(9840))
        assertEquals("₹12,290", Money.inr(12290))
        assertEquals("₹1,52,710", Money.inr(152710))
        assertEquals("₹3,52,400", Money.inr(352400))
        assertEquals("1,22,170", Money.group(122170))
        assertEquals("₹1,00,00,000", Money.inr(10_000_000))
    }
}
