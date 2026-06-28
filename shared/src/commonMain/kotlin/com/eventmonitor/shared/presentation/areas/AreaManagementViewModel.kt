package com.eventmonitor.shared.presentation.areas

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.AreaTemplateDto
import com.eventmonitor.shared.data.repository.VenueRepository
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AreaManagementUiState(
    val isLoading: Boolean = true,
    val areas: List<AreaTemplateDto> = emptyList(),
    val error: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && areas.isEmpty()
}

class AreaManagementViewModel(
    private val venueRepository: VenueRepository,
    private val venueId: String
) : SharedViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AreaManagementUiState> =
        combine(venueRepository.getAreaTemplatesByVenue(venueId), _error) { areas, error ->
            AreaManagementUiState(isLoading = false, areas = areas, error = error)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AreaManagementUiState())

    fun createArea(name: String, capacity: Int, type: String = "OTHER") {
        viewModelScope.launch {
            venueRepository.createAreaTemplate(
                AreaTemplateDto(
                    venueId = venueId,
                    name = name,
                    type = type,
                    capacity = capacity,
                    displayOrder = uiState.value.areas.size
                )
            ).onError { _error.value = it.toUserMessage() }
        }
    }

    fun deleteArea(templateId: String) {
        viewModelScope.launch {
            venueRepository.deleteAreaTemplate(templateId)
                .onError { _error.value = it.toUserMessage() }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
