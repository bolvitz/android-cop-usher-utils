package com.cop.app.headcounter.data.repository

import com.cop.app.headcounter.data.local.dao.IncidentDao
import com.cop.app.headcounter.data.local.entities.IncidentEntity
import com.cop.app.headcounter.domain.common.AppError
import com.cop.app.headcounter.domain.common.Result
import com.cop.app.headcounter.domain.common.resultOf
import com.cop.app.headcounter.domain.models.IncidentStatus
import com.cop.app.headcounter.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class IncidentRepositoryImpl @Inject constructor(
    private val incidentDao: IncidentDao
) : IncidentRepository {

    override fun getAllIncidents(): Flow<List<IncidentEntity>> =
        incidentDao.getAllIncidents()

    override fun getIncidentsByBranch(branchId: String): Flow<List<IncidentEntity>> =
        incidentDao.getIncidentsByBranch(branchId)

    override fun getIncidentsByStatus(status: String): Flow<List<IncidentEntity>> =
        incidentDao.getIncidentsByStatus(status)

    override fun getIncidentsBySeverity(severity: String): Flow<List<IncidentEntity>> =
        incidentDao.getIncidentsBySeverity(severity)

    override fun getIncidentById(incidentId: String): Flow<IncidentEntity?> =
        incidentDao.getIncidentById(incidentId)

    override fun getIncidentsByBranchAndStatus(
        branchId: String,
        status: String
    ): Flow<List<IncidentEntity>> =
        incidentDao.getIncidentsByBranchAndStatus(branchId, status)

    override fun getIncidentsByBranchAndSeverity(
        branchId: String,
        severity: String
    ): Flow<List<IncidentEntity>> =
        incidentDao.getIncidentsByBranchAndSeverity(branchId, severity)

    override fun searchIncidents(query: String): Flow<List<IncidentEntity>> =
        incidentDao.searchIncidents(query)

    override fun getActiveIncidentsBySeverity(): Flow<List<IncidentEntity>> =
        incidentDao.getActiveIncidentsBySeverity()

    override fun getIncidentCountByStatus(status: String): Flow<Int> =
        incidentDao.getIncidentCountByStatus(status)

    override fun getActiveIncidentCountBySeverity(severity: String): Flow<Int> =
        incidentDao.getActiveIncidentCountBySeverity(severity)

    override fun getActiveIncidentCountByBranch(branchId: String): Flow<Int> =
        incidentDao.getActiveIncidentCountByBranch(branchId)

    override fun getIncidentsByDateRange(startDate: Long, endDate: Long): Flow<List<IncidentEntity>> =
        incidentDao.getIncidentsByDateRange(startDate, endDate)

    override suspend fun createIncident(
        branchId: String,
        title: String,
        description: String,
        severity: String,
        category: String,
        location: String,
        photoUri: String,
        reportedBy: String,
        notes: String
    ): Result<String> {
        // Validate input
        if (title.isBlank()) {
            return Result.Error(
                AppError.ValidationError("Title cannot be empty")
            )
        }

        if (description.isBlank()) {
            return Result.Error(
                AppError.ValidationError("Description cannot be empty")
            )
        }

        return resultOf {
            val incidentId = UUID.randomUUID().toString()
            val incident = IncidentEntity(
                id = incidentId,
                branchId = branchId,
                title = title,
                description = description,
                severity = severity,
                status = IncidentStatus.REPORTED.name,
                category = category,
                location = location,
                photoUri = photoUri,
                reportedBy = reportedBy,
                reportedAt = System.currentTimeMillis(),
                notes = notes
            )

            incidentDao.insertIncident(incident)
            incidentId
        }
    }

    override suspend fun updateIncident(incident: IncidentEntity): Result<Unit> {
        if (incident.title.isBlank()) {
            return Result.Error(
                AppError.ValidationError("Title cannot be empty")
            )
        }

        if (incident.description.isBlank()) {
            return Result.Error(
                AppError.ValidationError("Description cannot be empty")
            )
        }

        return resultOf {
            incidentDao.updateIncident(incident.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun deleteIncident(incidentId: String): Result<Unit> {
        val incident = incidentDao.getIncidentById(incidentId).first()
            ?: return Result.Error(AppError.NotFound("Incident", incidentId))

        return resultOf {
            incidentDao.deleteIncident(incident)
        }
    }

    override suspend fun updateIncidentStatus(incidentId: String, status: String): Result<Unit> {
        val incident = incidentDao.getIncidentById(incidentId).first()
            ?: return Result.Error(AppError.NotFound("Incident", incidentId))

        return resultOf {
            incidentDao.updateIncidentStatus(
                incidentId = incidentId,
                status = status,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun assignIncident(
        incidentId: String,
        assignedTo: String,
        status: String
    ): Result<Unit> {
        val incident = incidentDao.getIncidentById(incidentId).first()
            ?: return Result.Error(AppError.NotFound("Incident", incidentId))

        if (assignedTo.isBlank()) {
            return Result.Error(
                AppError.ValidationError("Assignee name is required")
            )
        }

        return resultOf {
            incidentDao.assignIncident(
                incidentId = incidentId,
                assignedTo = assignedTo,
                status = status,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun resolveIncident(
        incidentId: String,
        status: String,
        actionsTaken: String
    ): Result<Unit> {
        val incident = incidentDao.getIncidentById(incidentId).first()
            ?: return Result.Error(AppError.NotFound("Incident", incidentId))

        return resultOf {
            val resolvedAt = if (status == IncidentStatus.RESOLVED.name || status == IncidentStatus.CLOSED.name) {
                System.currentTimeMillis()
            } else {
                incident.resolvedAt ?: 0
            }

            incidentDao.resolveIncident(
                incidentId = incidentId,
                status = status,
                resolvedAt = resolvedAt,
                actionsTaken = actionsTaken,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
