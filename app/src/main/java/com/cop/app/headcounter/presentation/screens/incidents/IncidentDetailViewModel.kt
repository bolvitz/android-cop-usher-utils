package com.cop.app.headcounter.presentation.screens.incidents

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cop.app.headcounter.data.local.entities.IncidentEntity
import com.cop.app.headcounter.domain.common.Result
import com.cop.app.headcounter.domain.models.IncidentStatus
import com.cop.app.headcounter.domain.repository.IncidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncidentDetailViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val incidentId: String = savedStateHandle.get<String>("incidentId") ?: ""

    val incident: Flow<IncidentEntity?> = incidentRepository.getIncidentById(incidentId)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun resolveIncident(actionsTaken: String) {
        viewModelScope.launch {
            when (incidentRepository.resolveIncident(
                incidentId,
                IncidentStatus.RESOLVED.name,
                actionsTaken
            )) {
                is Result.Success -> {
                    // Successfully resolved
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
