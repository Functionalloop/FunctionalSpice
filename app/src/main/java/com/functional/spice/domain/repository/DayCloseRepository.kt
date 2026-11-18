package com.functional.spice.domain.repository

import com.functional.spice.domain.model.DayClose
import kotlinx.coroutines.flow.Flow

interface DayCloseRepository {
    /** @param date ISO date, e.g. "2026-11-13" */
    fun observe(date: String): Flow<DayClose?>
    fun observePendingSyncCount(): Flow<Int>

    suspend fun setOpeningFloat(date: String, openingFloat: Int)
    suspend fun closeDay(date: String, openingFloat: Int, closedAt: Long): DayClose
    suspend fun syncPending(): Result<Int>
}
