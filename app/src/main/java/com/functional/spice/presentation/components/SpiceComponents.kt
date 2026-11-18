package com.functional.spice.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.functional.spice.core.theme.Charcoal
import com.functional.spice.core.theme.DeepEspresso
import com.functional.spice.core.theme.Lime
import com.functional.spice.core.theme.MonoStyles
import com.functional.spice.core.theme.MutedStone
import com.functional.spice.core.theme.OfflineBg
import com.functional.spice.core.theme.OfflineFg
import com.functional.spice.core.theme.SoftCream
import com.functional.spice.core.theme.SoftCreamBorder
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.core.theme.StoneBody
import com.functional.spice.core.theme.SuccessBg
import com.functional.spice.core.theme.SuccessDot
import com.functional.spice.core.theme.SuccessFg
import com.functional.spice.core.theme.SwitchOff
import com.functional.spice.core.theme.WarmAlabaster
import com.functional.spice.core.theme.WarmCardamom
import com.functional.spice.core.theme.WarningBg
import com.functional.spice.core.theme.WarningFg
import com.functional.spice.core.theme.WarningIcon
import com.functional.spice.domain.model.PrinterStatus

object Radius {
    val Card = 20.dp
    val Hero = 24.dp
    /** Buttons are pills: 28dp on a ~48dp-tall button. */
    val Button = 28.dp
    /** Keypad keys, inputs and grey rows inside cards. */
    val Field = 16.dp
    val Sheet = 28.dp
    val CheckoutBar = 24.dp
}

/** Surface card: white, large radius, no border or elevation (sits on the grey page). */
@Composable
fun SpiceCard(
    modifier: Modifier = Modifier,
    color: Color = SoftCream,
    borderColor: Color = Color.Transparent,
    shape: Shape = RoundedCornerShape(Radius.Card),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color)
            .border(1.dp, borderColor, shape)
            .padding(contentPadding),
        content = content
    )
}

typealias RowContent = @Composable RowScope.() -> Unit

/** Gives a row lambda its composable type so lists of rows infer cleanly. */
fun rowOf(content: RowContent): RowContent = content

/** Card holding rows separated by 1dp lines, like the design's lists. */
@Composable
fun RowsCard(
    modifier: Modifier = Modifier,
    rows: List<RowContent>,
    footer: (@Composable () -> Unit)? = null
) {
    SpiceCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = SoftCreamBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                content = row
            )
        }
        footer?.invoke()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = SpiceTypography.titleMedium.copy(fontSize = 18.sp, letterSpacing = (-0.2).sp),
        color = Charcoal,
        modifier = modifier.padding(top = 20.dp, bottom = 10.dp)
    )
}

data class SegmentOption<T>(val value: T, val label: String, val icon: ImageVector? = null)

/** Segmented control: white pill track, ink pill on the selected option. */
@Composable
fun <T> SegmentedControl(
    options: List<SegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    containerColor: Color = SoftCream
) {
    val outer = CircleShape
    val inner = CircleShape
    Row(
        modifier = modifier
            .clip(outer)
            .background(containerColor)
            .padding(if (compact) 3.dp else 4.dp)
    ) {
        options.forEach { option ->
            val isOn = option.value == selected
            Row(
                modifier = Modifier
                    .then(if (compact) Modifier else Modifier.weight(1f))
                    .clip(inner)
                    .background(if (isOn) DeepEspresso else Color.Transparent)
                    .clickable { onSelect(option.value) }
                    .padding(horizontal = if (compact) 12.dp else 4.dp, vertical = if (compact) 5.dp else 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val color = if (isOn) WarmAlabaster else MutedStone
                option.icon?.let {
                    Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(if (compact) 13.dp else 16.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = option.label,
                    style = SpiceTypography.labelLarge.copy(
                        fontSize = if (compact) 12.sp else 13.sp,
                        fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = color,
                    maxLines = 1
                )
            }
        }
    }
}

enum class ButtonKind { Primary, Dark, Ghost }

/** Buttons start with a verb: "Save and print", "Set quantity", "Add to menu". */
@Composable
fun SpiceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    large: Boolean = false
) {
    val shape = CircleShape
    val (bg, fg) = when (kind) {
        ButtonKind.Primary -> WarmCardamom to Color.White
        ButtonKind.Dark -> DeepEspresso to Color.White
        ButtonKind.Ghost -> SoftCream to Charcoal
    }
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (enabled) bg else SoftCreamBorder)
            .then(if (kind == ButtonKind.Ghost) Modifier.border(1.dp, SoftCreamBorder, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (large) 16.dp else 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = if (enabled) fg else MutedStone
        icon?.let {
            Icon(it, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = SpiceTypography.labelLarge.copy(fontSize = if (large) 15.sp else 14.sp),
            color = color,
            maxLines = 1
        )
    }
}

/** Rounded chip; espresso when selected. */
@Composable
fun SpiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    center: Boolean = false
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) DeepEspresso else SoftCream)
            .border(1.dp, if (selected) DeepEspresso else SoftCreamBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (mono) MonoStyles.PriceSmall.copy(fontWeight = FontWeight.SemiBold)
            else SpiceTypography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = if (selected) Color.White else if (mono) Charcoal else StoneBody,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
            maxLines = 1
        )
    }
}

const val KEY_BACKSPACE = "<"
val QuantityKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", KEY_BACKSPACE)
val CashKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", KEY_BACKSPACE)

/** 3-column number pad: white rounded keys on the grey page. */
@Composable
fun Keypad(keys: List<String>, onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val shape = RoundedCornerShape(Radius.Field)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(shape)
                            .background(SoftCream)
                            .clickable { onKey(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == KEY_BACKSPACE) {
                            Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete", tint = Charcoal)
                        } else {
                            Text(
                                text = key,
                                style = MonoStyles.Key.copy(fontSize = if (key == "C") 15.sp else 19.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Lime switch with a white thumb. */
@Composable
fun SpiceSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Lime,
            checkedBorderColor = Lime,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = SwitchOff,
            uncheckedBorderColor = SwitchOff
        )
    )
}

/** Thin progress track used for categories and top sellers. */
@Composable
fun ProgressTrack(fraction: Float, modifier: Modifier = Modifier, color: Color = WarmCardamom) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(SoftCreamBorder)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color)
        )
    }
}

/**
 * Simple bar chart. [highlight] bar is lime; bars from [futureFrom] on are drawn flat.
 */
@Composable
fun Bars(
    values: List<Int>,
    modifier: Modifier = Modifier,
    height: Dp = 74.dp,
    gap: Dp = 3.dp,
    highlight: Int? = null,
    futureFrom: Int? = null
) {
    val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.Bottom
    ) {
        values.forEachIndexed { i, v ->
            val future = futureFrom != null && i >= futureFrom
            val barHeight = if (future) 4.dp else (height * (v.toFloat() / max)).coerceAtLeast(2.dp)
            Box(
                Modifier
                    .weight(1f)
                    .height(barHeight)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(
                        when {
                            future -> SoftCreamBorder
                            i == highlight -> Lime
                            else -> DeepEspresso
                        }
                    )
            )
        }
    }
}

/** "KP206 connected" pill with status dot. */
@Composable
fun PrinterStatusPill(status: PrinterStatus, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val (bg, fg, dot, label) = when (status) {
        is PrinterStatus.Connected -> Quad(SuccessBg, SuccessFg, SuccessDot, "KP206 connected")
        is PrinterStatus.Connecting -> Quad(WarningBg, WarningFg, WarningIcon, "Connecting")
        is PrinterStatus.PermissionRequired -> Quad(WarningBg, WarningFg, WarningIcon, "Allow Bluetooth")
        is PrinterStatus.Error -> Quad(WarningBg, WarningFg, WarningIcon, "Printer issue")
        PrinterStatus.Disconnected -> Quad(OfflineBg, OfflineFg, MutedStone, "KP206 offline")
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(dot, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = SpiceTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = fg, maxLines = 1)
    }
}

private data class Quad(val bg: Color, val fg: Color, val dot: Color, val label: String)

/** "Label ........ value" row used in many lists. */
@Composable
fun RowScope.LabelValue(label: String, value: String, bold: Boolean = true) {
    Text(label, style = SpiceTypography.bodyMedium, modifier = Modifier.weight(1f))
    Text(value, style = if (bold) MonoStyles.PriceSmall else MonoStyles.PriceSmall.copy(fontWeight = FontWeight.Normal))
}

/** A note or coin to hand back, e.g. "₹50" or "2 × ₹100". */
@Composable
fun NoteChip(text: String) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(WarmAlabaster)
            .border(1.dp, SoftCreamBorder, shape)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(text, style = MonoStyles.PriceSmall.copy(fontSize = 12.sp))
    }
}

@Composable
fun BorderedBox(
    modifier: Modifier = Modifier,
    border: BorderStroke = BorderStroke(1.dp, SoftCreamBorder),
    shape: Shape = RoundedCornerShape(Radius.Card),
    color: Color = SoftCream,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .border(border, shape)
    ) { content() }
}

/** Outlined rounded-square icon button (back, search) from the design: 44dp, white. */
@Composable
fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(shape)
            .background(SoftCream)
            .border(1.dp, SoftCreamBorder, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Charcoal, modifier = Modifier.size(20.dp))
    }
}
