package com.eventmonitor.shared.presentation.headcounter

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.AreaCountDto
import com.eventmonitor.shared.data.models.AreaTemplateDto
import com.eventmonitor.shared.data.repository.AreaCountRepository
import com.eventmonitor.shared.data.repository.EventRepository
import com.eventmonitor.shared.data.repository.EventTypeRepository
import com.eventmonitor.shared.data.repository.SeatMapRepository
import com.eventmonitor.shared.data.repository.VenueRepository
import com.eventmonitor.shared.presentation.SharedViewModel
import com.eventmonitor.shared.util.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Multiplatform head-counter ViewModel. Created with the target [venueId] and,
 * optionally, an [existingEventId] to resume an in-progress count.
 */
class CountingViewModel(
    private val venueRepository: VenueRepository,
    private val eventRepository: EventRepository,
    private val eventTypeRepository: EventTypeRepository,
    private val areaCountRepository: AreaCountRepository,
    private val seatMapRepository: SeatMapRepository,
    private val venueId: String,
    private val existingEventId: String? = null
) : SharedViewModel() {

    val eventTypes = eventTypeRepository.getActiveEventTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(CountingUiState())
    val uiState: StateFlow<CountingUiState> = _uiState.asStateFlow()

    private val _undoStack = MutableStateFlow<List<CountAction>>(emptyList())
    private val _redoStack = MutableStateFlow<List<CountAction>>(emptyList())
    private val _excludedAreaIds = MutableStateFlow<Set<String>>(emptySet())
    private val counterMutex = Mutex()

    val canUndo: StateFlow<Boolean> = _undoStack.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val canRedo: StateFlow<Boolean> = _redoStack.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        if (existingEventId != null) {
            _uiState.update { it.copy(eventId = existingEventId) }
            observeEvent(existingEventId)
        } else {
            viewModelScope.launch {
                venueRepository.getVenueById(venueId).collect { venue ->
                    venue?.let {
                        _uiState.update { s ->
                            s.copy(venueName = it.name, venueCode = it.code, isLoading = false)
                        }
                    }
                }
            }
        }
    }

    fun createNewEvent(eventTypeId: String?, eventTypeName: String, date: Long, countedBy: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = eventRepository.createEvent(
                venueId = venueId,
                eventTypeId = eventTypeId,
                date = date,
                countedBy = countedBy,
                eventName = eventTypeName
            )
            result.onSuccess { eventId ->
                seedAreaCounts(eventId)
                _uiState.update {
                    it.copy(eventId = eventId, eventName = eventTypeName, counterName = countedBy)
                }
                observeEvent(eventId)
            }.onError { err ->
                _uiState.update { it.copy(isLoading = false, error = err.toUserMessage()) }
            }
        }
    }

    /** Create one area_count per active area template so the counter has rows. */
    private suspend fun seedAreaCounts(eventId: String) {
        val templates = venueRepository.getActiveAreaTemplatesByVenue(venueId).first()
        templates.forEach { template ->
            areaCountRepository.createAreaCount(
                AreaCountDto(
                    eventId = eventId,
                    areaTemplateId = template.id,
                    count = 0,
                    capacity = template.capacity
                )
            )
        }
    }

    private fun observeEvent(eventId: String) {
        viewModelScope.launch {
            combine(
                eventRepository.getEventById(eventId),
                areaCountRepository.getAreaCountsByEvent(eventId),
                _excludedAreaIds,
                venueRepository.getVenueById(venueId),
                seatMapRepository.observeOccupiedCountsByArea(eventId)
            ) { eventDetails, areaCounts, excluded, venue, seatCounts ->
                CombinedSnapshot(eventDetails, areaCounts, excluded, venue, seatCounts)
            }.collect { snap ->
                val details = snap.event ?: return@collect
                val areaStates = snap.areaCounts.map { ac ->
                    val template = ac.template
                    val raw = ac.areaCount.count
                    val effective = if (template.hasSeatMap) snap.seatCounts[template.id] ?: 0 else raw
                    AreaCountState(
                        id = ac.areaCount.id,
                        template = template,
                        count = effective,
                        capacity = ac.areaCount.capacity,
                        notes = ac.areaCount.notes,
                        isIncluded = ac.areaCount.id !in snap.excluded,
                        percentage = if (ac.areaCount.capacity > 0)
                            (effective.toFloat() / ac.areaCount.capacity * 100).toInt() else 0,
                        lastUpdated = ac.areaCount.lastUpdated
                    )
                }
                val included = areaStates.filter { it.isIncluded }
                _uiState.update { s ->
                    s.copy(
                        venueName = snap.venue?.name ?: s.venueName,
                        venueCode = snap.venue?.code ?: s.venueCode,
                        totalAttendance = included.sumOf { it.count },
                        totalCapacity = included.sumOf { it.capacity },
                        isLocked = details.event.isLocked,
                        notes = details.event.notes,
                        areaCounts = areaStates,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleAreaInclusion(areaCountId: String) {
        _excludedAreaIds.update { if (areaCountId in it) it - areaCountId else it + areaCountId }
    }

    fun incrementCount(areaCountId: String, amount: Int = 1) {
        val eventId = _uiState.value.eventId ?: return
        if (_uiState.value.isLocked) return
        viewModelScope.launch {
            counterMutex.withLock {
                val old = currentCount(areaCountId)
                if (amount >= 0) {
                    areaCountRepository.incrementCount(eventId, areaCountId, amount, "INCREMENT")
                } else {
                    areaCountRepository.decrementCount(eventId, areaCountId, -amount, "DECREMENT")
                }
                pushUndo(CountAction.UpdateCount(eventId, areaCountId, old, old + amount))
            }
        }
    }

    fun decrementCount(areaCountId: String, amount: Int = 1) = incrementCount(areaCountId, -amount)

    fun setCount(areaCountId: String, newCount: Int) {
        val eventId = _uiState.value.eventId ?: return
        if (_uiState.value.isLocked) return
        viewModelScope.launch {
            counterMutex.withLock {
                val old = currentCount(areaCountId)
                areaCountRepository.updateCount(eventId, areaCountId, newCount, "MANUAL_EDIT")
                pushUndo(CountAction.UpdateCount(eventId, areaCountId, old, newCount))
            }
        }
    }

    fun undo() {
        val action = _undoStack.value.lastOrNull() ?: return
        viewModelScope.launch {
            when (action) {
                is CountAction.UpdateCount ->
                    areaCountRepository.updateCount(action.eventId, action.areaCountId, action.oldCount, "UNDO")
                is CountAction.SeatStatus ->
                    seatMapRepository.setSeatStatus(action.eventId, action.seatId, action.oldStatus)
            }
            _undoStack.update { it.dropLast(1) }
            _redoStack.update { it + action }
        }
    }

    fun redo() {
        val action = _redoStack.value.lastOrNull() ?: return
        viewModelScope.launch {
            when (action) {
                is CountAction.UpdateCount ->
                    areaCountRepository.updateCount(action.eventId, action.areaCountId, action.newCount, "REDO")
                is CountAction.SeatStatus ->
                    seatMapRepository.setSeatStatus(action.eventId, action.seatId, action.newStatus)
            }
            _redoStack.update { it.dropLast(1) }
            _undoStack.update { it + action }
        }
    }

    fun lockEvent() {
        val eventId = _uiState.value.eventId ?: return
        viewModelScope.launch { eventRepository.lockEvent(eventId) }
    }

    fun unlockEvent() {
        val eventId = _uiState.value.eventId ?: return
        viewModelScope.launch { eventRepository.unlockEvent(eventId) }
    }

    fun updateNotes(notes: String) {
        val eventId = _uiState.value.eventId ?: return
        viewModelScope.launch { eventRepository.updateEventNotes(eventId, notes) }
    }

    fun observeSeatLayout(areaTemplateId: String) =
        seatMapRepository.observeRowsWithSeats(areaTemplateId)

    fun observeSeatStatuses(areaTemplateId: String) =
        _uiState.value.eventId?.let { seatMapRepository.observeStatusesForArea(it, areaTemplateId) }

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
                pushUndo(CountAction.SeatStatus(eventId, seatId, currentStatus, next))
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun pushUndo(action: CountAction) {
        _undoStack.update { (it + action).takeLast(50) }
        _redoStack.value = emptyList()
    }

    private fun currentCount(areaCountId: String): Int =
        _uiState.value.areaCounts.find { it.id == areaCountId }?.count ?: 0

    private data class CombinedSnapshot(
        val event: com.eventmonitor.shared.data.models.EventWithDetails?,
        val areaCounts: List<com.eventmonitor.shared.data.models.AreaCountWithTemplate>,
        val excluded: Set<String>,
        val venue: com.eventmonitor.shared.data.models.VenueDto?,
        val seatCounts: Map<String, Int>
    )
}

data class CountingUiState(
    val venueName: String = "",
    val venueCode: String = "",
    val eventId: String? = null,
    val eventName: String = "",
    val counterName: String = "",
    val areaCounts: List<AreaCountState> = emptyList(),
    val totalAttendance: Int = 0,
    val totalCapacity: Int = 0,
    val notes: String = "",
    val isLocked: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class AreaCountState(
    val id: String,
    val template: AreaTemplateDto,
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
        val timestamp: Long = nowMillis()
    ) : CountAction()

    data class SeatStatus(
        val eventId: String,
        val seatId: String,
        val oldStatus: String,
        val newStatus: String,
        val timestamp: Long = nowMillis()
    ) : CountAction()
}
