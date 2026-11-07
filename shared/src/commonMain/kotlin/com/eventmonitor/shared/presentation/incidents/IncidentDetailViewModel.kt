package com.eventmonitor.shared.presentation.incidents

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.IncidentDto
import com.eventmonitor.shared.data.repository.IncidentRepository
import com.eventmonitor.shared.domain.models.IncidentStatus
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IncidentDetailViewModel(
    private val incidentRepository: IncidentRepository,
    private val incidentId: String
) : SharedViewModel() {

    val incident: StateFlow<IncidentDto?> =
        incidentRepository.getIncidentById(incidentId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(updated: IncidentDto) {
        viewModelScope.launch { incidentRepository.updateIncident(updated) }
    }

    /** Field-wise save for Swift, which cannot use Kotlin's `copy` default arguments. */
    fun save(
        title: String,
        description: String,
        severity: String,
        category: String,
        location: String,
        assignedTo: String
    ) {
        val current = incident.value ?: return
        save(
            current.copy(
                title = title,
                description = description,
                severity = severity,
                category = category,
                location = location,
                assignedTo = assignedTo
            )
        )
    }

    fun updateStatus(status: String) {
        viewModelScope.launch { incidentRepository.updateIncidentStatus(incidentId, status) }
    }

    fun assign(assignedTo: String) {
        viewModelScope.launch {
            incidentRepository.assignIncident(incidentId, assignedTo, IncidentStatus.IN_PROGRESS.name)
        }
    }

    fun resolve(actionsTaken: String) {
        viewModelScope.launch {
            incidentRepository.resolveIncident(incidentId, IncidentStatus.RESOLVED.name, actionsTaken)
        }
    }

    fun delete() {
        viewModelScope.launch { incidentRepository.deleteIncident(incidentId) }
    }
}
