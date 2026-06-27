package com.eventmonitor.feature.headcounter.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.data.repository.interfaces.EventRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel constructor(
    private val eventRepository: EventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val venueId: String? = savedStateHandle.get<String>("venueId")
    private val branchId: String? = venueId

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadServices()
    }

    private fun loadServices() {
        viewModelScope.launch {
            val servicesFlow = if (branchId != null) {
                eventRepository.getRecentEventsByVenue(branchId, 100)
            } else {
                eventRepository.getRecentEvents(100)
            }

            servicesFlow.collect { services ->
                if (services.isEmpty()) {
                    _uiState.value = HistoryUiState.Empty
                } else {
                    _uiState.value = HistoryUiState.Success(services)
                }
            }
        }
    }

    fun unlockEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.unlockEvent(eventId)
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
        }
    }
}

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    object Empty : HistoryUiState()
    data class Success(val events: List<EventWithDetails>) : HistoryUiState()
}
