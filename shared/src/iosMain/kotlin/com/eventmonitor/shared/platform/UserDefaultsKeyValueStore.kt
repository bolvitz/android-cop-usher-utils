package com.eventmonitor.shared.platform

import platform.Foundation.NSUserDefaults

class UserDefaultsKeyValueStore : KeyValueStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, forKey = key)
    }
}
