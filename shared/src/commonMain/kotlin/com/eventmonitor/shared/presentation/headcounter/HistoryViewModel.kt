package com.eventmonitor.shared.presentation.headcounter

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.EventWithDetails
import com.eventmonitor.shared.data.repository.EventRepository
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val reportBuilder: HeadCountReportBuilder,
    private val venueId: String? = null
) : SharedViewModel() {

    private val _report = MutableStateFlow<HeadCountReport?>(null)

    /** Receipt for the event picked with [showReport]; null when none is open. */
    val report: StateFlow<HeadCountReport?> = _report.asStateFlow()

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

    fun showReport(eventId: String) {
        viewModelScope.launch { _report.value = reportBuilder.build(eventId) }
    }

    fun clearReport() {
        _report.value = null
    }
}
