package com.cop.app.headcounter.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cop.app.headcounter.data.local.entities.ServiceTypeEntity
import com.cop.app.headcounter.domain.repository.ServiceTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServiceTypeManagementViewModel @Inject constructor(
    private val serviceTypeRepository: ServiceTypeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val branchId: String = savedStateHandle.get<String>("branchId") ?: ""

    private val _uiState = MutableStateFlow(ServiceTypeManagementUiState())
    val uiState: StateFlow<ServiceTypeManagementUiState> = _uiState.asStateFlow()

    val serviceTypes = serviceTypeRepository.getServiceTypesByBranch(branchId)
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
            try {
                val displayOrder = serviceTypes.value.size
                serviceTypeRepository.createServiceType(
                    branchId = branchId,
                    name = name,
                    dayType = dayType,
                    time = time,
                    description = description,
                    displayOrder = displayOrder
                )
                _uiState.value = _uiState.value.copy(
                    message = "Service type created successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to create service type: ${e.message}"
                )
            }
        }
    }

    fun updateServiceType(serviceType: ServiceTypeEntity) {
        viewModelScope.launch {
            try {
                serviceTypeRepository.updateServiceType(serviceType)
                _uiState.value = _uiState.value.copy(
                    message = "Service type updated successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update service type: ${e.message}"
                )
            }
        }
    }

    fun deleteServiceType(id: String) {
        viewModelScope.launch {
            try {
                // Check if service type is being used in any service
                if (serviceTypeRepository.hasServices(id)) {
                    _uiState.value = _uiState.value.copy(
                        error = "Cannot delete service type: It is being used in one or more services. Delete those services first."
                    )
                    return@launch
                }

                serviceTypeRepository.deleteServiceType(id)
                _uiState.value = _uiState.value.copy(
                    message = "Service type deleted successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete service type: ${e.message}"
                )
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
