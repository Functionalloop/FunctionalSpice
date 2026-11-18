package com.functional.spice.presentation.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.functional.spice.core.theme.DeepEspresso
import com.functional.spice.core.theme.EspressoOutline
import com.functional.spice.core.theme.Lime
import com.functional.spice.core.theme.MonoStyles
import com.functional.spice.core.theme.OnEspressoMuted
import com.functional.spice.core.theme.OnEspressoSoft
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.presentation.components.Radius

/** Ink checkout card: item count, total, Clear and a lime "Save and print" pill (opens Payment). */
@Composable
fun StickyBottomCheckoutBar(
    totalItemCount: Int,
    totalAmountFormatted: String,
    onClearCart: () -> Unit,
    onSaveAndPrint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasItems = totalItemCount > 0
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(Radius.CheckoutBar))
            .background(DeepEspresso)
            .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = when (totalItemCount) {
                    0 -> "No items yet"
                    1 -> "1 item"
                    else -> "$totalItemCount items"
                },
                style = SpiceTypography.labelSmall,
                color = OnEspressoMuted
            )
            Text(totalAmountFormatted, style = MonoStyles.PriceLarge.copy(fontSize = 24.sp), color = Color.White, maxLines = 1)
        }
        if (hasItems) {
            Text(
                text = "Clear",
                style = SpiceTypography.labelLarge.copy(fontSize = 13.sp),
                color = OnEspressoSoft,
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, EspressoOutline, CircleShape)
                    .clickable(onClick = onClearCart)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (hasItems) Lime else EspressoOutline)
                .clickable(enabled = hasItems, onClick = onSaveAndPrint)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val fg = if (hasItems) DeepEspresso else OnEspressoMuted
            Icon(Icons.Outlined.Print, contentDescription = null, tint = fg, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save and print", style = SpiceTypography.labelLarge.copy(fontSize = 14.sp), color = fg)
        }
    }
}
