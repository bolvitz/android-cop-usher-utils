package com.cop.app.headcounter.presentation.screens.incidents

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cop.app.headcounter.domain.common.Result
import com.cop.app.headcounter.domain.models.IncidentSeverity
import com.cop.app.headcounter.domain.repository.IncidentRepository
import com.cop.app.headcounter.presentation.utils.IncidentNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditIncidentViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val branchId: String = savedStateHandle.get<String>("branchId") ?: ""
    private val incidentId: String? = savedStateHandle.get<String>("incidentId")

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _severity = MutableStateFlow(IncidentSeverity.LOW.name)
    val severity: StateFlow<String> = _severity.asStateFlow()

    private val _category = MutableStateFlow("")
    val category: StateFlow<String> = _category.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _photoUri = MutableStateFlow("")
    val photoUri: StateFlow<String> = _photoUri.asStateFlow()

    private val _reportedBy = MutableStateFlow("")
    val reportedBy: StateFlow<String> = _reportedBy.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        if (incidentId != null) {
            loadIncident(incidentId)
        }
    }

    private fun loadIncident(id: String) {
        viewModelScope.launch {
            incidentRepository.getIncidentById(id).collect { incident ->
                incident?.let {
                    _title.value = it.title
                    _description.value = it.description
                    _severity.value = it.severity
                    _category.value = it.category
                    _location.value = it.location
                    _photoUri.value = it.photoUri
                    _reportedBy.value = it.reportedBy
                    _notes.value = it.notes
                }
            }
        }
    }

    fun updateTitle(value: String) {
        _title.value = value
    }

    fun updateDescription(value: String) {
        _description.value = value
    }

    fun updateSeverity(value: String) {
        _severity.value = value
    }

    fun updateCategory(value: String) {
        _category.value = value
    }

    fun updateLocation(value: String) {
        _location.value = value
    }

    fun updatePhotoUri(uri: String) {
        _photoUri.value = uri
    }

    fun updateReportedBy(value: String) {
        _reportedBy.value = value
    }

    fun updateNotes(value: String) {
        _notes.value = value
    }

    fun saveIncident() {
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null

            val result = if (incidentId != null) {
                // Update existing incident
                val incident = incidentRepository.getIncidentById(incidentId).first()
                incident?.let {
                    incidentRepository.updateIncident(
                        it.copy(
                            title = _title.value,
                            description = _description.value,
                            severity = _severity.value,
                            category = _category.value,
                            location = _location.value,
                            photoUri = _photoUri.value,
                            reportedBy = _reportedBy.value,
                            notes = _notes.value
                        )
                    )
                } ?: Result.Success(Unit)
            } else {
                // Create new incident
                val createResult = incidentRepository.createIncident(
                    branchId = branchId,
                    title = _title.value,
                    description = _description.value,
                    severity = _severity.value,
                    category = _category.value,
                    location = _location.value,
                    photoUri = _photoUri.value,
                    reportedBy = _reportedBy.value,
                    notes = _notes.value
                )

                // Show notification for high/critical incidents
                if (createResult is Result.Success) {
                    val newIncidentId = createResult.data
                    val newIncident = incidentRepository.getIncidentById(newIncidentId).first()
                    newIncident?.let {
                        IncidentNotificationHelper.showIncidentNotification(application, it)
                    }
                }

                createResult
            }

            when (result) {
                is Result.Success -> {
                    _saveSuccess.value = true
                }
                is Result.Error -> {
                    _errorMessage.value = result.error.message
                }
            }

            _isSaving.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
