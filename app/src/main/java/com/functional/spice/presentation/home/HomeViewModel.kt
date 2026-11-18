package com.functional.spice.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.functional.spice.data.printer.PrintQueue
import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.BillItem
import com.functional.spice.domain.model.CartItem
import com.functional.spice.domain.model.Category
import com.functional.spice.domain.model.ItemSearch
import com.functional.spice.domain.model.MenuItem
import com.functional.spice.domain.model.PaymentMethod
import com.functional.spice.domain.model.PrinterMode
import com.functional.spice.domain.model.SyncStatus
import com.functional.spice.domain.repository.BillRepository
import com.functional.spice.domain.repository.MenuRepository
import com.functional.spice.domain.repository.PrinterRepository
import com.functional.spice.domain.repository.SettingsRepository
import com.functional.spice.presentation.payment.PaymentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Counter and Payment. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val billRepository: BillRepository,
    private val printerRepository: PrinterRepository,
    private val settingsRepository: SettingsRepository,
    private val printQueue: PrintQueue
) : ViewModel() {

    companion object {
        const val MAX_QUANTITY = 999
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            menuRepository.getMenuItems().collect { all ->
                val byId = all.associateBy { it.id }
                val available = all.filter { it.isAvailable }
                _uiState.update { state ->
                    // Keep cart lines current with menu edits; drop lines whose item was deleted
                    val cart = state.cartItems.mapNotNull { (id, line) ->
                        byId[id]?.let { id to line.copy(item = it) }
                    }.toMap(LinkedHashMap())
                    state.copy(
                        menuItems = available,
                        filteredMenuItems = filterItems(available, state.selectedCategory, state.searchQuery),
                        cartItems = cart
                    )
                }
            }
        }
        viewModelScope.launch {
            printerRepository.printerStatus.collect { status -> _uiState.update { it.copy(printerStatus = status) } }
        }
        viewModelScope.launch {
            printerRepository.currentMode.collect { mode -> _uiState.update { it.copy(printerMode = mode) } }
        }
        viewModelScope.launch {
            billRepository.observeNextBillNumber().collect { n -> _uiState.update { it.copy(nextBillNumber = n) } }
        }
        viewModelScope.launch {
            settingsRepository.shopConfig.collect { shop -> _uiState.update { it.copy(shop = shop) } }
        }
    }

    private fun filterItems(items: List<MenuItem>, category: Category, query: String): List<MenuItem> =
        ItemSearch.filter(if (category == Category.ALL) items else items.filter { it.category == category }, query)

    // ---- Counter --------------------------------------------------------------------------

    fun selectCategory(category: Category) {
        _uiState.update {
            it.copy(selectedCategory = category, filteredMenuItems = filterItems(it.menuItems, category, it.searchQuery))
        }
    }

    /** Starting a search jumps to "All" so items from every category are found. */
    fun onSearchChange(query: String) {
        _uiState.update { state ->
            val category = if (state.searchQuery.isBlank() && query.isNotBlank()) Category.ALL else state.selectedCategory
            state.copy(
                searchQuery = query,
                selectedCategory = category,
                filteredMenuItems = filterItems(state.menuItems, category, query)
            )
        }
    }

    fun incrementItem(item: MenuItem) = setQuantity(item, _uiState.value.quantityOf(item.id) + 1)

    fun decrementItem(item: MenuItem) = setQuantity(item, _uiState.value.quantityOf(item.id) - 1)

    /** 0 removes the line. Quantities are capped at 999 (3 digits). */
    fun setQuantity(item: MenuItem, quantity: Int) {
        _uiState.update { state ->
            val cart = LinkedHashMap(state.cartItems)
            val q = quantity.coerceIn(0, MAX_QUANTITY)
            if (q == 0) cart.remove(item.id) else cart[item.id] = CartItem(item = item, quantity = q)
            state.copy(cartItems = cart)
        }
    }

    fun openQuantityEditor(item: MenuItem) = _uiState.update { it.copy(quantityEditorItem = item) }

    fun closeQuantityEditor() = _uiState.update { it.copy(quantityEditorItem = null) }

    fun clearCart() = _uiState.update { it.copy(cartItems = emptyMap()) }

    fun setPrinterMode(mode: PrinterMode) = printerRepository.setPrinterMode(mode)

    /** "Save and print" opens the Payment screen with a draft bill. */
    fun checkout() {
        val state = _uiState.value
        if (state.cartItems.isEmpty() || state.payment != null) return
        viewModelScope.launch {
            val billId = UUID.randomUUID().toString()
            val lines = state.cartItems.values.map { line ->
                BillItem(
                    id = UUID.randomUUID().toString(),
                    billId = billId,
                    menuItemId = line.item.id,
                    itemName = line.item.name,
                    unitPrice = line.item.priceRupees.toDouble(),
                    quantity = line.quantity,
                    subtotal = line.subtotalRupees.toDouble(),
                    category = line.item.category.name
                )
            }
            val draft = Bill(
                id = billId,
                billNumber = billRepository.getNextBillNumber(),
                timestamp = System.currentTimeMillis(),
                items = lines,
                totalAmount = lines.sumOf { it.subtotal },
                syncStatus = SyncStatus.PENDING
            )
            _uiState.update { it.copy(payment = PaymentState(draft = draft, minPrintAmount = it.shop.minPrintAmount)) }
        }
    }

    // ---- Payment --------------------------------------------------------------------------

    private fun updatePayment(block: (PaymentState) -> PaymentState) {
        _uiState.update { s -> s.payment?.let { s.copy(payment = block(it)) } ?: s }
    }

    fun selectPaymentMethod(method: PaymentMethod) = updatePayment { it.copy(method = method, error = null) }

    fun onCashKey(key: String) = updatePayment { it.withKey(key) }

    fun onQuickAmount(amount: Int) = updatePayment { it.withAmount(amount) }

    fun clearCash() = updatePayment { it.copy(cashInput = "", error = null) }

    fun closePayment() {
        if (_uiState.value.payment?.isSaving == true) return
        _uiState.update { it.copy(payment = null) }
    }

    /**
     * Always saves the bill first, then prints. Offline printer = the bill waits in the print queue.
     * Bills below the shop's print minimum are only saved; they can still be printed from Bills.
     */
    fun confirmPayment() {
        val payment = _uiState.value.payment ?: return
        if (payment.isSaving) return
        payment.validate()?.let { error ->
            updatePayment { it.copy(error = error) }
            return
        }
        val bill = payment.finalBill(System.currentTimeMillis())
        updatePayment { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            if (billRepository.saveBill(bill).isFailure) {
                updatePayment { it.copy(isSaving = false, error = "Couldn't save the bill. Try again.") }
                return@launch
            }
            _uiState.update {
                it.copy(
                    payment = null,
                    cartItems = emptyMap(),
                    // Next customer starts with the full menu
                    searchQuery = "",
                    filteredMenuItems = filterItems(it.menuItems, it.selectedCategory, ""),
                    userMessage = if (payment.willPrint) "Bill #${bill.billNumber} saved. Printing…"
                    else "Bill #${bill.billNumber} saved, not printed (under ₹${payment.minPrintAmount})"
                )
            }
            if (!payment.willPrint) return@launch
            val printed = printQueue.flush()
            val message = if (bill.id in printed) {
                "Bill #${bill.billNumber} saved and printed"
            } else {
                "Bill #${bill.billNumber} saved. It will print when the printer reconnects."
            }
            _uiState.update { it.copy(userMessage = message) }
        }
    }

    fun clearUserMessage() = _uiState.update { it.copy(userMessage = null) }
}
