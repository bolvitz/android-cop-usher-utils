package com.cop.app.headcounter.data.repository

import androidx.room.withTransaction
import com.cop.app.headcounter.data.local.dao.AreaCountDao
import com.cop.app.headcounter.data.local.dao.AreaTemplateDao
import com.cop.app.headcounter.data.local.dao.BranchDao
import com.cop.app.headcounter.data.local.dao.ServiceDao
import com.cop.app.headcounter.data.local.database.AppDatabase
import com.cop.app.headcounter.data.local.entities.AreaCountEntity
import com.cop.app.headcounter.data.local.entities.ServiceEntity
import com.cop.app.headcounter.data.local.entities.ServiceWithDetails
import com.cop.app.headcounter.data.local.entities.ServiceWithAreaCounts
import com.cop.app.headcounter.data.models.CountHistoryItem
import com.cop.app.headcounter.domain.models.ServiceType
import com.cop.app.headcounter.domain.repository.ServiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ServiceRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val serviceDao: ServiceDao,
    private val areaCountDao: AreaCountDao,
    private val areaTemplateDao: AreaTemplateDao,
    private val branchDao: BranchDao
) : ServiceRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getRecentServices(limit: Int): Flow<List<ServiceWithDetails>> =
        serviceDao.getRecentServices(limit)

    override fun getRecentServicesByBranch(branchId: String, limit: Int): Flow<List<ServiceWithDetails>> =
        serviceDao.getRecentServicesByBranch(branchId, limit)

    override fun getServiceById(id: String): Flow<ServiceWithDetails?> =
        serviceDao.getServiceById(id)

    override fun getServicesByBranchAndDateRange(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithDetails>> =
        serviceDao.getServicesByBranchAndDateRange(branchId, startDate, endDate)

    override fun getServicesAcrossBranches(
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithDetails>> =
        serviceDao.getServicesAcrossBranchesByDateRange(startDate, endDate)

    override fun getAverageAttendance(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?> =
        serviceDao.getAverageAttendance(branchId, startDate, endDate)

    override fun getRecentServicesWithAreaCounts(limit: Int): Flow<List<ServiceWithAreaCounts>> =
        serviceDao.getRecentServicesWithAreaCounts(limit)

    override fun getServicesWithAreaCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<ServiceWithAreaCounts>> =
        serviceDao.getServicesWithAreaCountsByDateRange(startDate, endDate)

    override suspend fun createNewService(
        branchId: String,
        serviceType: ServiceType,
        date: Long,
        countedBy: String,
        serviceName: String,
        serviceTypeId: String?
    ): String {
        val serviceId = UUID.randomUUID().toString()

        // Get all active areas for this branch
        val areas = areaTemplateDao.getAreasByBranch(branchId).first()
        val totalCapacity = areas.sumOf { it.capacity }

        val service = ServiceEntity(
            id = serviceId,
            branchId = branchId,
            serviceTypeId = serviceTypeId,
            date = date,
            serviceType = serviceType.name,
            serviceName = serviceName,
            totalCapacity = totalCapacity,
            countedBy = countedBy
        )

        serviceDao.insertService(service)

        // Create area counts for all areas
        val areaCounts = areas.map { area ->
            AreaCountEntity(
                serviceId = serviceId,
                areaTemplateId = area.id,
                count = 0,
                capacity = area.capacity,
                countHistory = json.encodeToString(emptyList<CountHistoryItem>())
            )
        }

        areaCountDao.insertAreaCounts(areaCounts)

        return serviceId
    }

    override suspend fun updateServiceCount(
        serviceId: String,
        areaCountId: String,
        newCount: Int,
        action: String
    ) {
        // Wrap both updates in a transaction to prevent flickering from separate Flow emissions
        database.withTransaction {
            val areaCount = areaCountDao.getAreaCountById(areaCountId).first() ?: return@withTransaction

            val historyItem = CountHistoryItem(
                timestamp = System.currentTimeMillis(),
                oldCount = areaCount.count,
                newCount = newCount,
                action = action
            )

            val currentHistory = if (areaCount.countHistory.isEmpty()) {
                emptyList()
            } else {
                json.decodeFromString<List<CountHistoryItem>>(areaCount.countHistory)
            }
            val updatedHistory = currentHistory + historyItem

            areaCountDao.updateAreaCount(
                areaCount.copy(
                    count = newCount,
                    countHistory = json.encodeToString(updatedHistory),
                    lastUpdated = System.currentTimeMillis()
                )
            )

            updateServiceTotal(serviceId)
        }
    }

    override suspend fun incrementAreaCount(serviceId: String, areaCountId: String, amount: Int) {
        val areaCount = areaCountDao.getAreaCountById(areaCountId).first() ?: return
        val newCount = (areaCount.count + amount).coerceAtLeast(0)
        updateServiceCount(serviceId, areaCountId, newCount, if (amount > 0) "INCREMENT" else "DECREMENT")
    }

    override suspend fun decrementAreaCount(serviceId: String, areaCountId: String, amount: Int) {
        incrementAreaCount(serviceId, areaCountId, -amount)
    }

    override suspend fun resetAreaCount(serviceId: String, areaCountId: String) {
        updateServiceCount(serviceId, areaCountId, 0, "RESET")
    }

    override suspend fun updateServiceNotes(serviceId: String, notes: String) {
        val service = serviceDao.getServiceById(serviceId).first()?.service ?: return
        serviceDao.updateService(service.copy(notes = notes, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun lockService(serviceId: String) {
        val service = serviceDao.getServiceById(serviceId).first()?.service ?: return
        serviceDao.updateService(
            service.copy(
                isLocked = true,
                completedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun unlockService(serviceId: String) {
        val service = serviceDao.getServiceById(serviceId).first()?.service ?: return
        serviceDao.updateService(
            service.copy(
                isLocked = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteService(serviceId: String) {
        serviceDao.deleteServiceById(serviceId)
    }

    override suspend fun exportServiceReport(serviceId: String): String {
        val serviceWithDetails = serviceDao.getServiceById(serviceId).first() ?: return ""

        val service = serviceWithDetails.service
        val branch = serviceWithDetails.branch
        val areaCounts = areaCountDao.getAreaCountsByService(serviceId).first()

        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val serviceTypeName = ServiceType.fromString(service.serviceType).displayName

        return buildString {
            appendLine("ATTENDANCE REPORT")
            appendLine("=".repeat(40))
            appendLine("Branch: ${branch.name}")
            appendLine("Date: ${dateFormat.format(Date(service.date))}")
            appendLine("Service: $serviceTypeName")
            if (service.serviceName.isNotEmpty()) {
                appendLine("Event: ${service.serviceName}")
            }
            appendLine("Counted by: ${service.countedBy}")
            appendLine()

            appendLine("AREA BREAKDOWN")
            appendLine("-".repeat(40))

            areaCounts
                .sortedBy { it.template.displayOrder }
                .forEach { areaCountWithTemplate ->
                    val area = areaCountWithTemplate.areaCount
                    val template = areaCountWithTemplate.template

                    // Create a readable format with dots connecting name to count
                    val maxNameLength = 28
                    val truncatedName = if (template.name.length > maxNameLength) {
                        template.name.take(maxNameLength - 3) + "..."
                    } else {
                        template.name
                    }

                    val dotsNeeded = maxNameLength - truncatedName.length
                    val dots = ".".repeat(dotsNeeded)

                    appendLine("${truncatedName}${dots} ${area.count.toString().padStart(4)}")

                    if (area.notes.isNotEmpty()) {
                        appendLine("  Note: ${area.notes}")
                    }
                }

            appendLine("-".repeat(40))
            appendLine("TOTAL................... ${service.totalAttendance.toString().padStart(4)}")

            if (service.notes.isNotEmpty()) {
                appendLine()
                appendLine("SERVICE NOTES:")
                appendLine(service.notes)
            }

            if (service.weather.isNotEmpty()) {
                appendLine()
                appendLine("Weather: ${service.weather}")
            }

            appendLine()
            appendLine("Generated: ${timeFormat.format(Date())}")
        }
    }

    override suspend fun exportBranchComparisonReport(
        branchIds: List<String>,
        startDate: Long,
        endDate: Long
    ): String {
        val branches = branchIds.mapNotNull { branchId ->
            branchDao.getBranchById(branchId).first()
        }

        val servicesPerBranch = branches.associate { branch ->
            branch.id to serviceDao.getServicesByBranchAndDateRange(
                branch.id,
                startDate,
                endDate
            ).first()
        }

        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        return buildString {
            appendLine("MULTI-BRANCH ATTENDANCE COMPARISON")
            appendLine("=".repeat(60))
            appendLine("Period: ${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}")
            appendLine()

            branches.forEach { branch ->
                val services = servicesPerBranch[branch.id] ?: emptyList()
                val totalAttendance = services.sumOf { it.service.totalAttendance }
                val avgAttendance = if (services.isNotEmpty()) {
                    totalAttendance / services.size
                } else 0

                appendLine("${branch.name} (${branch.code})")
                appendLine("-".repeat(60))
                appendLine("  Total Services: ${services.size}")
                appendLine("  Total Attendance: $totalAttendance")
                appendLine("  Average Attendance: $avgAttendance")
                appendLine("  Location: ${branch.location}")
                appendLine()
            }

            appendLine("=".repeat(60))
            val grandTotal = servicesPerBranch.values.flatten()
                .sumOf { it.service.totalAttendance }
            appendLine("GRAND TOTAL ATTENDANCE: $grandTotal")
            appendLine()
            appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
        }
    }

    private suspend fun updateServiceTotal(serviceId: String) {
        val areaCounts = areaCountDao.getAreaCountsByService(serviceId).first()
        val total = areaCounts.sumOf { it.areaCount.count }

        val service = serviceDao.getServiceById(serviceId).first()?.service ?: return

        serviceDao.updateService(
            service.copy(
                totalAttendance = total,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
