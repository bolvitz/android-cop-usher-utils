package com.eventmonitor.shared.data.repository
import com.eventmonitor.shared.platform.TimeProvider

import com.eventmonitor.shared.data.models.AreaTemplateDto
import com.eventmonitor.shared.data.models.VenueDto
import com.eventmonitor.shared.domain.common.AppError
import com.eventmonitor.shared.domain.common.Result
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class VenueRepositoryImpl(
    private val firestore: FirebaseFirestore
) : VenueRepository {

    private val venuesCollection = firestore.collection("venues")

    override fun getAllVenues(): Flow<List<VenueDto>> {
        return venuesCollection
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data() }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getActiveVenues(): Flow<List<VenueDto>> {
        return venuesCollection
            .where { "isActive" equalTo true }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data() }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getVenueById(id: String): Flow<VenueDto?> {
        return venuesCollection
            .document(id)
            .snapshots
            .map { it.data<VenueDto>() }
            .catch { e ->
                emit(null)
            }
    }

    override fun getVenueByCode(code: String): Flow<VenueDto?> {
        return venuesCollection
            .where { "code" equalTo code }
            .snapshots
            .map { snapshot ->
                snapshot.documents.firstOrNull()?.data<VenueDto>()
            }
            .catch { e ->
                emit(null)
            }
    }

    override fun getAreaTemplatesByVenue(venueId: String): Flow<List<AreaTemplateDto>> {
        return venuesCollection
            .document(venueId)
            .collection("areaTemplates")
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data<AreaTemplateDto>() }
                    .sortedBy { it.displayOrder }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getActiveAreaTemplatesByVenue(venueId: String): Flow<List<AreaTemplateDto>> {
        return venuesCollection
            .document(venueId)
            .collection("areaTemplates")
            .where { "isActive" equalTo true }
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data<AreaTemplateDto>() }
                    .sortedBy { it.displayOrder }
            }
            .catch { e ->
                emit(emptyList())
            }
    }

    override suspend fun createVenue(venue: VenueDto): Result<String> {
        return try {
            val venueWithTimestamp = venue.copy(
                createdAt = TimeProvider.currentTimeMillis(),
                updatedAt = TimeProvider.currentTimeMillis()
            )
            val docRef = venuesCollection.document(venue.id.ifEmpty { venuesCollection.document.id })
            docRef.set(venueWithTimestamp)
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to create venue"))
        }
    }

    override suspend fun updateVenue(venue: VenueDto): Result<Unit> {
        return try {
            val venueWithTimestamp = venue.copy(updatedAt = TimeProvider.currentTimeMillis())
            venuesCollection.document(venue.id).set(venueWithTimestamp)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to update venue"))
        }
    }

    override suspend fun deleteVenue(venueId: String): Result<Unit> {
        return try {
            venuesCollection.document(venueId).delete()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete venue"))
        }
    }

    override suspend fun createAreaTemplate(template: AreaTemplateDto): Result<String> {
        return try {
            val templateWithTimestamp = template.copy(
                createdAt = TimeProvider.currentTimeMillis(),
                updatedAt = TimeProvider.currentTimeMillis()
            )
            val docRef = venuesCollection
                .document(template.venueId)
                .collection("areaTemplates")
                .document(template.id.ifEmpty { venuesCollection.document.id })
            docRef.set(templateWithTimestamp)
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to create area template"))
        }
    }

    override suspend fun updateAreaTemplate(template: AreaTemplateDto): Result<Unit> {
        return try {
            val templateWithTimestamp = template.copy(updatedAt = TimeProvider.currentTimeMillis())
            venuesCollection
                .document(template.venueId)
                .collection("areaTemplates")
                .document(template.id)
                .set(templateWithTimestamp)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to update area template"))
        }
    }

    override suspend fun deleteAreaTemplate(templateId: String): Result<Unit> {
        return try {
            // Note: We need venueId to delete, this is a limitation
            // In a real implementation, we might need to refactor this
            Result.Error(AppError.InvalidOperation("deleteAreaTemplate requires venueId"))
        } catch (e: Exception) {
            Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete area template"))
        }
    }
}
