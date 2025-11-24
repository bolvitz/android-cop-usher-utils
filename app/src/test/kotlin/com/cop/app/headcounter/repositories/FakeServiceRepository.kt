package com.cop.app.headcounter.repositories

import com.cop.app.headcounter.data.local.entities.ServiceWithAreaCounts
import com.cop.app.headcounter.data.local.entities.ServiceWithDetails
import com.cop.app.headcounter.domain.models.ServiceType
import com.cop.app.headcounter.domain.repository.ServiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeServiceRepository : ServiceRepository {
    private val services = MutableStateFlow<Map<String, ServiceWithDetails>>(emptyMap())
    private val areaCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

    fun addService(serviceWithDetails: ServiceWithDetails) {
        services.value = services.value + (serviceWithDetails.service.id to serviceWithDetails)
    }

    fun setAreaCount(areaCountId: String, count: Int) {
        areaCounts.value = areaCounts.value + (areaCountId to count)
    }

    override suspend fun createNewService(
        branchId: String,
        serviceType: ServiceType,
        date: Long,
        countedBy: String,
        serviceName: String,
        serviceTypeId: String?
    ): String {
        throw NotImplementedError("Implement as needed in tests")
    }

    override fun getRecentServices(limit: Int): Flow<List<ServiceWithDetails>> {
        return services.map { it.values.take(limit) }
    }

    override fun getRecentEventsByVenue(branchId: String, limit: Int): Flow<List<ServiceWithDetails>> {
        return services.map { servicesMap ->
            servicesMap.values.filter { it.service.branchId == branchId }.take(limit)
        }
    }

    override fun getServiceById(serviceId: String): Flow<ServiceWithDetails?> {
        return services.map { it[serviceId] }
    }

    override fun getEventsByVenueAndDateRange(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithDetails>> {
        return services.map { servicesMap ->
            servicesMap.values.filter {
                it.service.branchId == branchId &&
                it.service.date in startDate..endDate
            }
        }
    }

    override fun getEventsAcrossVenues(
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithDetails>> {
        return services.map { servicesMap ->
            servicesMap.values.filter { it.service.date in startDate..endDate }
        }
    }

    override fun getAverageAttendance(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?> {
        return services.map { servicesMap ->
            val branchServices = servicesMap.values.filter {
                it.service.branchId == branchId && it.service.date in startDate..endDate
            }
            if (branchServices.isEmpty()) null
            else branchServices.map { it.service.totalAttendance }.average()
        }
    }

    override fun getServicesWithAreaCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithAreaCounts>> {
        throw NotImplementedError("Implement as needed in tests")
    }

    override suspend fun incrementAreaCount(serviceId: String, areaCountId: String, amount: Int) {
        val currentCount = areaCounts.value[areaCountId] ?: 0
        areaCounts.value = areaCounts.value + (areaCountId to currentCount + amount)
    }

    override suspend fun updateServiceCount(
        serviceId: String,
        areaCountId: String,
        newCount: Int,
        action: String
    ) {
        areaCounts.value = areaCounts.value + (areaCountId to newCount)
    }

    override suspend fun lockService(serviceId: String) {
        val service = services.value[serviceId]
        service?.let {
            val updated = it.copy(service = it.service.copy(isLocked = true))
            services.value = services.value + (serviceId to updated)
        }
    }

    override suspend fun unlockService(serviceId: String) {
        val service = services.value[serviceId]
        service?.let {
            val updated = it.copy(service = it.service.copy(isLocked = false))
            services.value = services.value + (serviceId to updated)
        }
    }

    override suspend fun updateServiceNotes(serviceId: String, notes: String) {
        val service = services.value[serviceId]
        service?.let {
            val updated = it.copy(service = it.service.copy(notes = notes))
            services.value = services.value + (serviceId to updated)
        }
    }

    override suspend fun deleteService(serviceId: String) {
        services.value = services.value - serviceId
    }

    override suspend fun exportServiceReport(serviceId: String): String {
        return "Test Report"
    }

    override suspend fun decrementAreaCount(serviceId: String, areaCountId: String, amount: Int) {
        incrementAreaCount(serviceId, areaCountId, -amount)
    }

    override suspend fun resetAreaCount(serviceId: String, areaCountId: String) {
        areaCounts.value = areaCounts.value + (areaCountId to 0)
    }

    override fun getRecentServicesWithAreaCounts(limit: Int): Flow<List<ServiceWithAreaCounts>> {
        throw NotImplementedError("Implement as needed in tests")
    }

    override suspend fun exportVenueComparisonReport(
        branchIds: List<String>,
        startDate: Long,
        endDate: Long
    ): String {
        return "Test Comparison Report"
    }
}
