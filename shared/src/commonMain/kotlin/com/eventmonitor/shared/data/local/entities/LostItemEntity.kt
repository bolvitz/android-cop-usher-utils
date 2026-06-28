package com.eventmonitor.shared.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis

@Entity(
    tableName = "lost_items",
    foreignKeys = [
        ForeignKey(
            entity = VenueEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
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
        Index(value = ["locationId"]),
        Index(value = ["eventId"]),
        Index(value = ["status"]),
        Index(value = ["category"]),
        Index(value = ["foundDate"])
    ]
)
data class LostItemEntity(
    @PrimaryKey
    val id: String = newId(),
    val locationId: String,
    val eventId: String? = null,
    val description: String,
    val category: String,
    val foundZone: String,
    val foundDate: Long,
    val photoUri: String = "",
    val color: String = "",
    val brand: String = "",
    val identifyingMarks: String = "",
    val status: String,
    val claimedBy: String = "",
    val claimedDate: Long = 0,
    val claimerContact: String = "",
    val verificationNotes: String = "",
    val reportedBy: String = "",
    val notes: String = "",
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
    val isSyncedToCloud: Boolean = false,
    val cloudId: String = ""
)
