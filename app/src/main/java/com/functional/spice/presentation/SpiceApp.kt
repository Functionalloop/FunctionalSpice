package com.functional.spice.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.functional.spice.core.theme.DeepEspresso
import com.functional.spice.core.theme.MutedStone
import com.functional.spice.core.theme.StoneFaint
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.core.theme.WarmAlabaster
import com.functional.spice.presentation.components.Radius
import com.functional.spice.presentation.bills.BillsScreen
import com.functional.spice.presentation.bills.BillsViewModel
import com.functional.spice.presentation.home.HomeScreen
import com.functional.spice.presentation.home.HomeViewModel
import com.functional.spice.presentation.menu.MenuScreen
import com.functional.spice.presentation.menu.MenuViewModel
import com.functional.spice.presentation.payment.PaymentScreen
import com.functional.spice.presentation.printer.PrinterScreen
import com.functional.spice.presentation.printer.PrinterViewModel

/** Outline icon normally, filled icon on the active tab. */
enum class AppTab(val label: String, val icon: ImageVector, val activeIcon: ImageVector) {
    COUNTER("Counter", Icons.Outlined.GridView, Icons.Filled.GridView),
    BILLS("Bills", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    MENU("Menu", Icons.Outlined.RestaurantMenu, Icons.Filled.RestaurantMenu),
    PRINTER("Printer", Icons.Outlined.Print, Icons.Filled.Print)
}

/** Bottom nav: Counter, Bills, Menu, Printer. Payment opens full screen over the Counter. */
@Composable
fun SpiceApp() {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val billsViewModel: BillsViewModel = hiltViewModel()
    val menuViewModel: MenuViewModel = hiltViewModel()
    val printerViewModel: PrinterViewModel = hiltViewModel()

    var tab by rememberSaveable { mutableStateOf(AppTab.COUNTER) }
    val snackbarHostState = remember { SnackbarHostState() }
    val homeState by homeViewModel.uiState.collectAsState()
    val payment = homeState.payment

    Box(Modifier.fillMaxSize().background(WarmAlabaster)) {
        if (payment != null) {
            PaymentScreen(
                state = payment,
                shop = homeState.shop,
                onBack = homeViewModel::closePayment,
                onMethod = homeViewModel::selectPaymentMethod,
                onKey = homeViewModel::onCashKey,
                onQuickAmount = homeViewModel::onQuickAmount,
                onClear = homeViewModel::clearCash,
                onConfirm = homeViewModel::confirmPayment
            )
        } else {
            Scaffold(
                containerColor = WarmAlabaster,
                bottomBar = { BottomNav(selected = tab, onSelect = { tab = it }) },
                snackbarHost = {
                    SnackbarHost(snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            shape = RoundedCornerShape(Radius.Field),
                            containerColor = DeepEspresso,
                            contentColor = Color.White
                        )
                    }
                }
            ) { padding ->
                // Consume the insets the Scaffold already applied so imePadding() on the Menu and
                // shop-details forms only adds what the keyboard covers beyond the bottom nav.
                val content = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                when (tab) {
                    AppTab.COUNTER -> HomeScreen(
                        viewModel = homeViewModel,
                        snackbarHostState = snackbarHostState,
                        onOpenPrinter = { tab = AppTab.PRINTER },
                        modifier = content
                    )
                    AppTab.BILLS -> BillsScreen(billsViewModel, snackbarHostState, content)
                    AppTab.MENU -> MenuScreen(menuViewModel, snackbarHostState, content)
                    AppTab.PRINTER -> PrinterScreen(printerViewModel, snackbarHostState, content)
                }
            }
        }
    }
}

/** White bar with rounded top corners; ink filled icon + bold label on the active tab. */
@Composable
private fun BottomNav(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = Radius.Hero, topEnd = Radius.Hero))
            .background(Color.White)
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 10.dp)
    ) {
        AppTab.entries.forEach { t ->
            val on = t == selected
            val color = if (on) DeepEspresso else StoneFaint
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onSelect(t) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(if (on) t.activeIcon else t.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Text(
                    t.label,
                    style = SpiceTypography.labelSmall.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (on) DeepEspresso else MutedStone,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}
