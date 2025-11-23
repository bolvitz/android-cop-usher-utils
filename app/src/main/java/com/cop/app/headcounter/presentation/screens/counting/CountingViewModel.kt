package com.cop.app.headcounter.presentation.screens.counting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cop.app.headcounter.data.local.entities.AreaTemplateEntity
import com.cop.app.headcounter.data.local.entities.ServiceTypeEntity
import com.cop.app.headcounter.domain.models.ServiceType
import com.cop.app.headcounter.domain.repository.AreaCountRepository
import com.cop.app.headcounter.domain.repository.BranchRepository
import com.cop.app.headcounter.domain.repository.ServiceRepository
import com.cop.app.headcounter.domain.repository.ServiceTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CountingViewModel @Inject constructor(
    private val branchRepository: BranchRepository,
    private val serviceRepository: ServiceRepository,
    private val serviceTypeRepository: ServiceTypeRepository,
    private val areaCountRepository: AreaCountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val branchId: String = checkNotNull(savedStateHandle.get<String>("branchId"))
    private val existingServiceId: String? = savedStateHandle.get<String>("serviceId")

    val serviceTypes = serviceTypeRepository.getAllServiceTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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
        // If an existing service ID is provided, load it
        existingServiceId?.let { serviceId ->
            _uiState.update { it.copy(serviceId = serviceId) }
            loadServiceDetails(serviceId)
        }
    }

    private fun loadBranch() {
        viewModelScope.launch {
            branchRepository.getBranchById(branchId).collect { branchWithAreas ->
                branchWithAreas?.let {
                    _uiState.update { currentState ->
                        currentState.copy(
                            branchName = it.branch.name,
                            branchCode = it.branch.code,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun createNewService(
        serviceTypeId: String,
        serviceTypeName: String,
        date: Long,
        countedBy: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val serviceId = serviceRepository.createNewService(
                    branchId = branchId,
                    serviceType = ServiceType.SUNDAY_AM, // Deprecated, kept for compatibility
                    date = date,
                    countedBy = countedBy,
                    serviceName = serviceTypeName,
                    serviceTypeId = serviceTypeId
                )

                _uiState.update { currentState ->
                    currentState.copy(
                        serviceId = serviceId,
                        serviceType = ServiceType.SUNDAY_AM, // Deprecated
                        serviceDate = date,
                        serviceName = serviceTypeName,
                        counterName = countedBy,
                        isLoading = false
                    )
                }

                loadServiceDetails(serviceId)
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun loadServiceDetails(serviceId: String) {
        // Combine both flows to avoid flickering from separate updates
        viewModelScope.launch {
            combine(
                serviceRepository.getServiceById(serviceId),
                areaCountRepository.getAreaCountsByService(serviceId)
            ) { serviceWithDetails, areaCounts ->
                Pair(serviceWithDetails, areaCounts)
            }.collect { (serviceWithDetails, areaCounts) ->
                serviceWithDetails?.let { details ->
                    val areaCountStates = areaCounts.map { areaCountWithTemplate ->
                        AreaCountState(
                            id = areaCountWithTemplate.areaCount.id,
                            template = areaCountWithTemplate.template,
                            count = areaCountWithTemplate.areaCount.count,
                            capacity = areaCountWithTemplate.areaCount.capacity,
                            notes = areaCountWithTemplate.areaCount.notes,
                            percentage = if (areaCountWithTemplate.areaCount.capacity > 0) {
                                (areaCountWithTemplate.areaCount.count.toFloat() / areaCountWithTemplate.areaCount.capacity * 100).toInt()
                            } else 0,
                            lastUpdated = areaCountWithTemplate.areaCount.lastUpdated
                        )
                    }

                    // Single atomic update to prevent flickering
                    _uiState.update { currentState ->
                        currentState.copy(
                            totalAttendance = details.service.totalAttendance,
                            totalCapacity = details.service.totalCapacity,
                            isLocked = details.service.isLocked,
                            areaCounts = areaCountStates
                        )
                    }
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
                _uiState.update { it.copy(shareableReport = report) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private suspend fun getCurrentCount(areaCountId: String): Int {
        return _uiState.value.areaCounts.find { it.id == areaCountId }?.count ?: 0
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
