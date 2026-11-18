package com.functional.spice.domain.model

/**
 * One record per trading day.
 * @param date ISO date, e.g. "2026-11-13"
 * @param openingFloat cash in the drawer at the start of the day (whole rupees)
 * @param closedAt epoch millis when "Close day" was pressed; null while the day is open
 */
data class DayClose(
    val date: String,
    val openingFloat: Int,
    val closedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)
