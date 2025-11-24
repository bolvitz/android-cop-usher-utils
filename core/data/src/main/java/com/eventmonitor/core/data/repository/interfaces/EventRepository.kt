package com.eventmonitor.core.data.repository.interfaces

import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.data.local.entities.EventWithAreaCounts
import com.eventmonitor.core.domain.models.ServiceType
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getRecentEvents(limit: Int): Flow<List<EventWithDetails>>
    fun getRecentEventsByVenue(venueId: String, limit: Int): Flow<List<EventWithDetails>>
    fun getEventById(id: String): Flow<EventWithDetails?>
    fun getEventsByVenueAndDateRange(
        venueId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>>
    fun getEventsAcrossVenues(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>>
    fun getAverageAttendance(
        venueId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?>

    fun getRecentEventsWithAreaCounts(limit: Int): Flow<List<EventWithAreaCounts>>
    fun getEventsWithAreaCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithAreaCounts>>

    suspend fun createNewEvent(
        venueId: String,
        eventType: ServiceType,
        date: Long,
        countedBy: String,
        eventName: String = "",
        eventTypeId: String? = null
    ): String

    suspend fun updateEventCount(
        eventId: String,
        areaCountId: String,
        newCount: Int,
        action: String = "MANUAL_EDIT"
    )

    suspend fun incrementAreaCount(eventId: String, areaCountId: String, amount: Int = 1)
    suspend fun decrementAreaCount(eventId: String, areaCountId: String, amount: Int = 1)
    suspend fun resetAreaCount(eventId: String, areaCountId: String)
    suspend fun updateEventNotes(eventId: String, notes: String)
    suspend fun lockEvent(eventId: String)
    suspend fun unlockEvent(eventId: String)
    suspend fun deleteEvent(eventId: String)
    suspend fun exportEventReport(eventId: String): String
    suspend fun exportVenueComparisonReport(
        venueIds: List<String>,
        startDate: Long,
        endDate: Long
    ): String
}
