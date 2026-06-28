package com.eventmonitor.shared.presentation.lostandfound

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.LostItemDto
import com.eventmonitor.shared.data.repository.LostItemRepository
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LostAndFoundFilters(
    val status: String? = null,
    val query: String = ""
)

data class LostAndFoundUiState(
    val isLoading: Boolean = true,
    val items: List<LostItemDto> = emptyList(),
    val filters: LostAndFoundFilters = LostAndFoundFilters(),
    val error: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
class LostAndFoundViewModel(
    private val lostItemRepository: LostItemRepository,
    private val locationId: String? = null
) : SharedViewModel() {

    private val _filters = MutableStateFlow(LostAndFoundFilters())
    private val _error = MutableStateFlow<String?>(null)

    private val itemsFlow: Flow<List<LostItemDto>> =
        _filters.flatMapLatest { f -> sourceFor(f) }

    val uiState: StateFlow<LostAndFoundUiState> =
        combine(itemsFlow, _filters, _error) { items, filters, error ->
            LostAndFoundUiState(
                isLoading = false,
                items = items,
                filters = filters,
                error = error
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LostAndFoundUiState())

    private fun sourceFor(f: LostAndFoundFilters): Flow<List<LostItemDto>> = when {
        f.query.isNotBlank() -> lostItemRepository.searchItems(f.query)
        locationId != null && f.status != null -> lostItemRepository.getItemsByLocationAndStatus(locationId, f.status)
        locationId != null -> lostItemRepository.getItemsByLocation(locationId)
        f.status != null -> lostItemRepository.getItemsByStatus(f.status)
        else -> lostItemRepository.getAllItems()
    }

    fun filterByStatus(status: String?) = _filters.update { it.copy(status = status, query = "") }

    fun search(query: String) = _filters.update { it.copy(query = query) }

    fun createItem(
        description: String,
        category: String,
        foundZone: String,
        color: String = "",
        brand: String = "",
        reportedBy: String = ""
    ) {
        val location = locationId ?: return
        viewModelScope.launch {
            lostItemRepository.createItem(
                locationId = location,
                description = description,
                category = category,
                foundZone = foundZone,
                color = color,
                brand = brand,
                reportedBy = reportedBy
            ).onError { _error.value = it.toUserMessage() }
        }
    }

    fun updateStatus(itemId: String, status: String) {
        viewModelScope.launch {
            lostItemRepository.updateItemStatus(itemId, status)
                .onError { _error.value = it.toUserMessage() }
        }
    }

    fun claim(itemId: String, claimedBy: String, contact: String, notes: String = "") {
        viewModelScope.launch {
            lostItemRepository.claimItem(itemId, claimedBy, contact, notes)
                .onError { _error.value = it.toUserMessage() }
        }
    }

    fun delete(itemId: String) {
        viewModelScope.launch {
            lostItemRepository.deleteItem(itemId)
                .onError { _error.value = it.toUserMessage() }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
