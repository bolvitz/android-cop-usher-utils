package com.eventmonitor.feature.headcounter.seatmap.models

data class Seat(
    val id: String,
    val row: String,
    val number: Int,
    val status: SeatStatus,
    val seatType: SeatType = SeatType.STANDARD
) {
    val displayLabel: String
        get() = "$row$number"
}

enum class SeatType {
    STANDARD,
    VIP,
    WHEELCHAIR,
    COMPANION
}
