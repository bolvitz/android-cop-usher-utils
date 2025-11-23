package com.eventmonitor.core.data.repository

import androidx.room.withTransaction
import com.eventmonitor.core.data.local.dao.AreaCountDao
import com.eventmonitor.core.data.local.dao.AreaTemplateDao
import com.eventmonitor.core.data.local.dao.BranchDao
import com.eventmonitor.core.data.local.dao.EventDao
import com.eventmonitor.core.data.local.database.AppDatabase
import com.eventmonitor.core.data.local.entities.AreaCountEntity
import com.eventmonitor.core.data.local.entities.EventEntity
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.data.local.entities.EventWithAreaCounts
import com.eventmonitor.core.data.models.CountHistoryItem
import com.eventmonitor.core.domain.models.ServiceType
import com.eventmonitor.core.data.repository.interfaces.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val eventDao: EventDao,
    private val areaCountDao: AreaCountDao,
    private val areaTemplateDao: AreaTemplateDao,
    private val branchDao: BranchDao
) : EventRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun getRecentServices(limit: Int): Flow<List<EventWithDetails>> =
        eventDao.getRecentServices(limit)

    override fun getRecentServicesByBranch(branchId: String, limit: Int): Flow<List<EventWithDetails>> =
        eventDao.getRecentServicesByBranch(branchId, limit)

    override fun getServiceById(id: String): Flow<EventWithDetails?> =
        eventDao.getServiceById(id)

    override fun getServicesByBranchAndDateRange(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>> =
        eventDao.getServicesByBranchAndDateRange(branchId, startDate, endDate)

    override fun getServicesAcrossBranches(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithDetails>> =
        eventDao.getServicesAcrossBranchesByDateRange(startDate, endDate)

    override fun getAverageAttendance(
        branchId: String,
        startDate: Long,
        endDate: Long
    ): Flow<Double?> =
        eventDao.getAverageAttendance(branchId, startDate, endDate)

    override fun getRecentServicesWithAreaCounts(limit: Int): Flow<List<EventWithAreaCounts>> =
        eventDao.getRecentServicesWithAreaCounts(limit)

    override fun getServicesWithAreaCountsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<EventWithAreaCounts>> =
        eventDao.getServicesWithAreaCountsByDateRange(startDate, endDate)

    override suspend fun createNewService(
        branchId: String,
        eventType: ServiceType,
        date: Long,
        countedBy: String,
        eventName: String,
        eventTypeId: String?
    ): String {
        val eventId = UUID.randomUUID().toString()

        // Get all active areas for this branch
        val areas = areaTemplateDao.getAreasByBranch(branchId).first()
        val totalCapacity = areas.sumOf { it.capacity }

        val service = EventEntity(
            id = eventId,
            branchId = branchId,
            eventTypeId = eventTypeId,
            date = date,
            eventType = eventType.name,
            eventName = eventName,
            totalCapacity = totalCapacity,
            countedBy = countedBy
        )

        eventDao.insertService(service)

        // Create area counts for all areas
        val areaCounts = areas.map { area ->
            AreaCountEntity(
                eventId = eventId,
                areaTemplateId = area.id,
                count = 0,
                capacity = area.capacity,
                countHistory = json.encodeToString(emptyList<CountHistoryItem>())
            )
        }

        areaCountDao.insertAreaCounts(areaCounts)

        return eventId
    }

    override suspend fun updateServiceCount(
        eventId: String,
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

            updateServiceTotal(eventId)
        }
    }

    override suspend fun incrementAreaCount(eventId: String, areaCountId: String, amount: Int) {
        val areaCount = areaCountDao.getAreaCountById(areaCountId).first() ?: return
        val newCount = (areaCount.count + amount).coerceAtLeast(0)
        updateServiceCount(eventId, areaCountId, newCount, if (amount > 0) "INCREMENT" else "DECREMENT")
    }

    override suspend fun decrementAreaCount(eventId: String, areaCountId: String, amount: Int) {
        incrementAreaCount(eventId, areaCountId, -amount)
    }

    override suspend fun resetAreaCount(eventId: String, areaCountId: String) {
        updateServiceCount(eventId, areaCountId, 0, "RESET")
    }

    override suspend fun updateServiceNotes(eventId: String, notes: String) {
        val service = eventDao.getServiceById(eventId).first()?.event ?: return
        eventDao.updateService(service.copy(notes = notes, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun lockService(eventId: String) {
        val service = eventDao.getServiceById(eventId).first()?.event ?: return
        eventDao.updateService(
            service.copy(
                isLocked = true,
                completedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun unlockService(eventId: String) {
        val service = eventDao.getServiceById(eventId).first()?.event ?: return
        eventDao.updateService(
            service.copy(
                isLocked = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteService(eventId: String) {
        eventDao.deleteServiceById(eventId)
    }

    override suspend fun exportServiceReport(eventId: String): String {
        val serviceWithDetails = eventDao.getServiceById(eventId).first() ?: return ""

        val service = serviceWithDetails.event
        val branch = serviceWithDetails.branch
        val areaCounts = areaCountDao.getAreaCountsByService(eventId).first()

        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val eventTypeName = ServiceType.fromString(service.eventType).displayName

        return buildString {
            appendLine("ATTENDANCE REPORT")
            appendLine("=".repeat(40))
            appendLine("Branch: ${branch.name}")
            appendLine("Date: ${dateFormat.format(Date(service.date))}")
            appendLine("Service: $eventTypeName")
            if (service.eventName.isNotEmpty()) {
                appendLine("Event: ${service.eventName}")
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

                    // Create a readable format: "Name: Count" with proper spacing
                    val maxNameLength = 20
                    val truncatedName = if (template.name.length > maxNameLength) {
                        template.name.take(maxNameLength - 3) + "..."
                    } else {
                        template.name.padEnd(maxNameLength)
                    }

                    // Format count with leading zeros for better alignment
                    val countStr = area.count.toString().padStart(3, ' ')

                    appendLine("${truncatedName}    ${countStr}")

                    if (area.notes.isNotEmpty()) {
                        appendLine("  Note: ${area.notes}")
                    }
                }

            appendLine("-".repeat(40))
            appendLine("TOTAL${" ".repeat(19)}${service.totalAttendance.toString().padStart(3, ' ')}")

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
            branch.id to eventDao.getServicesByBranchAndDateRange(
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
                val totalAttendance = services.sumOf { it.event.totalAttendance }
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
                .sumOf { it.event.totalAttendance }
            appendLine("GRAND TOTAL ATTENDANCE: $grandTotal")
            appendLine()
            appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
        }
    }

    private suspend fun updateServiceTotal(eventId: String) {
        val areaCounts = areaCountDao.getAreaCountsByService(eventId).first()
        val total = areaCounts.sumOf { it.areaCount.count }

        val service = eventDao.getServiceById(eventId).first()?.event ?: return

        eventDao.updateService(
            service.copy(
                totalAttendance = total,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
