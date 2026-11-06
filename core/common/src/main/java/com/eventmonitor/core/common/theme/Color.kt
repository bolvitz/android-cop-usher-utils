package com.eventmonitor.core.common.theme

import androidx.compose.ui.graphics.Color
import com.eventmonitor.shared.design.DesignTokens
import com.eventmonitor.shared.design.ThemedColor

// Palette values live in the shared DesignTokens so the iOS app reads the same ones.
// Dark-first for dim halls: deep blue ground, brass accent, red for over capacity only.

private fun rgb(hex: Long) = Color(0xFF000000 or hex)

internal val ThemedColor.darkColor: Color get() = rgb(dark)
internal val ThemedColor.lightColor: Color get() = rgb(light)

// System bar colours, set before the theme composes.
val GroundDark = DesignTokens.ground.darkColor
val GroundLight = DesignTokens.ground.lightColor

// Receipt paper: the same in both appearances.
val ReceiptPaper = rgb(DesignTokens.PAPER)
val ReceiptInk = rgb(DesignTokens.PAPER_INK)
val ReceiptAccent = rgb(DesignTokens.PAPER_ACCENT)
