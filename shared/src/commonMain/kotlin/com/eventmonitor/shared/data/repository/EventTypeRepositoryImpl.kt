package com.eventmonitor.shared.data.repository
import com.eventmonitor.shared.platform.TimeProvider

import com.eventmonitor.shared.data.models.EventTypeDto
import com.eventmonitor.shared.domain.common.AppError
import com.eventmonitor.shared.domain.common.Result
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class EventTypeRepositoryImpl(
    private val firestore: FirebaseFirestore
) : EventTypeRepository {

    private val eventTypesCollection = firestore.collection("eventTypes")

    override fun getAllEventTypes(): Flow<List<EventTypeDto>> {
        return eventTypesCollection
            .orderBy("displayOrder")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data() }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getActiveEventTypes(): Flow<List<EventTypeDto>> {
        return eventTypesCollection
            .where { "isActive" equalTo true }
            .orderBy("displayOrder")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data() }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getEventTypeById(id: String): Flow<EventTypeDto?> {
        return eventTypesCollection
            .document(id)
            .snapshots
            .map { it.data<EventTypeDto>() }
            .catch { e ->
                emit(null)
            }
    }

    override suspend fun createEventType(eventType: EventTypeDto): Result<String> {
        return try {
            val docRef = eventTypesCollection.document(eventType.id.ifEmpty { eventTypesCollection.document.id })
            docRef.set(eventType)
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to create event type"))
        }
    }

    override suspend fun updateEventType(eventType: EventTypeDto): Result<Unit> {
        return try {
            eventTypesCollection.document(eventType.id).set(eventType)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to update event type"))
        }
    }

    override suspend fun deleteEventType(eventTypeId: String): Result<Unit> {
        return try {
            eventTypesCollection.document(eventTypeId).delete()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete event type"))
        }
    }
}
