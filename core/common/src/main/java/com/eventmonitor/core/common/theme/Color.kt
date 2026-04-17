package com.eventmonitor.core.common.theme

import androidx.compose.ui.graphics.Color

// FIELD palette — editorial-industrial ops console.
// Paper + ink + one signal colour. Dark mode inverts paper↔ink 1:1.

// Light (paper)
val Paper = Color(0xFFF1EAD8)
val Paper2 = Color(0xFFEADFC5)
val Paper3 = Color(0xFFE2D6B7)
val Ink = Color(0xFF141210)
val Ink2 = Color(0xFF2E2A24)
val Muted = Color(0xFF756B5B)
val Muted2 = Color(0xFF9B9075)
val Hairline = Color(0xFFCBC0A6)
val Hairline2 = Color(0xFFD9CFB6)

// Semantic signals (identical across light/dark)
val Signal = Color(0xFFD8301A) // critical · overcapacity · live
val SignalDeep = Color(0xFFB22512)
val Amber = Color(0xFFB8851A) // warning · in-progress
val Sage = Color(0xFF3E6B3B) // safe · closed · claimed
val Navy = Color(0xFF1F3A5F) // info

// Dark (invert of paper/ink)
val InkDark = Color(0xFF0E0C0A)
val InkDark2 = Color(0xFF17140F)
val PaperDark = Color(0xFFE9E1CC) // used as onBackground on dark
val PaperDark2 = Color(0xFFBEB39A)
val MutedDark = Color(0xFF8C8270)
val HairlineDark = Color(0xFF403932)
val HairlineDark2 = Color(0xFF2A2621)

// Capacity gradient helpers
val CapacityLow = Sage
val CapacityMedium = Amber
val CapacityHigh = Signal
