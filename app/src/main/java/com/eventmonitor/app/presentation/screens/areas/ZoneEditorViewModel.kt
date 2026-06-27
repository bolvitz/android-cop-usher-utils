package com.eventmonitor.app.presentation.screens.areas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.SeatRowWithSeats
import com.eventmonitor.core.data.repository.interfaces.AreaRepository
import com.eventmonitor.core.data.repository.interfaces.SeatMapRepository
import com.eventmonitor.core.data.repository.interfaces.VenueRepository
import com.eventmonitor.core.domain.models.AreaType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class EditorMode { SOLO, BATCH, EDIT }

data class ZoneEditorState(
    val venueId: String = "",
    val venueName: String = "",
    val mode: EditorMode = EditorMode.SOLO,
    val isInitializing: Boolean = true,
    val isSubmitting: Boolean = false,
    val zoneId: String? = null,
    val name: String = "",
    val type: AreaType = AreaType.SEATING,
    val capacity: Int = 100,
    val batchCount: Int = 6,
    val batchStart: Int = 1,
    val typeLocked: Boolean = false,
    val hasSeatMap: Boolean = false,
    val seatRows: List<SeatRowWithSeats> = emptyList(),
    val error: String? = null,
    val finished: Boolean = false,
)

class ZoneEditorViewModel constructor(
    private val areaRepository: AreaRepository,
    private val venueRepository: VenueRepository,
    private val seatMapRepository: SeatMapRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val venueId: String = checkNotNull(savedStateHandle.get<String>("venueId"))
    private val zoneId: String? = savedStateHandle.get<String>("zoneId")?.takeIf { it.isNotBlank() }
    private val initialMode: EditorMode = when (savedStateHandle.get<String>("mode")) {
        "batch" -> EditorMode.BATCH
        "edit" -> EditorMode.EDIT
        else -> if (zoneId != null) EditorMode.EDIT else EditorMode.SOLO
    }

    private val _state = MutableStateFlow(
        ZoneEditorState(
            venueId = venueId,
            mode = initialMode,
            zoneId = zoneId,
            typeLocked = initialMode == EditorMode.EDIT,
        )
    )
    val state: StateFlow<ZoneEditorState> = _state.asStateFlow()

    private var seatRowsJob: Job? = null

    init {
        viewModelScope.launch {
            val venueName = runCatching {
                venueRepository.getVenueById(venueId).first()?.venue?.name.orEmpty()
            }.getOrDefault("")

            if (zoneId != null) {
                val area = runCatching { areaRepository.getAreaById(zoneId).first() }.getOrNull()
                if (area != null) {
                    _state.value = _state.value.copy(
                        venueName = venueName,
                        name = area.name,
                        capacity = area.capacity,
                        type = AreaType.fromString(area.type),
                        hasSeatMap = area.hasSeatMap,
                        isInitializing = false,
                        typeLocked = true,
                        mode = EditorMode.EDIT,
                    )
                    observeSeatRows(zoneId)
                    return@launch
                }
            }
            _state.value = _state.value.copy(
                venueName = venueName,
                isInitializing = false,
            )
        }
    }

    private fun observeSeatRows(areaId: String) {
        seatRowsJob?.cancel()
        seatRowsJob = viewModelScope.launch {
            seatMapRepository.observeRowsWithSeats(areaId).collect { rows ->
                _state.value = _state.value.copy(seatRows = rows)
            }
        }
    }

    fun setMode(mode: EditorMode) {
        if (_state.value.mode == EditorMode.EDIT) return
        _state.value = _state.value.copy(mode = mode)
    }

    fun setName(value: String) {
        _state.value = _state.value.copy(name = value.take(48))
    }

    fun setType(type: AreaType) {
        if (_state.value.typeLocked) return
        _state.value = _state.value.copy(type = type)
    }

    fun setCapacity(value: Int) {
        _state.value = _state.value.copy(capacity = value.coerceIn(0, 50_000))
    }

    fun setBatchCount(value: Int) {
        _state.value = _state.value.copy(batchCount = value.coerceIn(1, 99))
    }

    fun setBatchStart(value: Int) {
        _state.value = _state.value.copy(batchStart = value.coerceIn(1, 9999))
    }

    fun setHasSeatMap(enabled: Boolean) {
        val s = _state.value
        if (s.mode == EditorMode.BATCH) return
        _state.value = s.copy(hasSeatMap = enabled)
        // For EDIT mode, persist immediately so the row editor reflects DB state.
        if (s.mode == EditorMode.EDIT && s.zoneId != null) {
            viewModelScope.launch {
                seatMapRepository.setHasSeatMap(s.zoneId, enabled)
            }
        }
    }

    fun addSeatRow(seatCount: Int = 10) {
        val areaId = _state.value.zoneId ?: return
        if (!_state.value.hasSeatMap) return
        viewModelScope.launch { seatMapRepository.addRow(areaId, seatCount) }
    }

    fun resizeSeatRow(rowId: String, newSeatCount: Int) {
        viewModelScope.launch { seatMapRepository.resizeRow(rowId, newSeatCount) }
    }

    fun renameSeatRow(rowId: String, label: String) {
        viewModelScope.launch { seatMapRepository.renameRow(rowId, label) }
    }

    fun deleteSeatRow(rowId: String) {
        viewModelScope.launch { seatMapRepository.deleteRow(rowId) }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun submit() {
        val s = _state.value
        if (s.isSubmitting) return
        when (s.mode) {
            EditorMode.SOLO -> submitSolo(s)
            EditorMode.BATCH -> submitBatch(s)
            EditorMode.EDIT -> submitEdit(s)
        }
    }

    private fun submitSolo(s: ZoneEditorState) {
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Name a zone before minting it.")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true, error = null)
            try {
                val nextOrder = nextDisplayOrder()
                areaRepository.createArea(
                    venueId = venueId,
                    name = s.name.trim(),
                    type = s.type,
                    capacity = s.capacity,
                    displayOrder = nextOrder,
                    hasSeatMap = s.hasSeatMap,
                )
                _state.value = _state.value.copy(isSubmitting = false, finished = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Failed to mint zone"
                )
            }
        }
    }

    private fun submitBatch(s: ZoneEditorState) {
        if (s.batchCount <= 0) {
            _state.value = s.copy(error = "A batch needs at least one zone.")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true, error = null)
            try {
                var order = nextDisplayOrder()
                repeat(s.batchCount) { i ->
                    val number = s.batchStart + i
                    val name = quickNameFor(s.type, number)
                    areaRepository.createArea(
                        venueId = venueId,
                        name = name,
                        type = s.type,
                        capacity = s.capacity,
                        displayOrder = order++,
                    )
                }
                _state.value = _state.value.copy(isSubmitting = false, finished = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Failed to mint batch"
                )
            }
        }
    }

    private fun submitEdit(s: ZoneEditorState) {
        val id = s.zoneId ?: return
        if (s.name.isBlank()) {
            _state.value = s.copy(error = "Name cannot be blank.")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true, error = null)
            try {
                val current = areaRepository.getAreaById(id).first()
                if (current == null) {
                    _state.value =
                        _state.value.copy(isSubmitting = false, error = "Zone no longer exists.")
                    return@launch
                }
                areaRepository.updateArea(
                    current.copy(
                        name = s.name.trim(),
                        capacity = s.capacity,
                        hasSeatMap = s.hasSeatMap,
                    ),
                )
                _state.value = _state.value.copy(isSubmitting = false, finished = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Failed to save zone"
                )
            }
        }
    }

    private suspend fun nextDisplayOrder(): Int {
        val areas = areaRepository.getAreasByVenue(venueId).first()
        return (areas.maxOfOrNull { it.displayOrder } ?: -1) + 1
    }
}

internal fun quickNameFor(type: AreaType, number: Int): String = when (type) {
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

internal fun AreaType.shortTagX(): String = when (this) {
    AreaType.SEATING -> "SEATING"
    AreaType.STANDING -> "STANDING"
    AreaType.VIP -> "VIP"
    AreaType.GENERAL_ADMISSION -> "GA"
    AreaType.OVERFLOW -> "OVERFLOW"
    AreaType.PARKING -> "PARKING"
    AreaType.REGISTRATION -> "REG"
    AreaType.LOBBY -> "LOBBY"
    AreaType.OUTDOOR -> "OUTDOOR"
    AreaType.STAGE -> "STAGE"
    AreaType.BACKSTAGE -> "BACKSTAGE"
    AreaType.CARE_ROOM -> "CARE"
    AreaType.FOOD_AREA -> "F&B"
    AreaType.RESTROOMS -> "WC"
    AreaType.EMERGENCY_EXIT -> "EXIT"
    AreaType.OTHER -> "OTHER"
}

internal fun AreaType.letterMark(): String = when (this) {
    AreaType.SEATING -> "S"
    AreaType.STANDING -> "T"
    AreaType.VIP -> "V"
    AreaType.GENERAL_ADMISSION -> "G"
    AreaType.OVERFLOW -> "O"
    AreaType.PARKING -> "P"
    AreaType.REGISTRATION -> "R"
    AreaType.LOBBY -> "L"
    AreaType.OUTDOOR -> "U"
    AreaType.STAGE -> "Z"
    AreaType.BACKSTAGE -> "B"
    AreaType.CARE_ROOM -> "C"
    AreaType.FOOD_AREA -> "F"
    AreaType.RESTROOMS -> "W"
    AreaType.EMERGENCY_EXIT -> "X"
    AreaType.OTHER -> "·"
}
