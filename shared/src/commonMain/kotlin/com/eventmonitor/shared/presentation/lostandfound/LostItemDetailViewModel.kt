package com.eventmonitor.shared.presentation.lostandfound

import androidx.lifecycle.viewModelScope
import com.eventmonitor.shared.data.models.LostItemDto
import com.eventmonitor.shared.data.repository.LostItemRepository
import com.eventmonitor.shared.presentation.SharedViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LostItemDetailViewModel(
    private val lostItemRepository: LostItemRepository,
    private val itemId: String
) : SharedViewModel() {

    val item: StateFlow<LostItemDto?> =
        lostItemRepository.getItemById(itemId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(updated: LostItemDto) {
        viewModelScope.launch { lostItemRepository.updateItem(updated) }
    }

    fun updateStatus(status: String) {
        viewModelScope.launch { lostItemRepository.updateItemStatus(itemId, status) }
    }

    fun claim(claimedBy: String, contact: String, notes: String = "") {
        viewModelScope.launch { lostItemRepository.claimItem(itemId, claimedBy, contact, notes) }
    }

    fun delete() {
        viewModelScope.launch { lostItemRepository.deleteItem(itemId) }
    }
}
