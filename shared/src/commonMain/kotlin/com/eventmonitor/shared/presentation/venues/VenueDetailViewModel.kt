package com.eventmonitor.shared.presentation.venues

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.VenueDto
import com.eventmonitor.shared.data.repository.VenueRepository
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VenueDetailViewModel(
    private val venueRepository: VenueRepository,
    private val venueId: String
) : SharedViewModel() {

    val venue: StateFlow<VenueDto?> =
        venueRepository.getVenueById(venueId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(updated: VenueDto) {
        viewModelScope.launch { venueRepository.updateVenue(updated) }
    }
}
