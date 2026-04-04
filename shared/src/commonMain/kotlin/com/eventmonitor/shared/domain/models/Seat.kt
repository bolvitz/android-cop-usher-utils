package com.eventmonitor.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
enum class SeatType {
    STANDARD,
    VIP,
    WHEELCHAIR,
    COMPANION
}
