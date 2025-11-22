package com.cop.app.headcounter.presentation.screens.branches

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cop.app.headcounter.domain.repository.BranchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BranchSetupViewModel @Inject constructor(
    private val branchRepository: BranchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val branchId: String? = savedStateHandle.get<String>("branchId")
    private val isNewBranch = branchId == null || branchId == "new"

    private val _uiState = MutableStateFlow(BranchSetupUiState())
    val uiState: StateFlow<BranchSetupUiState> = _uiState.asStateFlow()

    init {
        if (!isNewBranch && branchId != null) {
            loadBranch(branchId)
        }
    }

    private fun loadBranch(id: String) {
        viewModelScope.launch {
            branchRepository.getBranchById(id).collect { branchWithAreas ->
                branchWithAreas?.let {
                    _uiState.value = _uiState.value.copy(
                        name = it.branch.name,
                        location = it.branch.location,
                        code = it.branch.code,
                        contactPerson = it.branch.contactPerson,
                        contactEmail = it.branch.contactEmail,
                        contactPhone = it.branch.contactPhone,
                        color = it.branch.color,
                        isLoading = false
                    )
                }
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

    fun saveBranch(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank() || state.location.isBlank() || state.code.isBlank()) {
            _uiState.value = state.copy(error = "Please fill in all required fields")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = state.copy(isLoading = true)

                val newBranchId = branchRepository.createBranch(
                    name = state.name,
                    location = state.location,
                    code = state.code,
                    contactPerson = state.contactPerson,
                    contactEmail = state.contactEmail,
                    contactPhone = state.contactPhone,
                    color = state.color
                )

                _uiState.value = state.copy(isLoading = false)
                onSuccess(newBranchId)
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to save branch"
                )
            }
        }
    }
}

data class BranchSetupUiState(
    val name: String = "",
    val location: String = "",
    val code: String = "",
    val contactPerson: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val color: String = "#1976D2",
    val isLoading: Boolean = false,
    val error: String? = null
)
