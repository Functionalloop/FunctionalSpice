package com.functional.spice.presentation.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.functional.spice.domain.analytics.MonthStats
import com.functional.spice.domain.analytics.SalesAnalytics
import com.functional.spice.domain.analytics.TodayStats
import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.DayClose
import com.functional.spice.domain.repository.BillRepository
import com.functional.spice.domain.repository.DayCloseRepository
import com.functional.spice.domain.repository.MenuRepository
import com.functional.spice.domain.repository.PrinterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

enum class BillsTab { TODAY, MONTH }

/** Why the opening-float dialog is open. */
enum class FloatPrompt { SET, CLOSE_DAY }

data class BillsUiState(
    val tab: BillsTab = BillsTab.TODAY,
    val today: TodayStats? = null,
    val month: MonthStats? = null,
    val dayClose: DayClose? = null,
    /** Bills plus day closes waiting for Supabase. */
    val pendingSync: Int = 0,
    val isSyncing: Boolean = false,
    val isPrinting: Boolean = false,
    val showAllItems: Boolean = false,
    val showAllBills: Boolean = false,
    val floatPrompt: FloatPrompt? = null,
    val reprintCandidate: Bill? = null,
    val message: String? = null
)

/** Bills: Today and This month (design page 4). */
@HiltViewModel
class BillsViewModel @Inject constructor(
    private val billRepository: BillRepository,
    private val menuRepository: MenuRepository,
    private val dayCloseRepository: DayCloseRepository,
    private val printerRepository: PrinterRepository
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val _uiState = MutableStateFlow(BillsUiState())
    val uiState: StateFlow<BillsUiState> = _uiState.asStateFlow()

    /** Re-checks the date so "Today" rolls over at midnight while the app is open. */
    private val dates: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(zone))
            delay(30_000)
        }
    }.distinctUntilChanged()

    private data class Snapshot(val today: TodayStats, val month: MonthStats, val close: DayClose?)

    init {
        viewModelScope.launch {
            dates.flatMapLatest { date ->
                val ym = YearMonth.from(date)
                val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                combine(
                    billRepository.observeBillsBetween(start, end),
                    menuRepository.observeCategoryLookup(),
                    dayCloseRepository.observe(date.toString())
                ) { bills, categories, close ->
                    Snapshot(
                        today = SalesAnalytics.today(bills, date, zone, categories),
                        month = SalesAnalytics.month(bills, date, zone, categories),
                        close = close
                    )
                }
            }.flowOn(Dispatchers.Default).collect { s ->
                _uiState.update { it.copy(today = s.today, month = s.month, dayClose = s.close) }
            }
        }
        viewModelScope.launch {
            combine(
                billRepository.observePendingSyncCount(),
                dayCloseRepository.observePendingSyncCount()
            ) { bills, closes -> bills + closes }
                .collect { n -> _uiState.update { it.copy(pendingSync = n) } }
        }
    }

    fun selectTab(tab: BillsTab) = _uiState.update { it.copy(tab = tab) }
    fun toggleShowAllItems() = _uiState.update { it.copy(showAllItems = !it.showAllItems) }
    fun toggleShowAllBills() = _uiState.update { it.copy(showAllBills = !it.showAllBills) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }

    fun syncNow() {
        if (_uiState.value.isSyncing) return
        _uiState.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            val bills = billRepository.syncPendingBills()
            val closes = dayCloseRepository.syncPending()
            val message = if (bills.isFailure || closes.isFailure) {
                "No connection. Bills stay saved on this phone."
            } else {
                val n = bills.getOrDefault(0)
                if (n > 0) "Synced $n ${if (n == 1) "bill" else "bills"}" else "Everything is synced"
            }
            _uiState.update { it.copy(isSyncing = false, message = message) }
        }
    }

    // ---- Opening float and Close day ------------------------------------------------------

    fun editOpeningFloat() = _uiState.update { it.copy(floatPrompt = FloatPrompt.SET) }
    fun dismissFloatPrompt() = _uiState.update { it.copy(floatPrompt = null) }

    /** Asks for the opening float first if it hasn't been set today. */
    fun closeDay() {
        val close = _uiState.value.dayClose
        if (close == null) {
            _uiState.update { it.copy(floatPrompt = FloatPrompt.CLOSE_DAY) }
        } else {
            doCloseDay(close.openingFloat)
        }
    }

    fun onFloatEntered(amount: Int) {
        val prompt = _uiState.value.floatPrompt ?: return
        val date = _uiState.value.today?.date ?: LocalDate.now(zone)
        _uiState.update { it.copy(floatPrompt = null) }
        when (prompt) {
            FloatPrompt.SET -> viewModelScope.launch {
                dayCloseRepository.setOpeningFloat(date.toString(), amount)
                _uiState.update { it.copy(message = "Opening float set to ₹$amount") }
            }
            FloatPrompt.CLOSE_DAY -> doCloseDay(amount)
        }
    }

    private fun doCloseDay(openingFloat: Int) {
        val today = _uiState.value.today ?: return
        if (_uiState.value.isPrinting) return
        _uiState.update { it.copy(isPrinting = true) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dayCloseRepository.closeDay(today.date.toString(), openingFloat, now)
            val printed = printerRepository.printDaySummary(SalesAnalytics.toDaySummary(today, openingFloat, now, zone))
            val message = if (printed.isSuccess) "Day closed. Summary printed."
            else "Day closed and saved. Connect the printer, then tap Print summary."
            _uiState.update { it.copy(isPrinting = false, message = message) }
        }
    }

    // ---- Printing -------------------------------------------------------------------------

    fun printDaySummary() {
        val today = _uiState.value.today ?: return
        val float = _uiState.value.dayClose?.openingFloat ?: 0
        printWith("Day summary printed") {
            printerRepository.printDaySummary(SalesAnalytics.toDaySummary(today, float, System.currentTimeMillis(), zone))
        }
    }

    fun printMonthSummary() {
        val month = _uiState.value.month ?: return
        printWith("Month summary printed") {
            printerRepository.printMonthSummary(SalesAnalytics.toMonthSummary(month, zone))
        }
    }

    fun askReprint(bill: Bill) = _uiState.update { it.copy(reprintCandidate = bill) }
    fun dismissReprint() = _uiState.update { it.copy(reprintCandidate = null) }

    fun confirmReprint() {
        val bill = _uiState.value.reprintCandidate ?: return
        _uiState.update { it.copy(reprintCandidate = null) }
        printWith("Bill #${bill.billNumber} printed again") { printerRepository.printBill(bill) }
    }

    private fun printWith(success: String, block: suspend () -> Result<Unit>) {
        if (_uiState.value.isPrinting) return
        _uiState.update { it.copy(isPrinting = true) }
        viewModelScope.launch {
            val result = block()
            _uiState.update {
                it.copy(
                    isPrinting = false,
                    message = if (result.isSuccess) success else "Printer offline. Connect it in the Printer tab and try again."
                )
            }
        }
    }
}
