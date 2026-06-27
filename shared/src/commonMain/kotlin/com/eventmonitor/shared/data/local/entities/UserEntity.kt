package com.eventmonitor.shared.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eventmonitor.shared.util.newId
import com.eventmonitor.shared.util.nowMillis

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String = newId(),
    val email: String = "",
    val displayName: String,
    val role: String,
    val assignedBranchIds: String = "",
    val isActive: Boolean = true,
    val lastSyncTime: Long = 0,
    val firebaseUid: String = "",
    val createdAt: Long = nowMillis()
)
