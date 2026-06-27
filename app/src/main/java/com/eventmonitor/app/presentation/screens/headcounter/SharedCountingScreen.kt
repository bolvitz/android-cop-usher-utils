package com.eventmonitor.app.presentation.screens.headcounter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.presentation.headcounter.AreaCountState
import com.eventmonitor.shared.presentation.headcounter.CountingViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Head counter screen rendered from the shared KMP [CountingViewModel].
 * The ViewModel (and its Room-backed repositories) come from Koin; this proves
 * the Android UI can run entirely on the shared module.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedCountingScreen(
    venueId: String,
    onBack: () -> Unit,
    viewModel: CountingViewModel = koinViewModel { parametersOf(venueId, null) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.venueName.ifEmpty { "Head Count" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.eventId != null) {
                        TextButton(onClick = viewModel::undo, enabled = canUndo) { Text("Undo") }
                        TextButton(onClick = viewModel::redo, enabled = canRedo) { Text("Redo") }
                        IconButton(onClick = {
                            if (state.isLocked) viewModel.unlockEvent() else viewModel.lockEvent()
                        }) {
                            Icon(
                                if (state.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = "Lock"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.eventId == null -> StartPrompt(
                    Modifier.align(Alignment.Center),
                    onStart = {
                        viewModel.createNewEvent(
                            eventTypeId = null,
                            eventTypeName = "Head Count",
                            date = System.currentTimeMillis(),
                            countedBy = "Android"
                        )
                    }
                )
                else -> Column(Modifier.fillMaxSize()) {
                    TotalBanner(state.totalAttendance, state.totalCapacity)
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.areaCounts, key = { it.id }) { area ->
                            AreaCounterCard(
                                area = area,
                                locked = state.isLocked,
                                onIncrement = { viewModel.incrementCount(area.id, 1) },
                                onDecrement = { viewModel.decrementCount(area.id, 1) },
                                onToggle = { viewModel.toggleAreaInclusion(area.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartPrompt(modifier: Modifier = Modifier, onStart: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No active count for this venue.")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onStart) { Text("Start Head Count") }
    }
}

@Composable
private fun TotalBanner(total: Int, capacity: Int) {
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Total Attendance", style = MaterialTheme.typography.labelMedium)
                Text("$total", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Capacity", style = MaterialTheme.typography.labelMedium)
                Text("$capacity", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun AreaCounterCard(
    area: AreaCountState,
    locked: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onToggle: () -> Unit
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(area.template.name, style = MaterialTheme.typography.titleMedium)
                FilterChip(
                    selected = area.isIncluded,
                    onClick = onToggle,
                    label = { Text(if (area.isIncluded) "Included" else "Excluded") }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${area.count} / ${area.capacity}  (${area.percentage}%)",
                    style = MaterialTheme.typography.titleLarge
                )
                Row {
                    FilledTonalIconButton(onClick = onDecrement, enabled = !locked) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrement")
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(onClick = onIncrement, enabled = !locked) {
                        Icon(Icons.Filled.Add, contentDescription = "Increment")
                    }
                }
            }
        }
    }
}
