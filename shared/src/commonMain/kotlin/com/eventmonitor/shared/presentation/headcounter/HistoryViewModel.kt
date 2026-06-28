package com.eventmonitor.shared.presentation.headcounter

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.EventWithDetails
import com.eventmonitor.shared.data.repository.EventRepository
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = true,
    val events: List<EventWithDetails> = emptyList()
) {
    val isEmpty: Boolean get() = !isLoading && events.isEmpty()
}

class HistoryViewModel(
    private val eventRepository: EventRepository,
    private val venueId: String? = null
) : SharedViewModel() {

    val uiState: StateFlow<HistoryUiState> =
        (if (venueId != null) eventRepository.getRecentEventsByVenue(venueId, 100)
        else eventRepository.getRecentEvents(100))
            .map { events -> HistoryUiState(isLoading = false, events = events) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun unlockEvent(eventId: String) {
        viewModelScope.launch { eventRepository.unlockEvent(eventId) }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch { eventRepository.deleteEvent(eventId) }
    }
}
