package com.functional.spice.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.functional.spice.domain.model.Category
import com.functional.spice.presentation.components.SpiceChip

/** Category chips wrap to a second line on narrow screens instead of clipping. */
@Composable
fun CategoryFilterChips(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Category.entries.forEach { category ->
            SpiceChip(
                text = category.displayName,
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}
