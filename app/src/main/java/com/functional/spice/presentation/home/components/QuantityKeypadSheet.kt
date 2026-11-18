package com.functional.spice.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.functional.spice.core.theme.MonoStyles
import com.functional.spice.core.theme.MutedStone
import com.functional.spice.core.theme.SoftCreamBorder
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.core.theme.WarmAlabaster
import com.functional.spice.core.theme.WarmCardamom
import com.functional.spice.domain.model.MenuItem
import com.functional.spice.domain.model.Money
import com.functional.spice.presentation.components.ButtonKind
import com.functional.spice.presentation.components.KEY_BACKSPACE
import com.functional.spice.presentation.components.Keypad
import com.functional.spice.presentation.components.QuantityKeys
import com.functional.spice.presentation.components.Radius
import com.functional.spice.presentation.components.SpiceButton
import com.functional.spice.presentation.components.SpiceCard
import com.functional.spice.presentation.components.SpiceChip

private const val MAX_QTY_DIGITS = 3

/**
 * Quantity keypad: quick chips 5/10/20/50, keys 1-9, C, 0, backspace, max 3 digits.
 * The button reads "Remove item" at 0.
 */
@Composable
fun QuantityKeypadSheet(
    item: MenuItem,
    currentQuantity: Int,
    onSetQuantity: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by rememberSaveable(item.id) { mutableStateOf(if (currentQuantity > 0) currentQuantity.toString() else "") }
    // The first key press replaces the pre-filled quantity instead of appending to it
    var replaceOnType by rememberSaveable(item.id) { mutableStateOf(currentQuantity > 0) }
    val qty = text.toIntOrNull() ?: 0

    fun onKey(key: String) {
        text = when (key) {
            "C" -> ""
            KEY_BACKSPACE -> if (replaceOnType) "" else text.dropLast(1)
            else -> ((if (replaceOnType) "" else text) + key).trimStart('0').take(MAX_QTY_DIGITS)
        }
        replaceOnType = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = WarmAlabaster,
        shape = RoundedCornerShape(topStart = Radius.Sheet, topEnd = Radius.Sheet),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SoftCreamBorder)
            )
        }
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(item.name, style = SpiceTypography.titleMedium, modifier = Modifier.weight(1f))
                Text("${item.formattedPrice} each", style = MonoStyles.Caption.copy(fontSize = 12.sp))
            }
            Spacer(Modifier.height(12.dp))
            SpiceCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Text("Quantity", style = SpiceTypography.labelSmall, color = MutedStone)
                        Text(
                            buildAnnotatedString {
                                append(if (text.isEmpty()) "0" else text)
                                withStyle(SpanStyle(color = WarmCardamom, fontWeight = FontWeight.Normal)) { append("|") }
                            },
                            style = MonoStyles.Hero.copy(fontSize = 34.sp, lineHeight = 38.sp)
                        )
                    }
                    Text(Money.inr(qty * item.priceRupees), style = MonoStyles.PriceMedium.copy(fontSize = 16.sp), color = WarmCardamom)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5, 10, 20, 50).forEach { n ->
                    SpiceChip(
                        text = "$n",
                        selected = qty == n,
                        onClick = { text = "$n"; replaceOnType = false },
                        mono = true,
                        center = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Keypad(keys = QuantityKeys, onKey = ::onKey)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpiceButton("Cancel", onClick = onDismiss, kind = ButtonKind.Ghost, modifier = Modifier.weight(1f))
                SpiceButton(
                    text = if (qty == 0) "Remove item" else "Set quantity",
                    onClick = { onSetQuantity(qty) },
                    kind = ButtonKind.Primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
