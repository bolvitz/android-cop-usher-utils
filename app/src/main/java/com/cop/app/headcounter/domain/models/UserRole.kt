package com.cop.app.headcounter.domain.models

enum class UserRole(val displayName: String) {
    ADMIN("Administrator"),
    COUNTER("Counter"),
    VIEWER("Viewer");

    fun canEdit() = this == ADMIN || this == COUNTER
    fun canManageLocations() = this == ADMIN

    companion object {
        fun fromString(value: String): UserRole {
            return entries.find { it.name == value } ?: VIEWER
        }
    }
}
