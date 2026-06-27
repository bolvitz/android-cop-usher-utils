package com.eventmonitor.shared.presentation.areas

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.local.entities.SeatRowWithSeats
import com.eventmonitor.shared.data.repository.SeatMapRepository
import com.eventmonitor.shared.data.repository.VenueRepository
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SeatMapEditorUiState(
    val areaName: String = "",
    val hasSeatMap: Boolean = false,
    val rows: List<SeatRowWithSeats> = emptyList()
) {
    val totalSeats: Int get() = rows.sumOf { it.seats.size }
}

class SeatMapEditorViewModel(
    private val seatMapRepository: SeatMapRepository,
    private val venueRepository: VenueRepository,
    private val areaTemplateId: String
) : SharedViewModel() {

    val uiState: StateFlow<SeatMapEditorUiState> =
        combine(
            venueRepository.getAreaTemplateById(areaTemplateId),
            seatMapRepository.observeRowsWithSeats(areaTemplateId)
        ) { area, rows ->
            SeatMapEditorUiState(
                areaName = area?.name ?: "",
                hasSeatMap = area?.hasSeatMap ?: false,
                rows = rows
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeatMapEditorUiState())

    fun setHasSeatMap(enabled: Boolean) {
        viewModelScope.launch { seatMapRepository.setHasSeatMap(areaTemplateId, enabled) }
    }

    fun addRow(seatCount: Int) {
        viewModelScope.launch { seatMapRepository.addRow(areaTemplateId, seatCount) }
    }

    fun resizeRow(rowId: String, newSeatCount: Int) {
        viewModelScope.launch { seatMapRepository.resizeRow(rowId, newSeatCount) }
    }

    fun renameRow(rowId: String, label: String) {
        viewModelScope.launch { seatMapRepository.renameRow(rowId, label) }
    }

    fun deleteRow(rowId: String) {
        viewModelScope.launch { seatMapRepository.deleteRow(rowId) }
    }
}
