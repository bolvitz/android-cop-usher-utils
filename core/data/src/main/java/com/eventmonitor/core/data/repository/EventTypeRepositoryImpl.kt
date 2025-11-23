package com.eventmonitor.core.data.repository

import com.eventmonitor.core.data.local.dao.EventDao
import com.eventmonitor.core.data.local.dao.EventTypeDao
import com.eventmonitor.core.data.local.entities.EventTypeEntity
import com.eventmonitor.core.domain.common.AppError
import com.eventmonitor.core.domain.common.Result
import com.eventmonitor.core.domain.common.resultOf
import com.eventmonitor.core.data.repository.interfaces.EventTypeRepository
import com.eventmonitor.core.domain.validation.DomainValidators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class EventTypeRepositoryImpl @Inject constructor(
    private val eventTypeDao: EventTypeDao,
    private val eventDao: EventDao
) : EventTypeRepository {

    override fun getAllServiceTypes(): Flow<List<EventTypeEntity>> {
        return eventTypeDao.getAllServiceTypes()
    }

    override suspend fun getServiceTypeById(id: String): EventTypeEntity? {
        return eventTypeDao.getServiceTypeById(id)
    }

    override fun getServiceTypeByIdFlow(id: String): Flow<EventTypeEntity?> {
        return eventTypeDao.getServiceTypeByIdFlow(id)
    }

    override suspend fun eventTypeNameExists(name: String, excludeServiceTypeId: String?): Boolean {
        val allServiceTypes = eventTypeDao.getAllServiceTypes().first()
        return allServiceTypes.any { eventType ->
            eventType.name.equals(name, ignoreCase = true) &&
            eventType.id != excludeServiceTypeId
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
        if (eventTypeNameExists(name)) {
            return Result.Error(
                AppError.AlreadyExists("Service type", "name", name)
            )
        }

        // Create service type
        return resultOf {
            val eventTypeId = UUID.randomUUID().toString()
            val eventType = EventTypeEntity(
                id = eventTypeId,
                name = name,
                dayType = dayType,
                time = time,
                description = description,
                displayOrder = displayOrder,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            eventTypeDao.insertServiceType(eventType)
            eventTypeId
        }
    }

    override suspend fun updateServiceType(eventType: EventTypeEntity): Result<Unit> {
        // Validate input
        val validationResult = DomainValidators.validateServiceTypeInput(
            name = eventType.name,
            dayType = eventType.dayType,
            time = eventType.time,
            description = eventType.description.takeIf { it.isNotBlank() }
        )

        if (validationResult is Result.Error) {
            return validationResult
        }

        // Check for duplicate name
        if (eventTypeNameExists(eventType.name, excludeServiceTypeId = eventType.id)) {
            return Result.Error(
                AppError.AlreadyExists("Service type", "name", eventType.name)
            )
        }

        return resultOf {
            val updatedServiceType = eventType.copy(updatedAt = System.currentTimeMillis())
            eventTypeDao.updateServiceType(updatedServiceType)
        }
    }

    override suspend fun deleteServiceType(id: String): Result<Unit> {
        // Check if service type has services
        if (hasServices(id)) {
            return Result.Error(
                AppError.HasDependencies(
                    resource = "Service type",
                    dependencyCount = eventDao.getRecentServices(999).first().count { it.event.eventTypeId == id },
                    dependencyType = "events"
                )
            )
        }

        // Check if service type exists
        val eventType = getServiceTypeById(id)
            ?: return Result.Error(AppError.NotFound("Service type", id))

        return resultOf {
            eventTypeDao.deleteServiceType(id)
        }
    }

    override suspend fun getServiceTypeCount(): Int {
        return eventTypeDao.getServiceTypeCount()
    }

    override suspend fun hasServices(eventTypeId: String): Boolean {
        return eventDao.hasServicesWithType(eventTypeId)
    }
}
