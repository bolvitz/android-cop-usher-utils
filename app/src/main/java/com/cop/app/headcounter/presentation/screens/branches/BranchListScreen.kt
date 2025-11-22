package com.cop.app.headcounter.presentation.screens.branches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchListScreen(
    viewModel: BranchListViewModel = hiltViewModel(),
    onBranchClick: (String) -> Unit,
    onManageAreas: (String) -> Unit = {},
    onManageServiceTypes: (String) -> Unit = {},
    onAddBranch: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Church Attendance") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, "History")
                    }
                    IconButton(onClick = onNavigateToReports) {
                        Icon(Icons.Default.Analytics, "Reports")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBranch) {
                Icon(Icons.Default.Add, "Add Branch")
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is BranchListUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is BranchListUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Church,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No branches yet")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap + to add your first branch", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            is BranchListUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.branches) { branchWithAreas ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = branchWithAreas.branch.name,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = branchWithAreas.branch.location,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "${branchWithAreas.areas.size} areas configured",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { onManageAreas(branchWithAreas.branch.id) }) {
                                            Icon(
                                                Icons.Default.Settings,
                                                contentDescription = "Manage Areas",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { onManageServiceTypes(branchWithAreas.branch.id) }) {
                                            Icon(
                                                Icons.Default.CalendarMonth,
                                                contentDescription = "Manage Service Types",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { onBranchClick(branchWithAreas.branch.id) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Start Counting")
                                }
                            }
                        }
                    }
                }
            }

            is BranchListUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${state.message}")
                }
            }
        }
    }
}
