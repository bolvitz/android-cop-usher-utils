package com.eventmonitor.core.data.repository.interfaces

import com.eventmonitor.core.data.local.entities.IncidentEntity
import com.eventmonitor.core.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
    fun getAllIncidents(): Flow<List<IncidentEntity>>
    fun getIncidentsByBranch(branchId: String): Flow<List<IncidentEntity>>
    fun getIncidentsByStatus(status: String): Flow<List<IncidentEntity>>
    fun getIncidentsBySeverity(severity: String): Flow<List<IncidentEntity>>
    fun getIncidentById(incidentId: String): Flow<IncidentEntity?>
    fun getIncidentsByBranchAndStatus(branchId: String, status: String): Flow<List<IncidentEntity>>
    fun getIncidentsByBranchAndSeverity(branchId: String, severity: String): Flow<List<IncidentEntity>>
    fun searchIncidents(query: String): Flow<List<IncidentEntity>>
    fun getActiveIncidentsBySeverity(): Flow<List<IncidentEntity>>
    fun getIncidentCountByStatus(status: String): Flow<Int>
    fun getActiveIncidentCountBySeverity(severity: String): Flow<Int>
    fun getActiveIncidentCountByBranch(branchId: String): Flow<Int>
    fun getIncidentsByDateRange(startDate: Long, endDate: Long): Flow<List<IncidentEntity>>

    suspend fun createIncident(
        branchId: String,
        title: String,
        description: String,
        severity: String,
        category: String = "",
        location: String = "",
        photoUri: String = "",
        reportedBy: String = "",
        notes: String = "",
        eventId: String? = null
    ): Result<String>

    suspend fun updateIncident(incident: IncidentEntity): Result<Unit>
    suspend fun deleteIncident(incidentId: String): Result<Unit>
    suspend fun updateIncidentStatus(incidentId: String, status: String): Result<Unit>

    suspend fun assignIncident(
        incidentId: String,
        assignedTo: String,
        status: String
    ): Result<Unit>

    suspend fun resolveIncident(
        incidentId: String,
        status: String,
        actionsTaken: String
    ): Result<Unit>
}
