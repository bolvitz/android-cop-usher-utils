package com.eventmonitor.app.presentation.screens.headcounter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure helpers behind the head counter screens: fill percentage, capacity warning
 * crossings, hold-to-repeat timing and chart tick steps. They are plain functions with no Android or
 * Compose dependencies, so nothing is mocked and the tests run on the JVM.
 */
class CounterMathTest {

    /**
     * Scenario: percentages for zones with and without a capacity.
     * Expected: no capacity gives 0 instead of dividing by zero; others round to the nearest whole percent
     * and may exceed 100 when a zone is over capacity.
     * Why: the counter, seat map and History all show this number, and an empty zone must never crash.
     */
    @Test
    fun percentTreatsNoCapacityAsZero() {
        // Given / When / Then: each line pairs inputs with the expected display value.
        assertEquals(0, percent(12, 0))
        assertEquals(50, percent(50, 100))
        // 2 of 3 is 66.67%, which rounds up to 67 rather than truncating to 66.
        assertEquals(67, percent(2, 3))
        // Over capacity stays above 100 so the "N over" status can show.
        assertEquals(120, percent(120, 100))
    }

    /**
     * Scenario: a fill level moves between counts.
     * Expected: the warning fires only when the level rises across 90% or 100%.
     * Why: the counter buzzes once per crossing; firing on every count above 90%, or on the way down,
     * would make the haptic meaningless for ushers counting without looking.
     */
    @Test
    fun warningFiresOnlyWhenCrossingUpward() {
        // Given / When: old and new percentages. Then: true only for upward crossings.
        assertTrue(crossedWarning(89, 90))
        assertTrue(crossedWarning(99, 100))
        // One big jump past both thresholds still warns.
        assertTrue(crossedWarning(80, 105))
        // Already past 90 and not reaching 100: no new crossing.
        assertFalse(crossedWarning(90, 95))
        // Falling below a threshold never warns.
        assertFalse(crossedWarning(100, 89))
        // Already over 100: still no new crossing.
        assertFalse(crossedWarning(101, 102))
    }

    /**
     * Scenario: holding the +1 key repeats with a shrinking gap.
     * Expected: each gap is 86% of the previous one and never drops below 70 ms.
     * Why: matches the iOS hold timing; a missing floor would make a long hold run away.
     */
    @Test
    fun holdIntervalShrinksToFloor() {
        // Given: the first repeat gap. When: 50 repeats, far more than needed to reach the floor.
        var interval = FIRST_HOLD_INTERVAL_MS
        repeat(50) { interval = nextHoldInterval(interval) }
        // Then: the gap settles at the 70 ms floor.
        assertEquals(70.0, interval, 0.0)
        // And one step from 220 ms is exactly 86% of it.
        assertEquals(220.0 * 0.86, nextHoldInterval(220.0), 1e-9)
    }

    /**
     * Scenario: choosing y-axis tick steps for the Trends chart.
     * Expected: steps are 1, 2 or 5 times a power of ten, and at least 1.
     * Why: round steps keep the axis labels readable; a step below 1 would label fractional people.
     */
    @Test
    fun chartStepsAreRoundNumbers() {
        // Given / When: the chart's value range (peak times 1.08 headroom). Then: the rounded step.
        // 130 / 3 ticks is about 43, which rounds up to 50.
        assertEquals(50f, niceStep(130f), 0f)
        // 54 / 3 is 18, which rounds up to 20.
        assertEquals(20f, niceStep(54f), 0f)
        // A one-person peak (1 * 1.08) still gets a whole-number step.
        assertEquals(1f, niceStep(1.08f), 0f)
        // An empty period falls back to 1 instead of dividing by zero.
        assertEquals(1f, niceStep(0f), 0f)
    }
}
