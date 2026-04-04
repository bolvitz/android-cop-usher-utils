package com.eventmonitor.shared.data.models

import kotlinx.serialization.Serializable

@Serializable
data class AreaCountDto(
    val id: String = "",
    val eventId: String = "",
    val areaTemplateId: String = "",
    val count: Int = 0,
    val capacity: Int = 0,
    val notes: String = "",
    val countHistory: List<CountHistoryItem> = emptyList(),
    val lastUpdated: Long = 0L
)

@Serializable
data class CountHistoryItem(
    val timestamp: Long,
    val oldCount: Int,
    val newCount: Int,
    val action: String
)
