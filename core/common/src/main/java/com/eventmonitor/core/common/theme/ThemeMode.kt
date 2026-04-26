package com.eventmonitor.core.common.theme

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide theme controller. Owns a single observable `isDark` flag that
 * MainActivity binds to `HeadCounterTheme(darkTheme = ...)` and a UI control
 * (e.g. the masthead toggle) flips. The host is responsible for persisting
 * the value across launches.
 */
@Stable
class ThemeMode(initial: Boolean) {
    var isDark: Boolean by mutableStateOf(initial)
}

val LocalThemeMode = compositionLocalOf<ThemeMode> {
    error("ThemeMode not provided. Wrap content in CompositionLocalProvider(LocalThemeMode provides …).")
}
