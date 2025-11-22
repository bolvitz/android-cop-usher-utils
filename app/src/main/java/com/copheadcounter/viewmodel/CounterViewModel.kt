package com.copheadcounter.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.copheadcounter.model.CounterItem

class CounterViewModel : ViewModel() {
    private val _counterItems = mutableStateListOf<CounterItem>()
    val counterItems: List<CounterItem> = _counterItems

    init {
        // Add a default counter for demonstration
        addCounter("People")
    }

    fun addCounter(name: String) {
        _counterItems.add(CounterItem(name = name))
    }

    fun incrementCount(id: String) {
        val index = _counterItems.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = _counterItems[index]
            _counterItems[index] = item.copy(count = item.count + 1)
        }
    }

    fun decrementCount(id: String) {
        val index = _counterItems.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = _counterItems[index]
            if (item.count > 0) {
                _counterItems[index] = item.copy(count = item.count - 1)
            }
        }
    }

    fun resetCount(id: String) {
        val index = _counterItems.indexOfFirst { it.id == id }
        if (index != -1) {
            val item = _counterItems[index]
            _counterItems[index] = item.copy(count = 0)
        }
    }
}
