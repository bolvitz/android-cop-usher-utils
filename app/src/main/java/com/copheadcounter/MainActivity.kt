package com.copheadcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.copheadcounter.navigation.NavGraph
import com.copheadcounter.ui.theme.CopHeadCounterTheme
import com.copheadcounter.viewmodel.CounterViewModel
import com.copheadcounter.viewmodel.LostFoundViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CopHeadCounterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val counterViewModel: CounterViewModel = viewModel()
                    val lostFoundViewModel: LostFoundViewModel = viewModel()

                    NavGraph(
                        navController = navController,
                        counterItems = counterViewModel.counterItems,
                        onIncrementCount = { id -> counterViewModel.incrementCount(id) },
                        onDecrementCount = { id -> counterViewModel.decrementCount(id) },
                        onAddNewCounter = { name -> counterViewModel.addCounter(name) },
                        lostFoundItems = lostFoundViewModel.items,
                        onAddItem = { item ->
                            lostFoundViewModel.addItem(
                                name = item.name,
                                description = item.description,
                                category = item.category,
                                dateFound = item.dateFound,
                                location = item.location,
                                notes = item.notes
                            )
                        },
                        onUpdateItem = { item -> lostFoundViewModel.updateItem(item) },
                        onDeleteItem = { id -> lostFoundViewModel.deleteItem(id) },
                        onClaimItem = { id, name, contact ->
                            lostFoundViewModel.claimItem(id, name, contact)
                        },
                        onUpdateStatus = { id, status ->
                            lostFoundViewModel.updateStatus(id, status)
                        },
                        getItemById = { id -> lostFoundViewModel.getItemById(id) }
                    )
                }
            }
        }
    }
}
