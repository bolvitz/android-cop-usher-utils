package com.copheadcounter.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.copheadcounter.model.Branch

class BranchViewModel : ViewModel() {
    private val _branches = mutableStateListOf<Branch>()
    val branches: List<Branch> = _branches

    fun addBranch(branch: Branch) {
        _branches.add(branch)
    }

    fun updateBranch(updatedBranch: Branch) {
        val index = _branches.indexOfFirst { it.id == updatedBranch.id }
        if (index != -1) {
            _branches[index] = updatedBranch
        }
    }

    fun deleteBranch(id: String) {
        _branches.removeIf { it.id == id }
    }

    fun getBranchById(id: String): Branch? {
        return _branches.find { it.id == id }
    }
}
