package com.eventmonitor.feature.incidents.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.IncidentEntity
import com.eventmonitor.core.domain.common.Result
import com.eventmonitor.core.domain.models.IncidentStatus
import com.eventmonitor.core.data.repository.interfaces.IncidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncidentListViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val branchId: String? = savedStateHandle.get<String>("branchId")

    private val _uiState = MutableStateFlow<IncidentListUiState>(IncidentListUiState.Loading)
    val uiState: StateFlow<IncidentListUiState> = _uiState.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    private val _selectedSeverity = MutableStateFlow<String?>(null)
    val selectedSeverity: StateFlow<String?> = _selectedSeverity.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadIncidents()
    }

    private fun loadIncidents() {
        viewModelScope.launch {
            val incidentsFlow = when {
                _searchQuery.value.isNotBlank() -> {
                    incidentRepository.searchIncidents(_searchQuery.value)
                }
                branchId != null && _selectedStatus.value != null -> {
                    incidentRepository.getIncidentsByVenueAndStatus(branchId, _selectedStatus.value!!)
                }
                branchId != null && _selectedSeverity.value != null -> {
                    incidentRepository.getIncidentsByVenueAndSeverity(branchId, _selectedSeverity.value!!)
                }
                branchId != null -> {
                    incidentRepository.getIncidentsByVenue(branchId)
                }
                _selectedStatus.value != null -> {
                    incidentRepository.getIncidentsByStatus(_selectedStatus.value!!)
                }
                _selectedSeverity.value != null -> {
                    incidentRepository.getIncidentsBySeverity(_selectedSeverity.value!!)
                }
                else -> {
                    incidentRepository.getAllIncidents()
                }
            }

            incidentsFlow.collect { incidents ->
                if (incidents.isEmpty()) {
                    _uiState.value = IncidentListUiState.Empty
                } else {
                    _uiState.value = IncidentListUiState.Success(incidents)
                }
            }
        }
    }

    fun filterByStatus(status: String?) {
        _selectedStatus.value = status
        _selectedSeverity.value = null
        loadIncidents()
    }

    fun filterBySeverity(severity: String?) {
        _selectedSeverity.value = severity
        _selectedStatus.value = null
        loadIncidents()
    }

    fun searchIncidents(query: String) {
        _searchQuery.value = query
        loadIncidents()
    }

    fun deleteIncident(incidentId: String) {
        viewModelScope.launch {
            when (incidentRepository.deleteIncident(incidentId)) {
                is Result.Success -> {
                    // Incident deleted successfully
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to delete incident"
                }
            }
        }
    }

    fun updateIncidentStatus(incidentId: String, status: String) {
        viewModelScope.launch {
            when (incidentRepository.updateIncidentStatus(incidentId, status)) {
                is Result.Success -> {
                    // Status updated successfully
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to update status"
                }
            }
        }
    }

    fun assignIncident(incidentId: String, assignedTo: String, status: String) {
        viewModelScope.launch {
            when (incidentRepository.assignIncident(incidentId, assignedTo, status)) {
                is Result.Success -> {
                    // Incident assigned successfully
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to assign incident"
                }
            }
        }
    }

    fun resolveIncident(incidentId: String, actionsTaken: String) {
        viewModelScope.launch {
            when (incidentRepository.resolveIncident(
                incidentId,
                IncidentStatus.RESOLVED.name,
                actionsTaken
            )) {
                is Result.Success -> {
                    // Incident resolved successfully
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to resolve incident"
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

sealed class IncidentListUiState {
    object Loading : IncidentListUiState()
    object Empty : IncidentListUiState()
    data class Success(val incidents: List<IncidentEntity>) : IncidentListUiState()
    data class Error(val message: String) : IncidentListUiState()
}
