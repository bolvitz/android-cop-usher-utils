package com.cop.app.headcounter.presentation.screens.counting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cop.app.headcounter.data.local.entities.AreaTemplateEntity
import com.cop.app.headcounter.domain.models.ServiceType
import com.cop.app.headcounter.domain.repository.BranchRepository
import com.cop.app.headcounter.domain.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CountingViewModel @Inject constructor(
    private val branchRepository: BranchRepository,
    private val serviceRepository: ServiceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val branchId: String = checkNotNull(savedStateHandle.get<String>("branchId"))

    private val _uiState = MutableStateFlow(CountingUiState())
    val uiState: StateFlow<CountingUiState> = _uiState.asStateFlow()

    private val _undoStack = MutableStateFlow<List<CountAction>>(emptyList())
    private val _redoStack = MutableStateFlow<List<CountAction>>(emptyList())

    val canUndo: StateFlow<Boolean> = _undoStack.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canRedo: StateFlow<Boolean> = _redoStack.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadBranch()
    }

    private fun loadBranch() {
        viewModelScope.launch {
            branchRepository.getBranchById(branchId).collect { branchWithAreas ->
                branchWithAreas?.let {
                    _uiState.value = _uiState.value.copy(
                        branchName = it.branch.name,
                        branchCode = it.branch.code,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun createNewService(
        serviceType: ServiceType,
        date: Long,
        countedBy: String,
        serviceName: String = ""
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val serviceId = serviceRepository.createNewService(
                    branchId = branchId,
                    serviceType = serviceType,
                    date = date,
                    countedBy = countedBy,
                    serviceName = serviceName
                )

                _uiState.value = _uiState.value.copy(
                    serviceId = serviceId,
                    serviceType = serviceType,
                    serviceDate = date,
                    serviceName = serviceName,
                    counterName = countedBy,
                    isLoading = false
                )

                loadServiceDetails(serviceId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    private fun loadServiceDetails(serviceId: String) {
        viewModelScope.launch {
            serviceRepository.getServiceById(serviceId).collect { serviceWithDetails ->
                serviceWithDetails?.let { details ->
                    // Load area counts
                    _uiState.value = _uiState.value.copy(
                        totalAttendance = details.service.totalAttendance,
                        totalCapacity = details.service.totalCapacity,
                        isLocked = details.service.isLocked
                    )
                }
            }
        }
    }

    fun incrementCount(areaCountId: String, amount: Int = 1) {
        val serviceId = _uiState.value.serviceId ?: return
        if (_uiState.value.isLocked) return

        viewModelScope.launch {
            val oldCount = getCurrentCount(areaCountId)

            serviceRepository.incrementAreaCount(serviceId, areaCountId, amount)

            // Add to undo stack
            _undoStack.value += CountAction.UpdateCount(
                serviceId = serviceId,
                areaCountId = areaCountId,
                oldCount = oldCount,
                newCount = oldCount + amount
            )

            // Clear redo stack
            _redoStack.value = emptyList()
        }
    }

    fun decrementCount(areaCountId: String, amount: Int = 1) {
        incrementCount(areaCountId, -amount)
    }

    fun setCount(areaCountId: String, newCount: Int) {
        val serviceId = _uiState.value.serviceId ?: return
        if (_uiState.value.isLocked) return

        viewModelScope.launch {
            val oldCount = getCurrentCount(areaCountId)

            serviceRepository.updateServiceCount(
                serviceId = serviceId,
                areaCountId = areaCountId,
                newCount = newCount,
                action = "MANUAL_EDIT"
            )

            _undoStack.value += CountAction.UpdateCount(
                serviceId = serviceId,
                areaCountId = areaCountId,
                oldCount = oldCount,
                newCount = newCount
            )

            _redoStack.value = emptyList()
        }
    }

    fun undo() {
        val action = _undoStack.value.lastOrNull() ?: return

        viewModelScope.launch {
            when (action) {
                is CountAction.UpdateCount -> {
                    serviceRepository.updateServiceCount(
                        serviceId = action.serviceId,
                        areaCountId = action.areaCountId,
                        newCount = action.oldCount,
                        action = "UNDO"
                    )

                    _undoStack.value = _undoStack.value.dropLast(1)
                    _redoStack.value += action
                }
            }
        }
    }

    fun redo() {
        val action = _redoStack.value.lastOrNull() ?: return

        viewModelScope.launch {
            when (action) {
                is CountAction.UpdateCount -> {
                    serviceRepository.updateServiceCount(
                        serviceId = action.serviceId,
                        areaCountId = action.areaCountId,
                        newCount = action.newCount,
                        action = "REDO"
                    )

                    _redoStack.value = _redoStack.value.dropLast(1)
                    _undoStack.value += action
                }
            }
        }
    }

    fun lockService() {
        val serviceId = _uiState.value.serviceId ?: return
        viewModelScope.launch {
            serviceRepository.lockService(serviceId)
        }
    }

    fun unlockService() {
        val serviceId = _uiState.value.serviceId ?: return
        viewModelScope.launch {
            serviceRepository.unlockService(serviceId)
        }
    }

    fun updateNotes(notes: String) {
        val serviceId = _uiState.value.serviceId ?: return
        viewModelScope.launch {
            serviceRepository.updateServiceNotes(serviceId, notes)
        }
    }

    fun shareReport() {
        val serviceId = _uiState.value.serviceId ?: return
        viewModelScope.launch {
            try {
                val report = serviceRepository.exportServiceReport(serviceId)
                _uiState.value = _uiState.value.copy(shareableReport = report)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private suspend fun getCurrentCount(areaCountId: String): Int {
        // This would need to be implemented by fetching from the repository
        return 0 // Placeholder
    }
}

data class CountingUiState(
    val branchName: String = "",
    val branchCode: String = "",
    val serviceId: String? = null,
    val serviceType: ServiceType = ServiceType.SUNDAY_AM,
    val serviceDate: Long = System.currentTimeMillis(),
    val serviceName: String = "",
    val counterName: String = "",
    val areaCounts: List<AreaCountState> = emptyList(),
    val totalAttendance: Int = 0,
    val totalCapacity: Int = 0,
    val notes: String = "",
    val weather: String = "",
    val isLocked: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val shareableReport: String? = null
)

data class AreaCountState(
    val id: String,
    val template: AreaTemplateEntity,
    val count: Int,
    val capacity: Int,
    val notes: String,
    val percentage: Int,
    val lastUpdated: Long
)

sealed class CountAction {
    data class UpdateCount(
        val serviceId: String,
        val areaCountId: String,
        val oldCount: Int,
        val newCount: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : CountAction()
}
