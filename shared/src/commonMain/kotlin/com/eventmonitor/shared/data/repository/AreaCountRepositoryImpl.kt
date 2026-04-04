package com.eventmonitor.shared.data.repository
import com.eventmonitor.shared.platform.TimeProvider

import com.eventmonitor.shared.data.models.*
import com.eventmonitor.shared.domain.common.AppError
import com.eventmonitor.shared.domain.common.Result
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class AreaCountRepositoryImpl(
    private val firestore: FirebaseFirestore
) : AreaCountRepository {

    private val eventsCollection = firestore.collection("events")
    private val venuesCollection = firestore.collection("venues")

    override fun getAreaCountsByEvent(eventId: String): Flow<List<AreaCountWithTemplate>> {
        return eventsCollection
            .document(eventId)
            .collection("areaCounts")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    val areaCount = doc.data<AreaCountDto>()
                    enrichAreaCountWithTemplate(areaCount)
                }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getAreaCountById(areaCountId: String): Flow<AreaCountDto?> {
        // Note: This requires knowing the eventId, which is a limitation
        // In a real implementation, we might need to restructure the Firestore schema
        return kotlinx.coroutines.flow.flow {
            emit(null)
        }
    }

    override suspend fun createAreaCount(areaCount: AreaCountDto): Result<String> {
        return try {
            val areaCountWithTimestamp = areaCount.copy(
                lastUpdated = TimeProvider.currentTimeMillis()
            )
            val docRef = eventsCollection
                .document(areaCount.eventId)
                .collection("areaCounts")
                .document(areaCount.id.ifEmpty { eventsCollection.document.id })
            docRef.set(areaCountWithTimestamp)
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to create area count"))
        }
    }

    override suspend fun updateAreaCount(areaCount: AreaCountDto): Result<Unit> {
        return try {
            val areaCountWithTimestamp = areaCount.copy(
                lastUpdated = TimeProvider.currentTimeMillis()
            )
            eventsCollection
                .document(areaCount.eventId)
                .collection("areaCounts")
                .document(areaCount.id)
                .set(areaCountWithTimestamp)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to update area count"))
        }
    }

    override suspend fun incrementCount(
        eventId: String,
        areaCountId: String,
        amount: Int,
        action: String
    ): Result<Unit> {
        return try {
            firestore.runTransaction {
                val docRef = eventsCollection
                    .document(eventId)
                    .collection("areaCounts")
                    .document(areaCountId)

                val areaCount = docRef.get().data<AreaCountDto>()
                val newCount = areaCount.count + amount
                val historyItem = CountHistoryItem(
                    timestamp = TimeProvider.currentTimeMillis(),
                    oldCount = areaCount.count,
                    newCount = newCount,
                    action = action
                )

                val updatedAreaCount = areaCount.copy(
                    count = newCount,
                    countHistory = areaCount.countHistory + historyItem,
                    lastUpdated = TimeProvider.currentTimeMillis()
                )

                docRef.set(updatedAreaCount)

                // Update event total
                updateEventTotal(eventId, amount)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to increment count"))
        }
    }

    override suspend fun decrementCount(
        eventId: String,
        areaCountId: String,
        amount: Int,
        action: String
    ): Result<Unit> {
        return try {
            firestore.runTransaction {
                val docRef = eventsCollection
                    .document(eventId)
                    .collection("areaCounts")
                    .document(areaCountId)

                val areaCount = docRef.get().data<AreaCountDto>()
                val newCount = maxOf(0, areaCount.count - amount)
                val historyItem = CountHistoryItem(
                    timestamp = TimeProvider.currentTimeMillis(),
                    oldCount = areaCount.count,
                    newCount = newCount,
                    action = action
                )

                val updatedAreaCount = areaCount.copy(
                    count = newCount,
                    countHistory = areaCount.countHistory + historyItem,
                    lastUpdated = TimeProvider.currentTimeMillis()
                )

                docRef.set(updatedAreaCount)

                // Update event total
                updateEventTotal(eventId, -(areaCount.count - newCount))
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to decrement count"))
        }
    }

    override suspend fun updateCount(
        eventId: String,
        areaCountId: String,
        newCount: Int,
        action: String
    ): Result<Unit> {
        return try {
            firestore.runTransaction {
                val docRef = eventsCollection
                    .document(eventId)
                    .collection("areaCounts")
                    .document(areaCountId)

                val areaCount = docRef.get().data<AreaCountDto>()
                val historyItem = CountHistoryItem(
                    timestamp = TimeProvider.currentTimeMillis(),
                    oldCount = areaCount.count,
                    newCount = newCount,
                    action = action
                )

                val updatedAreaCount = areaCount.copy(
                    count = newCount,
                    countHistory = areaCount.countHistory + historyItem,
                    lastUpdated = TimeProvider.currentTimeMillis()
                )

                docRef.set(updatedAreaCount)

                // Update event total
                val delta = newCount - areaCount.count
                updateEventTotal(eventId, delta)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to update count"))
        }
    }

    override suspend fun resetCount(eventId: String, areaCountId: String): Result<Unit> {
        return updateCount(eventId, areaCountId, 0, "RESET")
    }

    override suspend fun deleteAreaCount(areaCountId: String): Result<Unit> {
        return try {
            // Note: Requires eventId which is a limitation
            Result.Error(AppError.InvalidOperation("deleteAreaCount requires eventId"))
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete area count"))
        }
    }

    private suspend fun enrichAreaCountWithTemplate(areaCount: AreaCountDto): AreaCountWithTemplate {
        val event = eventsCollection.document(areaCount.eventId).get().data<EventDto>()
        val template = try {
            venuesCollection
                .document(event.venueId)
                .collection("areaTemplates")
                .document(areaCount.areaTemplateId)
                .get()
                .data<AreaTemplateDto>()
        } catch (e: Exception) {
            AreaTemplateDto(id = areaCount.areaTemplateId, name = "Unknown Area")
        }

        return AreaCountWithTemplate(areaCount, template)
    }

    private suspend fun updateEventTotal(eventId: String, delta: Int) {
        val eventDoc = eventsCollection.document(eventId)
        val event = eventDoc.get().data<EventDto>()
        val newTotal = event.totalAttendance + delta

        eventDoc.update(
            "totalAttendance" to newTotal,
            "updatedAt" to TimeProvider.currentTimeMillis()
        )
    }
}
