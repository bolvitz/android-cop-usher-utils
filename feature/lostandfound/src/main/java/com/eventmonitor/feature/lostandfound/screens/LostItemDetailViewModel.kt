package com.eventmonitor.feature.lostandfound.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.data.local.entities.LostItemEntity
import com.eventmonitor.core.domain.common.Result
import com.eventmonitor.core.domain.models.ItemStatus
import com.eventmonitor.core.data.repository.interfaces.LostAndFoundRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LostItemDetailViewModel @Inject constructor(
    private val lostAndFoundRepository: LostAndFoundRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: String = savedStateHandle.get<String>("itemId") ?: ""

    val item: Flow<LostItemEntity?> = lostAndFoundRepository.getItemById(itemId)

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateItemStatus(status: String) {
        viewModelScope.launch {
            when (lostAndFoundRepository.updateItemStatus(itemId, status)) {
                is Result.Success -> {
                    // Successfully updated
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to update item status"
                }
            }
        }
    }

    fun claimItem(claimerName: String, contact: String, notes: String) {
        viewModelScope.launch {
            when (lostAndFoundRepository.claimItem(itemId, claimerName, contact, notes)) {
                is Result.Success -> {
                    // Successfully claimed
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to claim item"
                }
            }
        }
    }

    fun deleteItem() {
        viewModelScope.launch {
            when (lostAndFoundRepository.deleteItem(itemId)) {
                is Result.Success -> {
                    // Successfully deleted
                }
                is Result.Error -> {
                    _errorMessage.value = "Failed to delete item"
                }
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
