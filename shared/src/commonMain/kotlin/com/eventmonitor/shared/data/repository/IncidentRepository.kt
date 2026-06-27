package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.models.IncidentDto
import com.eventmonitor.shared.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    fun getAllIncidents(): Flow<List<IncidentDto>>
    fun getIncidentsByVenue(venueId: String): Flow<List<IncidentDto>>
    fun getIncidentsByStatus(status: String): Flow<List<IncidentDto>>
    fun getIncidentsBySeverity(severity: String): Flow<List<IncidentDto>>
    fun getIncidentsByVenueAndStatus(venueId: String, status: String): Flow<List<IncidentDto>>
    fun getIncidentsByVenueAndSeverity(venueId: String, severity: String): Flow<List<IncidentDto>>
    fun searchIncidents(query: String): Flow<List<IncidentDto>>
    fun getIncidentById(incidentId: String): Flow<IncidentDto?>

    suspend fun createIncident(
        venueId: String,
        title: String,
        description: String,
        severity: String,
        category: String = "",
        location: String = "",
        reportedBy: String = "",
        notes: String = "",
        eventId: String? = null
    ): Result<String>

    suspend fun updateIncident(incident: IncidentDto): Result<Unit>
    suspend fun deleteIncident(incidentId: String): Result<Unit>
    suspend fun updateIncidentStatus(incidentId: String, status: String): Result<Unit>
    suspend fun assignIncident(incidentId: String, assignedTo: String, status: String): Result<Unit>
    suspend fun resolveIncident(incidentId: String, status: String, actionsTaken: String): Result<Unit>
}
