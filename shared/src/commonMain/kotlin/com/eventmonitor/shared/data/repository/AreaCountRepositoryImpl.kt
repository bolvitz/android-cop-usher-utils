package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.local.dao.AreaCountDao
import com.eventmonitor.shared.data.local.dao.EventDao
import com.eventmonitor.shared.data.mappers.toDto
import com.eventmonitor.shared.data.mappers.toEntity
import com.eventmonitor.shared.data.models.AreaCountDto
import com.eventmonitor.shared.data.models.AreaCountWithTemplate
import com.eventmonitor.shared.data.models.CountHistoryItem
import com.eventmonitor.shared.domain.common.AppError
import com.eventmonitor.shared.domain.common.Result
import com.eventmonitor.shared.util.nowMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AreaCountRepositoryImpl(
    private val areaCountDao: AreaCountDao,
    private val eventDao: EventDao
) : AreaCountRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getAreaCountsByEvent(eventId: String): Flow<List<AreaCountWithTemplate>> =
        areaCountDao.getAreaCountsByService(eventId).map { list -> list.map { it.toDto() } }

    override fun getAreaCountById(areaCountId: String): Flow<AreaCountDto?> =
        areaCountDao.getAreaCountById(areaCountId).map { it?.toDto() }

    override suspend fun createAreaCount(areaCount: AreaCountDto): Result<String> = try {
        val entity = areaCount.toEntity()
        areaCountDao.insertAreaCount(entity)
        Result.Success(entity.id)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to create area count"))
    }

    override suspend fun updateAreaCount(areaCount: AreaCountDto): Result<Unit> = try {
        areaCountDao.updateAreaCount(areaCount.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to update area count"))
    }

    override suspend fun incrementCount(eventId: String, areaCountId: String, amount: Int, action: String): Result<Unit> =
        applyDelta(eventId, areaCountId, action) { current -> current + amount }

    override suspend fun decrementCount(eventId: String, areaCountId: String, amount: Int, action: String): Result<Unit> =
        applyDelta(eventId, areaCountId, action) { current -> maxOf(0, current - amount) }

    override suspend fun updateCount(eventId: String, areaCountId: String, newCount: Int, action: String): Result<Unit> =
        applyDelta(eventId, areaCountId, action) { _ -> maxOf(0, newCount) }

    override suspend fun resetCount(eventId: String, areaCountId: String): Result<Unit> =
        updateCount(eventId, areaCountId, 0, "RESET")

    override suspend fun deleteAreaCount(areaCountId: String): Result<Unit> = try {
        val entity = areaCountDao.getAreaCountById(areaCountId).first()
            ?: return Result.Error(AppError.NotFound("AreaCount", areaCountId))
        // Removing this area's contribution from the event total.
        adjustEventTotal(entity.eventId, -entity.count)
        areaCountDao.updateAreaCount(entity.copy(count = 0, lastUpdated = nowMillis()))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete area count"))
    }

    private suspend inline fun applyDelta(
        eventId: String,
        areaCountId: String,
        action: String,
        newValueOf: (Int) -> Int
    ): Result<Unit> = try {
        val entity = areaCountDao.getAreaCountById(areaCountId).first()
            ?: return Result.Error(AppError.NotFound("AreaCount", areaCountId))
        val oldCount = entity.count
        val newCount = newValueOf(oldCount)
        val history = decodeHistory(entity.countHistory) + CountHistoryItem(
            timestamp = nowMillis(),
            oldCount = oldCount,
            newCount = newCount,
            action = action
        )
        areaCountDao.updateAreaCount(
            entity.copy(
                count = newCount,
                countHistory = json.encodeToString(history),
                lastUpdated = nowMillis()
            )
        )
        adjustEventTotal(eventId, newCount - oldCount)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to update count"))
    }

    private suspend fun adjustEventTotal(eventId: String, delta: Int) {
        if (delta == 0) return
        val event = eventDao.getEventById(eventId).first()?.event ?: return
        eventDao.updateEvent(
            event.copy(
                totalAttendance = maxOf(0, event.totalAttendance + delta),
                updatedAt = nowMillis()
            )
        )
    }

    private fun decodeHistory(raw: String): List<CountHistoryItem> =
        if (raw.isEmpty()) emptyList() else json.decodeFromString(raw)
}
