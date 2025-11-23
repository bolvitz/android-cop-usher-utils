package com.eventmonitor.core.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "lost_items",
    foreignKeys = [
        ForeignKey(
            entity = BranchEntity::class,
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
    val id: String = UUID.randomUUID().toString(),
    val locationId: String, // Where the event is happening
    val eventId: String? = null, // Optional link to specific event/service
    val description: String, // Description of the lost item
    val category: String, // ELECTRONICS, CLOTHING, DOCUMENTS, ACCESSORIES, BAGS, PERSONAL_ITEMS, OTHER
    val foundZone: String, // Specific zone/area where item was found
    val foundDate: Long, // Timestamp when item was found
    val photoUri: String = "", // Local URI to item photo
    val color: String = "", // Primary color of the item
    val brand: String = "", // Brand name if applicable
    val identifyingMarks: String = "", // Unique identifiers
    val status: String, // PENDING, CLAIMED, DONATED, DISPOSED
    val claimedBy: String = "", // Name of person who claimed
    val claimedDate: Long = 0, // When item was claimed
    val claimerContact: String = "", // Phone/email of claimer
    val verificationNotes: String = "", // Notes for verification process
    val reportedBy: String = "", // Staff member who registered the item
    val notes: String = "", // Additional notes
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSyncedToCloud: Boolean = false,
    val cloudId: String = ""
)
