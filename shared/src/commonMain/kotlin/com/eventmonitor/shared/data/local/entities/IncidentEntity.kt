package com.eventmonitor.shared.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis

@Entity(
    tableName = "incidents",
    foreignKeys = [
        ForeignKey(
            entity = VenueEntity::class,
            parentColumns = ["id"],
            childColumns = ["venueId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["venueId"]),
        Index(value = ["eventId"]),
        Index(value = ["severity"]),
        Index(value = ["status"]),
        Index(value = ["reportedAt"]),
        Index(value = ["resolvedAt"])
    ]
)
data class IncidentEntity(
    @PrimaryKey
    val id: String = newId(),
    val venueId: String,
    val eventId: String? = null,
    val title: String,
    val description: String,
    val severity: String,
    val status: String,
    val category: String = "",
    val location: String = "",
    val photoUri: String = "",
    val reportedBy: String = "",
    val assignedTo: String = "",
    val reportedAt: Long = nowMillis(),
    val resolvedAt: Long? = null,
    val notes: String = "",
    val actionsTaken: String = "",
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
    val isSyncedToCloud: Boolean = false,
    val cloudId: String = ""
)
