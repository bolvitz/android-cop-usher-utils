package com.church.attendancecounter.presentation.screens.branches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.church.attendancecounter.data.local.entities.BranchWithAreas
import com.church.attendancecounter.domain.repository.BranchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BranchListViewModel @Inject constructor(
    private val branchRepository: BranchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BranchListUiState>(BranchListUiState.Loading)
    val uiState: StateFlow<BranchListUiState> = _uiState.asStateFlow()

    init {
        loadBranches()
    }

    private fun loadBranches() {
        viewModelScope.launch {
            branchRepository.getAllActiveBranches()
                .catch { e ->
                    _uiState.value = BranchListUiState.Error(e.message ?: "Unknown error")
                }
                .collect { branches ->
                    _uiState.value = if (branches.isEmpty()) {
                        BranchListUiState.Empty
                    } else {
                        BranchListUiState.Success(branches)
                    }
                }
        }
    }

    fun deleteBranch(branchId: String) {
        viewModelScope.launch {
            branchRepository.deleteBranch(branchId)
        }
    }
}

sealed class BranchListUiState {
    object Loading : BranchListUiState()
    object Empty : BranchListUiState()
    data class Success(val branches: List<BranchWithAreas>) : BranchListUiState()
    data class Error(val message: String) : BranchListUiState()
}
