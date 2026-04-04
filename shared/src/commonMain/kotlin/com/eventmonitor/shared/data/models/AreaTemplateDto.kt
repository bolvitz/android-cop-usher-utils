package com.eventmonitor.shared.data.models

import kotlinx.serialization.Serializable

@Serializable
data class AreaTemplateDto(
    val id: String = "",
    val venueId: String = "",
    val name: String = "",
    val type: String = "",
    val capacity: Int = 100,
    val isActive: Boolean = true,
    val displayOrder: Int = 0,
    val color: String = "#4CAF50",
    val icon: String = "chair",
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
