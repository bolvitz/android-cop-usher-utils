package com.eventmonitor.feature.headcounter.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.AreaTemplateEntity
import com.eventmonitor.core.data.repository.interfaces.AreaCountRepository
import com.eventmonitor.core.data.repository.interfaces.EventRepository
import com.eventmonitor.core.data.repository.interfaces.EventTypeRepository
import com.eventmonitor.core.data.repository.interfaces.SeatMapRepository
import com.eventmonitor.core.data.repository.interfaces.VenueRepository
import com.eventmonitor.core.domain.models.ServiceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CountingViewModel constructor(
    private val venueRepository: VenueRepository,
    private val eventRepository: EventRepository,
    private val eventTypeRepository: EventTypeRepository,
    private val areaCountRepository: AreaCountRepository,
    private val seatMapRepository: SeatMapRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val venueId: String = checkNotNull(savedStateHandle.get<String>("venueId"))
    private val existingServiceId: String? = savedStateHandle.get<String>("serviceId")

    val eventTypes = eventTypeRepository.getAllServiceTypes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(CountingUiState())
    val uiState: StateFlow<CountingUiState> = _uiState.asStateFlow()

    private val _undoStack = MutableStateFlow<List<CountAction>>(emptyList())
    private val _redoStack = MutableStateFlow<List<CountAction>>(emptyList())
    private val _excludedAreaIds = MutableStateFlow<Set<String>>(emptySet())
    private val counterMutex = Mutex()

    val canUndo: StateFlow<Boolean> = _undoStack.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canRedo: StateFlow<Boolean> = _redoStack.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // If an existing service ID is provided, load it (venue is fetched inside loadServiceDetails)
        existingServiceId?.let { serviceId ->
            _uiState.update { it.copy(eventId = serviceId) }
            loadServiceDetails(serviceId)
        } ?: run {
            // No service yet — still need venue name for the UI
            viewModelScope.launch {
                venueRepository.getVenueById(venueId).collect { venueWithAreas ->
                    venueWithAreas?.let {
                        _uiState.update { state ->
                            state.copy(
                                branchName = it.venue.name,
                                branchCode = it.venue.code,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun createNewService(
        eventTypeId: String,
        eventTypeName: String,
        date: Long,
        countedBy: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val serviceId = eventRepository.createNewEvent(
                    venueId = venueId,
                    eventType = ServiceType.GENERAL, // Default event type
                    date = date,
                    countedBy = countedBy,
                    eventName = eventTypeName,
                    eventTypeId = eventTypeId
                )

                _uiState.update { currentState ->
                    currentState.copy(
                        eventId = serviceId,
                        eventType = ServiceType.GENERAL,
                        serviceDate = date,
                        eventName = eventTypeName,
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
        // Combine all flows to avoid flickering from separate updates
        viewModelScope.launch {
            combine(
                eventRepository.getEventById(serviceId),
                areaCountRepository.getAreaCountsByService(serviceId),
                _excludedAreaIds,
                venueRepository.getVenueById(venueId),
                seatMapRepository.observeOccupiedCountsByArea(serviceId)
            ) { serviceWithDetails, areaCounts, excludedIds, venueWithAreas, seatOccupancy ->
                object {
                    val service = serviceWithDetails
                    val counts = areaCounts
                    val excluded = excludedIds
                    val venue = venueWithAreas
                    val seatCounts = seatOccupancy
                }
            }.collect { data ->
                data.service?.let { details ->
                    val areaCountStates = data.counts.map { areaCountWithTemplate ->
                        val template = areaCountWithTemplate.template
                        val rawCount = areaCountWithTemplate.areaCount.count
                        // For seat-mapped areas the live count is derived from
                        // seat_statuses; otherwise fall back to the +/- counter.
                        val effectiveCount = if (template.hasSeatMap) {
                            data.seatCounts[template.id] ?: 0
                        } else rawCount
                        AreaCountState(
                            id = areaCountWithTemplate.areaCount.id,
                            template = template,
                            count = effectiveCount,
                            capacity = areaCountWithTemplate.areaCount.capacity,
                            notes = areaCountWithTemplate.areaCount.notes,
                            isIncluded = areaCountWithTemplate.areaCount.id !in data.excluded,
                            percentage = if (areaCountWithTemplate.areaCount.capacity > 0) {
                                (effectiveCount.toFloat() / areaCountWithTemplate.areaCount.capacity * 100).toInt()
                            } else 0,
                            lastUpdated = areaCountWithTemplate.areaCount.lastUpdated
                        )
                    }

                    val includedAreas = areaCountStates.filter { it.isIncluded }
                    val totalAttendance = includedAreas.sumOf { it.count }
                    val totalCapacity = includedAreas.sumOf { it.capacity }

                    // Single atomic update to prevent flickering
                    _uiState.update { currentState ->
                        currentState.copy(
                            branchName = data.venue?.venue?.name ?: currentState.branchName,
                            branchCode = data.venue?.venue?.code ?: currentState.branchCode,
                            totalAttendance = totalAttendance,
                            totalCapacity = totalCapacity,
                            isLocked = details.event.isLocked,
                            areaCounts = areaCountStates,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun toggleAreaInclusion(areaCountId: String) {
        _excludedAreaIds.update { current ->
            if (areaCountId in current) current - areaCountId else current + areaCountId
        }
    }

    fun incrementCount(areaCountId: String, amount: Int = 1) {
        val eventId = _uiState.value.eventId ?: return
        if (_uiState.value.isLocked) return

        viewModelScope.launch {
            counterMutex.withLock {
                val oldCount = getCurrentCount(areaCountId)

                eventRepository.incrementAreaCount(eventId, areaCountId, amount)

                // Add to undo stack, capped at 50
                val newStack = (_undoStack.value + CountAction.UpdateCount(
                    eventId = eventId,
                    areaCountId = areaCountId,
                    oldCount = oldCount,
                    newCount = oldCount + amount
                )).takeLast(50)
                _undoStack.value = newStack

                // Clear redo stack
                _redoStack.value = emptyList()
            }
        }
    }

    fun decrementCount(areaCountId: String, amount: Int = 1) {
        incrementCount(areaCountId, -amount)
    }

    fun setCount(areaCountId: String, newCount: Int) {
        val eventId = _uiState.value.eventId ?: return
        if (_uiState.value.isLocked) return

        viewModelScope.launch {
            counterMutex.withLock {
                val oldCount = getCurrentCount(areaCountId)

                eventRepository.updateEventCount(
                    eventId = eventId,
                    areaCountId = areaCountId,
                    newCount = newCount,
                    action = "MANUAL_EDIT"
                )

                val newStack = (_undoStack.value + CountAction.UpdateCount(
                    eventId = eventId,
                    areaCountId = areaCountId,
                    oldCount = oldCount,
                    newCount = newCount
                )).takeLast(50)
                _undoStack.value = newStack

                _redoStack.value = emptyList()
            }
        }
    }

    fun undo() {
        val action = _undoStack.value.lastOrNull() ?: return

        viewModelScope.launch {
            when (action) {
                is CountAction.UpdateCount -> {
                    eventRepository.updateEventCount(
                        eventId = action.eventId,
                        areaCountId = action.areaCountId,
                        newCount = action.oldCount,
                        action = "UNDO"
                    )

                    _undoStack.value = _undoStack.value.dropLast(1)
                    _redoStack.value += action
                }

                is CountAction.SeatStatus -> {
                    seatMapRepository.setSeatStatus(
                        eventId = action.eventId,
                        seatId = action.seatId,
                        status = action.oldStatus,
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
                    eventRepository.updateEventCount(
                        eventId = action.eventId,
                        areaCountId = action.areaCountId,
                        newCount = action.newCount,
                        action = "REDO"
                    )

                    _redoStack.value = _redoStack.value.dropLast(1)
                    _undoStack.value += action
                }

                is CountAction.SeatStatus -> {
                    seatMapRepository.setSeatStatus(
                        eventId = action.eventId,
                        seatId = action.seatId,
                        status = action.newStatus,
                    )
                    _redoStack.value = _redoStack.value.dropLast(1)
                    _undoStack.value += action
                }
            }
        }
    }

    fun lockEvent() {
        val eventId = _uiState.value.eventId ?: return
        viewModelScope.launch {
            eventRepository.lockEvent(eventId)
        }
    }

    fun unlockEvent() {
        val eventId = _uiState.value.eventId ?: return
        viewModelScope.launch {
            eventRepository.unlockEvent(eventId)
        }
    }

    fun updateNotes(notes: String) {
        val eventId = _uiState.value.eventId ?: return
        viewModelScope.launch {
            eventRepository.updateEventNotes(eventId, notes)
        }
    }

    fun generateReport() {
        val eventId = _uiState.value.eventId ?: return
        viewModelScope.launch {
            try {
                val report = eventRepository.exportEventReport(eventId)
                _uiState.update { it.copy(shareableReport = report) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearReport() {
        _uiState.update { it.copy(shareableReport = null) }
    }

    /** Live rows + seats for an area's seat map (re-emits on edits). */
    fun observeSeatLayout(areaTemplateId: String) =
        seatMapRepository.observeRowsWithSeats(areaTemplateId)

    /** Live (eventId-scoped) statuses for a single area; absence = AVAILABLE. */
    fun observeSeatStatuses(areaTemplateId: String) =
        _uiState.value.eventId?.let { eventId ->
            seatMapRepository.observeStatusesForArea(eventId, areaTemplateId)
        }

    /** Cycle a seat: AVAILABLE → OCCUPIED → RESERVED → BLOCKED → AVAILABLE. */
    fun cycleSeatStatus(seatId: String, currentStatus: String) {
        val eventId = _uiState.value.eventId ?: return
        if (_uiState.value.isLocked) return
        val next = when (currentStatus) {
            "AVAILABLE" -> "OCCUPIED"
            "OCCUPIED" -> "RESERVED"
            "RESERVED" -> "BLOCKED"
            else -> "AVAILABLE"
        }
        viewModelScope.launch {
            counterMutex.withLock {
                seatMapRepository.setSeatStatus(eventId, seatId, next)
                val newStack = (_undoStack.value + CountAction.SeatStatus(
                    eventId = eventId,
                    seatId = seatId,
                    oldStatus = currentStatus,
                    newStatus = next,
                )).takeLast(50)
                _undoStack.value = newStack
                _redoStack.value = emptyList()
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
    val eventId: String? = null,
    val eventType: ServiceType = ServiceType.GENERAL,
    val serviceDate: Long = System.currentTimeMillis(),
    val eventName: String = "",
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
    val isIncluded: Boolean = true,
    val percentage: Int,
    val lastUpdated: Long
)

sealed class CountAction {
    data class UpdateCount(
        val eventId: String,
        val areaCountId: String,
        val oldCount: Int,
        val newCount: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : CountAction()

    data class SeatStatus(
        val eventId: String,
        val seatId: String,
        val oldStatus: String,
        val newStatus: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : CountAction()
}
