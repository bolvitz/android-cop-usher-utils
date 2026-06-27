package com.eventmonitor.shared.data.mappers

import com.eventmonitor.shared.data.local.entities.*
import com.eventmonitor.shared.data.models.*
import com.eventmonitor.shared.util.nowMillis
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val historyJson = Json { ignoreUnknownKeys = true }

// ---------- Venue ----------
fun VenueEntity.toDto() = VenueDto(
    id = id, name = name, location = location, code = code, isActive = isActive,
    logoUrl = logoUrl, color = color, contactPerson = contactPerson,
    contactPhone = contactPhone, contactEmail = contactEmail, timezone = timezone,
    notes = notes, createdAt = createdAt, updatedAt = updatedAt,
    isHeadCountEnabled = isHeadCountEnabled,
    isLostAndFoundEnabled = isLostAndFoundEnabled,
    isIncidentReportingEnabled = isIncidentReportingEnabled
)

fun VenueDto.toEntity() = VenueEntity(
    id = id.ifEmpty { com.eventmonitor.shared.util.newId() },
    name = name, location = location, code = code, isActive = isActive,
    logoUrl = logoUrl, color = color, contactPerson = contactPerson,
    contactPhone = contactPhone, contactEmail = contactEmail, timezone = timezone,
    notes = notes,
    createdAt = if (createdAt == 0L) nowMillis() else createdAt,
    updatedAt = nowMillis(),
    isHeadCountEnabled = isHeadCountEnabled,
    isLostAndFoundEnabled = isLostAndFoundEnabled,
    isIncidentReportingEnabled = isIncidentReportingEnabled
)

// ---------- AreaTemplate ----------
fun AreaTemplateEntity.toDto() = AreaTemplateDto(
    id = id, venueId = venueId, name = name, type = type, capacity = capacity,
    isActive = isActive, displayOrder = displayOrder, color = color, icon = icon,
    notes = notes, hasSeatMap = hasSeatMap, createdAt = createdAt, updatedAt = updatedAt
)

fun AreaTemplateDto.toEntity() = AreaTemplateEntity(
    id = id.ifEmpty { com.eventmonitor.shared.util.newId() },
    venueId = venueId, name = name, type = type, capacity = capacity,
    isActive = isActive, displayOrder = displayOrder, color = color, icon = icon,
    notes = notes, hasSeatMap = hasSeatMap,
    createdAt = if (createdAt == 0L) nowMillis() else createdAt,
    updatedAt = nowMillis()
)

// ---------- EventType ----------
fun EventTypeEntity.toDto() = EventTypeDto(
    id = id, name = name, description = description,
    isActive = isActive, displayOrder = displayOrder
)

/** Note: EventTypeDto is a simplified projection; dayType/time are preserved
 *  from [existing] when updating an event type. */
fun EventTypeDto.toEntity(existing: EventTypeEntity? = null) = EventTypeEntity(
    id = id.ifEmpty { com.eventmonitor.shared.util.newId() },
    name = name,
    dayType = existing?.dayType ?: "",
    time = existing?.time ?: "",
    description = description,
    isActive = isActive,
    displayOrder = displayOrder,
    createdAt = existing?.createdAt ?: nowMillis(),
    updatedAt = nowMillis()
)

// ---------- Event ----------
fun EventEntity.toDto() = EventDto(
    id = id, venueId = venueId, eventTypeId = eventTypeId, date = date,
    eventType = eventType, eventName = eventName, totalAttendance = totalAttendance,
    totalCapacity = totalCapacity, notes = notes, weather = weather,
    countedBy = countedBy, countedByUserId = countedByUserId, isLocked = isLocked,
    isSpecialEvent = isSpecialEvent, createdAt = createdAt, updatedAt = updatedAt,
    completedAt = completedAt
)

fun EventDto.toEntity() = EventEntity(
    id = id.ifEmpty { com.eventmonitor.shared.util.newId() },
    venueId = venueId, eventTypeId = eventTypeId, date = date, eventType = eventType,
    eventName = eventName, totalAttendance = totalAttendance, totalCapacity = totalCapacity,
    notes = notes, weather = weather, countedBy = countedBy, countedByUserId = countedByUserId,
    isLocked = isLocked, isSpecialEvent = isSpecialEvent,
    createdAt = if (createdAt == 0L) nowMillis() else createdAt,
    updatedAt = nowMillis(), completedAt = completedAt
)

// ---------- AreaCount ----------
fun AreaCountEntity.toDto(): AreaCountDto = AreaCountDto(
    id = id, eventId = eventId, areaTemplateId = areaTemplateId, count = count,
    capacity = capacity, notes = notes,
    countHistory = if (countHistory.isEmpty()) emptyList()
        else historyJson.decodeFromString(countHistory),
    lastUpdated = lastUpdated
)

fun AreaCountDto.toEntity(): AreaCountEntity = AreaCountEntity(
    id = id.ifEmpty { com.eventmonitor.shared.util.newId() },
    eventId = eventId, areaTemplateId = areaTemplateId, count = count, capacity = capacity,
    notes = notes,
    countHistory = if (countHistory.isEmpty()) "" else historyJson.encodeToString(countHistory),
    lastUpdated = nowMillis()
)

// ---------- Relations ----------
fun EventWithDetails.toDto(): com.eventmonitor.shared.data.models.EventWithDetails =
    com.eventmonitor.shared.data.models.EventWithDetails(
        event = event.toDto(),
        venue = venue.toDto(),
        eventType = eventType?.toDto(),
        areaCounts = emptyList()
    )

fun EventWithAreaCounts.toDto(): com.eventmonitor.shared.data.models.EventWithDetails =
    com.eventmonitor.shared.data.models.EventWithDetails(
        event = event.toDto(),
        venue = venue.toDto(),
        eventType = eventType?.toDto(),
        areaCounts = areaCounts.map { it.toDto() }
    )

fun AreaCountWithTemplate.toDto(): com.eventmonitor.shared.data.models.AreaCountWithTemplate =
    com.eventmonitor.shared.data.models.AreaCountWithTemplate(
        areaCount = areaCount.toDto(),
        template = template.toDto()
    )
