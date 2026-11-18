package com.functional.spice.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.functional.spice.core.theme.MutedStone
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.domain.model.CartItem
import com.functional.spice.domain.model.MenuItem

/** Long-press opens the quantity keypad after 450ms (the design spec), not the system default. */
private class CounterViewConfiguration(base: ViewConfiguration) : ViewConfiguration by base {
    override val longPressTimeoutMillis: Long get() = 450L
}

@Composable
fun MenuGrid(
    items: List<MenuItem>,
    cartItems: Map<String, CartItem>,
    onIncrement: (MenuItem) -> Unit,
    onDecrement: (MenuItem) -> Unit,
    onEditQuantity: (MenuItem) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Nothing to sell in this category. Add items or turn them back on in the Menu tab.",
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, bottom = 6.dp)
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = SpiceTypography.bodyMedium,
                color = MutedStone,
                textAlign = TextAlign.Center
            )
        }
        return
    }
    val base = LocalViewConfiguration.current
    CompositionLocalProvider(LocalViewConfiguration provides CounterViewConfiguration(base)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items, key = { it.id }) { item ->
                MenuItemCard(
                    item = item,
                    quantityInCart = cartItems[item.id]?.quantity ?: 0,
                    onIncrement = { onIncrement(item) },
                    onDecrement = { onDecrement(item) },
                    onEditQuantity = { onEditQuantity(item) }
                )
            }
        }
    }
}
