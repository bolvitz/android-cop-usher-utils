package com.eventmonitor.shared.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis

/**
 * Per-event seat occupancy. Absence of a row means the seat is AVAILABLE for that event.
 */
@Entity(
    tableName = "seat_statuses",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SeatEntity::class,
            parentColumns = ["id"],
            childColumns = ["seatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("eventId"),
        Index("seatId"),
        Index(value = ["eventId", "seatId"], unique = true, name = "idx_seat_statuses_event_seat"),
        Index(value = ["eventId", "status"], name = "idx_seat_statuses_event_status")
    ]
)
data class SeatStatusEntity(
    @PrimaryKey
    val id: String = newId(),
    val eventId: String,
    val seatId: String,
    val status: String,
    val lastUpdated: Long = nowMillis()
)
