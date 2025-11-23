package com.cop.app.headcounter.data.repository

import com.cop.app.headcounter.data.local.dao.ServiceDao
import com.cop.app.headcounter.data.local.dao.ServiceTypeDao
import com.cop.app.headcounter.data.local.entities.ServiceTypeEntity
import com.cop.app.headcounter.domain.common.AppError
import com.cop.app.headcounter.domain.common.Result
import com.cop.app.headcounter.domain.common.resultOf
import com.cop.app.headcounter.domain.repository.ServiceTypeRepository
import com.cop.app.headcounter.domain.validation.DomainValidators
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
    ): Result<String> {
        // Validate input
        val validationResult = DomainValidators.validateServiceTypeInput(
            name = name,
            dayType = dayType,
            time = time,
            description = description.takeIf { it.isNotBlank() }
        )

        if (validationResult is Result.Error) {
            return validationResult
        }

        // Check for duplicate name
        if (serviceTypeNameExists(name)) {
            return Result.Error(
                AppError.AlreadyExists("Service type", "name", name)
            )
        }

        // Create service type
        return resultOf {
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
            serviceTypeId
        }
    }

    override suspend fun updateServiceType(serviceType: ServiceTypeEntity): Result<Unit> {
        // Validate input
        val validationResult = DomainValidators.validateServiceTypeInput(
            name = serviceType.name,
            dayType = serviceType.dayType,
            time = serviceType.time,
            description = serviceType.description.takeIf { it.isNotBlank() }
        )

        if (validationResult is Result.Error) {
            return validationResult
        }

        // Check for duplicate name
        if (serviceTypeNameExists(serviceType.name, excludeServiceTypeId = serviceType.id)) {
            return Result.Error(
                AppError.AlreadyExists("Service type", "name", serviceType.name)
            )
        }

        return resultOf {
            val updatedServiceType = serviceType.copy(updatedAt = System.currentTimeMillis())
            serviceTypeDao.updateServiceType(updatedServiceType)
        }
    }

    override suspend fun deleteServiceType(id: String): Result<Unit> {
        // Check if service type has services
        if (hasServices(id)) {
            return Result.Error(
                AppError.HasDependencies(
                    resource = "Service type",
                    dependencyCount = serviceDao.getRecentServices(999).first().count { it.service.serviceTypeId == id },
                    dependencyType = "services"
                )
            )
        }

        // Check if service type exists
        val serviceType = getServiceTypeById(id)
            ?: return Result.Error(AppError.NotFound("Service type", id))

        return resultOf {
            serviceTypeDao.deleteServiceType(id)
        }
    }

    override suspend fun getServiceTypeCount(): Int {
        return serviceTypeDao.getServiceTypeCount()
    }

    override suspend fun hasServices(serviceTypeId: String): Boolean {
        return serviceDao.hasServicesWithType(serviceTypeId)
    }
}
