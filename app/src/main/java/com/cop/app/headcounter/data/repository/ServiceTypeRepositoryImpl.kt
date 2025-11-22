package com.cop.app.headcounter.data.repository

import com.cop.app.headcounter.data.local.dao.ServiceTypeDao
import com.cop.app.headcounter.data.local.entities.ServiceTypeEntity
import com.cop.app.headcounter.domain.repository.ServiceTypeRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class ServiceTypeRepositoryImpl @Inject constructor(
    private val serviceTypeDao: ServiceTypeDao
) : ServiceTypeRepository {

    override fun getServiceTypesByBranch(branchId: String): Flow<List<ServiceTypeEntity>> {
        return serviceTypeDao.getServiceTypesByBranch(branchId)
    }

    override suspend fun getServiceTypeById(id: String): ServiceTypeEntity? {
        return serviceTypeDao.getServiceTypeById(id)
    }

    override fun getServiceTypeByIdFlow(id: String): Flow<ServiceTypeEntity?> {
        return serviceTypeDao.getServiceTypeByIdFlow(id)
    }

    override suspend fun createServiceType(
        branchId: String,
        name: String,
        dayType: String,
        time: String,
        description: String,
        displayOrder: Int
    ): String {
        val serviceTypeId = UUID.randomUUID().toString()
        val serviceType = ServiceTypeEntity(
            id = serviceTypeId,
            branchId = branchId,
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

    override suspend fun getServiceTypeCount(branchId: String): Int {
        return serviceTypeDao.getServiceTypeCount(branchId)
    }
}
