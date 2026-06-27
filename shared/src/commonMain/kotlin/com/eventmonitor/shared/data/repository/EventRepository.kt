package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.models.EventDto
import com.eventmonitor.shared.data.models.EventWithDetails
import com.eventmonitor.shared.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getRecentEvents(limit: Int): Flow<List<EventWithDetails>>
    fun getRecentEventsByVenue(venueId: String, limit: Int): Flow<List<EventWithDetails>>
    fun getEventById(eventId: String): Flow<EventWithDetails?>
    fun getEventsByVenueAndDateRange(
        venueId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>>
    fun getEventsAcrossVenues(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>>

    /** Events in range with their per-area counts populated (for reports). */
    fun getEventsWithAreaCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>>

    suspend fun createEvent(
        venueId: String,
        eventTypeId: String?,
        date: Long,
        countedBy: String,
        eventName: String = ""
    ): Result<String>

    suspend fun updateEvent(event: EventDto): Result<Unit>
    suspend fun updateEventNotes(eventId: String, notes: String): Result<Unit>
    suspend fun lockEvent(eventId: String): Result<Unit>
    suspend fun unlockEvent(eventId: String): Result<Unit>
    suspend fun deleteEvent(eventId: String): Result<Unit>
}
