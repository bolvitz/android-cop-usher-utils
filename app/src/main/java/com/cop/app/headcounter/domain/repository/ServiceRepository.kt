package com.cop.app.headcounter.domain.repository

import com.cop.app.headcounter.data.local.entities.ServiceWithDetails
import com.cop.app.headcounter.data.local.entities.ServiceWithAreaCounts
import com.cop.app.headcounter.domain.models.ServiceType
import kotlinx.coroutines.flow.Flow

interface ServiceRepository {
    fun getRecentServices(limit: Int): Flow<List<ServiceWithDetails>>
    fun getRecentServicesByBranch(branchId: String, limit: Int): Flow<List<ServiceWithDetails>>
    fun getServiceById(id: String): Flow<ServiceWithDetails?>
    fun getServicesByBranchAndDateRange(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithDetails>>
    fun getServicesAcrossBranches(
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithDetails>>
    fun getAverageAttendance(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?>

    fun getRecentServicesWithAreaCounts(limit: Int): Flow<List<ServiceWithAreaCounts>>
    fun getServicesWithAreaCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithAreaCounts>>

    suspend fun createNewService(
        branchId: String,
        serviceType: ServiceType,
        date: Long,
        countedBy: String,
        serviceName: String = "",
        serviceTypeId: String? = null
    ): String

    suspend fun updateServiceCount(
        serviceId: String,
        areaCountId: String,
        newCount: Int,
        action: String = "MANUAL_EDIT"
    )

    suspend fun incrementAreaCount(serviceId: String, areaCountId: String, amount: Int = 1)
    suspend fun decrementAreaCount(serviceId: String, areaCountId: String, amount: Int = 1)
    suspend fun resetAreaCount(serviceId: String, areaCountId: String)
    suspend fun updateServiceNotes(serviceId: String, notes: String)
    suspend fun lockService(serviceId: String)
    suspend fun unlockService(serviceId: String)
    suspend fun deleteService(serviceId: String)
    suspend fun exportServiceReport(serviceId: String): String
    suspend fun exportBranchComparisonReport(
        branchIds: List<String>,
        startDate: Long,
        endDate: Long
    ): String
}
