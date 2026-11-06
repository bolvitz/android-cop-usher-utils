package com.eventmonitor.shared.presentation.scope

import com.eventmonitor.shared.data.models.IncidentDto
import com.eventmonitor.shared.data.models.LostItemDto
import com.eventmonitor.shared.data.models.VenueDto
import com.eventmonitor.shared.platform.KeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests the tab shell's state rules: [buildShellState] (badge counts and scope fallback) and
 * [ScopeHolder] (persisted venue selection).
 *
 * [buildShellState] is a pure function, so it is called directly with plain DTO lists instead
 * of going through [AppShellViewModel] and its repositories. [ScopeHolder] gets an in-memory
 * [MapStore] fake in place of SharedPreferences / NSUserDefaults, so the test runs on every
 * KMP target with no platform storage.
 */
class AppShellStateTest {
    // Two venues: "a" (Main Hall) and "b" (North). Order matters for the per-venue assertions.
    private val venues = listOf(VenueDto(id = "a", name = "Main Hall"), VenueDto(id = "b", name = "North"))

    // One incident per status, so every open/closed branch of isOpen() is covered:
    // venue a has 2 open (REPORTED, IN_PROGRESS) + 1 RESOLVED;
    // venue b has 1 open (INVESTIGATING) + 1 CLOSED. Total open = 3.
    private val incidents = listOf(
        IncidentDto(id = "1", venueId = "a", status = "REPORTED"),
        IncidentDto(id = "2", venueId = "a", status = "IN_PROGRESS"),
        IncidentDto(id = "3", venueId = "a", status = "RESOLVED"),
        IncidentDto(id = "4", venueId = "b", status = "CLOSED"),
        IncidentDto(id = "5", venueId = "b", status = "INVESTIGATING")
    )

    // Lost items key their venue by locationId. Only PENDING counts as unclaimed:
    // venue a has 1, venue b has 1 (the CLAIMED one is excluded). Total unclaimed = 2.
    private val items = listOf(
        LostItemDto(id = "x", locationId = "a", status = "PENDING"),
        LostItemDto(id = "y", locationId = "b", status = "CLAIMED"),
        LostItemDto(id = "z", locationId = "b", status = "PENDING")
    )

    /**
     * Scenario: no venue selected ("All venues").
     * Expected: tab badges sum open incidents and unclaimed items across every venue, and each
     * venue row still carries its own count for the Live tab's venue cards.
     * Why: the coordinator watches several venues at once and needs the combined total.
     */
    @Test
    fun allVenuesSumsOpenIncidentsAndUnclaimedItems() {
        // Given / When: the fixtures above with a null scope
        val state = buildShellState(venues, incidents, items, selectedVenueId = null)

        // Then: totals are summed; RESOLVED, CLOSED and CLAIMED are left out
        assertEquals(3, state.openIncidents)
        assertEquals(2, state.unclaimedItems)
        assertEquals(AppShellUiState.ALL_VENUES, state.scopeName)
        // Per-venue counts in venue order: a = 2 open, b = 1 open
        assertEquals(listOf(2, 1), state.venues.map { it.openIncidents })
    }

    /**
     * Scenario: one venue selected.
     * Expected: badges count only that venue, and the scope name is the venue's name.
     * Why: an usher scoped to one venue must not see another venue's incidents in the badge.
     */
    @Test
    fun venueScopeCountsOnlyThatVenue() {
        // Given / When: scope set to venue b (North)
        val state = buildShellState(venues, incidents, items, selectedVenueId = "b")

        // Then: only b's INVESTIGATING incident and its one PENDING item count
        assertEquals(1, state.openIncidents)
        assertEquals(1, state.unclaimedItems)
        assertEquals("North", state.scopeName)
    }

    /**
     * Scenario: the persisted scope points at a venue that no longer exists (deleted or deactivated).
     * Expected: the state falls back to "All venues" with all-venue totals.
     * Why: without the fallback the app would reopen on an empty scope with no way to tell why.
     */
    @Test
    fun unknownVenueFallsBackToAllVenues() {
        // Given / When: an id that matches no venue in the list
        val state = buildShellState(venues, incidents, items, selectedVenueId = "deleted")

        // Then: scope is cleared and totals match the all-venues case
        assertNull(state.selectedVenueId)
        assertEquals(3, state.openIncidents)
    }

    /**
     * Scenario: a venue is selected, then cleared, each time read back by a fresh [ScopeHolder].
     * Expected: a new holder over the same store starts on the last saved value.
     * Why: the app must reopen on the venue the user was working in, and clearing must remove
     * the key instead of leaving a stale id behind.
     */
    @Test
    fun scopeHolderPersistsSelection() {
        // Given: one store shared by several holders, standing in for app restarts
        val store = MapStore()

        // When / Then: selecting "a" survives into a new holder
        ScopeHolder(store).select("a")
        assertEquals("a", ScopeHolder(store).selectedVenueId.value)

        // When / Then: selecting null (All venues) also survives
        ScopeHolder(store).select(null)
        assertNull(ScopeHolder(store).selectedVenueId.value)
    }

    /** In-memory [KeyValueStore]; a null value removes the key, matching both platform stores. */
    private class MapStore : KeyValueStore {
        private val map = mutableMapOf<String, String>()
        override fun getString(key: String) = map[key]
        override fun putString(key: String, value: String?) {
            if (value == null) map.remove(key) else map[key] = value
        }
    }
}
