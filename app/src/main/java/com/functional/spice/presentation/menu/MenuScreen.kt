package com.functional.spice.presentation.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.functional.spice.core.theme.Charcoal
import com.functional.spice.core.theme.Danger
import com.functional.spice.core.theme.MonoStyles
import com.functional.spice.core.theme.MutedStone
import com.functional.spice.core.theme.SoftCreamBorder
import com.functional.spice.core.theme.SpiceTypography
import com.functional.spice.core.theme.StoneBody
import com.functional.spice.core.theme.StoneFaint
import com.functional.spice.core.theme.WarmAlabaster
import com.functional.spice.core.theme.WarmCardamom
import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.MenuItem
import com.functional.spice.presentation.components.ButtonKind
import com.functional.spice.presentation.components.RowsCard
import com.functional.spice.presentation.components.SearchField
import com.functional.spice.presentation.components.SectionLabel
import com.functional.spice.presentation.components.SegmentOption
import com.functional.spice.presentation.components.SegmentedControl
import com.functional.spice.presentation.components.SpiceButton
import com.functional.spice.presentation.components.SpiceCard
import com.functional.spice.presentation.components.SpiceSwitch
import com.functional.spice.presentation.components.rowOf

/** Menu manager (design page 5, left). Sold-out items stay here but leave the Counter. */
@Composable
fun MenuScreen(viewModel: MenuViewModel, snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
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
            Text("Menu", style = SpiceTypography.headlineSmall, modifier = Modifier.weight(1f))
            Text("${state.items.size} ${if (state.items.size == 1) "item" else "items"}", style = SpiceTypography.bodySmall)
        }
        SearchField(
            query = state.searchQuery,
            onQueryChange = viewModel::onSearchChange,
            placeholder = "Search the menu",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp)
        )
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
        ) {
            // While searching, results come first; the form returns when the search is cleared or an item is edited
            if (state.searchQuery.isBlank() || state.form.editingId != null) MenuFormCard(state.form, viewModel)
            if (state.grouped.isEmpty() && state.searchQuery.isNotBlank()) {
                Text(
                    "No items match \"${state.searchQuery.trim()}\".",
                    style = SpiceTypography.bodyMedium,
                    color = MutedStone,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            state.grouped.forEach { (category, items) ->
                SectionLabel(category.displayName)
                RowsCard(rows = items.map { item ->
                    rowOf {
                        MenuRow(
                            item = item,
                            armedForDelete = state.pendingDeleteId == item.id,
                            onToggle = { viewModel.setAvailable(item, it) },
                            onEdit = { viewModel.edit(item) },
                            onDelete = { viewModel.onDeleteTap(item) }
                        )
                    }
                })
            }
        }
    }
}

@Composable
private fun MenuFormCard(form: MenuForm, vm: MenuViewModel) {
    val editing = form.editingId != null
    SpiceCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Text(if (editing) "Edit item" else "Add an item", style = SpiceTypography.titleSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(10.dp))
        FieldLabel("Name")
        SpiceTextField(
            value = form.name,
            onValueChange = vm::onNameChange,
            error = form.nameError,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.width(110.dp)) {
                FieldLabel("Price in ₹")
                SpiceTextField(
                    value = form.price,
                    onValueChange = vm::onPriceChange,
                    error = null,
                    mono = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done)
                )
            }
            Text(
                "Shows on the counter right away",
                style = SpiceTypography.labelSmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 34.dp)
            )
        }
        form.priceError?.let { ErrorText(it) }
        FieldLabel("Category", Modifier.padding(top = 10.dp))
        SegmentedControl(
            options = Category.menuCategories.map { SegmentOption(it, it.displayName) },
            selected = form.category,
            onSelect = vm::onCategoryChange,
            containerColor = WarmAlabaster,
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (editing) {
                SpiceButton("Cancel", onClick = vm::cancelEdit, kind = ButtonKind.Ghost, modifier = Modifier.weight(1f))
            }
            SpiceButton(
                text = if (editing) "Save changes" else "Add to menu",
                onClick = vm::save,
                icon = if (editing) null else Icons.Outlined.Add,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.MenuRow(
    item: MenuItem,
    armedForDelete: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(Modifier.weight(1f)) {
        Text(
            item.name,
            style = SpiceTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (item.isAvailable) Charcoal else StoneFaint
        )
        Row {
            Text(item.formattedPrice, style = MonoStyles.Caption)
            if (!item.isAvailable) Text(", sold out", style = SpiceTypography.labelSmall)
        }
        if (armedForDelete) {
            Text("Tap the bin again to delete ${item.name}", style = SpiceTypography.labelSmall, color = Danger)
        }
    }
    SpiceSwitch(checked = item.isAvailable, onCheckedChange = onToggle)
    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
        Icon(Icons.Outlined.Edit, contentDescription = "Edit ${item.name}", tint = StoneBody, modifier = Modifier.size(20.dp))
    }
    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
        Icon(
            Icons.Outlined.Delete,
            contentDescription = if (armedForDelete) "Confirm delete ${item.name}" else "Delete ${item.name}",
            tint = if (armedForDelete) Danger else StoneFaint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = SpiceTypography.labelSmall, color = MutedStone, modifier = modifier.padding(bottom = 4.dp))
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = SpiceTypography.labelSmall, color = Danger, modifier = Modifier.padding(top = 4.dp))
}

@Composable
fun SpiceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            isError = error != null,
            textStyle = if (mono) MonoStyles.PriceSmall.copy(fontWeight = FontWeight.Normal) else TextStyle(
                fontFamily = SpiceTypography.bodyLarge.fontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = SpiceTypography.bodyLarge.fontSize
            ),
            placeholder = placeholder?.let { { Text(it, style = SpiceTypography.bodyMedium, color = StoneFaint) } },
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = WarmAlabaster,
                unfocusedContainerColor = WarmAlabaster,
                errorContainerColor = WarmAlabaster,
                focusedBorderColor = WarmCardamom,
                unfocusedBorderColor = SoftCreamBorder,
                errorBorderColor = Danger,
                cursorColor = WarmCardamom,
                focusedTextColor = Charcoal,
                unfocusedTextColor = Charcoal
            ),
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { ErrorText(it) }
    }
}
