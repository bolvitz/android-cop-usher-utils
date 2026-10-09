package com.eventmonitor.shared.presentation.headcounter

import com.eventmonitor.shared.data.repository.AreaCountRepository
import com.eventmonitor.shared.data.repository.EventRepository
import com.eventmonitor.shared.data.repository.SeatMapRepository
import com.eventmonitor.shared.util.nowMillis
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** One area line on the receipt. Excluded areas are listed but left out of the totals. */
data class HeadCountReportLine(
    val name: String,
    val count: Int,
    val capacity: Int,
    val notes: String,
    val isIncluded: Boolean
)

/** Receipt-style summary of one head count; [text] is the plain-text copy for sharing. */
data class HeadCountReport(
    val eventId: String,
    val reference: String,
    val venueName: String,
    val venueLocation: String,
    val lines: List<HeadCountReportLine>,
    val totalCount: Int,
    val totalCapacity: Int,
    val utilizationPercent: Int,
    val eventNotes: String,
    val weather: String,
    val eventName: String,
    val dateText: String,
    val timeText: String,
    val countedBy: String,
    val generatedText: String
) {
    val text: String
        get() = buildString {
            appendLine("HEAD COUNT REPORT")
            appendLine(venueName.uppercase())
            if (venueLocation.isNotEmpty()) appendLine(venueLocation)
            appendLine(RULE)
            appendLine()
            appendLine("AREA BREAKDOWN")
            appendLine()
            lines.forEach { line ->
                val name = if (line.isIncluded) line.name else "${line.name} (excluded)"
                val count = line.count.toString()
                val gap = WIDTH - name.length - count.length
                appendLine(name + (if (gap > 0) " ".repeat(gap) else "  ") + count)
                if (line.notes.isNotEmpty()) appendLine("Note:        ${line.notes}")
                appendLine(".".repeat(WIDTH))
            }
            appendLine(RULE)
            appendLine()
            appendLine("TOTAL")
            appendLine()
            appendLine("Total Count:     $totalCount")
            appendLine("Total Capacity:  $totalCapacity")
            appendLine("Utilization:     $utilizationPercent%")
            appendLine(RULE)
            appendLine()
            if (eventNotes.isNotEmpty()) {
                appendLine("EVENT NOTES")
                appendLine(eventNotes)
                appendLine()
            }
            if (weather.isNotEmpty()) {
                appendLine("Weather: $weather")
                appendLine()
            }
            appendLine("Event:       $eventName")
            appendLine("Date:        $dateText")
            appendLine("Time:        $timeText")
            appendLine("Counted By:  $countedBy")
            appendLine()
            appendLine("Generated: $generatedText")
            append("ID: $reference")
        }

    private companion object {
        const val WIDTH = 40
        val RULE = "_".repeat(WIDTH)
    }
}

/** Builds a [HeadCountReport] from the stored event, its area counts and seat-map occupancy. */
class HeadCountReportBuilder(
    private val eventRepository: EventRepository,
    private val areaCountRepository: AreaCountRepository,
    private val seatMapRepository: SeatMapRepository
) {
    /**
     * @param excludedAreaCountIds areas the counter has toggled out of the total, so the
     * receipt's total matches what the counter shows.
     */
    suspend fun build(eventId: String, excludedAreaCountIds: Set<String> = emptySet()): HeadCountReport? {
        val details = eventRepository.getEventById(eventId).first() ?: return null
        val event = details.event
        val seatCounts = seatMapRepository.observeOccupiedCountsByArea(eventId).first()
        val lines = areaCountRepository.getAreaCountsByEvent(eventId).first()
            .sortedBy { it.template.displayOrder }
            .map { ac ->
                HeadCountReportLine(
                    name = ac.template.name,
                    // Seat-mapped areas are counted by occupied seats, like the counter.
                    count = if (ac.template.hasSeatMap) seatCounts[ac.template.id] ?: 0 else ac.areaCount.count,
                    capacity = ac.areaCount.capacity,
                    notes = ac.areaCount.notes,
                    isIncluded = ac.areaCount.id !in excludedAreaCountIds
                )
            }
        val included = lines.filter { it.isIncluded }
        val total = included.sumOf { it.count }
        val capacity = included.sumOf { it.capacity }
        val date = event.date.toLocal()
        return HeadCountReport(
            eventId = eventId,
            reference = eventId.take(8).uppercase(),
            venueName = details.venue.name,
            venueLocation = details.venue.location,
            lines = lines,
            totalCount = total,
            totalCapacity = capacity,
            utilizationPercent = if (capacity > 0) (total.toFloat() / capacity * 100).toInt() else 0,
            eventNotes = event.notes,
            weather = event.weather,
            eventName = event.eventName.ifEmpty { details.eventType?.name ?: "Head Count" },
            dateText = "${MONTHS[date.monthNumber - 1]} ${date.dayOfMonth}, ${date.year}",
            timeText = date.hhmm(),
            countedBy = event.countedBy,
            generatedText = nowMillis().toLocal().let {
                "${it.monthNumber.pad()}/${it.dayOfMonth.pad()}/${(it.year % 100).pad()} ${it.hhmm()}"
            }
        )
    }

    private fun Long.toLocal(): LocalDateTime =
        Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())

    private fun LocalDateTime.hhmm() = "${hour.pad()}:${minute.pad()}"

    private fun Int.pad() = toString().padStart(2, '0')

    private companion object {
        val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
}
