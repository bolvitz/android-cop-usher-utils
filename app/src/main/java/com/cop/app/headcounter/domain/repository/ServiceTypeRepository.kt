package com.cop.app.headcounter.domain.repository

import com.cop.app.headcounter.data.local.entities.ServiceTypeEntity
import com.cop.app.headcounter.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface ServiceTypeRepository {
    fun getAllServiceTypes(): Flow<List<ServiceTypeEntity>>

    suspend fun getServiceTypeById(id: String): ServiceTypeEntity?

    fun getServiceTypeByIdFlow(id: String): Flow<ServiceTypeEntity?>

    suspend fun serviceTypeNameExists(name: String, excludeServiceTypeId: String? = null): Boolean

    suspend fun createServiceType(
        name: String,
        dayType: String,
        time: String,
        description: String = "",
        displayOrder: Int = 0
    ): Result<String>

    suspend fun updateServiceType(serviceType: ServiceTypeEntity): Result<Unit>

    suspend fun deleteServiceType(id: String): Result<Unit>

    suspend fun getServiceTypeCount(): Int

    suspend fun hasServices(serviceTypeId: String): Boolean
}
