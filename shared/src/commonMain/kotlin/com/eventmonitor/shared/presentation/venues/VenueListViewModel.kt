package com.eventmonitor.shared.presentation.venues

import com.eventmonitor.shared.data.models.VenueDto
import com.eventmonitor.shared.data.repository.VenueRepository
import com.eventmonitor.shared.presentation.SharedViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class VenueListUiState(
    val isLoading: Boolean = true,
    val venues: List<VenueDto> = emptyList(),
    val errorMessage: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && venues.isEmpty()
}

class VenueListViewModel(
    private val venueRepository: VenueRepository
) : SharedViewModel() {

    private val _uiState = MutableStateFlow(VenueListUiState())
    val uiState: StateFlow<VenueListUiState> = _uiState.asStateFlow()

    init {
        loadVenues()
    }

    private fun loadVenues() {
        viewModelScope.launch {
            venueRepository.getActiveVenues()
                .catch { e -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message) }
                .collect { venues ->
                    _uiState.value = VenueListUiState(isLoading = false, venues = venues)
                }
        }
    }

    fun deleteVenue(venueId: String) {
        viewModelScope.launch {
            venueRepository.deleteVenue(venueId).onError { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.toUserMessage())
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
