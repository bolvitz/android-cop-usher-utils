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
import com.copheadcounter.viewmodel.BranchViewModel
import com.copheadcounter.viewmodel.CounterViewModel
import com.copheadcounter.viewmodel.LostFoundViewModel
import com.copheadcounter.viewmodel.SettingsViewModel

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
                    val branchViewModel: BranchViewModel = viewModel()
                    val counterViewModel: CounterViewModel = viewModel()
                    val lostFoundViewModel: LostFoundViewModel = viewModel()
                    val settingsViewModel: SettingsViewModel = viewModel()

                    NavGraph(
                        navController = navController,
                        branches = branchViewModel.branches,
                        settings = settingsViewModel.settings,
                        onAddBranch = { name, location, description ->
                            branchViewModel.addBranch(name, location, description)
                        },
                        getBranchById = { id -> branchViewModel.getBranchById(id) },
                        getCountersForBranch = { branchId ->
                            counterViewModel.getCountersForBranch(branchId)
                        },
                        onIncrementCount = { id -> counterViewModel.incrementCount(id) },
                        onDecrementCount = { id -> counterViewModel.decrementCount(id) },
                        onAddNewCounter = { branchId, name ->
                            counterViewModel.addCounter(branchId, name)
                        },
                        getItemsForBranch = { branchId ->
                            lostFoundViewModel.getItemsForBranch(branchId)
                        },
                        onAddItem = { branchId, item ->
                            lostFoundViewModel.addItem(
                                branchId = branchId,
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
                        getItemById = { id -> lostFoundViewModel.getItemById(id) },
                        onCounterEnabledChange = { enabled ->
                            settingsViewModel.updateCounterEnabled(enabled)
                        },
                        onLostFoundEnabledChange = { enabled ->
                            settingsViewModel.updateLostFoundEnabled(enabled)
                        }
                    )
                }
            }
        }
    }
}
