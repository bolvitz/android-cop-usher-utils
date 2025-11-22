package com.cop.app.headcounter.presentation.screens.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cop.app.headcounter.data.local.entities.ServiceWithDetails
import com.cop.app.headcounter.domain.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val branchId: String? = savedStateHandle.get<String>("branchId")

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
                serviceRepository.getRecentServicesByBranch(branchId, 100)
            } else {
                serviceRepository.getRecentServices(100)
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

    fun generateReport(serviceId: String) {
        viewModelScope.launch {
            try {
                val report = serviceRepository.exportServiceReport(serviceId)
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

    fun unlockService(serviceId: String) {
        viewModelScope.launch {
            serviceRepository.unlockService(serviceId)
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            serviceRepository.deleteService(serviceId)
        }
    }
}

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    object Empty : HistoryUiState()
    data class Success(val services: List<ServiceWithDetails>) : HistoryUiState()
}
