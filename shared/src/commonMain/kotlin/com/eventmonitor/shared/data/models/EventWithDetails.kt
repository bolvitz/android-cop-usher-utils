package com.eventmonitor.shared.data.models

import kotlinx.serialization.Serializable

@Serializable
data class EventWithDetails(
    val event: EventDto,
    val venue: VenueDto,
    val eventType: EventTypeDto? = null,
    val areaCounts: List<AreaCountWithTemplate> = emptyList()
) {
    val attendancePercentage: Float
        get() = if (event.totalCapacity > 0) {
            (event.totalAttendance.toFloat() / event.totalCapacity) * 100
        } else 0f

    val isComplete: Boolean
        get() = event.totalAttendance > 0 && event.isLocked
}

@Serializable
data class AreaCountWithTemplate(
    val areaCount: AreaCountDto,
    val template: AreaTemplateDto
) {
    val fillPercentage: Float
        get() = if (areaCount.capacity > 0) {
            (areaCount.count.toFloat() / areaCount.capacity) * 100
        } else 0f

    val displayName: String
        get() = template.name
}
