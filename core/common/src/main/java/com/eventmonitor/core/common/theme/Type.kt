package com.eventmonitor.core.common.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// RETRO typography — one heavy geometric sans does everything.
// Golos Text-slot for display + UI. JetBrains Mono-slot for timestamps and tags.
// No serif. No light weights. Big, set, decisive. Built so users can read and
// edit on a phone without squinting.
val DisplaySerif: FontFamily = FontFamily.SansSerif // legacy name kept; now sans
val BodySans: FontFamily = FontFamily.SansSerif
val DataMono: FontFamily = FontFamily.Monospace

val Typography = Typography(
    // --- Display — used for hero numbers, timer faces, screen titles. ---
    displayLarge = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Black,
        fontSize = 96.sp,
        lineHeight = 100.sp,
        letterSpacing = (-3.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-2.0).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp,
    ),

    // --- Headline — section titles, big enough to anchor a screen. ---
    headlineLarge = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
    ),

    // --- Title — card heads, dialog titles, list-row primaries. ---
    titleLarge = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // --- Body — readable on phones, larger than Material default. ---
    bodyLarge = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodySans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),

    // --- Labels — tracked uppercase mono. The "TALK / TRANSCRIBE" tags. ---
    labelLarge = TextStyle(
        fontFamily = DataMono,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.6.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = DataMono,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = DataMono,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 1.2.sp,
    ),
)

// Reusable mono style for timestamps / IDs not covered by label slots.
val MonoTiny = TextStyle(
    fontFamily = DataMono,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.2.sp,
)
