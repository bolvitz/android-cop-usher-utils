package com.eventmonitor.core.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Haptic feedback utility for consistent user experience
 */
@Composable
fun rememberHapticFeedback(): HapticFeedbackHelper {
    val haptic = LocalHapticFeedback.current
    return remember { HapticFeedbackHelper(haptic) }
}

class HapticFeedbackHelper(private val haptic: HapticFeedback) {

    /**
     * Light tap feedback for general UI interactions
     */
    fun light() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Medium feedback for important actions
     */
    fun medium() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Strong feedback for critical actions
     */
    fun strong() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Success feedback for completed actions
     */
    fun success() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Error feedback for failed actions
     */
    fun error() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Selection feedback for picking items
     */
    fun selection() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Increment/decrement feedback for counter changes
     */
    fun counter() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}
