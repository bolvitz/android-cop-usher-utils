package com.copheadcounter.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.copheadcounter.model.Branch

class BranchViewModel : ViewModel() {
    private val _branches = mutableStateListOf<Branch>()
    val branches: List<Branch> = _branches

    init {
        // Add default branches for demonstration
        addBranch("Main Office", "123 Main St", "Headquarters")
        addBranch("North Branch", "456 North Ave", "Northern location")
        addBranch("South Branch", "789 South Blvd", "Southern location")
    }

    fun addBranch(name: String, location: String = "", description: String = "") {
        _branches.add(
            Branch(
                name = name,
                location = location,
                description = description
            )
        )
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
