package com.eventmonitor.app.presentation.screens.venues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.VenueWithAreas
import com.eventmonitor.core.data.repository.interfaces.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VenueListViewModel @Inject constructor(
    private val venueRepository: VenueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VenueListUiState>(VenueListUiState.Loading)
    val uiState: StateFlow<VenueListUiState> = _uiState.asStateFlow()

    init {
        loadVenues()
    }

    private fun loadVenues() {
        viewModelScope.launch {
            venueRepository.getAllActiveVenues()
                .catch { e ->
                    _uiState.value = VenueListUiState.Error(e.message ?: "Unknown error")
                }
                .collect { venues ->
                    _uiState.value = if (venues.isEmpty()) {
                        VenueListUiState.Empty
                    } else {
                        VenueListUiState.Success(venues)
                    }
                }
        }
    }

    fun deleteVenue(venueId: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Check if venue has services
                if (venueRepository.hasEvents(venueId)) {
                    onError("Cannot delete venue: It has event records. Delete those events first.")
                    return@launch
                }

                venueRepository.deleteVenue(venueId)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete venue")
            }
        }
    }
}

sealed class VenueListUiState {
    object Loading : VenueListUiState()
    object Empty : VenueListUiState()
    data class Success(val venues: List<VenueWithAreas>) : VenueListUiState()
    data class Error(val message: String) : VenueListUiState()
}
