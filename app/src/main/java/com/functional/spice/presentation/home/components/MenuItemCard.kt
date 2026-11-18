package com.functional.spice.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.functional.spice.core.theme.Charcoal
import com.functional.spice.core.theme.FieldFill
import com.functional.spice.core.theme.MonoStyles
import com.functional.spice.core.theme.MutedStone
import com.functional.spice.core.theme.SelectedCard
import com.functional.spice.core.theme.SoftCream
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.core.theme.WarmCardamom
import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.MenuItem
import com.functional.spice.presentation.components.Radius

fun Category.icon(): ImageVector = when (this) {
    Category.HOT_TEA -> Icons.Outlined.LocalCafe
    Category.SNACKS -> Icons.Outlined.BakeryDining
    Category.COLD_DRINKS -> Icons.Outlined.LocalDrink
    Category.ALL -> Icons.Outlined.Restaurant
}

/**
 * Tap = +1. Minus button = -1. Long-press (450ms) or tapping the quantity badge
 * opens the quantity keypad. Selected cards get a terracotta border and badge.
 */
@Composable
fun MenuItemCard(
    item: MenuItem,
    quantityInCart: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onEditQuantity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val selected = quantityInCart > 0
    val shape = RoundedCornerShape(Radius.Card)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(shape)
            .background(if (selected) SelectedCard else SoftCream)
            .border(1.5.dp, if (selected) WarmCardamom else Color.Transparent, shape)
            .combinedClickable(
                onClickLabel = "Add one",
                onLongClickLabel = "Type a quantity",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onIncrement()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEditQuantity()
                }
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.padding(end = if (selected) 32.dp else 0.dp)) {
            // Round grey backing, like the merchant logos in the design
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else FieldFill),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.category.icon(), contentDescription = null, tint = WarmCardamom, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.name,
                style = SpiceTypography.bodyMedium.copy(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
                color = Charcoal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(item.formattedPrice, style = MonoStyles.PriceSmall.copy(fontWeight = FontWeight.Normal), color = MutedStone)
        }

        if (selected) {
            // Quantity badge: tap to type a quantity
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .defaultMinSize(minWidth = 30.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WarmCardamom)
                    .clickable(onClickLabel = "Type a quantity") { onEditQuantity() }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$quantityInCart", style = MonoStyles.PriceSmall, color = Color.White)
            }
            // Minus: remove one
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, WarmCardamom, CircleShape)
                    .clickable(onClickLabel = "Remove one") {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDecrement()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Remove, contentDescription = "Remove one ${item.name}", tint = WarmCardamom, modifier = Modifier.size(18.dp))
            }
        }
    }
}
