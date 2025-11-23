package com.eventmonitor.core.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = BranchEntity::class,
            parentColumns = ["id"],
            childColumns = ["branchId"],
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
        Index("branchId"),
        Index("date"),
        Index("eventType"),
        Index("eventTypeId"),
        Index(value = ["branchId", "date"], name = "idx_services_branch_date"),
        Index(value = ["branchId", "eventTypeId"], name = "idx_services_branch_type")
    ]
)
data class EventEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val branchId: String,
    val eventTypeId: String? = null, // References EventTypeEntity for dynamic service types
    val date: Long, // Unix timestamp
    val eventType: String = "", // Deprecated: kept for backward compatibility
    val eventName: String = "", // Optional custom name like "Easter Service", "Christmas Eve"
    val totalAttendance: Int = 0,
    val totalCapacity: Int = 0,
    val notes: String = "",
    val weather: String = "", // Optional: "Sunny", "Rainy" - affects attendance
    val countedBy: String = "",
    val countedByUserId: String = "",
    val isLocked: Boolean = false,
    val isSpecialEvent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isSyncedToCloud: Boolean = false,
    val cloudId: String = ""
)
