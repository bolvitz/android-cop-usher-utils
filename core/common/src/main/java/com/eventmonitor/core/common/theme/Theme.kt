package com.eventmonitor.core.common.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// RETRO colour schemes. Dynamic colour is disabled on purpose — the palette is
// strict B&W + one red, and must read identically on every device.

private val LightScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Ink,
    onPrimaryContainer = Paper,

    secondary = Muted,
    onSecondary = Paper,
    secondaryContainer = Paper2,
    onSecondaryContainer = Ink,

    // Tertiary now carries the only chromatic accent — vivid signal red.
    tertiary = Signal,
    onTertiary = Paper,
    tertiaryContainer = Signal,
    onTertiaryContainer = Paper,

    error = Signal,
    onError = Paper,
    errorContainer = Signal,
    onErrorContainer = Paper,

    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,

    surfaceVariant = Paper2,
    onSurfaceVariant = Muted,
    surfaceTint = Paper,

    outline = Hairline,
    outlineVariant = Hairline2,

    scrim = Ink,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    inversePrimary = Signal,
)

private val DarkScheme = darkColorScheme(
    primary = PaperDark,
    onPrimary = InkDark,
    primaryContainer = PaperDark,
    onPrimaryContainer = InkDark,

    secondary = MutedDark,
    onSecondary = InkDark,
    secondaryContainer = InkDark2,
    onSecondaryContainer = PaperDark,

    tertiary = Signal,
    onTertiary = PaperDark,
    tertiaryContainer = Signal,
    onTertiaryContainer = PaperDark,

    error = Signal,
    onError = PaperDark,
    errorContainer = Signal,
    onErrorContainer = PaperDark,

    background = InkDark,
    onBackground = PaperDark,
    surface = InkDark,
    onSurface = PaperDark,

    surfaceVariant = InkDark2,
    onSurfaceVariant = MutedDark,
    surfaceTint = InkDark,

    outline = HairlineDark,
    outlineVariant = HairlineDark2,

    scrim = InkDark,
    inverseSurface = PaperDark,
    inverseOnSurface = InkDark,
    inversePrimary = Signal,
)

@Composable
fun HeadCounterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic colour intentionally ignored — FIELD palette is the brand.
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
