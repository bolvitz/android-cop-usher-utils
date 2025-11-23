package com.cop.app.headcounter.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cop.app.headcounter.data.local.entities.ServiceTypeEntity
import com.cop.app.headcounter.domain.common.Result
import com.cop.app.headcounter.domain.repository.ServiceTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServiceTypeManagementViewModel @Inject constructor(
    private val serviceTypeRepository: ServiceTypeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceTypeManagementUiState())
    val uiState: StateFlow<ServiceTypeManagementUiState> = _uiState.asStateFlow()

    val serviceTypes = serviceTypeRepository.getAllServiceTypes()
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

            val displayOrder = serviceTypes.value.size
            val result = serviceTypeRepository.createServiceType(
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

    fun updateServiceType(serviceType: ServiceTypeEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, message = null) }

            val result = serviceTypeRepository.updateServiceType(serviceType)

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

            val result = serviceTypeRepository.deleteServiceType(id)

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
