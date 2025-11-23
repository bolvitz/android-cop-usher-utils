package com.copheadcounter.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.copheadcounter.model.ItemCategory
import com.copheadcounter.model.ItemStatus
import com.copheadcounter.model.LostFoundItem
import java.time.LocalDate
import java.time.LocalDateTime

class LostFoundViewModel : ViewModel() {
    private val _items = mutableStateListOf<LostFoundItem>()
    val items: List<LostFoundItem> = _items

    init {
        // Add sample items for demonstration
        addItem(
            name = "iPhone 13 Pro",
            description = "Black iPhone with cracked screen protector",
            category = ItemCategory.PHONE,
            dateFound = LocalDate.now().minusDays(5),
            location = "Library 2nd Floor",
            notes = "Found near study area"
        )
        addItem(
            name = "Brown Leather Wallet",
            description = "Contains student ID and credit cards",
            category = ItemCategory.WALLET,
            dateFound = LocalDate.now().minusDays(12),
            location = "Cafeteria",
            notes = "Handed in by cleaning staff"
        )
    }

    fun addItem(
        name: String,
        description: String,
        category: ItemCategory,
        dateFound: LocalDate,
        location: String,
        notes: String = ""
    ) {
        _items.add(
            LostFoundItem(
                name = name,
                description = description,
                category = category,
                dateFound = dateFound,
                location = location,
                notes = notes
            )
        )
    }

    fun updateItem(updatedItem: LostFoundItem) {
        val index = _items.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            _items[index] = updatedItem.copy(updatedAt = LocalDateTime.now())
        }
    }

    fun deleteItem(id: String) {
        _items.removeIf { it.id == id }
    }

    fun claimItem(id: String, claimantName: String, claimantContact: String) {
        val index = _items.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = _items[index]
            _items[index] = item.copy(
                status = ItemStatus.CLAIMED,
                claimantName = claimantName,
                claimantContact = claimantContact,
                claimDate = LocalDate.now(),
                updatedAt = LocalDateTime.now()
            )
        }
    }

    fun updateStatus(id: String, status: ItemStatus) {
        val index = _items.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = _items[index]
            _items[index] = item.copy(
                status = status,
                updatedAt = LocalDateTime.now()
            )
        }
    }

    fun getItemById(id: String): LostFoundItem? {
        return _items.find { it.id == id }
    }

    fun filterByStatus(status: ItemStatus): List<LostFoundItem> {
        return _items.filter { it.status == status }
    }

    fun filterByCategory(category: ItemCategory): List<LostFoundItem> {
        return _items.filter { it.category == category }
    }

    fun searchItems(query: String): List<LostFoundItem> {
        if (query.isBlank()) return _items
        return _items.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true) ||
            it.location.contains(query, ignoreCase = true)
        }
    }

    fun markOverdueAsUnclaimed() {
        _items.forEachIndexed { index, item ->
            if (item.isOverdue() && item.status == ItemStatus.ACTIVE) {
                _items[index] = item.copy(
                    status = ItemStatus.UNCLAIMED,
                    updatedAt = LocalDateTime.now()
                )
            }
        }
    }
}
