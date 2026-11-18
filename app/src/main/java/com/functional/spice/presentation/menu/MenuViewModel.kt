package com.functional.spice.presentation.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.ItemSearch
import com.functional.spice.domain.model.MenuItem
import com.functional.spice.domain.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuForm(
    /** Null = adding a new item. */
    val editingId: String? = null,
    val name: String = "",
    val price: String = "",
    val category: Category = Category.HOT_TEA,
    val nameError: String? = null,
    val priceError: String? = null
) {
    companion object {
        const val MAX_NAME = 24
        const val MAX_PRICE_DIGITS = 4

        /** Returns the form with errors set, or null if it can be saved. */
        fun validate(form: MenuForm, items: List<MenuItem>): MenuForm? {
            val name = form.name.trim()
            val price = form.price.toIntOrNull() ?: 0
            val nameError = when {
                name.isEmpty() -> "Enter an item name"
                items.any { it.id != form.editingId && it.name.trim().equals(name, ignoreCase = true) } ->
                    "$name is already on the menu"
                else -> null
            }
            val priceError = if (price < 1) "Enter a price of at least ₹1" else null
            return if (nameError != null || priceError != null) form.copy(nameError = nameError, priceError = priceError) else null
        }
    }
}

data class MenuUiState(
    val items: List<MenuItem> = emptyList(),
    val form: MenuForm = MenuForm(),
    /** First tap on the bin arms delete; a second tap within a few seconds deletes. */
    val pendingDeleteId: String? = null,
    val message: String? = null,
    val searchQuery: String = ""
) {
    /** Items by category, narrowed by the search box (sold-out items included). */
    val grouped: List<Pair<Category, List<MenuItem>>>
        get() {
            val shown = ItemSearch.filter(items, searchQuery)
            return Category.menuCategories.mapNotNull { c ->
                shown.filter { it.category == c }.takeIf { it.isNotEmpty() }?.let { c to it }
            }
        }
}

/** Menu manager (design page 5, left). */
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()
    private var disarmJob: Job? = null

    init {
        viewModelScope.launch {
            menuRepository.getMenuItems().collect { items -> _uiState.update { it.copy(items = items) } }
        }
    }

    fun onSearchChange(query: String) = _uiState.update { it.copy(searchQuery = query) }

    private fun updateForm(block: (MenuForm) -> MenuForm) = _uiState.update { it.copy(form = block(it.form)) }

    fun onNameChange(value: String) = updateForm { it.copy(name = value.take(MenuForm.MAX_NAME), nameError = null) }

    fun onPriceChange(value: String) =
        updateForm { it.copy(price = value.filter(Char::isDigit).take(MenuForm.MAX_PRICE_DIGITS), priceError = null) }

    fun onCategoryChange(category: Category) = updateForm { it.copy(category = category) }

    fun edit(item: MenuItem) = _uiState.update {
        it.copy(
            form = MenuForm(editingId = item.id, name = item.name, price = item.priceRupees.toString(), category = item.category),
            pendingDeleteId = null
        )
    }

    fun cancelEdit() = _uiState.update { it.copy(form = MenuForm(category = it.form.category)) }

    fun save() {
        val state = _uiState.value
        MenuForm.validate(state.form, state.items)?.let { invalid ->
            _uiState.update { it.copy(form = invalid) }
            return
        }
        val form = state.form
        val existing = state.items.firstOrNull { it.id == form.editingId }
        val item = MenuItem(
            id = form.editingId ?: "",
            name = form.name.trim(),
            konkaniSubtitle = existing?.konkaniSubtitle ?: "",
            category = form.category,
            price = (form.price.toIntOrNull() ?: 0).toDouble(),
            isAvailable = existing?.isAvailable ?: true,
            sortOrder = existing?.sortOrder ?: 0
        )
        viewModelScope.launch {
            menuRepository.saveMenuItem(item)
            _uiState.update {
                it.copy(
                    form = MenuForm(category = form.category),
                    message = if (existing == null) "Added ${item.name} to the menu" else "Saved ${item.name}"
                )
            }
        }
    }

    fun setAvailable(item: MenuItem, available: Boolean) {
        viewModelScope.launch { menuRepository.setAvailability(item.id, available) }
    }

    fun onDeleteTap(item: MenuItem) {
        if (_uiState.value.pendingDeleteId == item.id) {
            disarmJob?.cancel()
            _uiState.update {
                it.copy(
                    pendingDeleteId = null,
                    form = if (it.form.editingId == item.id) MenuForm(category = it.form.category) else it.form,
                    message = "Deleted ${item.name}"
                )
            }
            viewModelScope.launch { menuRepository.deleteMenuItem(item.id) }
        } else {
            _uiState.update { it.copy(pendingDeleteId = item.id) }
            disarmJob?.cancel()
            disarmJob = viewModelScope.launch {
                delay(4_000)
                _uiState.update { s -> if (s.pendingDeleteId == item.id) s.copy(pendingDeleteId = null) else s }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
