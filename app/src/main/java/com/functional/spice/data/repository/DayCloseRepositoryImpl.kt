package com.functional.spice.data.repository

import com.functional.spice.data.local.dao.DayCloseDao
import com.functional.spice.data.local.entity.DayCloseEntity
import com.functional.spice.data.remote.SupabaseClientProvider
import com.functional.spice.data.remote.dto.SupabaseDayCloseDto
import com.functional.spice.domain.model.DayClose
import com.functional.spice.domain.model.SyncStatus
import com.functional.spice.domain.repository.DayCloseRepository
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DayCloseRepositoryImpl @Inject constructor(
    private val dao: DayCloseDao,
    private val supabaseProvider: SupabaseClientProvider
) : DayCloseRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncLock = Mutex()

    override fun observe(date: String): Flow<DayClose?> = dao.observe(date).map { it?.toDomain() }

    override fun observePendingSyncCount(): Flow<Int> = dao.getPendingCountFlow()

    override suspend fun setOpeningFloat(date: String, openingFloat: Int) = withContext(Dispatchers.IO) {
        val existing = dao.get(date)
        dao.upsert(
            DayCloseEntity(
                date = date,
                openingFloat = openingFloat,
                closedAt = existing?.closedAt,
                syncState = SyncStatus.PENDING.name
            )
        )
        scope.launch { runCatching { syncPending() } }
        Unit
    }

    override suspend fun closeDay(date: String, openingFloat: Int, closedAt: Long): DayClose = withContext(Dispatchers.IO) {
        val entity = DayCloseEntity(date, openingFloat, closedAt, SyncStatus.PENDING.name)
        dao.upsert(entity)
        scope.launch { runCatching { syncPending() } }
        entity.toDomain()
    }

    override suspend fun syncPending(): Result<Int> = withContext(Dispatchers.IO) {
        syncLock.withLock { syncLocked() }
    }

    private suspend fun syncLocked(): Result<Int> {
        return try {
            var count = 0
            for (row in dao.getPending()) {
                supabaseProvider.client.from("day_closes").upsert(
                    SupabaseDayCloseDto(
                        date = row.date,
                        openingFloat = row.openingFloat,
                        closedAt = row.closedAt?.let { Instant.ofEpochMilli(it).toString() }
                    )
                )
                dao.markSynced(row.date, row.openingFloat, row.closedAt)
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
