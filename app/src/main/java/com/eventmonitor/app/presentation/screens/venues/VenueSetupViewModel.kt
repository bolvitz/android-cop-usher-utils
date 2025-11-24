package com.eventmonitor.app.presentation.screens.venues

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.domain.common.Result
import com.eventmonitor.core.data.repository.interfaces.VenueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VenueSetupViewModel @Inject constructor(
    private val venueRepository: VenueRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val venueId: String? = savedStateHandle.get<String>("venueId")
    private val isNewVenue = venueId == null || venueId == "new"

    private val _uiState = MutableStateFlow(VenueSetupUiState())
    val uiState: StateFlow<VenueSetupUiState> = _uiState.asStateFlow()

    init {
        if (!isNewVenue && venueId != null) {
            loadVenue(venueId)
        }
    }

    private fun loadVenue(id: String) {
        viewModelScope.launch {
            val venueWithAreas = venueRepository.getVenueById(id).first()
            venueWithAreas?.let {
                _uiState.value = _uiState.value.copy(
                    name = it.venue.name,
                    location = it.venue.location,
                    code = it.venue.code,
                    contactPerson = it.venue.contactPerson,
                    contactEmail = it.venue.contactEmail,
                    contactPhone = it.venue.contactPhone,
                    color = it.venue.color,
                    isHeadCountEnabled = it.venue.isHeadCountEnabled,
                    isLostAndFoundEnabled = it.venue.isLostAndFoundEnabled,
                    isIncidentReportingEnabled = it.venue.isIncidentReportingEnabled,
                    isLoading = false
                )
            }
        }
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateLocation(location: String) {
        _uiState.value = _uiState.value.copy(location = location)
    }

    fun updateCode(code: String) {
        _uiState.value = _uiState.value.copy(code = code)
    }

    fun updateContactPerson(contactPerson: String) {
        _uiState.value = _uiState.value.copy(contactPerson = contactPerson)
    }

    fun updateContactEmail(contactEmail: String) {
        _uiState.value = _uiState.value.copy(contactEmail = contactEmail)
    }

    fun updateContactPhone(contactPhone: String) {
        _uiState.value = _uiState.value.copy(contactPhone = contactPhone)
    }

    fun updateHeadCountEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isHeadCountEnabled = enabled)
    }

    fun updateLostAndFoundEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isLostAndFoundEnabled = enabled)
    }

    fun updateIncidentReportingEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isIncidentReportingEnabled = enabled)
    }

    fun saveBranch(onSuccess: (String) -> Unit) {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (isNewVenue) {
                // Create new branch
                val result = venueRepository.createVenue(
                    name = state.name,
                    location = state.location,
                    code = state.code,
                    contactPerson = state.contactPerson,
                    contactEmail = state.contactEmail,
                    contactPhone = state.contactPhone,
                    color = state.color,
                    isHeadCountEnabled = state.isHeadCountEnabled,
                    isLostAndFoundEnabled = state.isLostAndFoundEnabled,
                    isIncidentReportingEnabled = state.isIncidentReportingEnabled
                )

                when (result) {
                    is Result.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        onSuccess(result.data)
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.error.toUserMessage()
                            )
                        }
                    }
                }
            } else {
                // Update existing branch
                val venueWithAreas = venueRepository.getVenueById(venueId!!).first()
                if (venueWithAreas != null) {
                    val updatedBranch = venueWithAreas.venue.copy(
                        name = state.name,
                        location = state.location,
                        code = state.code,
                        contactPerson = state.contactPerson,
                        contactEmail = state.contactEmail,
                        contactPhone = state.contactPhone,
                        color = state.color,
                        isHeadCountEnabled = state.isHeadCountEnabled,
                        isLostAndFoundEnabled = state.isLostAndFoundEnabled,
                        isIncidentReportingEnabled = state.isIncidentReportingEnabled,
                        updatedAt = System.currentTimeMillis()
                    )

                    val result = venueRepository.updateVenue(updatedBranch)
                    when (result) {
                        is Result.Success -> {
                            _uiState.update { it.copy(isLoading = false) }
                            onSuccess(venueId)
                        }
                        is Result.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = result.error.toUserMessage()
                                )
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Branch not found"
                        )
                    }
                }
            }
        }
    }

    fun isEditMode(): Boolean = !isNewVenue

    fun getBranchId(): String? = if (isNewVenue) null else venueId
}

data class VenueSetupUiState(
    val name: String = "",
    val location: String = "",
    val code: String = "",
    val contactPerson: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val color: String = "#1976D2",
    val isHeadCountEnabled: Boolean = true,
    val isLostAndFoundEnabled: Boolean = false,
    val isIncidentReportingEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
