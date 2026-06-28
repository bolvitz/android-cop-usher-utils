package com.eventmonitor.shared.presentation.reports

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.EventTypeDto
import com.eventmonitor.shared.data.models.EventWithDetails
import com.eventmonitor.shared.data.models.VenueDto
import com.eventmonitor.shared.data.repository.EventRepository
import com.eventmonitor.shared.data.repository.EventTypeRepository
import com.eventmonitor.shared.data.repository.VenueRepository
import com.eventmonitor.shared.platform.FileExporter
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

enum class ReportPeriod(val displayName: String) {
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    LAST_90_DAYS("Last 90 Days"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time");

    fun dateRange(): Pair<Long, Long> {
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val end = now.toEpochMilliseconds()
        val start = when (this) {
            LAST_7_DAYS -> (now - 7.days).toEpochMilliseconds()
            LAST_30_DAYS -> (now - 30.days).toEpochMilliseconds()
            LAST_90_DAYS -> (now - 90.days).toEpochMilliseconds()
            THIS_YEAR -> {
                val year = now.toLocalDateTime(tz).year
                LocalDate(year, 1, 1).atStartOfDayIn(tz).toEpochMilliseconds()
            }
            ALL_TIME -> 0L
        }
        return start to end
    }
}

data class AreaStatistics(
    val areaName: String,
    val totalCount: Int,
    val averageCount: Int,
    val maxCount: Int,
    val minCount: Int,
    val capacity: Int,
    val eventsCount: Int
)

data class ReportData(
    val totalEvents: Int = 0,
    val totalAttendance: Int = 0,
    val averageAttendance: Int = 0,
    val areaStatistics: List<AreaStatistics> = emptyList()
)

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.LAST_30_DAYS,
    val selectedVenueId: String? = null,
    val selectedEventTypeId: String? = null,
    val venues: List<VenueDto> = emptyList(),
    val eventTypes: List<EventTypeDto> = emptyList(),
    val data: ReportData = ReportData(),
    val exportMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val eventRepository: EventRepository,
    private val venueRepository: VenueRepository,
    private val eventTypeRepository: EventTypeRepository,
    private val fileExporter: FileExporter
) : SharedViewModel() {

    private val period = MutableStateFlow(ReportPeriod.LAST_30_DAYS)
    private val venueId = MutableStateFlow<String?>(null)
    private val eventTypeId = MutableStateFlow<String?>(null)
    private val exportMessage = MutableStateFlow<String?>(null)

    private val filteredEvents: StateFlow<List<EventWithDetails>> =
        combine(period, venueId, eventTypeId) { p, v, t -> Triple(p, v, t) }
            .flatMapLatest { (p, v, t) ->
                val (start, end) = p.dateRange()
                eventRepository.getEventsWithAreaCountsByDateRange(start, end).map { events ->
                    events.filter { v == null || it.event.venueId == v }
                        .filter { t == null || it.event.eventTypeId == t }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<ReportsUiState> =
        combine(
            filteredEvents,
            venueRepository.getAllVenues(),
            eventTypeRepository.getActiveEventTypes(),
            combine(period, venueId, eventTypeId, exportMessage) { p, v, t, m -> Quad(p, v, t, m) }
        ) { events, venues, types, sel ->
            ReportsUiState(
                period = sel.a,
                selectedVenueId = sel.b,
                selectedEventTypeId = sel.c,
                venues = venues,
                eventTypes = types,
                data = aggregate(events),
                exportMessage = sel.d
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    fun selectPeriod(p: ReportPeriod) { period.value = p }
    fun selectVenue(id: String?) { venueId.value = id }
    fun selectEventType(id: String?) { eventTypeId.value = id }
    fun clearExportMessage() { exportMessage.value = null }

    fun exportCsv() {
        viewModelScope.launch {
            val csv = buildCsv(filteredEvents.value)
            val result = fileExporter.exportCsv("event_report.csv", csv)
            exportMessage.value = result.fold(
                onSuccess = { "Exported to $it" },
                onFailure = { "Export failed: ${it.message}" }
            )
        }
    }

    private fun aggregate(events: List<EventWithDetails>): ReportData {
        if (events.isEmpty()) return ReportData()
        val totalEvents = events.size
        val totalAttendance = events.sumOf { it.event.totalAttendance }
        val stats = mutableMapOf<String, AreaStatistics>()
        events.forEach { e ->
            e.areaCounts.forEach { ac ->
                val name = ac.template.name
                val cur = stats[name] ?: AreaStatistics(name, 0, 0, 0, Int.MAX_VALUE, ac.areaCount.capacity, 0)
                stats[name] = cur.copy(
                    totalCount = cur.totalCount + ac.areaCount.count,
                    maxCount = maxOf(cur.maxCount, ac.areaCount.count),
                    minCount = minOf(cur.minCount, ac.areaCount.count),
                    eventsCount = cur.eventsCount + 1
                )
            }
        }
        val areaStats = stats.values.map {
            it.copy(
                averageCount = if (it.eventsCount > 0) it.totalCount / it.eventsCount else 0,
                minCount = if (it.minCount == Int.MAX_VALUE) 0 else it.minCount
            )
        }.sortedBy { it.areaName }
        return ReportData(totalEvents, totalAttendance, totalAttendance / totalEvents, areaStats)
    }

    private fun buildCsv(events: List<EventWithDetails>): String {
        val sb = StringBuilder()
        sb.appendLine("Venue,Event,Date,Area,Count,Capacity")
        events.forEach { e ->
            val eventName = e.event.eventName.ifEmpty { e.eventType?.name ?: "Event" }
            if (e.areaCounts.isEmpty()) {
                sb.appendLine(csvRow(e.venue.name, eventName, e.event.date.toString(), "", e.event.totalAttendance.toString(), e.event.totalCapacity.toString()))
            } else {
                e.areaCounts.forEach { ac ->
                    sb.appendLine(csvRow(e.venue.name, eventName, e.event.date.toString(), ac.template.name, ac.areaCount.count.toString(), ac.areaCount.capacity.toString()))
                }
            }
        }
        return sb.toString()
    }

    private fun csvRow(vararg fields: String): String =
        fields.joinToString(",") { f -> "\"${f.replace("\"", "\"\"")}\"" }

    private data class Quad(val a: ReportPeriod, val b: String?, val c: String?, val d: String?)
}
