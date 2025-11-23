package com.copheadcounter.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.copheadcounter.model.AppSettings

class SettingsViewModel : ViewModel() {
    var settings by mutableStateOf(AppSettings())
        private set

    fun updateCounterEnabled(enabled: Boolean) {
        settings = settings.copy(isCounterEnabled = enabled)
    }

    fun updateLostFoundEnabled(enabled: Boolean) {
        settings = settings.copy(isLostFoundEnabled = enabled)
    }

    fun resetSettings() {
        settings = AppSettings()
    }
}
