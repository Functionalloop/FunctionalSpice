package com.functional.spice.presentation.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.functional.spice.core.theme.MonoStyles
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.core.theme.StoneBody
import com.functional.spice.domain.model.PrinterMode
import com.functional.spice.domain.model.PrinterStatus
import com.functional.spice.presentation.components.PrinterStatusPill
import com.functional.spice.presentation.components.SegmentOption
import com.functional.spice.presentation.components.SegmentedControl

/** "Good morning" etc. from the phone's clock. */
private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

@Composable
fun CounterHeader(
    shopName: String,
    printerStatus: PrinterStatus,
    printerMode: PrinterMode,
    nextBillNumber: Int?,
    onPrinterModeChange: (PrinterMode) -> Unit,
    onOpenPrinter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = greeting(),
                    style = SpiceTypography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = shopName,
                    style = SpiceTypography.bodySmall.copy(fontSize = 13.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            PrinterStatusPill(printerStatus, onClick = onOpenPrinter)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text("Next bill ", style = SpiceTypography.bodySmall, color = StoneBody)
                Text(nextBillNumber?.let { "#$it" } ?: "", style = MonoStyles.PriceSmall)
            }
            SegmentedControl(
                options = listOf(
                    SegmentOption(PrinterMode.BLUETOOTH, "BT", Icons.Outlined.Bluetooth),
                    SegmentOption(PrinterMode.USB, "USB", Icons.Outlined.Usb)
                ),
                selected = printerMode,
                onSelect = onPrinterModeChange,
                compact = true
            )
        }
    }
}
