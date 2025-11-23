package com.cop.app.headcounter.data.local.dao

import androidx.room.*
import com.cop.app.headcounter.data.local.entities.ServiceEntity
import com.cop.app.headcounter.data.local.entities.ServiceWithDetails
import com.cop.app.headcounter.data.local.entities.ServiceWithAreaCounts
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceDao {
    @Transaction
    @Query("SELECT * FROM services ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentServices(limit: Int = 50): Flow<List<ServiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM services WHERE branchId = :branchId ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentServicesByBranch(branchId: String, limit: Int = 50): Flow<List<ServiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM services WHERE id = :serviceId")
    fun getServiceById(serviceId: String): Flow<ServiceWithDetails?>

    @Transaction
    @Query("""
        SELECT * FROM services
        WHERE branchId = :branchId
        AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, createdAt ASC
    """)
    fun getServicesByBranchAndDateRange(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithDetails>>

    @Transaction
    @Query("""
        SELECT * FROM services
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, branchId ASC
    """)
    fun getServicesAcrossBranchesByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithDetails>>

    @Query("SELECT * FROM services WHERE branchId = :branchId AND serviceType = :serviceType ORDER BY date DESC LIMIT 1")
    fun getLastServiceOfType(branchId: String, serviceType: String): Flow<ServiceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceEntity): Long

    @Update
    suspend fun updateService(service: ServiceEntity)

    @Delete
    suspend fun deleteService(service: ServiceEntity)

    @Query("DELETE FROM services WHERE id = :serviceId")
    suspend fun deleteServiceById(serviceId: String)

    @Query("""
        SELECT AVG(totalAttendance)
        FROM services
        WHERE branchId = :branchId
        AND date BETWEEN :startDate AND :endDate
    """)
    fun getAverageAttendance(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?>

    @Transaction
    @Query("SELECT * FROM services ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentServicesWithAreaCounts(limit: Int = 50): Flow<List<ServiceWithAreaCounts>>

    @Transaction
    @Query("""
        SELECT * FROM services
        WHERE date BETWEEN :startDate AND :endDate
        ORDER BY date DESC, createdAt DESC
    """)
    fun getServicesWithAreaCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithAreaCounts>>

    @Query("SELECT EXISTS(SELECT 1 FROM services WHERE serviceTypeId = :serviceTypeId LIMIT 1)")
    suspend fun hasServicesWithType(serviceTypeId: String): Boolean
}
