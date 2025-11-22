package com.cop.app.headcounter.domain.models

enum class ServiceType(val displayName: String) {
    FRIDAY("Friday Evening"),
    SATURDAY_AM("Saturday Morning"),
    SATURDAY_PM("Saturday Afternoon"),
    SUNDAY_AM("Sunday Morning"),
    SUNDAY_PM("Sunday Afternoon"),
    SUNDAY_EVENING("Sunday Evening"),
    MIDWEEK("Midweek Service"),
    SPECIAL("Special Event");

    companion object {
        fun getDefault() = SUNDAY_AM

        fun fromString(value: String): ServiceType {
            return entries.find { it.name == value } ?: getDefault()
        }
    }
}
