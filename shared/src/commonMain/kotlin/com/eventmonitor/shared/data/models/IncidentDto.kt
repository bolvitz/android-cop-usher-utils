package com.eventmonitor.shared.data.models

import kotlinx.serialization.Serializable

@Serializable
data class IncidentDto(
    val id: String = "",
    val venueId: String = "",
    val eventId: String? = null,
    val title: String = "",
    val description: String = "",
    val severity: String = "LOW",
    val status: String = "REPORTED",
    val category: String = "",
    val location: String = "",
    val photoUri: String = "",
    val reportedBy: String = "",
    val assignedTo: String = "",
    val reportedAt: Long = 0L,
    val resolvedAt: Long? = null,
    val notes: String = "",
    val actionsTaken: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
