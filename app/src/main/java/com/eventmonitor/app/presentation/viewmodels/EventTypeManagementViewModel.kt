package com.eventmonitor.app.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.EventTypeEntity
import com.eventmonitor.core.domain.common.Result
import com.eventmonitor.core.data.repository.interfaces.EventTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventTypeManagementViewModel @Inject constructor(
    private val eventTypeRepository: EventTypeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceTypeManagementUiState())
    val uiState: StateFlow<ServiceTypeManagementUiState> = _uiState.asStateFlow()

    val eventTypes = eventTypeRepository.getAllServiceTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createServiceType(
        name: String,
        dayType: String,
        time: String,
        description: String = ""
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, message = null) }

            val displayOrder = eventTypes.value.size
            val result = eventTypeRepository.createServiceType(
                name = name,
                dayType = dayType,
                time = time,
                description = description,
                displayOrder = displayOrder
            )

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(message = "Service type created successfully")
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

    fun updateServiceType(eventType: EventTypeEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, message = null) }

            val result = eventTypeRepository.updateServiceType(eventType)

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(message = "Service type updated successfully")
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

    fun deleteServiceType(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, message = null) }

            val result = eventTypeRepository.deleteServiceType(id)

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(message = "Service type deleted successfully")
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

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}

data class ServiceTypeManagementUiState(
    val message: String? = null,
    val error: String? = null
)
