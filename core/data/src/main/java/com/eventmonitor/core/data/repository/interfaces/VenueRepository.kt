package com.eventmonitor.core.data.repository.interfaces

import com.eventmonitor.core.data.local.entities.VenueEntity
import com.eventmonitor.core.data.local.entities.VenueWithAreas
import com.eventmonitor.core.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface VenueRepository {
    fun getAllActiveVenues(): Flow<List<VenueWithAreas>>
    fun getAllVenues(): Flow<List<VenueWithAreas>>
    fun getVenueById(id: String): Flow<VenueWithAreas?>
    fun getActiveVenueCount(): Flow<Int>
    suspend fun venueNameExists(name: String, excludeVenueId: String? = null): Boolean
    suspend fun venueCodeExists(code: String, excludeVenueId: String? = null): Boolean

    suspend fun createVenue(
        name: String,
        location: String,
        code: String,
        contactPerson: String = "",
        contactEmail: String = "",
        contactPhone: String = "",
        color: String = "#1976D2",
        isHeadCountEnabled: Boolean = true,
        isLostAndFoundEnabled: Boolean = false,
        isIncidentReportingEnabled: Boolean = false
    ): Result<String>

    suspend fun updateVenue(venue: VenueEntity): Result<Unit>
    suspend fun deleteVenue(venueId: String): Result<Unit>
    suspend fun setVenueActive(venueId: String, isActive: Boolean)
    suspend fun createDefaultAreasForVenue(venueId: String, areaCount: Int = 6)
    suspend fun hasEvents(venueId: String): Boolean
}
