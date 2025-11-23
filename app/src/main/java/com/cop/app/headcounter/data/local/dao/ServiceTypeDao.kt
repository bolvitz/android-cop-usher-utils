package com.cop.app.headcounter.data.local.dao

import androidx.room.*
import com.cop.app.headcounter.data.local.entities.ServiceTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceTypeDao {
    @Query("SELECT * FROM service_types WHERE isActive = 1 ORDER BY displayOrder ASC, dayType ASC, time ASC")
    fun getAllServiceTypes(): Flow<List<ServiceTypeEntity>>

    @Query("SELECT * FROM service_types WHERE id = :id")
    suspend fun getServiceTypeById(id: String): ServiceTypeEntity?

    @Query("SELECT * FROM service_types WHERE id = :id")
    fun getServiceTypeByIdFlow(id: String): Flow<ServiceTypeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceType(serviceType: ServiceTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceTypes(serviceTypes: List<ServiceTypeEntity>)

    @Update
    suspend fun updateServiceType(serviceType: ServiceTypeEntity)

    @Query("UPDATE service_types SET isActive = 0 WHERE id = :id")
    suspend fun deleteServiceType(id: String)

    @Query("DELETE FROM service_types WHERE id = :id")
    suspend fun permanentlyDeleteServiceType(id: String)

    @Query("SELECT COUNT(*) FROM service_types WHERE isActive = 1")
    suspend fun getServiceTypeCount(): Int
}
