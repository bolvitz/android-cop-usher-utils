package com.eventmonitor.shared.presentation.eventtypes

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.EventTypeDto
import com.eventmonitor.shared.data.repository.EventTypeRepository
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventTypeManagementUiState(
    val eventTypes: List<EventTypeDto> = emptyList(),
    val message: String? = null,
    val error: String? = null
)

class EventTypeManagementViewModel(
    private val eventTypeRepository: EventTypeRepository
) : SharedViewModel() {

    private val _status = MutableStateFlow(EventTypeManagementUiState())

    val uiState: StateFlow<EventTypeManagementUiState> =
        eventTypeRepository.getAllEventTypes()
            .let { typesFlow ->
                kotlinx.coroutines.flow.combine(typesFlow, _status) { types, status ->
                    status.copy(eventTypes = types)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = EventTypeManagementUiState()
            )

    fun createEventType(name: String, description: String = "") {
        viewModelScope.launch {
            clearStatus()
            val displayOrder = uiState.value.eventTypes.size
            val dto = EventTypeDto(name = name, description = description, displayOrder = displayOrder)
            eventTypeRepository.createEventType(dto)
                .onSuccess { _status.value = _status.value.copy(message = "Event type created") }
                .onError { _status.value = _status.value.copy(error = it.toUserMessage()) }
        }
    }

    fun updateEventType(eventType: EventTypeDto) {
        viewModelScope.launch {
            clearStatus()
            eventTypeRepository.updateEventType(eventType)
                .onSuccess { _status.value = _status.value.copy(message = "Event type updated") }
                .onError { _status.value = _status.value.copy(error = it.toUserMessage()) }
        }
    }

    fun deleteEventType(id: String) {
        viewModelScope.launch {
            clearStatus()
            eventTypeRepository.deleteEventType(id)
                .onSuccess { _status.value = _status.value.copy(message = "Event type deleted") }
                .onError { _status.value = _status.value.copy(error = it.toUserMessage()) }
        }
    }

    fun toggleStatus(eventType: EventTypeDto, isActive: Boolean) {
        updateEventType(eventType.copy(isActive = isActive))
    }

    fun clearMessage() {
        _status.value = _status.value.copy(message = null, error = null)
    }

    private fun clearStatus() {
        _status.value = _status.value.copy(message = null, error = null)
    }
}
