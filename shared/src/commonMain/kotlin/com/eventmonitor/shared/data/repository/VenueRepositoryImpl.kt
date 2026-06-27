package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.local.dao.AreaTemplateDao
import com.eventmonitor.shared.data.local.dao.VenueDao
import com.eventmonitor.shared.data.mappers.toDto
import com.eventmonitor.shared.data.mappers.toEntity
import com.eventmonitor.shared.data.models.AreaTemplateDto
import com.eventmonitor.shared.data.models.VenueDto
import com.eventmonitor.shared.domain.common.AppError
import com.eventmonitor.shared.domain.common.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class VenueRepositoryImpl(
    private val venueDao: VenueDao,
    private val areaTemplateDao: AreaTemplateDao
) : VenueRepository {

    override fun getAllVenues(): Flow<List<VenueDto>> =
        venueDao.getAllVenues().map { list -> list.map { it.toDto() } }

    override fun getActiveVenues(): Flow<List<VenueDto>> =
        venueDao.getAllActiveVenues().map { list -> list.map { it.toDto() } }

    override fun getVenueById(id: String): Flow<VenueDto?> =
        venueDao.getVenueById(id).map { it?.toDto() }

    override fun getVenueByCode(code: String): Flow<VenueDto?> =
        venueDao.getAllVenues().map { list -> list.firstOrNull { it.code == code }?.toDto() }

    override fun getAreaTemplatesByVenue(venueId: String): Flow<List<AreaTemplateDto>> =
        areaTemplateDao.getAreasByVenue(venueId).map { list -> list.map { it.toDto() } }

    override fun getActiveAreaTemplatesByVenue(venueId: String): Flow<List<AreaTemplateDto>> =
        areaTemplateDao.getAreasByVenue(venueId).map { list -> list.filter { it.isActive }.map { it.toDto() } }

    override suspend fun createVenue(venue: VenueDto): Result<String> = try {
        val entity = venue.toEntity()
        venueDao.insertVenue(entity)
        Result.Success(entity.id)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to create venue"))
    }

    override suspend fun updateVenue(venue: VenueDto): Result<Unit> = try {
        venueDao.updateVenue(venue.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to update venue"))
    }

    override suspend fun deleteVenue(venueId: String): Result<Unit> = try {
        val venue = venueDao.getVenueById(venueId).first()
            ?: return Result.Error(AppError.NotFound("Venue", venueId))
        venueDao.deleteVenue(venue)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete venue"))
    }

    override suspend fun createAreaTemplate(template: AreaTemplateDto): Result<String> = try {
        val entity = template.toEntity()
        areaTemplateDao.insertArea(entity)
        Result.Success(entity.id)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to create area template"))
    }

    override suspend fun updateAreaTemplate(template: AreaTemplateDto): Result<Unit> = try {
        areaTemplateDao.updateArea(template.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to update area template"))
    }

    override suspend fun deleteAreaTemplate(templateId: String): Result<Unit> = try {
        val template = areaTemplateDao.getAreaById(templateId).first()
            ?: return Result.Error(AppError.NotFound("AreaTemplate", templateId))
        areaTemplateDao.deleteArea(template)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.DatabaseError(e.message ?: "Failed to delete area template"))
    }
}
