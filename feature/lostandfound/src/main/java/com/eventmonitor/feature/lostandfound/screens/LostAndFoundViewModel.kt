package com.eventmonitor.feature.lostandfound.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.LostItemEntity
import com.eventmonitor.core.domain.common.Result
import com.eventmonitor.core.domain.models.ItemCategory
import com.eventmonitor.core.domain.models.ItemStatus
import com.eventmonitor.core.data.repository.interfaces.LostItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LostAndFoundViewModel @Inject constructor(
    private val lostItemRepository: LostItemRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val locationId: String? = savedStateHandle.get<String>("locationId")

    private val _uiState = MutableStateFlow<LostAndFoundUiState>(LostAndFoundUiState.Loading)
    val uiState: StateFlow<LostAndFoundUiState> = _uiState.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            val itemsFlow = when {
                _searchQuery.value.isNotBlank() -> {
                    lostItemRepository.searchItems(_searchQuery.value)
                }
                locationId != null && _selectedStatus.value != null -> {
                    lostItemRepository.getItemsByLocationAndStatus(locationId, _selectedStatus.value!!)
                }
                locationId != null -> {
                    lostItemRepository.getItemsByLocation(locationId)
                }
                _selectedStatus.value != null -> {
                    lostItemRepository.getItemsByStatus(_selectedStatus.value!!)
                }
                else -> {
                    lostItemRepository.getAllItems()
                }
            }

            itemsFlow.collect { items ->
                if (items.isEmpty()) {
                    _uiState.value = LostAndFoundUiState.Empty
                } else {
                    _uiState.value = LostAndFoundUiState.Success(items)
                }
            }
        }
    }

    fun filterByStatus(status: String?) {
        _selectedStatus.value = status
        loadItems()
    }

    fun searchItems(query: String) {
        _searchQuery.value = query
        loadItems()
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            when (lostItemRepository.deleteItem(itemId)) {
                is Result.Success -> {
                    // Item deleted successfully
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to delete item"
                }
            }
        }
    }

    fun updateItemStatus(itemId: String, status: String) {
        viewModelScope.launch {
            when (lostItemRepository.updateItemStatus(itemId, status)) {
                is Result.Success -> {
                    // Status updated successfully
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to update status"
                }
            }
        }
    }

    fun claimItem(itemId: String, claimedBy: String, contact: String, notes: String) {
        viewModelScope.launch {
            when (lostItemRepository.claimItem(itemId, claimedBy, contact, notes)) {
                is Result.Success -> {
                    // Item claimed successfully
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to claim item"
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

sealed class LostAndFoundUiState {
    object Loading : LostAndFoundUiState()
    object Empty : LostAndFoundUiState()
    data class Success(val items: List<LostItemEntity>) : LostAndFoundUiState()
    data class Error(val message: String) : LostAndFoundUiState()
}
