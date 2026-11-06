package com.eventmonitor.shared.design

import kotlin.math.PI

/** One colour in both appearances, as 0xRRGGBB. */
data class ThemedColor(val dark: Long, val light: Long)

/**
 * A spring in SwiftUI terms (response in seconds, damping fraction).
 * Compose takes [stiffness] and the same damping ratio.
 */
data class SpringToken(val response: Double, val damping: Double) {
    /** Stiffness for unit mass: (2π / response)². */
    val stiffness: Double get() = (2 * PI / response).let { it * it }
}

/**
 * The app's design tokens, shared by the Compose and SwiftUI apps so both read
 * the same values. Dark-first for dim halls; the light variants are tuned to
 * keep text at 4.5:1 or better on [ground].
 */
object DesignTokens {
    /** Deep blue ground; lower glare than black in a dark room. */
    val ground = ThemedColor(dark = 0x161B2E, light = 0xF4F5F8)
    /** Raised surfaces: cards, the counter's body. */
    val plate = ThemedColor(dark = 0x232A42, light = 0xFFFFFF)
    val plateHigh = ThemedColor(dark = 0x2E3756, light = 0xDDE0E9)
    /** Primary accent: warm light that dark-adapted eyes tolerate. */
    val brass = ThemedColor(dark = 0xE0B45A, light = 0x936810)
    /** Text and icons on [brass]. */
    val brassInk = ThemedColor(dark = 0x161B2E, light = 0xFFFFFF)
    /** Primary text. */
    val chalk = ThemedColor(dark = 0xE8E4D8, light = 0x161B2E)
    /** Secondary text, captions, timestamps. */
    val pew = ThemedColor(dark = 0x8A92AA, light = 0x5B6278)
    /** Over capacity and critical only. */
    val signal = ThemedColor(dark = 0xE5484D, light = 0xC9302C)
    /** Hairlines are [chalk] at this alpha. */
    const val LINE_ALPHA = 0.12

    /** Receipt paper: the same in both appearances. */
    const val PAPER = 0xEEF0F3L
    const val PAPER_INK = 0x1E2338L
    const val PAPER_ACCENT = 0x8A5F0EL

    /** Minimum touch target, in pt / dp. */
    const val MIN_TOUCH = 48

    /** Button and key press feedback. */
    val pressSpring = SpringToken(response = 0.25, damping = 0.7)
    /** Odometer digit roll. */
    val rollSpring = SpringToken(response = 0.42, damping = 0.68)
}
