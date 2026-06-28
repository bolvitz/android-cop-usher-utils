package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.local.dao.EventTypeDao
import com.eventmonitor.shared.data.mappers.toDto
import com.eventmonitor.shared.data.mappers.toEntity
import com.eventmonitor.shared.data.models.EventTypeDto
import com.eventmonitor.shared.domain.common.AppError
import com.eventmonitor.shared.domain.common.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventTypeRepositoryImpl(
    private val eventTypeDao: EventTypeDao
) : EventTypeRepository {

    override fun getAllEventTypes(): Flow<List<EventTypeDto>> =
        eventTypeDao.getAllServiceTypesIncludingInactive().map { list -> list.map { it.toDto() } }

    override fun getActiveEventTypes(): Flow<List<EventTypeDto>> =
        eventTypeDao.getAllServiceTypes().map { list -> list.map { it.toDto() } }

    override fun getEventTypeById(id: String): Flow<EventTypeDto?> =
        eventTypeDao.getServiceTypeByIdFlow(id).map { it?.toDto() }

    override suspend fun createEventType(eventType: EventTypeDto): Result<String> = try {
        val entity = eventType.toEntity()
        eventTypeDao.insertServiceType(entity)
        Result.Success(entity.id)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to create event type"))
    }

    override suspend fun updateEventType(eventType: EventTypeDto): Result<Unit> = try {
        val existing = eventTypeDao.getServiceTypeById(eventType.id)
        eventTypeDao.updateServiceType(eventType.toEntity(existing))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to update event type"))
    }

    override suspend fun deleteEventType(eventTypeId: String): Result<Unit> = try {
        eventTypeDao.permanentlyDeleteServiceType(eventTypeId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete event type"))
    }
}
