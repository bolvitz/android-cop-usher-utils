package com.eventmonitor.core.data.repository

import androidx.room.withTransaction
import com.eventmonitor.core.data.local.dao.AreaCountDao
import com.eventmonitor.core.data.local.dao.AreaTemplateDao
import com.eventmonitor.core.data.local.dao.VenueDao
import com.eventmonitor.core.data.local.dao.EventDao
import com.eventmonitor.core.data.local.database.AppDatabase
import com.eventmonitor.core.data.local.entities.AreaCountEntity
import com.eventmonitor.core.data.local.entities.EventEntity
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.data.local.entities.EventWithAreaCounts
import com.eventmonitor.core.data.models.CountHistoryItem
import com.eventmonitor.core.domain.models.ServiceType
import com.eventmonitor.core.data.repository.interfaces.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val eventDao: EventDao,
    private val areaCountDao: AreaCountDao,
    private val areaTemplateDao: AreaTemplateDao,
    private val venueDao: VenueDao
) : EventRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getRecentEvents(limit: Int): Flow<List<EventWithDetails>> =
        eventDao.getRecentEvents(limit)

    override fun getRecentEventsByVenue(venueId: String, limit: Int): Flow<List<EventWithDetails>> =
        eventDao.getRecentEventsByVenue(venueId, limit)

    override fun getEventById(id: String): Flow<EventWithDetails?> =
        eventDao.getEventById(id)

    override fun getEventsByVenueAndDateRange(
        venueId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>> =
        eventDao.getEventsByVenueAndDateRange(venueId, startDate, endDate)

    override fun getEventsAcrossVenues(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>> =
        eventDao.getEventsAcrossVenuesByDateRange(startDate, endDate)

    override fun getAverageAttendance(
        venueId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?> =
        eventDao.getAverageAttendance(venueId, startDate, endDate)

    override fun getRecentEventsWithAreaCounts(limit: Int): Flow<List<EventWithAreaCounts>> =
        eventDao.getRecentEventsWithAreaCounts(limit)

    override fun getEventsWithAreaCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithAreaCounts>> =
        eventDao.getEventsWithAreaCountsByDateRange(startDate, endDate)

    override suspend fun createNewEvent(
        venueId: String,
        eventType: ServiceType,
        date: Long,
        countedBy: String,
        eventName: String,
        eventTypeId: String?
    ): String {
        val eventId = UUID.randomUUID().toString()

        // Get all active areas for this venue
        val areas = areaTemplateDao.getAreasByVenue(venueId).first()
        val totalCapacity = areas.sumOf { it.capacity }

        val event = EventEntity(
            id = eventId,
            venueId = venueId,
            eventTypeId = eventTypeId,
            date = date,
            eventType = eventType.name,
            eventName = eventName,
            totalCapacity = totalCapacity,
            countedBy = countedBy
        )

        eventDao.insertEvent(event)

        // Create area counts for all areas
        val areaCounts = areas.map { area ->
            AreaCountEntity(
                eventId = eventId,
                areaTemplateId = area.id,
                count = 0,
                capacity = area.capacity,
                countHistory = json.encodeToString(emptyList<CountHistoryItem>())
            )
        }

        areaCountDao.insertAreaCounts(areaCounts)

        return eventId
    }

    override suspend fun updateEventCount(
        eventId: String,
        areaCountId: String,
        newCount: Int,
        action: String
    ) {
        // Wrap both updates in a transaction to prevent flickering from separate Flow emissions
        database.withTransaction {
            val areaCount = areaCountDao.getAreaCountById(areaCountId).first() ?: return@withTransaction

            val historyItem = CountHistoryItem(
                timestamp = System.currentTimeMillis(),
                oldCount = areaCount.count,
                newCount = newCount,
                action = action
            )

            val currentHistory = if (areaCount.countHistory.isEmpty()) {
                emptyList()
            } else {
                json.decodeFromString<List<CountHistoryItem>>(areaCount.countHistory)
            }
            val updatedHistory = currentHistory + historyItem

            areaCountDao.updateAreaCount(
                areaCount.copy(
                    count = newCount,
                    countHistory = json.encodeToString(updatedHistory),
                    lastUpdated = System.currentTimeMillis()
                )
            )

            updateEventTotal(eventId)
        }
    }

    override suspend fun incrementAreaCount(eventId: String, areaCountId: String, amount: Int) {
        val areaCount = areaCountDao.getAreaCountById(areaCountId).first() ?: return
        val newCount = (areaCount.count + amount).coerceAtLeast(0)
        updateEventCount(eventId, areaCountId, newCount, if (amount > 0) "INCREMENT" else "DECREMENT")
    }

    override suspend fun decrementAreaCount(eventId: String, areaCountId: String, amount: Int) {
        incrementAreaCount(eventId, areaCountId, -amount)
    }

    override suspend fun resetAreaCount(eventId: String, areaCountId: String) {
        updateEventCount(eventId, areaCountId, 0, "RESET")
    }

    override suspend fun updateEventNotes(eventId: String, notes: String) {
        val event = eventDao.getEventById(eventId).first()?.event ?: return
        eventDao.updateEvent(event.copy(notes = notes, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun lockEvent(eventId: String) {
        val event = eventDao.getEventById(eventId).first()?.event ?: return
        eventDao.updateEvent(
            event.copy(
                isLocked = true,
                completedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun unlockEvent(eventId: String) {
        val event = eventDao.getEventById(eventId).first()?.event ?: return
        eventDao.updateEvent(
            event.copy(
                isLocked = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteEvent(eventId: String) {
        eventDao.deleteEventById(eventId)
    }

    override suspend fun exportEventReport(eventId: String): String {
        val eventWithDetails = eventDao.getEventById(eventId).first() ?: return ""

        val event = eventWithDetails.event
        val venue = eventWithDetails.venue
        val areaCounts = areaCountDao.getAreaCountsByService(eventId).first()

        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val shortDateFormat = SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault())
        val eventTypeName = event.eventName.ifEmpty {
            ServiceType.fromString(event.eventType).displayName
        }

        val totalCount = event.totalAttendance
        val totalCapacity = event.totalCapacity
        val utilizationPercent = if (totalCapacity > 0) {
            (totalCount.toFloat() / totalCapacity * 100).toInt()
        } else 0

        return buildString {
            // Header
            appendLine("HEAD COUNT REPORT")
            appendLine(venue.name.uppercase())
            appendLine(venue.location)
            appendLine("_".repeat(40))
            appendLine()

            // Area breakdown
            appendLine("AREA BREAKDOWN")
            appendLine()

            areaCounts
                .sortedBy { it.template.displayOrder }
                .forEach { areaCountWithTemplate ->
                    val area = areaCountWithTemplate.areaCount
                    val template = areaCountWithTemplate.template

                    // Format: "Area Name" followed by spaces, then the count right-aligned
                    val maxLineLength = 40
                    val countStr = area.count.toString()
                    val nameLength = template.name.length
                    val spacesNeeded = maxLineLength - nameLength - countStr.length
                    val spaces = if (spacesNeeded > 0) " ".repeat(spacesNeeded) else "  "

                    appendLine("${template.name}${spaces}${countStr}")

                    // Include area notes if present
                    if (area.notes.isNotEmpty()) {
                        appendLine("Note:        ${area.notes}")
                    }

                    appendLine(".".repeat(40))
                }

            appendLine("_".repeat(40))
            appendLine()

            // Total section
            appendLine("TOTAL")
            appendLine()
            appendLine("Total Count:     $totalCount")
            appendLine("Total Capacity:  $totalCapacity")
            appendLine("Utilization:     $utilizationPercent%")
            appendLine("_".repeat(40))
            appendLine()

            // Event notes section
            if (event.notes.isNotEmpty()) {
                appendLine("EVENT NOTES")
                appendLine(event.notes)
                appendLine()
            }

            // Weather section
            if (event.weather.isNotEmpty()) {
                appendLine("Weather: ${event.weather}")
                appendLine()
            }

            // Event details at the bottom
            appendLine("Event:       $eventTypeName")
            appendLine("Date:        ${dateFormat.format(Date(event.date))}")
            appendLine("Time:        ${timeFormat.format(Date(event.date))}")
            appendLine("Counted By:  ${event.countedBy}")
            appendLine()

            // Footer
            appendLine("Generated: ${shortDateFormat.format(Date())}")
            appendLine("ID: ${eventId.take(8).uppercase()}")
        }
    }

    override suspend fun exportVenueComparisonReport(
        venueIds: List<String>,
        startDate: Long,
        endDate: Long
    ): String {
        val venues = venueIds.mapNotNull { venueId ->
            venueDao.getVenueById(venueId).first()
        }

        val eventsPerVenue = venues.associate { venue ->
            venue.id to eventDao.getEventsByVenueAndDateRange(
                venue.id,
                startDate,
                endDate
            ).first()
        }

        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        return buildString {
            appendLine("MULTI-VENUE ATTENDANCE COMPARISON")
            appendLine("=".repeat(60))
            appendLine("Period: ${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}")
            appendLine()

            venues.forEach { venue ->
                val events = eventsPerVenue[venue.id] ?: emptyList()
                val totalAttendance = events.sumOf { it.event.totalAttendance }
                val avgAttendance = if (events.isNotEmpty()) {
                    totalAttendance / events.size
                } else 0

                appendLine("${venue.name} (${venue.code})")
                appendLine("-".repeat(60))
                appendLine("  Total Events: ${events.size}")
                appendLine("  Total Attendance: $totalAttendance")
                appendLine("  Average Attendance: $avgAttendance")
                appendLine("  Location: ${venue.location}")
                appendLine()
            }

            appendLine("=".repeat(60))
            val grandTotal = eventsPerVenue.values.flatten()
                .sumOf { it.event.totalAttendance }
            appendLine("GRAND TOTAL ATTENDANCE: $grandTotal")
            appendLine()
            appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
        }
    }

    private suspend fun updateEventTotal(eventId: String) {
        val areaCounts = areaCountDao.getAreaCountsByService(eventId).first()
        val total = areaCounts.sumOf { it.areaCount.count }

        val event = eventDao.getEventById(eventId).first()?.event ?: return

        eventDao.updateEvent(
            event.copy(
                totalAttendance = total,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
