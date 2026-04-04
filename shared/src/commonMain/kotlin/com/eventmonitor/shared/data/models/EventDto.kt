package com.eventmonitor.shared.data.models

import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    val id: String = "",
    val venueId: String = "",
    val eventTypeId: String? = null,
    val date: Long = 0L,
    val eventType: String = "",
    val eventName: String = "",
    val totalAttendance: Int = 0,
    val totalCapacity: Int = 0,
    val notes: String = "",
    val weather: String = "",
    val countedBy: String = "",
    val countedByUserId: String = "",
    val isLocked: Boolean = false,
    val isSpecialEvent: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val completedAt: Long? = null
)
