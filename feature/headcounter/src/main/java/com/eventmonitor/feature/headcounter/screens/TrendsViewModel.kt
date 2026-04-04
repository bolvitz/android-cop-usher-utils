package com.eventmonitor.feature.headcounter.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.EventWithAreaCounts
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.data.repository.interfaces.EventRepository
import com.eventmonitor.core.data.repository.interfaces.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TrendsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val venueRepository: VenueRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val venueId: String? = savedStateHandle.get<String>("venueId")

    private val _uiState = MutableStateFlow(TrendsUiState())
    val uiState: StateFlow<TrendsUiState> = _uiState.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(TrendPeriod.LAST_30_DAYS)
    val selectedPeriod: StateFlow<TrendPeriod> = _selectedPeriod.asStateFlow()

    init {
        loadTrends()
    }

    fun selectPeriod(period: TrendPeriod) {
        _selectedPeriod.value = period
        loadTrends()
    }

    private fun loadTrends() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val period = _selectedPeriod.value
            val calendar = Calendar.getInstance()
            val endDate = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -period.days)
            val startDate = calendar.timeInMillis

            val eventsFlow = if (venueId != null) {
                eventRepository.getEventsByVenueAndDateRange(venueId, startDate, endDate)
            } else {
                eventRepository.getEventsAcrossVenues(startDate, endDate)
            }

            eventsFlow.collect { events ->
                if (events.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, isEmpty = true) }
                    return@collect
                }

                val sortedEvents = events.sortedBy { it.event.date }

                // Attendance over time data points
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                val attendancePoints = sortedEvents.map { event ->
                    TrendDataPoint(
                        label = dateFormat.format(Date(event.event.date)),
                        value = event.event.totalAttendance.toFloat(),
                        date = event.event.date,
                        eventName = event.event.eventName.ifEmpty {
                            event.eventType?.name ?: "Event"
                        }
                    )
                }

                // Summary stats
                val totalEvents = events.size
                val avgAttendance = events.map { it.event.totalAttendance }.average()
                val peakAttendance = events.maxOf { it.event.totalAttendance }
                val peakEvent = events.maxByOrNull { it.event.totalAttendance }
                val lowestAttendance = events.minOf { it.event.totalAttendance }

                // Growth: compare first half vs second half
                val midpoint = events.size / 2
                val firstHalfAvg = if (midpoint > 0) {
                    events.take(midpoint).map { it.event.totalAttendance }.average()
                } else 0.0
                val secondHalfAvg = if (midpoint > 0) {
                    events.drop(midpoint).map { it.event.totalAttendance }.average()
                } else 0.0
                val growthPercentage = if (firstHalfAvg > 0) {
                    ((secondHalfAvg - firstHalfAvg) / firstHalfAvg * 100).toInt()
                } else 0

                // Capacity utilization
                val avgCapacityPercent = events
                    .filter { it.event.totalCapacity > 0 }
                    .map { (it.event.totalAttendance.toFloat() / it.event.totalCapacity * 100).toInt() }
                    .average()
                    .takeIf { !it.isNaN() }
                    ?.toInt() ?: 0

                // Events by event type
                val byEventType = events.groupBy {
                    it.eventType?.name ?: it.event.eventName.ifEmpty { "Other" }
                }.map { (type, typeEvents) ->
                    EventTypeBreakdown(
                        typeName = type,
                        count = typeEvents.size,
                        avgAttendance = typeEvents.map { it.event.totalAttendance }.average().toInt()
                    )
                }.sortedByDescending { it.avgAttendance }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEmpty = false,
                        attendanceOverTime = attendancePoints,
                        totalEvents = totalEvents,
                        averageAttendance = avgAttendance.toInt(),
                        peakAttendance = peakAttendance,
                        peakEventName = peakEvent?.event?.eventName?.ifEmpty {
                            peakEvent.eventType?.name
                        } ?: "",
                        peakEventDate = peakEvent?.event?.date ?: 0L,
                        lowestAttendance = lowestAttendance,
                        growthPercentage = growthPercentage,
                        avgCapacityUtilization = avgCapacityPercent,
                        eventTypeBreakdown = byEventType,
                        venueName = events.firstOrNull()?.venue?.name ?: ""
                    )
                }
            }
        }
    }
}

data class TrendsUiState(
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val attendanceOverTime: List<TrendDataPoint> = emptyList(),
    val totalEvents: Int = 0,
    val averageAttendance: Int = 0,
    val peakAttendance: Int = 0,
    val peakEventName: String = "",
    val peakEventDate: Long = 0L,
    val lowestAttendance: Int = 0,
    val growthPercentage: Int = 0,
    val avgCapacityUtilization: Int = 0,
    val eventTypeBreakdown: List<EventTypeBreakdown> = emptyList(),
    val venueName: String = ""
)

data class TrendDataPoint(
    val label: String,
    val value: Float,
    val date: Long,
    val eventName: String
)

data class EventTypeBreakdown(
    val typeName: String,
    val count: Int,
    val avgAttendance: Int
)

enum class TrendPeriod(val label: String, val days: Int) {
    LAST_7_DAYS("7 Days", 7),
    LAST_30_DAYS("30 Days", 30),
    LAST_90_DAYS("90 Days", 90),
    LAST_YEAR("1 Year", 365)
}
