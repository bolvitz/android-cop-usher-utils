package com.eventmonitor.shared.platform

enum class HapticFeedbackType {
    LIGHT,
    MEDIUM,
    HEAVY,
    SUCCESS,
    WARNING,
    ERROR
}

expect class HapticFeedback {
    fun perform(type: HapticFeedbackType)
}
