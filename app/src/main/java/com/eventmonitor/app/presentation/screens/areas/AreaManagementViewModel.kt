package com.eventmonitor.app.presentation.screens.areas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.AreaTemplateEntity
import com.eventmonitor.core.domain.models.AreaType
import com.eventmonitor.core.data.repository.interfaces.AreaRepository
import com.eventmonitor.core.data.repository.interfaces.BranchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AreaManagementViewModel @Inject constructor(
    private val areaRepository: AreaRepository,
    private val branchRepository: BranchRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val branchId: String = checkNotNull(savedStateHandle.get<String>("branchId"))

    private val _uiState = MutableStateFlow(AreaManagementUiState())
    val uiState: StateFlow<AreaManagementUiState> = _uiState.asStateFlow()

    init {
        loadBranchInfo()
        loadAreas()
    }

    private fun loadBranchInfo() {
        viewModelScope.launch {
            branchRepository.getBranchById(branchId).collect { branchWithAreas ->
                branchWithAreas?.let {
                    _uiState.value = _uiState.value.copy(
                        branchName = it.branch.name
                    )
                }
            }
        }
    }

    private fun loadAreas() {
        viewModelScope.launch {
            areaRepository.getAreasByBranch(branchId).collect { areas ->
                _uiState.value = _uiState.value.copy(
                    areas = areas,
                    isLoading = false
                )
            }
        }
    }

    fun addArea(name: String, type: AreaType, capacity: Int) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Area name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val currentAreas = _uiState.value.areas
                val displayOrder = currentAreas.maxOfOrNull { it.displayOrder }?.plus(1) ?: 0

                areaRepository.createArea(
                    branchId = branchId,
                    name = name,
                    type = type,
                    capacity = capacity,
                    displayOrder = displayOrder
                )

                _uiState.value = _uiState.value.copy(
                    showAddDialog = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to add area"
                )
            }
        }
    }

    fun deleteArea(areaId: String) {
        viewModelScope.launch {
            try {
                // Check if area has counts in any service
                if (areaRepository.hasAreaCounts(areaId)) {
                    _uiState.value = _uiState.value.copy(
                        error = "Cannot delete area: It has attendance counts in one or more services. Delete those services first."
                    )
                    return@launch
                }

                areaRepository.deleteArea(areaId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete area"
                )
            }
        }
    }

    fun updateArea(area: AreaTemplateEntity) {
        viewModelScope.launch {
            try {
                areaRepository.updateArea(area)
                _uiState.value = _uiState.value.copy(
                    editingArea = null,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update area"
                )
            }
        }
    }

    fun reorderAreas(areas: List<AreaTemplateEntity>) {
        viewModelScope.launch {
            try {
                val areaIds = areas.map { it.id }
                areaRepository.reorderAreas(areaIds)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to reorder areas"
                )
            }
        }
    }

    fun toggleAddDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddDialog = show)
    }

    fun setEditingArea(area: AreaTemplateEntity?) {
        _uiState.value = _uiState.value.copy(editingArea = area)
    }

    fun createQuickAreas(areaType: AreaType, count: Int, startNumber: Int = 1) {
        viewModelScope.launch {
            try {
                val currentAreas = _uiState.value.areas
                var displayOrder = currentAreas.maxOfOrNull { it.displayOrder }?.plus(1) ?: 0

                repeat(count) { index ->
                    val number = startNumber + index
                    val name = when (areaType) {
                        AreaType.SEATING -> "Seating $number"
                        AreaType.STANDING -> "Standing $number"
                        AreaType.VIP -> "VIP $number"
                        AreaType.GENERAL_ADMISSION -> "General Admission $number"
                        AreaType.OVERFLOW -> "Overflow $number"
                        AreaType.PARKING -> "Parking $number"
                        AreaType.REGISTRATION -> "Registration $number"
                        AreaType.LOBBY -> "Lobby $number"
                        AreaType.OUTDOOR -> "Outdoor $number"
                        AreaType.STAGE -> "Stage $number"
                        AreaType.BACKSTAGE -> "Backstage $number"
                        AreaType.CARE_ROOM -> "Care Room $number"
                        AreaType.FOOD_AREA -> "Food Area $number"
                        AreaType.RESTROOMS -> "Restrooms $number"
                        AreaType.EMERGENCY_EXIT -> "Emergency Exit $number"
                        AreaType.OTHER -> "Area $number"
                    }

                    areaRepository.createArea(
                        branchId = branchId,
                        name = name,
                        type = areaType,
                        capacity = 100,
                        displayOrder = displayOrder
                    )
                    displayOrder++
                }

                _uiState.value = _uiState.value.copy(
                    showQuickAddDialog = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create areas"
                )
            }
        }
    }

    fun toggleQuickAddDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showQuickAddDialog = show)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AreaManagementUiState(
    val branchName: String = "",
    val areas: List<AreaTemplateEntity> = emptyList(),
    val showAddDialog: Boolean = false,
    val showQuickAddDialog: Boolean = false,
    val editingArea: AreaTemplateEntity? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
