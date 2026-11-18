package com.functional.spice.presentation.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.functional.spice.core.theme.Charcoal
import com.functional.spice.core.theme.DeepEspresso
import com.functional.spice.core.theme.EspressoDivider
import com.functional.spice.core.theme.MonoStyles
import com.functional.spice.core.theme.MutedStone
import com.functional.spice.core.theme.OnEspressoMuted
import com.functional.spice.core.theme.OnEspressoSoft
import com.functional.spice.core.theme.SoftCreamBorder
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.core.theme.StoneBody
import com.functional.spice.core.theme.SuccessDot
import com.functional.spice.core.theme.WarmAlabaster
import com.functional.spice.core.theme.WarmCardamom
import com.functional.spice.core.theme.WarningBg
import com.functional.spice.core.theme.WarningBorder
import com.functional.spice.core.theme.WarningFg
import com.functional.spice.core.theme.WarningIcon
import com.functional.spice.core.theme.WarningOutline
import com.functional.spice.domain.analytics.MonthStats
import com.functional.spice.domain.analytics.SalesAnalytics
import com.functional.spice.domain.analytics.TodayStats
import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.Money
import com.functional.spice.domain.model.SyncStatus
import com.functional.spice.presentation.components.Bars
import com.functional.spice.presentation.components.ButtonKind
import com.functional.spice.presentation.components.LabelValue
import com.functional.spice.presentation.components.ProgressTrack
import com.functional.spice.presentation.components.Radius
import com.functional.spice.presentation.components.RowsCard
import com.functional.spice.presentation.components.rowOf
import com.functional.spice.presentation.components.SectionLabel
import com.functional.spice.presentation.components.SegmentOption
import com.functional.spice.presentation.components.SegmentedControl
import com.functional.spice.presentation.components.SpiceButton
import com.functional.spice.presentation.components.SpiceCard
import com.functional.spice.presentation.home.components.icon
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val ITEMS_PREVIEW = 3
private const val BILLS_PREVIEW = 5
private const val BILLS_MAX = 100

@Composable
fun BillsScreen(viewModel: BillsViewModel, snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.message) {
        state.message?.let {
            viewModel.clearMessage()
            snackbarHostState.showSnackbar(it)
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text("Bills", style = SpiceTypography.headlineSmall, modifier = Modifier.weight(1f))
            val subtitle = when (state.tab) {
                BillsTab.TODAY -> state.today?.date?.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
                BillsTab.MONTH -> state.month?.month?.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
            }
            Text(subtitle ?: "", style = SpiceTypography.bodySmall)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
        ) {
            SegmentedControl(
                options = listOf(SegmentOption(BillsTab.TODAY, "Today"), SegmentOption(BillsTab.MONTH, "This month")),
                selected = state.tab,
                onSelect = viewModel::selectTab,
                modifier = Modifier.fillMaxWidth()
            )
            when (state.tab) {
                BillsTab.TODAY -> state.today?.let { TodayContent(it, state, viewModel) }
                BillsTab.MONTH -> state.month?.let { MonthContent(it, state, viewModel) }
            }
        }
    }

    state.floatPrompt?.let { prompt ->
        OpeningFloatDialog(
            prompt = prompt,
            initial = state.dayClose?.openingFloat,
            onConfirm = viewModel::onFloatEntered,
            onDismiss = viewModel::dismissFloatPrompt
        )
    }
    state.reprintCandidate?.let { bill ->
        AlertDialog(
            onDismissRequest = viewModel::dismissReprint,
            containerColor = WarmAlabaster,
            title = { Text("Print bill #${bill.billNumber} again?", style = SpiceTypography.titleMedium) },
            text = {
                Text(
                    "${bill.formattedTime}, ${bill.totalItemCount} items, ${bill.paymentMethod.label}, ${Money.inr(bill.totalRupees)}",
                    style = SpiceTypography.bodyMedium
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmReprint) { Text("Print again", color = WarmCardamom) } },
            dismissButton = { TextButton(onClick = viewModel::dismissReprint) { Text("Cancel", color = StoneBody) } }
        )
    }
}

// ---- Today ------------------------------------------------------------------------------------

@Composable
private fun TodayContent(t: TodayStats, state: BillsUiState, vm: BillsViewModel) {
    Hero(
        label = "Sales today",
        value = Money.inr(t.total),
        stats = listOf(Money.group(t.bills) to "Bills", Money.group(t.itemsSold) to "Items sold", Money.inr(t.avgBill) to "Avg bill")
    )

    if (state.pendingSync > 0) SyncBanner(state.pendingSync, state.isSyncing, vm::syncNow)

    if (t.bills == 0) {
        Text(
            "No bills yet today. Sales show up here as soon as you save a bill.",
            style = SpiceTypography.bodyMedium,
            color = MutedStone,
            modifier = Modifier.padding(vertical = 20.dp)
        )
    } else {
        SectionLabel("Sales by hour")
        SpiceCard {
            Bars(values = t.hourly, highlight = t.busiestHour?.minus(SalesAnalytics.FIRST_HOUR))
            AxisLabels(listOf("6a", "10a", "2p", "6p", "9p"))
            t.busiestHour?.let {
                Text("Busiest: ${hourLabel(it)} to ${hourLabel(it + 1)}", style = SpiceTypography.bodySmall, color = StoneBody, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }

    SectionLabel("Cash and UPI")
    val float = state.dayClose?.openingFloat
    RowsCard(
        rows = listOf(
            rowOf { LabelValue("Cash sales", Money.inr(t.cashSales)) },
            rowOf { LabelValue("UPI sales", Money.inr(t.upiSales)) },
            rowOf {
                Row(
                    Modifier
                        .weight(1f)
                        .clickable(onClickLabel = "Set opening float", onClick = vm::editOpeningFloat),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cash in drawer ", style = SpiceTypography.bodyMedium)
                    Text(
                        if (float != null) "incl. ${Money.inr(float)} float" else "Set opening float",
                        style = SpiceTypography.labelSmall,
                        color = if (float != null) MutedStone else WarmCardamom
                    )
                }
                Text(Money.inr((float ?: 0) + t.cashSales), style = MonoStyles.PriceSmall)
            }
        )
    )

    if (t.categories.isNotEmpty()) {
        SectionLabel("By category")
        CategoryCard(t.categories.map { it.name to it.amount }, t.total)
    }

    if (t.items.isNotEmpty()) {
        SectionLabel("Item-wise sales")
        val shown = if (state.showAllItems) t.items else t.items.take(ITEMS_PREVIEW)
        RowsCard(
            rows = shown.map { item ->
                rowOf {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = SpiceTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            text = "${item.qty} sold" + (item.rate?.let { " at ${Money.inr(it)}" } ?: ""),
                            style = SpiceTypography.labelSmall
                        )
                    }
                    Text(Money.inr(item.amount), style = MonoStyles.PriceSmall)
                }
            },
            footer = if (t.items.size > ITEMS_PREVIEW) {
                { ShowAllToggle(state.showAllItems, "Show all ${t.items.size} items", vm::toggleShowAllItems) }
            } else null
        )
    }

    if (t.recent.isNotEmpty()) {
        SectionLabel("Recent bills")
        val shown = t.recent.take(if (state.showAllBills) BILLS_MAX else BILLS_PREVIEW)
        RowsCard(
            rows = shown.map { bill ->
                rowOf {
                    Column(
                        Modifier
                            .weight(1f)
                            .clickable(onClickLabel = "Print again") { vm.askReprint(bill) }
                    ) {
                        Text("#${bill.billNumber}", style = MonoStyles.PriceSmall)
                        Text(
                            "${bill.formattedTime}, ${bill.totalItemCount} ${if (bill.totalItemCount == 1) "item" else "items"}, ${bill.paymentMethod.label}" +
                                if (bill.printPending) ", not printed yet" else "",
                            style = SpiceTypography.labelSmall
                        )
                    }
                    Text(Money.inr(bill.totalRupees), style = MonoStyles.PriceSmall.copy(fontWeight = FontWeight.Normal))
                    val synced = bill.syncStatus == SyncStatus.SYNCED
                    Icon(
                        if (synced) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                        contentDescription = if (synced) "Synced" else "Waiting to sync",
                        tint = if (synced) SuccessDot else WarningIcon,
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            footer = if (t.recent.size > BILLS_PREVIEW) {
                { ShowAllToggle(state.showAllBills, "Show all ${t.recent.size} bills", vm::toggleShowAllBills) }
            } else null
        )
    }

    state.dayClose?.closedAt?.let { closedAt ->
        Text(
            "Day closed at " + java.text.SimpleDateFormat("HH:mm", Locale.ENGLISH).format(java.util.Date(closedAt)) +
                ". Closing again updates the record and prints a fresh summary.",
            style = SpiceTypography.labelSmall,
            modifier = Modifier.padding(top = 12.dp)
        )
    }

    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SpiceButton(
            "Print summary", onClick = vm::printDaySummary, kind = ButtonKind.Ghost, icon = Icons.Outlined.Print,
            enabled = !state.isPrinting, modifier = Modifier.weight(1f)
        )
        SpiceButton(
            "Close day", onClick = vm::closeDay, kind = ButtonKind.Primary,
            enabled = !state.isPrinting, modifier = Modifier.weight(1f)
        )
    }
}

// ---- This month -------------------------------------------------------------------------------

@Composable
private fun MonthContent(m: MonthStats, state: BillsUiState, vm: BillsViewModel) {
    val monthName = m.month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    val shortMonth = m.month.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    Hero(
        label = "$monthName so far",
        value = Money.inr(m.total),
        stats = listOf(
            "${m.daysOpen}" to "Days open",
            Money.inr(Math.round(m.dailyAvg)) to "Daily avg",
            Money.group(m.bills) to "Bills"
        ),
        footer = if (m.total > 0) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("On pace for ", style = SpiceTypography.bodySmall, color = OnEspressoSoft)
                    Text(Money.inr(m.onPace), style = MonoStyles.PriceSmall, color = WarmAlabaster)
                    Text(" by month end", style = SpiceTypography.bodySmall, color = OnEspressoSoft)
                }
            }
        } else null
    )

    if (state.pendingSync > 0) SyncBanner(state.pendingSync, state.isSyncing, vm::syncNow)

    SectionLabel("Daily sales")
    SpiceCard {
        Bars(values = m.daily, height = 80.dp, gap = 2.dp, highlight = m.today - 1, futureFrom = m.today)
        AxisLabels(listOf("1", "10", "20", "${m.daily.size}"))
        m.bestDay?.let { day ->
            HorizontalDivider(Modifier.padding(top = 8.dp), thickness = 1.dp, color = SoftCreamBorder)
            Row(Modifier.padding(top = 8.dp)) {
                Text("Best day: $day $shortMonth", style = SpiceTypography.bodySmall, color = StoneBody, modifier = Modifier.weight(1f))
                Text(Money.inr(m.bestDaySales), style = MonoStyles.PriceSmall)
            }
        }
    }

    if (m.weeks.isNotEmpty()) {
        SectionLabel("Week by week")
        RowsCard(
            rows = m.weeks.map { w ->
                rowOf {
                    Column(Modifier.weight(1f)) {
                        Text("Week ${w.index}", style = SpiceTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            "${w.startDay} to ${w.endDay} $shortMonth, ${w.days} ${if (w.days == 1) "day" else "days"}" +
                                if (w.complete) "" else " so far",
                            style = SpiceTypography.labelSmall
                        )
                    }
                    Text(Money.inr(w.amount), style = MonoStyles.PriceSmall)
                }
            }
        )
    }

    if (m.topItems.isNotEmpty()) {
        SectionLabel("Top sellers this month")
        SpiceCard {
            val top = m.topItems.first().qty.coerceAtLeast(1)
            m.topItems.forEachIndexed { i, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", style = MonoStyles.PriceSmall, color = WarmCardamom)
                    Spacer(Modifier.width(8.dp))
                    Text(item.name, style = SpiceTypography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(Money.group(item.qty), style = MonoStyles.PriceSmall)
                }
                ProgressTrack(
                    fraction = item.qty.toFloat() / top,
                    color = DeepEspresso,
                    modifier = Modifier.padding(top = 4.dp, bottom = if (i == m.topItems.lastIndex) 0.dp else 10.dp)
                )
            }
        }
    }

    SectionLabel("By category, cash and UPI")
    RowsCard(
        rows = m.categories.map { c ->
            rowOf {
                Row(Modifier.weight(1f)) {
                    Text(c.name + " ", style = SpiceTypography.bodyMedium)
                    Text("${c.percentOf(m.total)}%", style = SpiceTypography.bodyMedium, color = MutedStone)
                }
                Text(Money.inr(c.amount), style = MonoStyles.PriceSmall)
            }
        } + rowOf { LabelValue("Cash / UPI", "${Money.inr(m.cashSales)} / ${Money.inr(m.upiSales)}") }
    )

    SpiceButton(
        "Print month summary", onClick = vm::printMonthSummary, kind = ButtonKind.Ghost, icon = Icons.Outlined.Print,
        enabled = !state.isPrinting, modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
    )
}

// ---- Pieces -----------------------------------------------------------------------------------

@Composable
private fun Hero(
    label: String,
    value: String,
    stats: List<Pair<String, String>>,
    footer: (@Composable () -> Unit)? = null
) {
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.Hero))
            .background(DeepEspresso)
            .padding(16.dp)
    ) {
        Text(label, style = SpiceTypography.bodySmall, color = OnEspressoMuted)
        Text(value, style = MonoStyles.Hero, color = WarmAlabaster)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            stats.forEach { (v, l) ->
                Column(Modifier.weight(1f)) {
                    Text(v, style = MonoStyles.PriceMedium, color = WarmAlabaster)
                    Text(l, style = SpiceTypography.labelSmall, color = OnEspressoMuted)
                }
            }
        }
        footer?.let {
            HorizontalDivider(Modifier.padding(top = 10.dp), thickness = 1.dp, color = EspressoDivider)
            Spacer(Modifier.height(8.dp))
            it()
        }
    }
}

@Composable
private fun SyncBanner(count: Int, syncing: Boolean, onSync: () -> Unit) {
    val shape = RoundedCornerShape(Radius.Card)
    Row(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(WarningBg)
            .border(1.dp, WarningBorder, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = WarningFg, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "$count ${if (count == 1) "item" else "items"} waiting to sync",
                style = SpiceTypography.labelLarge, color = WarningFg
            )
            Text("Saved on this phone", style = SpiceTypography.bodySmall, color = WarningFg)
        }
        Text(
            if (syncing) "Syncing…" else "Sync now",
            style = SpiceTypography.labelMedium,
            color = WarningFg,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, WarningOutline, RoundedCornerShape(10.dp))
                .clickable(enabled = !syncing, onClick = onSync)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CategoryCard(categories: List<Pair<String, Int>>, total: Int) {
    SpiceCard {
        categories.forEachIndexed { i, (name, amount) ->
            val pct = if (total > 0) Math.round(amount * 100f / total) else 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Category.fromString(name).icon(),
                    contentDescription = null,
                    tint = WarmCardamom,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("$name ", style = SpiceTypography.bodyMedium)
                Text("$pct%", style = SpiceTypography.bodyMedium, color = MutedStone, modifier = Modifier.weight(1f))
                Text(Money.inr(amount), style = MonoStyles.PriceSmall)
            }
            ProgressTrack(
                fraction = pct / 100f,
                modifier = Modifier.padding(top = 4.dp, bottom = if (i == categories.lastIndex) 0.dp else 10.dp)
            )
        }
    }
}

@Composable
private fun AxisLabels(labels: List<String>) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEach { Text(it, style = MonoStyles.Caption) }
    }
}

@Composable
private fun ShowAllToggle(expanded: Boolean, label: String, onToggle: () -> Unit) {
    HorizontalDivider(thickness = 1.dp, color = SoftCreamBorder)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (expanded) "Show less" else label, style = SpiceTypography.labelMedium, color = WarmCardamom)
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = WarmCardamom,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun OpeningFloatDialog(prompt: FloatPrompt, initial: Int?, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WarmAlabaster,
        title = { Text("Opening float", style = SpiceTypography.titleMedium) },
        text = {
            Column {
                Text(
                    if (prompt == FloatPrompt.CLOSE_DAY) "How much cash was in the drawer when you opened today? It's needed to work out cash in drawer."
                    else "Cash in the drawer when you opened today.",
                    style = SpiceTypography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter(Char::isDigit).take(5) },
                    prefix = { Text("₹", style = MonoStyles.PriceMedium) },
                    textStyle = MonoStyles.PriceMedium,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WarmCardamom,
                        unfocusedBorderColor = SoftCreamBorder,
                        cursorColor = WarmCardamom,
                        focusedTextColor = Charcoal,
                        unfocusedTextColor = Charcoal
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull() ?: 0) }) {
                Text(if (prompt == FloatPrompt.CLOSE_DAY) "Close day and print" else "Save float", color = WarmCardamom)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = StoneBody) } }
    )
}

private fun hourLabel(hour: Int): String {
    val h = hour % 24
    val suffix = if (h < 12) "AM" else "PM"
    val twelve = if (h % 12 == 0) 12 else h % 12
    return "$twelve $suffix"
}
