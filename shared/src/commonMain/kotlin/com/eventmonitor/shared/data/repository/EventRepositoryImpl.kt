package com.eventmonitor.shared.data.repository
import com.eventmonitor.shared.platform.TimeProvider

import com.eventmonitor.shared.data.models.*
import com.eventmonitor.shared.domain.common.AppError
import com.eventmonitor.shared.domain.common.Result
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.flow.*

class EventRepositoryImpl(
    private val firestore: FirebaseFirestore
) : EventRepository {

    private val eventsCollection = firestore.collection("events")
    private val venuesCollection = firestore.collection("venues")
    private val eventTypesCollection = firestore.collection("eventTypes")

    override fun getRecentEvents(limit: Int): Flow<List<EventWithDetails>> {
        return eventsCollection
            .orderBy("date", dev.gitlive.firebase.firestore.Direction.DESCENDING)
            .limit(limit)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    val event = doc.data<EventDto>()
                    enrichEventWithDetails(event)
                }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getRecentEventsByVenue(venueId: String, limit: Int): Flow<List<EventWithDetails>> {
        return eventsCollection
            .where { "venueId" equalTo venueId }
            .orderBy("date", dev.gitlive.firebase.firestore.Direction.DESCENDING)
            .limit(limit)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    val event = doc.data<EventDto>()
                    enrichEventWithDetails(event)
                }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getEventById(eventId: String): Flow<EventWithDetails?> {
        return eventsCollection
            .document(eventId)
            .snapshots
            .map { doc ->
                val event = doc.data<EventDto>()
                enrichEventWithDetails(event)
            }
            .catch { e ->
                emit(null)
            }
    }

    override fun getEventsByVenueAndDateRange(
        venueId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>> {
        return eventsCollection
            .where { "venueId" equalTo venueId }
            .where { "date" greaterThanOrEqualTo startDate }
            .where { "date" lessThanOrEqualTo endDate }
            .orderBy("date", dev.gitlive.firebase.firestore.Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    val event = doc.data<EventDto>()
                    enrichEventWithDetails(event)
                }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getEventsAcrossVenues(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>> {
        return eventsCollection
            .where { "date" greaterThanOrEqualTo startDate }
            .where { "date" lessThanOrEqualTo endDate }
            .orderBy("date", dev.gitlive.firebase.firestore.Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    val event = doc.data<EventDto>()
                    enrichEventWithDetails(event)
                }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    override suspend fun createEvent(
        venueId: String,
        eventTypeId: String?,
        date: Long,
        countedBy: String,
        eventName: String
    ): Result<String> {
        return try {
            val event = EventDto(
                id = eventsCollection.document.id,
                venueId = venueId,
                eventTypeId = eventTypeId,
                date = date,
                countedBy = countedBy,
                eventName = eventName,
                createdAt = TimeProvider.currentTimeMillis(),
                updatedAt = TimeProvider.currentTimeMillis()
            )

            eventsCollection.document(event.id).set(event)

            // Create area counts for all active templates in the venue
            val templates = venuesCollection
                .document(venueId)
                .collection("areaTemplates")
                .where { "isActive" equalTo true }
                .get()
                .documents
                .map { it.data<AreaTemplateDto>() }

            templates.forEach { template ->
                val areaCount = AreaCountDto(
                    id = eventsCollection.document.id,
                    eventId = event.id,
                    areaTemplateId = template.id,
                    count = 0,
                    capacity = template.capacity,
                    lastUpdated = TimeProvider.currentTimeMillis()
                )
                eventsCollection
                    .document(event.id)
                    .collection("areaCounts")
                    .document(areaCount.id)
                    .set(areaCount)
            }

            Result.Success(event.id)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to create event"))
        }
    }

    override suspend fun updateEvent(event: EventDto): Result<Unit> {
        return try {
            val eventWithTimestamp = event.copy(updatedAt = TimeProvider.currentTimeMillis())
            eventsCollection.document(event.id).set(eventWithTimestamp)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to update event"))
        }
    }

    override suspend fun updateEventNotes(eventId: String, notes: String): Result<Unit> {
        return try {
            eventsCollection.document(eventId).update(
                "notes" to notes,
                "updatedAt" to TimeProvider.currentTimeMillis()
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to update notes"))
        }
    }

    override suspend fun lockEvent(eventId: String): Result<Unit> {
        return try {
            eventsCollection.document(eventId).update(
                "isLocked" to true,
                "completedAt" to TimeProvider.currentTimeMillis(),
                "updatedAt" to TimeProvider.currentTimeMillis()
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to lock event"))
        }
    }

    override suspend fun unlockEvent(eventId: String): Result<Unit> {
        return try {
            eventsCollection.document(eventId).update(
                "isLocked" to false,
                "completedAt" to null,
                "updatedAt" to TimeProvider.currentTimeMillis()
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to unlock event"))
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            eventsCollection.document(eventId).delete()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete event"))
        }
    }

    private suspend fun enrichEventWithDetails(event: EventDto): EventWithDetails {
        val venue = try {
            venuesCollection.document(event.venueId).get().data<VenueDto>()
        } catch (e: Exception) {
            VenueDto(id = event.venueId, name = "Unknown Venue")
        }

        val eventType = event.eventTypeId?.let { typeId ->
            try {
                eventTypesCollection.document(typeId).get().data<EventTypeDto>()
            } catch (e: Exception) {
                null
            }
        }

        val areaCounts = try {
            eventsCollection
                .document(event.id)
                .collection("areaCounts")
                .get()
                .documents
                .map { doc ->
                    val areaCount = doc.data<AreaCountDto>()
                    val template = venuesCollection
                        .document(event.venueId)
                        .collection("areaTemplates")
                        .document(areaCount.areaTemplateId)
                        .get()
                        .data<AreaTemplateDto>()
                    AreaCountWithTemplate(areaCount, template)
                }
        } catch (e: Exception) {
            emptyList()
        }

        return EventWithDetails(
            event = event,
            venue = venue,
            eventType = eventType,
            areaCounts = areaCounts
        )
    }
}
