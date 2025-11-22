package com.cop.app.headcounter.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "services",
    foreignKeys = [
        ForeignKey(
            entity = BranchEntity::class,
            parentColumns = ["id"],
            childColumns = ["branchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("branchId"), Index("date"), Index("serviceType")]
)
data class ServiceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val branchId: String,
    val date: Long, // Unix timestamp
    val serviceType: String, // FRIDAY, SATURDAY_AM, SATURDAY_PM, SUNDAY_AM, SUNDAY_PM, SUNDAY_EVENING, MIDWEEK, SPECIAL
    val serviceName: String = "", // Optional custom name like "Easter Service", "Christmas Eve"
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
