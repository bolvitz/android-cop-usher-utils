package com.cop.app.headcounter.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val email: String = "",
    val displayName: String,
    val role: String, // ADMIN, COUNTER, VIEWER
    val assignedBranchIds: String = "", // JSON array of branch IDs
    val isActive: Boolean = true,
    val lastSyncTime: Long = 0,
    val firebaseUid: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
