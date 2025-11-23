package com.eventmonitor.core.data.repository.interfaces

import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.data.local.entities.EventWithAreaCounts
import com.eventmonitor.core.domain.models.ServiceType
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getRecentServices(limit: Int): Flow<List<EventWithDetails>>
    fun getRecentServicesByBranch(branchId: String, limit: Int): Flow<List<EventWithDetails>>
    fun getServiceById(id: String): Flow<EventWithDetails?>
    fun getServicesByBranchAndDateRange(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>>
    fun getServicesAcrossBranches(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>>
    fun getAverageAttendance(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?>

    fun getRecentServicesWithAreaCounts(limit: Int): Flow<List<EventWithAreaCounts>>
    fun getServicesWithAreaCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithAreaCounts>>

    suspend fun createNewService(
        branchId: String,
        eventType: ServiceType,
        date: Long,
        countedBy: String,
        eventName: String = "",
        eventTypeId: String? = null
    ): String

    suspend fun updateServiceCount(
        eventId: String,
        areaCountId: String,
        newCount: Int,
        action: String = "MANUAL_EDIT"
    )

    suspend fun incrementAreaCount(eventId: String, areaCountId: String, amount: Int = 1)
    suspend fun decrementAreaCount(eventId: String, areaCountId: String, amount: Int = 1)
    suspend fun resetAreaCount(eventId: String, areaCountId: String)
    suspend fun updateServiceNotes(eventId: String, notes: String)
    suspend fun lockService(eventId: String)
    suspend fun unlockService(eventId: String)
    suspend fun deleteService(eventId: String)
    suspend fun exportServiceReport(eventId: String): String
    suspend fun exportBranchComparisonReport(
        branchIds: List<String>,
        startDate: Long,
        endDate: Long
    ): String
}
