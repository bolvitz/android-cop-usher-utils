package com.cop.app.headcounter.domain.models

enum class AreaType(val displayName: String, val defaultIcon: String) {
    BAY("Bay", "event_seat"),
    BABY_ROOM("Baby Room", "child_care"),
    BALCONY("Balcony", "stairs"),
    OVERFLOW("Overflow", "group_add"),
    PARKING("Parking", "local_parking"),
    LOBBY("Lobby", "meeting_room"),
    OUTDOOR("Outdoor", "nature_people"),
    OTHER("Other", "place");

    companion object {
        fun fromString(value: String): AreaType {
            return entries.find { it.name == value } ?: OTHER
        }
    }
}
