package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.local.dao.EventDao
import com.eventmonitor.shared.data.mappers.toDto
import com.eventmonitor.shared.data.mappers.toEntity
import com.eventmonitor.shared.data.models.EventDto
import com.eventmonitor.shared.data.models.EventWithDetails
import com.eventmonitor.shared.domain.common.AppError
import com.eventmonitor.shared.domain.common.Result
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class EventRepositoryImpl(
    private val eventDao: EventDao
) : EventRepository {

    override fun getRecentEvents(limit: Int): Flow<List<EventWithDetails>> =
        eventDao.getRecentEvents(limit).map { list -> list.map { it.toDto() } }

    override fun getRecentEventsByVenue(venueId: String, limit: Int): Flow<List<EventWithDetails>> =
        eventDao.getRecentEventsByVenue(venueId, limit).map { list -> list.map { it.toDto() } }

    override fun getEventById(eventId: String): Flow<EventWithDetails?> =
        eventDao.getEventById(eventId).map { it?.toDto() }

    override fun getEventsByVenueAndDateRange(
        venueId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>> =
        eventDao.getEventsByVenueAndDateRange(venueId, startDate, endDate)
            .map { list -> list.map { it.toDto() } }

    override fun getEventsAcrossVenues(startDate: Long, endDate: Long): Flow<List<EventWithDetails>> =
        eventDao.getEventsAcrossVenuesByDateRange(startDate, endDate)
            .map { list -> list.map { it.toDto() } }

    override suspend fun createEvent(
        venueId: String,
        eventTypeId: String?,
        date: Long,
        countedBy: String,
        eventName: String
    ): Result<String> = try {
        val id = newId()
        eventDao.insertEvent(
            com.eventmonitor.shared.data.local.entities.EventEntity(
                id = id,
                venueId = venueId,
                eventTypeId = eventTypeId,
                date = date,
                countedBy = countedBy,
                eventName = eventName
            )
        )
        Result.Success(id)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to create event"))
    }

    override suspend fun updateEvent(event: EventDto): Result<Unit> = try {
        eventDao.updateEvent(event.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to update event"))
    }

    override suspend fun updateEventNotes(eventId: String, notes: String): Result<Unit> =
        mutateEvent(eventId) { it.copy(notes = notes, updatedAt = nowMillis()) }

    override suspend fun lockEvent(eventId: String): Result<Unit> =
        mutateEvent(eventId) { it.copy(isLocked = true, completedAt = nowMillis(), updatedAt = nowMillis()) }

    override suspend fun unlockEvent(eventId: String): Result<Unit> =
        mutateEvent(eventId) { it.copy(isLocked = false, updatedAt = nowMillis()) }

    override suspend fun deleteEvent(eventId: String): Result<Unit> = try {
        eventDao.deleteEventById(eventId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete event"))
    }

    private suspend inline fun mutateEvent(
        eventId: String,
        transform: (com.eventmonitor.shared.data.local.entities.EventEntity) -> com.eventmonitor.shared.data.local.entities.EventEntity
    ): Result<Unit> = try {
        val current = eventDao.getEventById(eventId).first()?.event
            ?: return Result.Error(AppError.NotFound("Event", eventId))
        eventDao.updateEvent(transform(current))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to update event"))
    }
}
