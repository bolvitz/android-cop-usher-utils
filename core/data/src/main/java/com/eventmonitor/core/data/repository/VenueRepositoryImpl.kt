package com.eventmonitor.core.data.repository

import com.eventmonitor.core.data.local.dao.AreaTemplateDao
import com.eventmonitor.core.data.local.dao.VenueDao
import com.eventmonitor.core.data.local.dao.EventDao
import com.eventmonitor.core.data.local.entities.AreaTemplateEntity
import com.eventmonitor.core.data.local.entities.VenueEntity
import com.eventmonitor.core.data.local.entities.VenueWithAreas
import com.eventmonitor.core.domain.common.AppError
import com.eventmonitor.core.domain.common.Result
import com.eventmonitor.core.domain.common.resultOf
import com.eventmonitor.core.domain.models.AreaType
import com.eventmonitor.core.data.repository.interfaces.VenueRepository
import com.eventmonitor.core.domain.validation.DomainValidators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class VenueRepositoryImpl @Inject constructor(
    private val venueDao: VenueDao,
    private val areaTemplateDao: AreaTemplateDao,
    private val eventDao: EventDao
) : VenueRepository {

    override fun getAllActiveVenues(): Flow<List<VenueWithAreas>> =
        venueDao.getAllActiveVenuesWithAreas()

    override fun getAllVenues(): Flow<List<VenueWithAreas>> =
        venueDao.getAllVenuesWithAreas()

    override fun getVenueById(id: String): Flow<VenueWithAreas?> =
        venueDao.getVenueWithAreas(id)

    override fun getActiveVenueCount(): Flow<Int> =
        venueDao.getActiveVenueCount()

    override suspend fun venueNameExists(name: String, excludeVenueId: String?): Boolean {
        val allVenues = venueDao.getAllVenuesWithAreas().first()
        return allVenues.any { venueWithAreas ->
            venueWithAreas.venue.name.equals(name, ignoreCase = true) &&
            venueWithAreas.venue.id != excludeVenueId
        }
    }

    override suspend fun venueCodeExists(code: String, excludeVenueId: String?): Boolean {
        val allVenues = venueDao.getAllVenuesWithAreas().first()
        return allVenues.any { venueWithAreas ->
            venueWithAreas.venue.code.equals(code, ignoreCase = true) &&
            venueWithAreas.venue.id != excludeVenueId
        }
    }

    override suspend fun createVenue(
        name: String,
        location: String,
        code: String,
        contactPerson: String,
        contactEmail: String,
        contactPhone: String,
        color: String,
        isHeadCountEnabled: Boolean,
        isLostAndFoundEnabled: Boolean,
        isIncidentReportingEnabled: Boolean
    ): Result<String> {
        // Validate input
        val validationResult = DomainValidators.validateVenueInput(
            name = name,
            location = location,
            code = code,
            contactEmail = contactEmail.takeIf { it.isNotBlank() },
            contactPhone = contactPhone.takeIf { it.isNotBlank() }
        )

        if (validationResult is Result.Error) {
            return validationResult
        }

        // Check for duplicate name
        if (venueNameExists(name)) {
            return Result.Error(
                AppError.AlreadyExists("Venue", "name", name)
            )
        }

        // Check for duplicate code
        if (venueCodeExists(code)) {
            return Result.Error(
                AppError.AlreadyExists("Venue", "code", code)
            )
        }

        // Create venue
        return resultOf {
            val venueId = UUID.randomUUID().toString()
            val venue = VenueEntity(
                id = venueId,
                name = name,
                location = location,
                code = code.uppercase(),
                contactPerson = contactPerson,
                contactEmail = contactEmail,
                contactPhone = contactPhone,
                color = color,
                isHeadCountEnabled = isHeadCountEnabled,
                isLostAndFoundEnabled = isLostAndFoundEnabled,
                isIncidentReportingEnabled = isIncidentReportingEnabled
            )

            venueDao.insertVenue(venue)
            venueId
        }
    }

    override suspend fun updateVenue(venue: VenueEntity): Result<Unit> {
        // Validate input
        val validationResult = DomainValidators.validateVenueInput(
            name = venue.name,
            location = venue.location,
            code = venue.code,
            contactEmail = venue.contactEmail.takeIf { it.isNotBlank() },
            contactPhone = venue.contactPhone.takeIf { it.isNotBlank() }
        )

        if (validationResult is Result.Error) {
            return validationResult
        }

        // Check for duplicate name
        if (venueNameExists(venue.name, excludeVenueId = venue.id)) {
            return Result.Error(
                AppError.AlreadyExists("Venue", "name", venue.name)
            )
        }

        // Check for duplicate code
        if (venueCodeExists(venue.code, excludeVenueId = venue.id)) {
            return Result.Error(
                AppError.AlreadyExists("Venue", "code", venue.code)
            )
        }

        return resultOf {
            venueDao.updateVenue(venue.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun deleteVenue(venueId: String): Result<Unit> {
        // Check if venue has events
        if (hasEvents(venueId)) {
            return Result.Error(
                AppError.HasDependencies(
                    resource = "Venue",
                    dependencyCount = eventDao.getRecentEventsByVenue(venueId, 999).first().size,
                    dependencyType = "events"
                )
            )
        }

        val venue = venueDao.getVenueById(venueId).first()
            ?: return Result.Error(AppError.NotFound("Venue", venueId))

        return resultOf {
            venueDao.deleteVenue(venue)
        }
    }

    override suspend fun setVenueActive(venueId: String, isActive: Boolean) {
        venueDao.setVenueActive(venueId, isActive)
    }

    override suspend fun createDefaultAreasForVenue(venueId: String, areaCount: Int) {
        val defaultAreas = mutableListOf<AreaTemplateEntity>()

        // Create bays
        repeat(areaCount) { index ->
            defaultAreas.add(
                AreaTemplateEntity(
                    venueId = venueId,
                    name = "Bay ${index + 1}",
                    type = AreaType.SEATING.name,
                    capacity = 100,
                    displayOrder = index,
                    icon = AreaType.SEATING.defaultIcon,
                    color = "#4CAF50"
                )
            )
        }

        // Add baby rooms
        defaultAreas.add(
            AreaTemplateEntity(
                venueId = venueId,
                name = "Baby Room 1",
                type = AreaType.CARE_ROOM.name,
                capacity = 100,
                displayOrder = areaCount,
                icon = AreaType.CARE_ROOM.defaultIcon,
                color = "#FFC107"
            )
        )

        defaultAreas.add(
            AreaTemplateEntity(
                venueId = venueId,
                name = "Baby Room 2",
                type = AreaType.CARE_ROOM.name,
                capacity = 100,
                displayOrder = areaCount + 1,
                icon = AreaType.CARE_ROOM.defaultIcon,
                color = "#FFC107"
            )
        )

        // Add balcony
        defaultAreas.add(
            AreaTemplateEntity(
                venueId = venueId,
                name = "Balcony",
                type = AreaType.OVERFLOW.name,
                capacity = 100,
                displayOrder = areaCount + 2,
                icon = AreaType.OVERFLOW.defaultIcon,
                color = "#2196F3"
            )
        )

        areaTemplateDao.insertAreas(defaultAreas)
    }

    override suspend fun hasEvents(venueId: String): Boolean {
        val events = eventDao.getRecentEventsByVenue(venueId, 1).first()
        return events.isNotEmpty()
    }
}
