package com.functional.spice.presentation.home

import com.functional.spice.domain.model.CartItem
import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.MenuItem
import com.functional.spice.domain.model.Money
import com.functional.spice.domain.model.PrinterMode
import com.functional.spice.domain.model.PrinterStatus
import com.functional.spice.domain.model.ShopConfig
import com.functional.spice.presentation.payment.PaymentState

data class HomeUiState(
    val shop: ShopConfig = ShopConfig(),
    /** Available (not sold out) items only. */
    val menuItems: List<MenuItem> = emptyList(),
    val filteredMenuItems: List<MenuItem> = emptyList(),
    val selectedCategory: Category = Category.ALL,
    /** Counter search box; filters across all categories (combined with the selected chip). */
    val searchQuery: String = "",
    val cartItems: Map<String, CartItem> = emptyMap(),
    val printerStatus: PrinterStatus = PrinterStatus.Disconnected,
    val printerMode: PrinterMode = PrinterMode.BLUETOOTH,
    val nextBillNumber: Int? = null,
    /** Item whose quantity keypad sheet is open. */
    val quantityEditorItem: MenuItem? = null,
    /** Non-null while the Payment screen is open. */
    val payment: PaymentState? = null,
    val userMessage: String? = null
) {
    val totalCartCount: Int
        get() = cartItems.values.sumOf { it.quantity }

    val totalCartRupees: Int
        get() = cartItems.values.sumOf { it.subtotalRupees }

    val formattedTotalCartAmount: String
        get() = Money.inr(totalCartRupees)

    fun quantityOf(itemId: String): Int = cartItems[itemId]?.quantity ?: 0
}
