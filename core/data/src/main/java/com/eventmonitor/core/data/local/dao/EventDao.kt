package com.eventmonitor.core.data.local.dao

import androidx.room.*
import com.eventmonitor.core.data.local.entities.EventEntity
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.data.local.entities.EventWithAreaCounts
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Transaction
    @Query("SELECT * FROM events ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentServices(limit: Int = 50): Flow<List<EventWithDetails>>

    @Transaction
    @Query("SELECT * FROM events WHERE branchId = :branchId ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentServicesByBranch(branchId: String, limit: Int = 50): Flow<List<EventWithDetails>>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :eventId")
    fun getServiceById(eventId: String): Flow<EventWithDetails?>

    @Transaction
    @Query("""
        SELECT * FROM events
        WHERE branchId = :branchId
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, createdAt ASC
    """)
    fun getServicesByBranchAndDateRange(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM events
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, branchId ASC
    """)
    fun getServicesAcrossBranchesByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>>

    @Query("SELECT * FROM events WHERE branchId = :branchId AND eventType = :eventType ORDER BY date DESC LIMIT 1")
    fun getLastServiceOfType(branchId: String, eventType: String): Flow<EventEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: EventEntity): Long

    @Update
    suspend fun updateService(service: EventEntity)

    @Delete
    suspend fun deleteService(service: EventEntity)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteServiceById(eventId: String)

    @Query("""
        SELECT AVG(totalAttendance)
        FROM events
        WHERE branchId = :branchId
        AND date BETWEEN :startDate AND :endDate
    """)
    fun getAverageAttendance(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?>

    @Transaction
    @Query("SELECT * FROM events ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentServicesWithAreaCounts(limit: Int = 50): Flow<List<EventWithAreaCounts>>

    @Transaction
    @Query("""
        SELECT * FROM events
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC, createdAt DESC
    """)
    fun getServicesWithAreaCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithAreaCounts>>

    @Query("SELECT EXISTS(SELECT 1 FROM events WHERE eventTypeId = :eventTypeId LIMIT 1)")
    suspend fun hasServicesWithType(eventTypeId: String): Boolean
}
