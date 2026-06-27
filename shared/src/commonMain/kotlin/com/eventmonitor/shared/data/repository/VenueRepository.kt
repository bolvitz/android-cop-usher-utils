package com.eventmonitor.shared.data.repository

import com.eventmonitor.shared.data.models.AreaTemplateDto
import com.eventmonitor.shared.data.models.VenueDto
import com.eventmonitor.shared.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface VenueRepository {
    fun getAllVenues(): Flow<List<VenueDto>>
    fun getActiveVenues(): Flow<List<VenueDto>>
    fun getVenueById(id: String): Flow<VenueDto?>
    fun getVenueByCode(code: String): Flow<VenueDto?>

    fun getAreaTemplatesByVenue(venueId: String): Flow<List<AreaTemplateDto>>
    fun getActiveAreaTemplatesByVenue(venueId: String): Flow<List<AreaTemplateDto>>
    fun getAreaTemplateById(templateId: String): Flow<AreaTemplateDto?>

    suspend fun createVenue(venue: VenueDto): Result<String>
    suspend fun updateVenue(venue: VenueDto): Result<Unit>
    suspend fun deleteVenue(venueId: String): Result<Unit>

    suspend fun createAreaTemplate(template: AreaTemplateDto): Result<String>
    suspend fun updateAreaTemplate(template: AreaTemplateDto): Result<Unit>
    suspend fun deleteAreaTemplate(templateId: String): Result<Unit>
}
