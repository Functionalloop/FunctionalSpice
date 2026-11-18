package com.functional.spice.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.functional.spice.data.local.entity.DayCloseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayCloseDao {
    @Query("SELECT * FROM day_closes WHERE date = :date LIMIT 1")
    fun observe(date: String): Flow<DayCloseEntity?>

    @Query("SELECT * FROM day_closes WHERE date = :date LIMIT 1")
    suspend fun get(date: String): DayCloseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DayCloseEntity)

    @Query("SELECT * FROM day_closes WHERE syncState = 'PENDING'")
    suspend fun getPending(): List<DayCloseEntity>

    @Query("SELECT COUNT(*) FROM day_closes WHERE syncState = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    /** Marks a row synced only if it hasn't changed since it was sent. */
    @Query(
        "UPDATE day_closes SET syncState = 'SYNCED' WHERE date = :date " +
            "AND openingFloat = :openingFloat AND closedAt IS :closedAt"
    )
    suspend fun markSynced(date: String, openingFloat: Int, closedAt: Long?)
}
