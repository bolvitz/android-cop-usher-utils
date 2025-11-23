package com.cop.app.headcounter.domain.models

enum class IncidentStatus(val displayName: String, val color: String) {
    REPORTED("Reported", "#FFA726"),      // Orange
    INVESTIGATING("Investigating", "#42A5F5"), // Blue
    IN_PROGRESS("In Progress", "#AB47BC"),     // Purple
    RESOLVED("Resolved", "#66BB6A"),       // Green
    CLOSED("Closed", "#78909C");           // Grey

    companion object {
        fun fromString(value: String): IncidentStatus {
            return entries.find { it.name == value } ?: REPORTED
        }
    }
}
