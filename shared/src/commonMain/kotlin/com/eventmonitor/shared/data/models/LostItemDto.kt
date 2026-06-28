package com.eventmonitor.shared.data.models

import kotlinx.serialization.Serializable

@Serializable
data class LostItemDto(
    val id: String = "",
    val locationId: String = "",
    val eventId: String? = null,
    val description: String = "",
    val category: String = "OTHER",
    val foundZone: String = "",
    val foundDate: Long = 0L,
    val photoUri: String = "",
    val color: String = "",
    val brand: String = "",
    val identifyingMarks: String = "",
    val status: String = "PENDING",
    val claimedBy: String = "",
    val claimedDate: Long = 0L,
    val claimerContact: String = "",
    val verificationNotes: String = "",
    val reportedBy: String = "",
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
