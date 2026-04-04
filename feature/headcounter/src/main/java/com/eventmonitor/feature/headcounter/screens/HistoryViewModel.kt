package com.eventmonitor.feature.headcounter.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.data.repository.interfaces.AreaCountRepository
import com.eventmonitor.core.data.repository.interfaces.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val areaCountRepository: AreaCountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val venueId: String? = savedStateHandle.get<String>("venueId")
    private val branchId: String? = venueId

    private val _uiState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _selectedServiceReport = MutableStateFlow<String?>(null)
    val selectedServiceReport: StateFlow<String?> = _selectedServiceReport.asStateFlow()

    private val _csvExportData = MutableStateFlow<String?>(null)
    val csvExportData: StateFlow<String?> = _csvExportData.asStateFlow()

    init {
        loadServices()
    }

    private fun loadServices() {
        viewModelScope.launch {
            val servicesFlow = if (branchId != null) {
                eventRepository.getRecentEventsByVenue(branchId, 100)
            } else {
                eventRepository.getRecentEvents(100)
            }

            servicesFlow.collect { services ->
                if (services.isEmpty()) {
                    _uiState.value = HistoryUiState.Empty
                } else {
                    _uiState.value = HistoryUiState.Success(services)
                }
            }
        }
    }

    fun generateReport(eventId: String) {
        viewModelScope.launch {
            try {
                val report = eventRepository.exportEventReport(eventId)
                _selectedServiceReport.value = report
            } catch (e: Exception) {
                _selectedServiceReport.value = "Error generating report: ${e.message}"
            }
        }
    }

    fun generateCsvExport(eventId: String) {
        viewModelScope.launch {
            try {
                val eventFlow = eventRepository.getEventById(eventId)
                val eventWithDetails = eventFlow.first() ?: return@launch
                val areaCounts = areaCountRepository.getAreaCountsByService(eventId).first()

                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val event = eventWithDetails.event
                val venue = eventWithDetails.venue

                val csv = buildString {
                    appendLine("Area,Count,Capacity,Utilization %,Last Updated")
                    areaCounts.sortedBy { it.template.displayOrder }.forEach { ac ->
                        val utilization = if (ac.areaCount.capacity > 0) {
                            (ac.areaCount.count.toFloat() / ac.areaCount.capacity * 100).toInt()
                        } else 0
                        val updated = dateFormat.format(Date(ac.areaCount.lastUpdated))
                        appendLine("\"${ac.template.name}\",${ac.areaCount.count},${ac.areaCount.capacity},$utilization%,$updated")
                    }
                    appendLine()
                    appendLine("Total,${event.totalAttendance},${event.totalCapacity},${
                        if (event.totalCapacity > 0) (event.totalAttendance.toFloat() / event.totalCapacity * 100).toInt() else 0
                    }%,")
                    appendLine()
                    appendLine("Event:,\"${event.eventName}\"")
                    appendLine("Venue:,\"${venue.name}\"")
                    appendLine("Date:,\"${dateFormat.format(Date(event.date))}\"")
                    appendLine("Counted By:,\"${event.countedBy}\"")
                }
                _csvExportData.value = csv
            } catch (e: Exception) {
                _csvExportData.value = null
            }
        }
    }

    fun clearCsvExport() {
        _csvExportData.value = null
    }

    fun clearReport() {
        _selectedServiceReport.value = null
    }

    fun unlockEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.unlockEvent(eventId)
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            eventRepository.deleteEvent(eventId)
        }
    }
}

sealed class HistoryUiState {
    object Loading : HistoryUiState()
    object Empty : HistoryUiState()
    data class Success(val events: List<EventWithDetails>) : HistoryUiState()
}
