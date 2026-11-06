package com.eventmonitor.shared.platform

import android.content.Context

class SharedPrefsKeyValueStore(context: Context) : KeyValueStore {
    private val prefs = context.getSharedPreferences("app_scope", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String?) {
        prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
    }
}
