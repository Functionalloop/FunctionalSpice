package com.functional.spice.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.functional.spice.core.theme.Charcoal
import com.functional.spice.core.theme.MutedStone
import com.functional.spice.core.theme.SoftCream
import com.functional.spice.core.theme.SoftCreamBorder
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.core.theme.StoneFaint
import com.functional.spice.core.theme.WarmCardamom

/** Compact search box: magnifier, text, and a clear (x) button once something is typed. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val focus = LocalFocusManager.current
    val shape = RoundedCornerShape(Radius.Button)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(SoftCream, shape)
            .border(1.dp, if (query.isNotEmpty()) WarmCardamom else SoftCreamBorder, shape)
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = MutedStone, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(placeholder, style = SpiceTypography.bodyMedium, color = StoneFaint, maxLines = 1)
            }
            BasicTextField(
                value = query,
                onValueChange = { onQueryChange(it.take(40)) },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = SpiceTypography.bodyMedium.fontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = SpiceTypography.bodyMedium.fontSize,
                    color = Charcoal
                ),
                cursorBrush = SolidColor(WarmCardamom),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = MutedStone, modifier = Modifier.size(20.dp))
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}
