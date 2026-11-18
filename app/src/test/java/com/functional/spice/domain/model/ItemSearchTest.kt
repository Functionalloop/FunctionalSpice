package com.functional.spice.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemSearchTest {

    private fun item(name: String, subtitle: String = "") =
        MenuItem(id = name, name = name, konkaniSubtitle = subtitle, category = Category.HOT_TEA, price = 10.0)

    private val menu = listOf(
        item("Special chai"),
        item("Cutting chai"),
        item("Bhaji pav", "भाजी पाव"),
        item("Chai biscuit"),
        item("Ros omelette")
    )

    private fun names(q: String) = ItemSearch.filter(menu, q).map { it.name }

    @Test
    fun blankQueryReturnsEverythingInMenuOrder() {
        assertEquals(menu.map { it.name }, names(""))
        assertEquals(menu.map { it.name }, names("   "))
    }

    @Test
    fun caseInsensitivePartialMatch() {
        assertEquals(listOf("Ros omelette"), names("OMEL"))
        assertEquals(listOf("Bhaji pav"), names("pav"))
    }

    @Test
    fun wordsMatchInAnyOrder() {
        assertEquals(listOf("Cutting chai"), names("chai cut"))
        assertEquals(listOf("Cutting chai"), names("  cut   chai "))
    }

    @Test
    fun namesStartingWithTheQueryComeFirst() {
        // "Chai biscuit" starts with "chai"; the other two keep their menu order after it
        assertEquals(listOf("Chai biscuit", "Special chai", "Cutting chai"), names("chai"))
    }

    @Test
    fun konkaniSubtitleIsSearchedToo() {
        assertTrue(ItemSearch.matches(menu[2], "भाजी"))
        assertFalse(ItemSearch.matches(menu[0], "भाजी"))
    }

    @Test
    fun noMatchGivesEmptyList() {
        assertTrue(names("coffee").isEmpty())
    }
}
