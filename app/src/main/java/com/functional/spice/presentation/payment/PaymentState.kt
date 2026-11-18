package com.functional.spice.presentation.payment

import com.functional.spice.domain.model.Bill
import com.functional.spice.domain.model.Money
import com.functional.spice.domain.model.PaymentMethod
import com.functional.spice.domain.payment.ChangeCalculator
import com.functional.spice.presentation.components.KEY_BACKSPACE

/** What the live result card under "Cash received" shows. */
sealed class CashResult {
    /** Nothing typed yet. */
    object Empty : CashResult()
    /** Warning colors. */
    data class StillToCollect(val amount: Int) : CashResult()
    /** Success colors. */
    object Exact : CashResult()
    /** Espresso card with the notes to hand back. */
    data class ReturnChange(val amount: Int, val notes: List<Pair<Int, Int>>) : CashResult()
}

data class PaymentState(
    val draft: Bill,
    val method: PaymentMethod = PaymentMethod.CASH,
    /** Digits typed on the cash keypad (max [MAX_CASH_DIGITS]). */
    val cashInput: String = "",
    val error: String? = null,
    val isSaving: Boolean = false,
    /** From ShopConfig.minPrintAmount: bills below this total are saved without printing. */
    val minPrintAmount: Int = 0
) {
    companion object {
        const val MAX_CASH_DIGITS = 5
    }

    val total: Int get() = draft.totalRupees
    val received: Int? get() = cashInput.toIntOrNull()
    val suggestions: List<Int> get() = ChangeCalculator.suggestions(total)

    /** False when the total is below the shop's print minimum: the bill is only saved. */
    val willPrint: Boolean get() = total >= minPrintAmount

    val confirmLabel: String
        get() = when {
            method == PaymentMethod.CASH && willPrint -> "Confirm and print"
            method == PaymentMethod.CASH -> "Confirm (no print)"
            willPrint -> "Mark paid and print"
            else -> "Mark paid (no print)"
        }

    val cashResult: CashResult
        get() {
            val r = received ?: return CashResult.Empty
            val change = ChangeCalculator.change(total, r)
            return when {
                change < 0 -> CashResult.StillToCollect(-change)
                change == 0 -> CashResult.Exact
                else -> CashResult.ReturnChange(change, ChangeCalculator.breakdown(change))
            }
        }

    /** Checked when the cashier taps the button; null means OK to save. */
    fun validate(): String? {
        if (method == PaymentMethod.UPI) return null
        return when (val result = cashResult) {
            CashResult.Empty -> "Enter the cash received first"
            is CashResult.StillToCollect -> "Collect ${Money.inr(result.amount)} more first"
            else -> null
        }
    }

    /** Applies one keypad press: 1-9, 0, 00 or backspace. */
    fun withKey(key: String): PaymentState {
        val next = when (key) {
            KEY_BACKSPACE -> cashInput.dropLast(1)
            else -> (cashInput + key).trimStart('0').take(MAX_CASH_DIGITS)
        }
        return copy(cashInput = next, error = null)
    }

    fun withAmount(amount: Int): PaymentState =
        copy(cashInput = amount.toString().take(MAX_CASH_DIGITS), error = null)

    /** The bill as it will be saved. */
    fun finalBill(now: Long): Bill {
        val cash = method == PaymentMethod.CASH
        return draft.copy(
            timestamp = now,
            paymentMethod = method,
            cashReceived = if (cash) received else null,
            changeReturned = if (cash) received?.let { ChangeCalculator.change(total, it) } else null,
            printPending = willPrint
        )
    }
}
