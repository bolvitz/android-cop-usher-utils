package com.eventmonitor.core.domain.models

enum class ItemStatus(val displayName: String, val color: String) {
    PENDING("Pending", "#FFA726"),      // Orange
    CLAIMED("Claimed", "#66BB6A"),       // Green
    DONATED("Donated", "#42A5F5"),       // Blue
    DISPOSED("Disposed", "#EF5350");     // Red

    companion object {
        fun fromString(value: String): ItemStatus {
            return entries.find { it.name == value } ?: PENDING
        }
    }
}
