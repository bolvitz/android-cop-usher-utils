package com.eventmonitor.shared.presentation.scope

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.IncidentDto
import com.eventmonitor.shared.data.models.LostItemDto
import com.eventmonitor.shared.data.models.VenueDto
import com.eventmonitor.shared.data.repository.IncidentRepository
import com.eventmonitor.shared.data.repository.LostItemRepository
import com.eventmonitor.shared.data.repository.VenueRepository
import com.eventmonitor.shared.domain.models.IncidentStatus
import com.eventmonitor.shared.domain.models.ItemStatus
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class VenueScopeRow(
    val id: String,
    val name: String,
    val location: String,
    val openIncidents: Int,
    val unclaimedItems: Int
)

data class AppShellUiState(
    val isLoading: Boolean = true,
    val venues: List<VenueScopeRow> = emptyList(),
    /** Null means "All venues". Always null or the id of a venue in [venues]. */
    val selectedVenueId: String? = null,
    val openIncidents: Int = 0,
    val unclaimedItems: Int = 0
) {
    val selectedVenue: VenueScopeRow? get() = venues.find { it.id == selectedVenueId }
    val scopeName: String get() = selectedVenue?.name ?: ALL_VENUES
    val isAllVenues: Boolean get() = selectedVenueId == null

    companion object {
        const val ALL_VENUES = "All venues"
    }
}

/** Shared by the Android and iOS tab shells: venue scope plus tab badge counts. */
class AppShellViewModel(
    private val scopeHolder: ScopeHolder,
    venueRepository: VenueRepository,
    incidentRepository: IncidentRepository,
    lostItemRepository: LostItemRepository
) : SharedViewModel() {

    val uiState: StateFlow<AppShellUiState> = combine(
        venueRepository.getActiveVenues(),
        incidentRepository.getAllIncidents(),
        lostItemRepository.getAllItems(),
        scopeHolder.selectedVenueId
    ) { venues, incidents, items, selected ->
        buildShellState(venues, incidents, items, selected)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppShellUiState(selectedVenueId = scopeHolder.selectedVenueId.value)
    )

    /** Pass null for "All venues". */
    fun selectVenue(venueId: String?) = scopeHolder.select(venueId)
}

internal fun IncidentDto.isOpen(): Boolean =
    IncidentStatus.fromString(status).let { it != IncidentStatus.RESOLVED && it != IncidentStatus.CLOSED }

internal fun LostItemDto.isUnclaimed(): Boolean = ItemStatus.fromString(status) == ItemStatus.PENDING

internal fun buildShellState(
    venues: List<VenueDto>,
    incidents: List<IncidentDto>,
    items: List<LostItemDto>,
    selectedVenueId: String?
): AppShellUiState {
    val openByVenue = incidents.filter { it.isOpen() }.groupingBy { it.venueId }.eachCount()
    val unclaimedByVenue = items.filter { it.isUnclaimed() }.groupingBy { it.locationId }.eachCount()
    val rows = venues.map {
        VenueScopeRow(it.id, it.name, it.location, openByVenue[it.id] ?: 0, unclaimedByVenue[it.id] ?: 0)
    }
    // A deleted or deactivated venue falls back to "All venues".
    val selected = rows.find { it.id == selectedVenueId }
    return AppShellUiState(
        isLoading = false,
        venues = rows,
        selectedVenueId = selected?.id,
        openIncidents = selected?.openIncidents ?: rows.sumOf { it.openIncidents },
        unclaimedItems = selected?.unclaimedItems ?: rows.sumOf { it.unclaimedItems }
    )
}
