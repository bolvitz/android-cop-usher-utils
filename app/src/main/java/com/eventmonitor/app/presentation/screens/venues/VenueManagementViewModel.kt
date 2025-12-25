package com.eventmonitor.app.presentation.screens.venues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.VenueEntity
import com.eventmonitor.core.domain.common.Result
import com.eventmonitor.core.data.repository.interfaces.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VenueManagementViewModel @Inject constructor(
    private val venueRepository: VenueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VenueManagementUiState())
    val uiState: StateFlow<VenueManagementUiState> = _uiState.asStateFlow()

    val venues = venueRepository.getAllVenues()
        .map { venuesWithAreas -> venuesWithAreas.map { it.venue } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteVenue(venueId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, message = null) }

            if (venueRepository.hasEvents(venueId)) {
                _uiState.update {
                    it.copy(error = "Cannot delete venue: It has event records. Delete those events first.")
                }
                return@launch
            }

            val result = venueRepository.deleteVenue(venueId)

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(message = "Branch deleted successfully")
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(error = result.error.toUserMessage())
                    }
                }
            }
        }
    }

    fun toggleVenueStatus(venueId: String, isActive: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, message = null) }
            venueRepository.setVenueActive(venueId, isActive)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}

data class VenueManagementUiState(
    val message: String? = null,
    val error: String? = null
)
