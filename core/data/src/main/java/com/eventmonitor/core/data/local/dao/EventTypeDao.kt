package com.eventmonitor.core.data.local.dao

import androidx.room.*
import com.eventmonitor.core.data.local.entities.EventTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventTypeDao {
    @Query("SELECT * FROM event_types WHERE isActive = 1 ORDER BY displayOrder ASC, dayType ASC, time ASC")
    fun getAllServiceTypes(): Flow<List<EventTypeEntity>>

    @Query("SELECT * FROM event_types WHERE id = :id")
    suspend fun getServiceTypeById(id: String): EventTypeEntity?

    @Query("SELECT * FROM event_types WHERE id = :id")
    fun getServiceTypeByIdFlow(id: String): Flow<EventTypeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceType(eventType: EventTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceTypes(eventTypes: List<EventTypeEntity>)

    @Update
    suspend fun updateServiceType(eventType: EventTypeEntity)

    @Query("UPDATE event_types SET isActive = 0 WHERE id = :id")
    suspend fun deleteServiceType(id: String)

    @Query("DELETE FROM event_types WHERE id = :id")
    suspend fun permanentlyDeleteServiceType(id: String)

    @Query("SELECT COUNT(*) FROM event_types WHERE isActive = 1")
    suspend fun getServiceTypeCount(): Int
}
