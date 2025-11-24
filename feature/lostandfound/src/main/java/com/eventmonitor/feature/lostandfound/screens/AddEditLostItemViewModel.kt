package com.eventmonitor.feature.lostandfound.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventmonitor.core.domain.common.Result
import com.eventmonitor.core.domain.models.ItemCategory
import com.eventmonitor.core.data.local.dao.EventDao
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.data.repository.interfaces.LostItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditLostItemViewModel @Inject constructor(
    private val lostItemRepository: LostItemRepository,
    private val eventDao: EventDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val locationId: String = savedStateHandle.get<String>("locationId") ?: ""
    private val itemId: String? = savedStateHandle.get<String>("itemId")

    private val _events = MutableStateFlow<List<EventWithDetails>>(emptyList())
    val events: StateFlow<List<EventWithDetails>> = _events.asStateFlow()

    private val _selectedEventId = MutableStateFlow<String?>(null)
    val selectedEventId: StateFlow<String?> = _selectedEventId.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _category = MutableStateFlow(ItemCategory.OTHER.name)
    val category: StateFlow<String> = _category.asStateFlow()

    private val _foundZone = MutableStateFlow("")
    val foundZone: StateFlow<String> = _foundZone.asStateFlow()

    private val _photoUri = MutableStateFlow("")
    val photoUri: StateFlow<String> = _photoUri.asStateFlow()

    private val _color = MutableStateFlow("")
    val color: StateFlow<String> = _color.asStateFlow()

    private val _brand = MutableStateFlow("")
    val brand: StateFlow<String> = _brand.asStateFlow()

    private val _identifyingMarks = MutableStateFlow("")
    val identifyingMarks: StateFlow<String> = _identifyingMarks.asStateFlow()

    private val _reportedBy = MutableStateFlow("")
    val reportedBy: StateFlow<String> = _reportedBy.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        loadEvents()
        if (itemId != null) {
            loadItem(itemId)
        }
    }

    private fun loadEvents() {
        viewModelScope.launch {
            eventDao.getRecentServicesByBranch(locationId, limit = 20).collect { eventsList ->
                _events.value = eventsList
            }
        }
    }

    private fun loadItem(id: String) {
        viewModelScope.launch {
            lostItemRepository.getItemById(id).collect { item ->
                item?.let {
                    _description.value = it.description
                    _category.value = it.category
                    _foundZone.value = it.foundZone
                    _photoUri.value = it.photoUri
                    _color.value = it.color
                    _brand.value = it.brand
                    _identifyingMarks.value = it.identifyingMarks
                    _reportedBy.value = it.reportedBy
                    _notes.value = it.notes
                    _selectedEventId.value = it.eventId
                }
            }
        }
    }

    fun updateDescription(value: String) {
        _description.value = value
    }

    fun updateCategory(value: String) {
        _category.value = value
    }

    fun updateFoundZone(value: String) {
        _foundZone.value = value
    }

    fun updatePhotoUri(uri: String) {
        _photoUri.value = uri
    }

    fun updateColor(value: String) {
        _color.value = value
    }

    fun updateBrand(value: String) {
        _brand.value = value
    }

    fun updateIdentifyingMarks(value: String) {
        _identifyingMarks.value = value
    }

    fun updateReportedBy(value: String) {
        _reportedBy.value = value
    }

    fun updateNotes(value: String) {
        _notes.value = value
    }

    fun updateSelectedEvent(eventId: String?) {
        _selectedEventId.value = eventId
    }

    fun saveItem() {
        if (_isSaving.value) return

        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null

            val result = if (itemId != null) {
                // Update existing item
                lostItemRepository.getItemById(itemId).collect { item ->
                    item?.let {
                        lostItemRepository.updateItem(
                            it.copy(
                                description = _description.value,
                                category = _category.value,
                                foundZone = _foundZone.value,
                                photoUri = _photoUri.value,
                                color = _color.value,
                                brand = _brand.value,
                                identifyingMarks = _identifyingMarks.value,
                                reportedBy = _reportedBy.value,
                                notes = _notes.value,
                                eventId = _selectedEventId.value
                            )
                        )
                    }
                }
                Result.Success(Unit)
            } else {
                // Create new item
                lostItemRepository.createItem(
                    locationId = locationId,
                    description = _description.value,
                    category = _category.value,
                    foundZone = _foundZone.value,
                    photoUri = _photoUri.value,
                    color = _color.value,
                    brand = _brand.value,
                    identifyingMarks = _identifyingMarks.value,
                    reportedBy = _reportedBy.value,
                    notes = _notes.value,
                    eventId = _selectedEventId.value
                )
            }

            when (result) {
                is Result.Success -> {
                    _saveSuccess.value = true
                }
                is Result.Error -> {
                    _errorMessage.value = result.error.message
                }
            }

            _isSaving.value = false
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
