package com.functional.spice.presentation.menu

import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuFormTest {

    private val items = listOf(MenuItem("a", "Cutting chai", category = Category.HOT_TEA, price = 10.0))

    @Test
    fun emptyName() {
        val r = MenuForm.validate(MenuForm(name = "  ", price = "10"), items)!!
        assertEquals("Enter an item name", r.nameError)
        assertNull(r.priceError)
    }

    @Test
    fun zeroPrice() {
        val r = MenuForm.validate(MenuForm(name = "Masala milk", price = "0"), items)!!
        assertEquals("Enter a price of at least ₹1", r.priceError)
    }

    @Test
    fun duplicateNameIgnoresCase() {
        val r = MenuForm.validate(MenuForm(name = "cutting CHAI ", price = "12"), items)!!
        assertEquals("cutting CHAI is already on the menu", r.nameError)
    }

    @Test
    fun editingTheSameItemIsNotADuplicate() {
        assertNull(MenuForm.validate(MenuForm(editingId = "a", name = "Cutting chai", price = "12"), items))
    }

    @Test
    fun valid() {
        assertNull(MenuForm.validate(MenuForm(name = "Masala milk", price = "25"), items))
    }
}
