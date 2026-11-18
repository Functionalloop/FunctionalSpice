package com.functional.spice.presentation.printer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.functional.spice.core.theme.Charcoal
import com.functional.spice.core.theme.DeepEspresso
import com.functional.spice.core.theme.MonoStyles
import com.functional.spice.core.theme.MutedStone
import com.functional.spice.core.theme.OfflineBg
import com.functional.spice.core.theme.OfflineFg
import com.functional.spice.core.theme.SoftCreamBorder
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.core.theme.StoneFaint
import com.functional.spice.core.theme.SuccessBg
import com.functional.spice.core.theme.SuccessBorder
import com.functional.spice.core.theme.SuccessDot
import com.functional.spice.core.theme.SuccessFg
import com.functional.spice.core.theme.WarmAlabaster
import com.functional.spice.core.theme.WarmCardamom
import com.functional.spice.core.theme.WarningBg
import com.functional.spice.core.theme.WarningBorder
import com.functional.spice.core.theme.WarningFg
import com.functional.spice.core.theme.WarningOutline
import com.functional.spice.domain.model.PrinterMode
import com.functional.spice.domain.model.PrinterStatus
import com.functional.spice.domain.model.ShopConfig
import com.functional.spice.presentation.components.ButtonKind
import com.functional.spice.presentation.components.Radius
import com.functional.spice.presentation.components.RowsCard
import com.functional.spice.presentation.components.SectionLabel
import com.functional.spice.presentation.components.SegmentOption
import com.functional.spice.presentation.components.SegmentedControl
import com.functional.spice.presentation.components.SpiceButton
import com.functional.spice.presentation.components.SpiceCard
import com.functional.spice.presentation.components.SpiceSwitch
import com.functional.spice.presentation.components.rowOf
import com.functional.spice.presentation.menu.SpiceTextField

private fun bluetoothPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
    }

private fun hasBluetoothPermissions(context: Context) = bluetoothPermissions().all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun PrinterScreen(viewModel: PrinterViewModel, snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            viewModel.clearMessage()
            snackbarHostState.showSnackbar(it)
        }
    }

    // Android 12+ runtime Bluetooth permissions (Android 15 / One UI 7)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.refreshDevices()
        if (result.values.all { it }) viewModel.connect()
    }
    val connect: () -> Unit = {
        if (state.mode == PrinterMode.BLUETOOTH && !hasBluetoothPermissions(context)) {
            permissionLauncher.launch(bluetoothPermissions())
        } else {
            viewModel.connect()
        }
    }

    Column(modifier.fillMaxSize()) {
        Text(
            "Printer",
            style = SpiceTypography.headlineSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
        ) {
            SegmentedControl(
                options = listOf(
                    SegmentOption(PrinterMode.BLUETOOTH, "Bluetooth", Icons.Outlined.Bluetooth),
                    SegmentOption(PrinterMode.USB, "USB cable", Icons.Outlined.Usb)
                ),
                selected = state.mode,
                onSelect = viewModel::setMode,
                modifier = Modifier.fillMaxWidth()
            )

            StatusCard(state.status, state.mode, onAllow = connect)

            if (state.printQueueCount > 0) PrintQueueBanner(state.printQueueCount, viewModel::skipPrintQueue)

            if (state.mode == PrinterMode.BLUETOOTH) {
                SectionLabel("Paired devices")
                when {
                    !hasBluetoothPermissions(context) -> InfoText("Allow Bluetooth to see your paired printers. Tap Connect below.")
                    state.devices.isEmpty() -> InfoText(
                        "No paired devices. Pair the KP206 in Android Bluetooth settings first (PIN 0000 or 1234), then come back."
                    )
                    else -> RowsCard(rows = state.devices.map { d ->
                        rowOf {
                            val connectedHere = (state.status as? PrinterStatus.Connected)?.address == d.address
                            val chosen = connectedHere || state.selectedAddress == d.address
                            Row(
                                Modifier
                                    .weight(1f)
                                    .clickable(enabled = d.isPrinter && !state.isBusy) { viewModel.selectDevice(d) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (d.isPrinter) Icons.Outlined.Print else Icons.Outlined.Bluetooth,
                                    contentDescription = null,
                                    tint = if (d.isPrinter) WarmCardamom else StoneFaint,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    d.name,
                                    style = SpiceTypography.bodyMedium,
                                    color = if (d.isPrinter) Charcoal else MutedStone,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp)
                                )
                            }
                            when {
                                !d.isPrinter -> Text("Not a printer", style = SpiceTypography.labelSmall, color = StoneFaint)
                                chosen -> Icon(Icons.Outlined.CheckCircle, contentDescription = "Selected", tint = SuccessDot, modifier = Modifier.size(18.dp))
                                else -> Text("Use", style = SpiceTypography.labelMedium, color = WarmCardamom)
                            }
                        }
                    })
                }
            } else {
                SectionLabel("USB cable")
                InfoText("Connect the KP206 with a USB OTG cable, then tap Connect. Allow USB access when Android asks.")
            }

            SectionLabel("Paper")
            SpiceCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp, horizontal = 4.dp)) {
                Row {
                    listOf("58mm" to "width", "203" to "dpi", "32" to "chars/line").forEach { (v, l) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(v, style = MonoStyles.PriceMedium)
                            Text(l, style = SpiceTypography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            SpiceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reconnect automatically", style = SpiceTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        Text("Retries every 5 seconds if the link drops", style = SpiceTypography.labelSmall)
                    }
                    SpiceSwitch(checked = state.autoReconnect, onCheckedChange = viewModel::setAutoReconnect)
                }
            }

            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpiceButton(
                    "Print test slip", onClick = viewModel::printTestSlip, kind = ButtonKind.Dark,
                    enabled = !state.isBusy, modifier = Modifier.weight(1f)
                )
                if (state.status.isConnected) {
                    SpiceButton("Disconnect", onClick = viewModel::disconnect, kind = ButtonKind.Ghost, enabled = !state.isBusy, modifier = Modifier.weight(1f))
                } else {
                    SpiceButton("Connect", onClick = connect, kind = ButtonKind.Ghost, enabled = !state.isBusy, modifier = Modifier.weight(1f))
                }
            }

            ShopDetails(state, viewModel)
        }
    }
}

@Composable
private fun StatusCard(status: PrinterStatus, mode: PrinterMode, onAllow: () -> Unit) {
    val (bg, border, fg) = when (status) {
        is PrinterStatus.Connected -> Triple(SuccessBg, SuccessBorder, SuccessFg)
        PrinterStatus.Disconnected -> Triple(OfflineBg, SoftCreamBorder, OfflineFg)
        else -> Triple(WarningBg, WarningBorder, WarningFg)
    }
    val (title, line) = when (status) {
        is PrinterStatus.Connected -> status.deviceName to "Connected, ready to print"
        PrinterStatus.Connecting -> "Posiflow KP206" to "Connecting…"
        PrinterStatus.PermissionRequired -> "Posiflow KP206" to "Allow Bluetooth so the app can reach the printer"
        is PrinterStatus.Error -> "Posiflow KP206" to status.message
        PrinterStatus.Disconnected -> "Posiflow KP206" to
            if (mode == PrinterMode.BLUETOOTH) "Not connected. Check it's on and paired." else "Not connected. Check the USB cable."
    }
    val shape = RoundedCornerShape(Radius.Card)
    Row(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DeepEspresso),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Print, contentDescription = null, tint = WarmAlabaster, modifier = Modifier.size(22.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(title, style = SpiceTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp))
            Text(line, style = SpiceTypography.bodySmall, color = fg)
            (status as? PrinterStatus.Connected)?.address?.let { Text(it, style = MonoStyles.Caption) }
        }
        if (status is PrinterStatus.PermissionRequired) {
            Text(
                "Allow",
                style = SpiceTypography.labelMedium,
                color = WarningFg,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, WarningOutline, RoundedCornerShape(10.dp))
                    .clickable(onClick = onAllow)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun PrintQueueBanner(count: Int, onSkip: () -> Unit) {
    val shape = RoundedCornerShape(Radius.Card)
    Row(
        Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(WarningBg)
            .border(1.dp, WarningBorder, shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("$count ${if (count == 1) "bill" else "bills"} waiting to print", style = SpiceTypography.labelLarge, color = WarningFg)
            Text("They print as soon as the printer connects", style = SpiceTypography.bodySmall, color = WarningFg)
        }
        Text(
            "Skip",
            style = SpiceTypography.labelMedium,
            color = WarningFg,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, WarningOutline, RoundedCornerShape(10.dp))
                .clickable(onClick = onSkip)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun InfoText(text: String) {
    SpiceCard { Text(text, style = SpiceTypography.bodySmall) }
}

@Composable
private fun ShopDetails(state: PrinterUiState, vm: PrinterViewModel) {
    SectionLabel("Shop details on bills")
    val draft = state.shopDraft
    if (draft == null) {
        val s = state.shop
        RowsCard(
            rows = listOf(
                rowOf { DetailRow("Shop name", s.shopName) },
                rowOf { DetailRow("Name on bill", s.printName) },
                rowOf { DetailRow("Line under name", s.subtitle.ifBlank { "(none)" }) },
                rowOf { DetailRow("Bill footer", "Built for ${s.displayName}") },
                rowOf { DetailRow("Address line", s.address) },
                rowOf { DetailRow("UPI ID", s.upiId) },
                rowOf {
                    DetailRow(
                        "Print bills from",
                        if (s.minPrintAmount <= 0) "Every bill" else "₹${s.minPrintAmount} and above"
                    )
                },
                rowOf { DetailRow("Functional phone", s.supportPhone) }
            ),
            footer = {
                SpiceButton(
                    "Edit shop details", onClick = vm::editShop, kind = ButtonKind.Ghost, icon = Icons.Outlined.Edit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )
            }
        )
    } else {
        SpiceCard {
            ShopField("Shop name (on screen)", draft.shopName) { v -> vm.updateShopDraft { it.copy(shopName = v.take(40)) } }
            ShopField("Name on bill (16 characters print large)", draft.printName) { v ->
                vm.updateShopDraft { it.copy(printName = v.uppercase().take(32)) }
            }
            ShopField("Line under name (e.g. TEA STALL)", draft.subtitle) { v ->
                vm.updateShopDraft { it.copy(subtitle = v.uppercase().take(16)) }
            }
            ShopField("Short name for \"Built for …\"", draft.displayName) { v -> vm.updateShopDraft { it.copy(displayName = v.take(20)) } }
            ShopField("Address line", draft.address) { v -> vm.updateShopDraft { it.copy(address = v.take(32)) } }
            ShopField("UPI ID", draft.upiId, KeyboardType.Email) { v -> vm.updateShopDraft { it.copy(upiId = v.trim().take(60)) } }
            ShopField(
                "Print bills from ₹ (0 = print every bill)",
                if (draft.minPrintAmount == 0) "" else draft.minPrintAmount.toString(),
                KeyboardType.Number
            ) { v ->
                val amount = v.filter(Char::isDigit).take(5).toIntOrNull() ?: 0
                vm.updateShopDraft { it.copy(minPrintAmount = amount) }
            }
            ShopField("Functional phone (bill footer)", draft.supportPhone, KeyboardType.Phone) { v -> vm.updateShopDraft { it.copy(supportPhone = v.take(16)) } }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpiceButton("Cancel", onClick = vm::cancelShop, kind = ButtonKind.Ghost, modifier = Modifier.weight(1f))
                SpiceButton("Save details", onClick = vm::saveShop, modifier = Modifier.weight(1f), enabled = isValid(draft))
            }
        }
    }
}

private fun isValid(c: ShopConfig) =
    c.shopName.isNotBlank() && c.printName.isNotBlank() && c.displayName.isNotBlank()

@Composable
private fun androidx.compose.foundation.layout.RowScope.DetailRow(label: String, value: String) {
    Text(label, style = SpiceTypography.bodySmall, modifier = Modifier.weight(0.42f))
    Text(value, style = SpiceTypography.bodyMedium, modifier = Modifier.weight(0.58f))
}

@Composable
private fun ShopField(label: String, value: String, keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    Text(label, style = SpiceTypography.labelSmall, color = MutedStone, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
    SpiceTextField(value = value, onValueChange = onChange, error = null, keyboardOptions = KeyboardOptions(keyboardType = keyboard))
}
