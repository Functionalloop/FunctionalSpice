package com.functional.spice.data.printer

import com.functional.spice.domain.repository.BillRepository
import com.functional.spice.domain.repository.PrinterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bills are always saved first with printPending = true. This queue prints them in bill
 * order and clears the flag after each successful print. It runs after every checkout and
 * again whenever the printer reconnects, so bills saved offline print once it is back.
 *
 * Everything goes through [flush] under one lock, so a bill is never printed twice.
 */
@Singleton
class PrintQueue @Inject constructor(
    private val billRepository: BillRepository,
    private val printerRepository: PrinterRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var started = false

    val pendingCount: Flow<Int> get() = billRepository.observePrintPendingCount()

    /** Called once from SpiceApplication. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            printerRepository.printerStatus
                .distinctUntilChangedBy { it.isConnected }
                .collect { status -> if (status.isConnected) flush() }
        }
    }

    /** Prints every pending bill, oldest first. Returns the ids that printed. */
    suspend fun flush(): Set<String> = lock.withLock {
        val printed = mutableSetOf<String>()
        for (bill in billRepository.getPrintPendingBills()) {
            if (printerRepository.printBill(bill).isFailure) break
            billRepository.setPrintPending(bill.id, false)
            printed += bill.id
        }
        printed
    }

    /** "Skip": the bills stay saved, they just won't be printed. */
    suspend fun clear() = lock.withLock { billRepository.clearPrintQueue() }
}
