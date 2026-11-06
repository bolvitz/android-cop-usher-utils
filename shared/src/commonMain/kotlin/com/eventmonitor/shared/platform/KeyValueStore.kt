package com.eventmonitor.shared.platform

/**
 * Small persistent string store for UI preferences (SharedPreferences on
 * Android, NSUserDefaults on iOS). Not for user data: that lives in Room.
 */
interface KeyValueStore {
    fun getString(key: String): String?

    /** Writes [value], or removes the key when [value] is null. */
    fun putString(key: String, value: String?)
}
