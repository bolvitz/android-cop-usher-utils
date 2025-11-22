package com.cop.app.headcounter.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CountHistoryItem(
    val timestamp: Long,
    val oldCount: Int,
    val newCount: Int,
    val action: String // INCREMENT, DECREMENT, MANUAL_EDIT, RESET
)
