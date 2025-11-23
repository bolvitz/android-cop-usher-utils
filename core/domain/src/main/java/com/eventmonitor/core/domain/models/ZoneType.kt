package com.eventmonitor.core.domain.models

enum class ZoneType(val displayName: String, val defaultIcon: String) {
    SEATING("Seating Area", "event_seat"),
    STANDING("Standing Area", "group"),
    VIP("VIP Section", "star"),
    GENERAL_ADMISSION("General Admission", "groups"),
    OVERFLOW("Overflow Area", "group_add"),
    PARKING("Parking", "local_parking"),
    REGISTRATION("Registration", "assignment"),
    LOBBY("Lobby/Entrance", "meeting_room"),
    OUTDOOR("Outdoor Area", "nature_people"),
    STAGE("Stage/Platform", "podium"),
    BACKSTAGE("Backstage", "theater_comedy"),
    CARE_ROOM("Care Room", "child_care"),
    FOOD_AREA("Food & Beverage", "restaurant"),
    RESTROOMS("Restrooms", "wc"),
    EMERGENCY_EXIT("Emergency Exit", "emergency_exit"),
    OTHER("Other", "place");

    companion object {
        fun fromString(value: String): ZoneType {
            return entries.find { it.name == value } ?: OTHER
        }
    }
}
