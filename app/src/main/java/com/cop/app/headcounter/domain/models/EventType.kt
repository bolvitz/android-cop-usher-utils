package com.cop.app.headcounter.domain.models

enum class EventType(val displayName: String) {
    GENERAL("General Event"),
    CONFERENCE("Conference"),
    WORKSHOP("Workshop"),
    SEMINAR("Seminar"),
    MEETING("Meeting"),
    EXHIBITION("Exhibition"),
    CONCERT("Concert"),
    SPORTS("Sports Event"),
    FESTIVAL("Festival"),
    CEREMONY("Ceremony"),
    TRAINING("Training Session"),
    NETWORKING("Networking Event"),
    CUSTOM("Custom Event");

    companion object {
        fun getDefault() = GENERAL

        fun fromString(value: String): EventType {
            return entries.find { it.name == value } ?: getDefault()
        }
    }
}
