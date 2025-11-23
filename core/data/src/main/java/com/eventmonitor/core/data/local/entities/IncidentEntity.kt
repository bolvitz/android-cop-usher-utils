package com.eventmonitor.core.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "incidents",
    foreignKeys = [
        ForeignKey(
            entity = BranchEntity::class,
            parentColumns = ["id"],
            childColumns = ["branchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["branchId"]),
        Index(value = ["severity"]),
        Index(value = ["status"]),
        Index(value = ["reportedAt"]),
        Index(value = ["resolvedAt"])
    ]
)
data class IncidentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val branchId: String, // Location where incident occurred
    val title: String, // Brief title of the incident
    val description: String, // Detailed description
    val severity: String, // LOW, MEDIUM, HIGH, CRITICAL
    val status: String, // REPORTED, INVESTIGATING, IN_PROGRESS, RESOLVED, CLOSED
    val category: String = "", // Type of incident (safety, security, maintenance, etc.)
    val location: String = "", // Specific area/zone within the branch
    val photoUri: String = "", // Photo evidence URI
    val reportedBy: String = "", // Who reported the incident
    val assignedTo: String = "", // Who is handling the incident
    val reportedAt: Long = System.currentTimeMillis(), // When incident was reported
    val resolvedAt: Long? = null, // When incident was resolved (null if not resolved)
    val notes: String = "", // Additional notes
    val actionsTaken: String = "", // Actions taken to resolve
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSyncedToCloud: Boolean = false,
    val cloudId: String = ""
)
