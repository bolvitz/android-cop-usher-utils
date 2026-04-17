package com.eventmonitor.core.common.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// FIELD typography.
// Three families, used exactly: Serif for drama, Sans for UI, Mono for data.
// System fallbacks now; Fraunces / Instrument Sans / JetBrains Mono bundling is follow-up.
val DisplaySerif: FontFamily = FontFamily.Serif      // Fraunces-slot (editorial display, counts)
val BodySans: FontFamily = FontFamily.SansSerif  // Instrument Sans-slot (UI, body, labels)
val DataMono: FontFamily = FontFamily.Monospace  // JetBrains Mono-slot (timestamps, IDs, tags)

val Typography = Typography(
    // --- Display (Fraunces-slot) ---
    displayLarge = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Light,
        fontSize = 96.sp,
        lineHeight = 96.sp,
        letterSpacing = (-4.0).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Normal,
        fontSize = 60.sp,
        lineHeight = 60.sp,
        letterSpacing = (-2.0).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.0).sp,
    ),

    // --- Headline (Fraunces-slot, section titles) ---
    headlineLarge = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplaySerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
    ),

    // --- Title (Sans, card heads) ---
    titleLarge = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),

    // --- Body (Sans) ---
    bodyLarge = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),

    // --- Labels (Mono — data grammar of the app) ---
    labelLarge = TextStyle(
        fontFamily = DataMono,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = DataMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = DataMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 1.0.sp,
    ),
)

// Reusable mono style for timestamps / IDs not covered by label slots.
val MonoTiny = TextStyle(
    fontFamily = DataMono,
    fontWeight = FontWeight.Normal,
    fontSize = 9.5.sp,
    lineHeight = 12.sp,
    letterSpacing = 0.8.sp,
)
