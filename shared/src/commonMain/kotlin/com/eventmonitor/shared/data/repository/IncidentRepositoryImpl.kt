package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.local.dao.IncidentDao
import com.eventmonitor.shared.data.local.entities.IncidentEntity
import com.eventmonitor.shared.data.mappers.toDto
import com.eventmonitor.shared.data.models.IncidentDto
import com.eventmonitor.shared.domain.common.AppError
import com.eventmonitor.shared.domain.common.Result
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IncidentRepositoryImpl(
    private val incidentDao: IncidentDao
) : IncidentRepository {

    override fun getAllIncidents(): Flow<List<IncidentDto>> =
        incidentDao.getAllIncidents().map { list -> list.map { it.toDto() } }

    override fun getIncidentsByVenue(venueId: String): Flow<List<IncidentDto>> =
        incidentDao.getIncidentsByVenue(venueId).map { list -> list.map { it.toDto() } }

    override fun getIncidentsByStatus(status: String): Flow<List<IncidentDto>> =
        incidentDao.getIncidentsByStatus(status).map { list -> list.map { it.toDto() } }

    override fun getIncidentsBySeverity(severity: String): Flow<List<IncidentDto>> =
        incidentDao.getIncidentsBySeverity(severity).map { list -> list.map { it.toDto() } }

    override fun getIncidentsByVenueAndStatus(venueId: String, status: String): Flow<List<IncidentDto>> =
        incidentDao.getIncidentsByVenueAndStatus(venueId, status).map { list -> list.map { it.toDto() } }

    override fun getIncidentsByVenueAndSeverity(venueId: String, severity: String): Flow<List<IncidentDto>> =
        incidentDao.getIncidentsByVenueAndSeverity(venueId, severity).map { list -> list.map { it.toDto() } }

    override fun searchIncidents(query: String): Flow<List<IncidentDto>> =
        incidentDao.searchIncidents(query).map { list -> list.map { it.toDto() } }

    override fun getIncidentById(incidentId: String): Flow<IncidentDto?> =
        incidentDao.getIncidentById(incidentId).map { it?.toDto() }

    override suspend fun createIncident(
        venueId: String,
        title: String,
        description: String,
        severity: String,
        category: String,
        location: String,
        reportedBy: String,
        notes: String,
        eventId: String?
    ): Result<String> = try {
        val now = nowMillis()
        val entity = IncidentEntity(
            id = newId(),
            venueId = venueId,
            eventId = eventId,
            title = title,
            description = description,
            severity = severity,
            status = "REPORTED",
            category = category,
            location = location,
            reportedBy = reportedBy,
            notes = notes,
            reportedAt = now,
            createdAt = now,
            updatedAt = now
        )
        incidentDao.insertIncident(entity)
        Result.Success(entity.id)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to create incident"))
    }

    override suspend fun updateIncident(incident: IncidentDto): Result<Unit> = try {
        incidentDao.updateIncident(incident.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to update incident"))
    }

    override suspend fun deleteIncident(incidentId: String): Result<Unit> = try {
        incidentDao.deleteIncidentById(incidentId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete incident"))
    }

    override suspend fun updateIncidentStatus(incidentId: String, status: String): Result<Unit> = try {
        incidentDao.updateIncidentStatus(incidentId, status, nowMillis())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to update status"))
    }

    override suspend fun assignIncident(incidentId: String, assignedTo: String, status: String): Result<Unit> = try {
        incidentDao.assignIncident(incidentId, assignedTo, status, nowMillis())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to assign incident"))
    }

    override suspend fun resolveIncident(incidentId: String, status: String, actionsTaken: String): Result<Unit> = try {
        val now = nowMillis()
        incidentDao.resolveIncident(incidentId, status, now, actionsTaken, now)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to resolve incident"))
    }
}
