package com.eventmonitor.shared.presentation.incidents

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.IncidentDto
import com.eventmonitor.shared.data.repository.IncidentRepository
import com.eventmonitor.shared.domain.models.IncidentStatus
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IncidentFilters(
    val status: String? = null,
    val severity: String? = null,
    val query: String = ""
)

data class IncidentListUiState(
    val isLoading: Boolean = true,
    val incidents: List<IncidentDto> = emptyList(),
    val filters: IncidentFilters = IncidentFilters(),
    val error: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && incidents.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentListViewModel(
    private val incidentRepository: IncidentRepository,
    private val venueId: String? = null
) : SharedViewModel() {

    private val _filters = MutableStateFlow(IncidentFilters())
    private val _error = MutableStateFlow<String?>(null)

    private val incidentsFlow: Flow<List<IncidentDto>> =
        _filters.flatMapLatest { f -> sourceFor(f) }

    val uiState: StateFlow<IncidentListUiState> =
        combine(incidentsFlow, _filters, _error) { incidents, filters, error ->
            IncidentListUiState(
                isLoading = false,
                incidents = incidents,
                filters = filters,
                error = error
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IncidentListUiState())

    private fun sourceFor(f: IncidentFilters): Flow<List<IncidentDto>> = when {
        f.query.isNotBlank() -> incidentRepository.searchIncidents(f.query)
        venueId != null && f.status != null -> incidentRepository.getIncidentsByVenueAndStatus(venueId, f.status)
        venueId != null && f.severity != null -> incidentRepository.getIncidentsByVenueAndSeverity(venueId, f.severity)
        venueId != null -> incidentRepository.getIncidentsByVenue(venueId)
        f.status != null -> incidentRepository.getIncidentsByStatus(f.status)
        f.severity != null -> incidentRepository.getIncidentsBySeverity(f.severity)
        else -> incidentRepository.getAllIncidents()
    }

    fun filterByStatus(status: String?) =
        _filters.update { it.copy(status = status, severity = null, query = "") }

    fun filterBySeverity(severity: String?) =
        _filters.update { it.copy(severity = severity, status = null, query = "") }

    fun search(query: String) = _filters.update { it.copy(query = query) }

    fun createIncident(
        title: String,
        description: String,
        severity: String,
        category: String = "",
        location: String = "",
        reportedBy: String = ""
    ) {
        val venue = venueId ?: return
        viewModelScope.launch {
            incidentRepository.createIncident(
                venueId = venue,
                title = title,
                description = description,
                severity = severity,
                category = category,
                location = location,
                reportedBy = reportedBy
            ).onError { _error.value = it.toUserMessage() }
        }
    }

    fun updateStatus(incidentId: String, status: String) {
        viewModelScope.launch {
            incidentRepository.updateIncidentStatus(incidentId, status)
                .onError { _error.value = it.toUserMessage() }
        }
    }

    fun resolve(incidentId: String, actionsTaken: String) {
        viewModelScope.launch {
            incidentRepository.resolveIncident(incidentId, IncidentStatus.RESOLVED.name, actionsTaken)
                .onError { _error.value = it.toUserMessage() }
        }
    }

    fun delete(incidentId: String) {
        viewModelScope.launch {
            incidentRepository.deleteIncident(incidentId)
                .onError { _error.value = it.toUserMessage() }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
