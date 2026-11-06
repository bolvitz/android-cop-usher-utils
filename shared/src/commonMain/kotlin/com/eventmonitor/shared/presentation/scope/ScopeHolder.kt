package com.eventmonitor.shared.presentation.scope

import com.eventmonitor.shared.platform.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide venue scope: null means "All venues". One instance per app (Koin single),
 * persisted so the app reopens on the venue the user was working in.
 */
class ScopeHolder(private val store: KeyValueStore) {
    private val _selectedVenueId = MutableStateFlow(store.getString(KEY_VENUE_ID))
    val selectedVenueId: StateFlow<String?> = _selectedVenueId.asStateFlow()

    fun select(venueId: String?) {
        _selectedVenueId.value = venueId
        store.putString(KEY_VENUE_ID, venueId)
    }

    private companion object {
        const val KEY_VENUE_ID = "scope_venue_id"
    }
}
