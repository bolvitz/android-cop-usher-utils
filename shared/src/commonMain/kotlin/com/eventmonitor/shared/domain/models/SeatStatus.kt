package com.eventmonitor.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class SeatStatus(val displayName: String, val colorHex: String) {
    AVAILABLE("Available", "#4CAF50"),      // Green
    OCCUPIED("Occupied", "#F44336"),        // Red
    RESERVED("Reserved", "#FF9800"),        // Orange
    BLOCKED("Blocked", "#9E9E9E"),          // Gray
    WHEELCHAIR("Wheelchair", "#2196F3");    // Blue

    companion object {
        fun fromString(value: String): SeatStatus {
            return entries.find { it.name == value } ?: AVAILABLE
        }
    }
}
