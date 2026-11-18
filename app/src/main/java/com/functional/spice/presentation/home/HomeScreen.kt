package com.functional.spice.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.domain.model.Category
import com.functional.spice.core.theme.StoneFaint
import com.functional.spice.presentation.components.SearchField
import com.functional.spice.presentation.home.components.CategoryFilterChips
import com.functional.spice.presentation.home.components.CounterHeader
import com.functional.spice.presentation.home.components.MenuGrid
import com.functional.spice.presentation.home.components.QuantityKeypadSheet
import com.functional.spice.presentation.home.components.StickyBottomCheckoutBar

/** Counter (design page 2). */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState,
    onOpenPrinter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            viewModel.clearUserMessage()
            snackbarHostState.showSnackbar(message)
        }
    }

    // imePadding keeps the results and the checkout bar above the keyboard while searching
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        CounterHeader(
            shopName = uiState.shop.shopName,
            printerStatus = uiState.printerStatus,
            printerMode = uiState.printerMode,
            nextBillNumber = uiState.nextBillNumber,
            onPrinterModeChange = viewModel::setPrinterMode,
            onOpenPrinter = onOpenPrinter
        )
        SearchField(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchChange,
            placeholder = "Search items",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 2.dp)
        )
        CategoryFilterChips(
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = viewModel::selectCategory
        )
        MenuGrid(
            items = uiState.filteredMenuItems,
            cartItems = uiState.cartItems,
            onIncrement = viewModel::incrementItem,
            onDecrement = viewModel::decrementItem,
            onEditQuantity = viewModel::openQuantityEditor,
            emptyMessage = if (uiState.searchQuery.isNotBlank()) {
                "No items match \"${uiState.searchQuery.trim()}\"." +
                    if (uiState.selectedCategory != Category.ALL) " Try the All chip." else ""
            } else {
                "Nothing to sell in this category. Add items or turn them back on in the Menu tab."
            },
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Tap to add one. Hold or tap the number to type a quantity.",
            style = SpiceTypography.labelSmall.copy(fontSize = 11.sp),
            color = StoneFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp)
        )
        StickyBottomCheckoutBar(
            totalItemCount = uiState.totalCartCount,
            totalAmountFormatted = uiState.formattedTotalCartAmount,
            onClearCart = viewModel::clearCart,
            onSaveAndPrint = viewModel::checkout
        )
    }

    uiState.quantityEditorItem?.let { item ->
        QuantityKeypadSheet(
            item = item,
            currentQuantity = uiState.quantityOf(item.id),
            onSetQuantity = { qty ->
                viewModel.setQuantity(item, qty)
                viewModel.closeQuantityEditor()
            },
            onDismiss = viewModel::closeQuantityEditor
        )
    }
}
