package com.eventmonitor.feature.headcounter.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.data.repository.interfaces.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val venueId: String? = savedStateHandle.get<String>("venueId")
    private val branchId: String? = venueId // For backward compatibility

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _selectedServiceReport = MutableStateFlow<String?>(null)
    val selectedServiceReport: StateFlow<String?> = _selectedServiceReport.asStateFlow()

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

    fun generateReport(eventId: String) {
        viewModelScope.launch {
            try {
                val report = eventRepository.exportEventReport(eventId)
                _selectedServiceReport.value = report
            } catch (e: Exception) {
                // Handle error
                _selectedServiceReport.value = "Error generating report: ${e.message}"
            }
        }
    }

    fun clearReport() {
        _selectedServiceReport.value = null
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
