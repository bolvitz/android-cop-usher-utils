package com.eventmonitor.core.data.repository.interfaces

import com.eventmonitor.core.data.local.entities.EventTypeEntity
import com.eventmonitor.core.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface EventTypeRepository {
    fun getAllServiceTypes(): Flow<List<EventTypeEntity>>

    fun getAllServiceTypesIncludingInactive(): Flow<List<EventTypeEntity>>

    suspend fun getServiceTypeById(id: String): EventTypeEntity?

    fun getServiceTypeByIdFlow(id: String): Flow<EventTypeEntity?>

    suspend fun eventTypeNameExists(name: String, excludeServiceTypeId: String? = null): Boolean

    suspend fun createServiceType(
        name: String,
        dayType: String,
        time: String,
        description: String = "",
        displayOrder: Int = 0
    ): Result<String>

    suspend fun updateServiceType(eventType: EventTypeEntity): Result<Unit>

    suspend fun deleteServiceType(id: String): Result<Unit>

    suspend fun getServiceTypeCount(): Int

    suspend fun hasEvents(eventTypeId: String): Boolean

    suspend fun setServiceTypeActive(id: String, isActive: Boolean)
}
