package com.eventmonitor.shared.presentation.headcounter

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.repository.EventRepository
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

enum class TrendPeriod(val label: String, val days: Int) {
    LAST_7_DAYS("7 Days", 7),
    LAST_30_DAYS("30 Days", 30),
    LAST_90_DAYS("90 Days", 90),
    LAST_YEAR("1 Year", 365)
}

data class TrendDataPoint(val label: String, val value: Float, val date: Long, val eventName: String)

data class EventTypeBreakdown(val typeName: String, val count: Int, val avgAttendance: Int)

data class TrendsUiState(
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val period: TrendPeriod = TrendPeriod.LAST_30_DAYS,
    val attendanceOverTime: List<TrendDataPoint> = emptyList(),
    val totalEvents: Int = 0,
    val averageAttendance: Int = 0,
    val peakAttendance: Int = 0,
    val peakEventName: String = "",
    val lowestAttendance: Int = 0,
    val growthPercentage: Int = 0,
    val avgCapacityUtilization: Int = 0,
    val eventTypeBreakdown: List<EventTypeBreakdown> = emptyList(),
    val venueName: String = ""
)

class TrendsViewModel(
    private val eventRepository: EventRepository,
    private val venueId: String? = null
) : SharedViewModel() {

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadTrends(TrendPeriod.LAST_30_DAYS)
    }

    fun selectPeriod(period: TrendPeriod) = loadTrends(period)

    private fun loadTrends(period: TrendPeriod) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, period = period) }
            val end = Clock.System.now().toEpochMilliseconds()
            val start = (Clock.System.now() - period.days.days).toEpochMilliseconds()

            val flow = if (venueId != null) {
                eventRepository.getEventsByVenueAndDateRange(venueId, start, end)
            } else {
                eventRepository.getEventsAcrossVenues(start, end)
            }

            flow.collectLatest { events ->
                if (events.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, isEmpty = true, period = period) }
                    return@collectLatest
                }
                val sorted = events.sortedBy { it.event.date }
                val points = sorted.map { e ->
                    TrendDataPoint(
                        label = shortDate(e.event.date),
                        value = e.event.totalAttendance.toFloat(),
                        date = e.event.date,
                        eventName = e.event.eventName.ifEmpty { e.eventType?.name ?: "Event" }
                    )
                }
                val attendances = events.map { it.event.totalAttendance }
                val midpoint = events.size / 2
                val firstHalf = if (midpoint > 0) events.take(midpoint).map { it.event.totalAttendance }.average() else 0.0
                val secondHalf = if (midpoint > 0) events.drop(midpoint).map { it.event.totalAttendance }.average() else 0.0
                val growth = if (firstHalf > 0) ((secondHalf - firstHalf) / firstHalf * 100).toInt() else 0
                val capUtil = events.filter { it.event.totalCapacity > 0 }
                    .map { (it.event.totalAttendance.toFloat() / it.event.totalCapacity * 100).toInt() }
                    .let { if (it.isEmpty()) 0 else it.average().toInt() }
                val peak = events.maxByOrNull { it.event.totalAttendance }
                val byType = events.groupBy { it.eventType?.name ?: it.event.eventName.ifEmpty { "Other" } }
                    .map { (type, list) ->
                        EventTypeBreakdown(type, list.size, list.map { it.event.totalAttendance }.average().toInt())
                    }.sortedByDescending { it.avgAttendance }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEmpty = false,
                        period = period,
                        attendanceOverTime = points,
                        totalEvents = events.size,
                        averageAttendance = attendances.average().toInt(),
                        peakAttendance = attendances.max(),
                        peakEventName = peak?.event?.eventName?.ifEmpty { peak.eventType?.name } ?: "",
                        lowestAttendance = attendances.min(),
                        growthPercentage = growth,
                        avgCapacityUtilization = capUtil,
                        eventTypeBreakdown = byType,
                        venueName = events.firstOrNull()?.venue?.name ?: ""
                    )
                }
            }
        }
    }

    private fun shortDate(epochMillis: Long): String {
        val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        return "${months[dt.monthNumber - 1]} ${dt.dayOfMonth}"
    }
}
