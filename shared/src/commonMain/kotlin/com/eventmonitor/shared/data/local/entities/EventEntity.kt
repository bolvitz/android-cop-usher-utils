package com.eventmonitor.shared.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = VenueEntity::class,
            parentColumns = ["id"],
            childColumns = ["venueId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EventTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventTypeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("venueId"),
        Index("date"),
        Index("eventType"),
        Index("eventTypeId"),
        Index(value = ["venueId", "date"], name = "idx_events_venue_date"),
        Index(value = ["venueId", "eventTypeId"], name = "idx_events_venue_type")
    ]
)
data class EventEntity(
    @PrimaryKey
    val id: String = newId(),
    val venueId: String,
    val eventTypeId: String? = null,
    val date: Long,
    val eventType: String = "",
    val eventName: String = "",
    val totalAttendance: Int = 0,
    val totalCapacity: Int = 0,
    val notes: String = "",
    val weather: String = "",
    val countedBy: String = "",
    val countedByUserId: String = "",
    val isLocked: Boolean = false,
    val isSpecialEvent: Boolean = false,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
    val completedAt: Long? = null,
    val isSyncedToCloud: Boolean = false,
    val cloudId: String = ""
)
