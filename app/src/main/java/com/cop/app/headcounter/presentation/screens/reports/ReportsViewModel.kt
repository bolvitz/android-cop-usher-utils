package com.cop.app.headcounter.presentation.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cop.app.headcounter.data.local.entities.ServiceWithAreaCounts
import com.cop.app.headcounter.domain.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.LAST_30_DAYS)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    private val dateRange = _selectedPeriod.map { period ->
        period.getDateRange()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportPeriod.LAST_30_DAYS.getDateRange()
    )

    val servicesWithAreaCounts: StateFlow<List<ServiceWithAreaCounts>> = dateRange.flatMapLatest { (startDate, endDate) ->
        serviceRepository.getServicesWithAreaCountsByDateRange(startDate, endDate)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val reportData: StateFlow<ReportData> = servicesWithAreaCounts.map { services ->
        calculateReportData(services)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportData()
    )

    fun selectPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    private fun calculateReportData(services: List<ServiceWithAreaCounts>): ReportData {
        if (services.isEmpty()) {
            return ReportData()
        }

        val totalServices = services.size
        val totalAttendance = services.sumOf { it.service.totalAttendance }
        val averageAttendance = totalAttendance / totalServices

        // Calculate area statistics
        val areaStats = mutableMapOf<String, AreaStatistics>()

        services.forEach { serviceWithAreas ->
            serviceWithAreas.areaCounts.forEach { areaCount ->
                val areaName = areaCount.template.name
                val currentStats = areaStats[areaName] ?: AreaStatistics(
                    areaName = areaName,
                    totalCount = 0,
                    averageCount = 0,
                    maxCount = 0,
                    minCount = Int.MAX_VALUE,
                    capacity = areaCount.areaCount.capacity,
                    servicesCount = 0
                )

                areaStats[areaName] = currentStats.copy(
                    totalCount = currentStats.totalCount + areaCount.areaCount.count,
                    maxCount = maxOf(currentStats.maxCount, areaCount.areaCount.count),
                    minCount = minOf(currentStats.minCount, areaCount.areaCount.count),
                    servicesCount = currentStats.servicesCount + 1
                )
            }
        }

        // Calculate averages for areas
        val finalAreaStats = areaStats.values.map { stats ->
            stats.copy(
                averageCount = stats.totalCount / stats.servicesCount,
                minCount = if (stats.minCount == Int.MAX_VALUE) 0 else stats.minCount
            )
        }.sortedByDescending { it.totalCount }

        return ReportData(
            totalServices = totalServices,
            totalAttendance = totalAttendance,
            averageAttendance = averageAttendance,
            areaStatistics = finalAreaStats
        )
    }
}

data class ReportData(
    val totalServices: Int = 0,
    val totalAttendance: Int = 0,
    val averageAttendance: Int = 0,
    val areaStatistics: List<AreaStatistics> = emptyList()
)

data class AreaStatistics(
    val areaName: String,
    val totalCount: Int,
    val averageCount: Int,
    val maxCount: Int,
    val minCount: Int,
    val capacity: Int,
    val servicesCount: Int
)

enum class ReportPeriod(val displayName: String) {
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time");

    fun getDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        val startDate = when (this) {
            LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                calendar.timeInMillis
            }
            LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                calendar.timeInMillis
            }
            LAST_90_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -90)
                calendar.timeInMillis
            }
            THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            ALL_TIME -> 0L
        }

        return Pair(startDate, endDate)
    }
}
