package com.cop.app.headcounter.data.repository

import com.cop.app.headcounter.data.local.dao.ServiceDao
import com.cop.app.headcounter.data.local.dao.ServiceTypeDao
import com.cop.app.headcounter.data.local.entities.ServiceTypeEntity
import com.cop.app.headcounter.domain.repository.ServiceTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class ServiceTypeRepositoryImpl @Inject constructor(
    private val serviceTypeDao: ServiceTypeDao,
    private val serviceDao: ServiceDao
) : ServiceTypeRepository {

    override fun getAllServiceTypes(): Flow<List<ServiceTypeEntity>> {
        return serviceTypeDao.getAllServiceTypes()
    }

    override suspend fun getServiceTypeById(id: String): ServiceTypeEntity? {
        return serviceTypeDao.getServiceTypeById(id)
    }

    override fun getServiceTypeByIdFlow(id: String): Flow<ServiceTypeEntity?> {
        return serviceTypeDao.getServiceTypeByIdFlow(id)
    }

    override suspend fun serviceTypeNameExists(name: String, excludeServiceTypeId: String?): Boolean {
        val allServiceTypes = serviceTypeDao.getAllServiceTypes().first()
        return allServiceTypes.any { serviceType ->
            serviceType.name.equals(name, ignoreCase = true) &&
            serviceType.id != excludeServiceTypeId
        }
    }

    override suspend fun createServiceType(
        name: String,
        dayType: String,
        time: String,
        description: String,
        displayOrder: Int
    ): String {
        val serviceTypeId = UUID.randomUUID().toString()
        val serviceType = ServiceTypeEntity(
            id = serviceTypeId,
            name = name,
            dayType = dayType,
            time = time,
            description = description,
            displayOrder = displayOrder,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        serviceTypeDao.insertServiceType(serviceType)
        return serviceTypeId
    }

    override suspend fun updateServiceType(serviceType: ServiceTypeEntity) {
        val updatedServiceType = serviceType.copy(updatedAt = System.currentTimeMillis())
        serviceTypeDao.updateServiceType(updatedServiceType)
    }

    override suspend fun deleteServiceType(id: String) {
        serviceTypeDao.deleteServiceType(id)
    }

    override suspend fun getServiceTypeCount(): Int {
        return serviceTypeDao.getServiceTypeCount()
    }

    override suspend fun hasServices(serviceTypeId: String): Boolean {
        val services = serviceDao.getRecentServices(999).first()
        return services.any { it.service.serviceTypeId == serviceTypeId }
    }
}
