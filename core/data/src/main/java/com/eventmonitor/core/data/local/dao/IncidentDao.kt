package com.eventmonitor.core.data.local.dao

import androidx.room.*
import com.eventmonitor.core.data.local.entities.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Query("SELECT * FROM incidents WHERE branchId = :branchId ORDER BY reportedAt DESC")
    fun getIncidentsByBranch(branchId: String): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE status = :status ORDER BY reportedAt DESC")
    fun getIncidentsByStatus(status: String): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE severity = :severity ORDER BY reportedAt DESC")
    fun getIncidentsBySeverity(severity: String): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :incidentId")
    fun getIncidentById(incidentId: String): Flow<IncidentEntity?>

    @Query("SELECT * FROM incidents ORDER BY reportedAt DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("""
        SELECT * FROM incidents
        WHERE branchId = :branchId
        AND status = :status
        ORDER BY reportedAt DESC
    """)
    fun getIncidentsByBranchAndStatus(
        branchId: String,
        status: String
    ): Flow<List<IncidentEntity>>

    @Query("""
        SELECT * FROM incidents
        WHERE branchId = :branchId
        AND severity = :severity
        ORDER BY reportedAt DESC
    """)
    fun getIncidentsByBranchAndSeverity(
        branchId: String,
        severity: String
    ): Flow<List<IncidentEntity>>

    @Query("""
        SELECT * FROM incidents
        WHERE title LIKE '%' || :searchQuery || '%'
        OR description LIKE '%' || :searchQuery || '%'
        OR category LIKE '%' || :searchQuery || '%'
        ORDER BY reportedAt DESC
    """)
    fun searchIncidents(searchQuery: String): Flow<List<IncidentEntity>>

    @Query("""
        SELECT * FROM incidents
        WHERE status != 'RESOLVED' AND status != 'CLOSED'
        ORDER BY
            CASE severity
                WHEN 'CRITICAL' THEN 1
                WHEN 'HIGH' THEN 2
                WHEN 'MEDIUM' THEN 3
                WHEN 'LOW' THEN 4
            END,
            reportedAt ASC
    """)
    fun getActiveIncidentsBySeverity(): Flow<List<IncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity): Long

    @Update
    suspend fun updateIncident(incident: IncidentEntity)

    @Delete
    suspend fun deleteIncident(incident: IncidentEntity)

    @Query("UPDATE incidents SET status = :status, updatedAt = :updatedAt WHERE id = :incidentId")
    suspend fun updateIncidentStatus(incidentId: String, status: String, updatedAt: Long)

    @Query("""
        UPDATE incidents
        SET status = :status,
            resolvedAt = :resolvedAt,
            actionsTaken = :actionsTaken,
            updatedAt = :updatedAt
        WHERE id = :incidentId
    """)
    suspend fun resolveIncident(
        incidentId: String,
        status: String,
        resolvedAt: Long,
        actionsTaken: String,
        updatedAt: Long
    )

    @Query("""
        UPDATE incidents
        SET assignedTo = :assignedTo,
            status = :status,
            updatedAt = :updatedAt
        WHERE id = :incidentId
    """)
    suspend fun assignIncident(
        incidentId: String,
        assignedTo: String,
        status: String,
        updatedAt: Long
    )

    @Query("SELECT COUNT(*) FROM incidents WHERE status = :status")
    fun getIncidentCountByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM incidents WHERE severity = :severity AND status != 'RESOLVED' AND status != 'CLOSED'")
    fun getActiveIncidentCountBySeverity(severity: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM incidents WHERE branchId = :branchId AND status != 'RESOLVED' AND status != 'CLOSED'")
    fun getActiveIncidentCountByBranch(branchId: String): Flow<Int>

    @Query("""
        SELECT * FROM incidents
        WHERE reportedAt BETWEEN :startDate AND :endDate
        ORDER BY reportedAt DESC
    """)
    fun getIncidentsByDateRange(startDate: Long, endDate: Long): Flow<List<IncidentEntity>>
}
