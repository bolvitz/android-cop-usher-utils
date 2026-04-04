package com.eventmonitor.shared.data.models

import kotlinx.serialization.Serializable

@Serializable
data class VenueDto(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val code: String = "",
    val isActive: Boolean = true,
    val logoUrl: String = "",
    val color: String = "#1976D2",
    val contactPerson: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val timezone: String = "UTC",
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isHeadCountEnabled: Boolean = true,
    val isLostAndFoundEnabled: Boolean = false,
    val isIncidentReportingEnabled: Boolean = false
)
