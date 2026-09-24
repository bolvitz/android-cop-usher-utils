package com.eventmonitor.core.common.theme

import androidx.compose.ui.graphics.Color

// RETRO palette — black, paper-cream, and one vivid red.
// Inspired by editorial broadcast & 80s industrial signage. Strict three-tone.
// All cream/paper tokens map onto #EDEBE8 family. All ink tokens map onto pure black.
// Signal red #FD3436 is the only chromatic accent.

// Light (paper · cream)
val Paper = Color(0xFFEDEBE8)        // page
val Paper2 = Color(0xFFE3E0DB)       // recessed
val Paper3 = Color(0xFFD8D5CF)       // deeper recess / divider fill
val Ink = Color(0xFF000000)          // pure black ink
val Ink2 = Color(0xFF1A1A1A)         // near-black for inverted surfaces
val Muted = Color(0xFF777069)        // mid-grey · captions, timestamps
val Muted2 = Color(0xFF9C958D)       // ghost text
val Hairline = Color(0xFFC9C5BE)     // visible dividers
val Hairline2 = Color(0xFFDAD6CF)    // subtle dividers

// Semantic signals — collapsed to ONE accent: vivid red.
val Signal = Color(0xFFFD3436)        // critical · live · primary action accent
val SignalDeep = Color(0xFFD32428)    // pressed-state red
val SignalGlow = Color(0xFFFF5A5C)    // highlight ring · live pulse
val Amber = Color(0xFF777069)         // demoted to grey — no warm accent
val Sage = Color(0xFF000000)          // demoted to ink — safe = "set"
val Navy = Color(0xFF000000)          // demoted to ink — info = "set"

// Dark (full black)
val InkDark = Color(0xFF000000)
val InkDark2 = Color(0xFF0A0A0A)
val PaperDark = Color(0xFFEDEBE8)
val PaperDark2 = Color(0xFFB8B5AE)
val MutedDark = Color(0xFF8A847C)
val HairlineDark = Color(0xFF2A2A2A)
val HairlineDark2 = Color(0xFF1A1A1A)

// Capacity gradient helpers — kept for compatibility but collapsed to retro tones.
val CapacityLow = Ink
val CapacityMedium = Muted
val CapacityHigh = Signal
