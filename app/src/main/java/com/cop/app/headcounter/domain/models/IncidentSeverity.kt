package com.cop.app.headcounter.domain.models

enum class IncidentSeverity(val displayName: String, val color: String) {
    LOW("Low", "#66BB6A"),           // Green
    MEDIUM("Medium", "#FFA726"),      // Orange
    HIGH("High", "#FF7043"),          // Deep Orange
    CRITICAL("Critical", "#EF5350");  // Red

    companion object {
        fun fromString(value: String): IncidentSeverity {
            return entries.find { it.name == value } ?: LOW
        }
    }
}
