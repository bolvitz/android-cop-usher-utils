package com.eventmonitor.shared.data.models

import kotlinx.serialization.Serializable

@Serializable
data class EventTypeDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)
