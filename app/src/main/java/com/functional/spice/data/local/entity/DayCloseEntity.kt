package com.functional.spice.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.functional.spice.domain.model.DayClose
import com.functional.spice.domain.model.SyncStatus

@Entity(tableName = "day_closes")
data class DayCloseEntity(
    /** ISO date, e.g. "2026-11-13". */
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: String,
    @ColumnInfo(name = "openingFloat")
    val openingFloat: Int,
    @ColumnInfo(name = "closedAt")
    val closedAt: Long?,
    @ColumnInfo(name = "syncState")
    val syncState: String = SyncStatus.PENDING.name
) {
    fun toDomain() = DayClose(
        date = date,
        openingFloat = openingFloat,
        closedAt = closedAt,
        syncStatus = runCatching { SyncStatus.valueOf(syncState) }.getOrDefault(SyncStatus.PENDING)
    )

    companion object {
        fun fromDomain(d: DayClose) = DayCloseEntity(
            date = d.date,
            openingFloat = d.openingFloat,
            closedAt = d.closedAt,
            syncState = d.syncStatus.name
        )
    }
}
