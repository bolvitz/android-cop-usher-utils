package com.eventmonitor.core.common.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.eventmonitor.shared.design.DesignTokens
import com.eventmonitor.shared.design.ThemedColor

// Colour schemes built from the shared DesignTokens. Dynamic colour is off on
// purpose: the palette must read the same on every device and on iOS.

private fun scheme(dark: Boolean): ColorScheme {
    fun ThemedColor.pick() = if (dark) darkColor else lightColor
    val t = DesignTokens
    val ground = t.ground.pick()
    val plate = t.plate.pick()
    val plateHigh = t.plateHigh.pick()
    val brass = t.brass.pick()
    val brassInk = t.brassInk.pick()
    val chalk = t.chalk.pick()
    val pew = t.pew.pick()
    val signal = t.signal.pick()
    val line = chalk.copy(alpha = DesignTokens.LINE_ALPHA.toFloat())
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = brass,
        onPrimary = brassInk,
        primaryContainer = plateHigh,
        onPrimaryContainer = brass,
        inversePrimary = brass,

        secondary = pew,
        onSecondary = ground,
        secondaryContainer = plate,
        onSecondaryContainer = chalk,

        tertiary = brass,
        onTertiary = brassInk,
        tertiaryContainer = plateHigh,
        onTertiaryContainer = brass,

        error = signal,
        onError = Color.White,
        errorContainer = signal,
        onErrorContainer = Color.White,

        background = ground,
        onBackground = chalk,
        surface = ground,
        onSurface = chalk,
        surfaceVariant = plate,
        onSurfaceVariant = pew,
        surfaceTint = Color.Transparent,
        surfaceContainerLowest = ground,
        surfaceContainerLow = plate,
        surfaceContainer = plate,
        surfaceContainerHigh = plateHigh,
        surfaceContainerHighest = plateHigh,

        outline = line.copy(alpha = 0.24f),
        outlineVariant = line,

        scrim = Color.Black,
        inverseSurface = chalk,
        inverseOnSurface = ground,
    )
}

private val LightScheme = scheme(dark = false)
private val DarkScheme = scheme(dark = true)

@Composable
fun HeadCounterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic colour intentionally ignored: the shared palette is the brand.
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Edge-to-edge: system bars stay transparent; only tint the icons.
            val window = (view.context as Activity).window
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !darkTheme
            insets.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FieldShapes,
        content = content,
    )
}
