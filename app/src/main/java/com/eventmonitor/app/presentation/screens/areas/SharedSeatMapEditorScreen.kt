package com.eventmonitor.app.presentation.screens.areas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.data.local.entities.SeatRowWithSeats
import com.eventmonitor.shared.presentation.areas.SeatMapEditorViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedSeatMapEditorScreen(
    areaTemplateId: String,
    onBack: () -> Unit,
    viewModel: SeatMapEditorViewModel = koinViewModel { parametersOf(areaTemplateId) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.areaName.ifEmpty { "Seat Map" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.hasSeatMap) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.addRow(10) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add Row") }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text("Seat map enabled") },
                supportingContent = { Text("Count is derived from occupied seats") },
                trailingContent = {
                    Switch(
                        checked = state.hasSeatMap,
                        onCheckedChange = { viewModel.setHasSeatMap(it) }
                    )
                }
            )
            HorizontalDivider()
            if (!state.hasSeatMap) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Enable the seat map to add rows and seats.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "${state.rows.size} rows • ${state.totalSeats} seats",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.labelLarge
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.rows, key = { it.row.id }) { rowWithSeats ->
                        RowCard(
                            rowWithSeats = rowWithSeats,
                            onResize = { delta ->
                                viewModel.resizeRow(
                                    rowWithSeats.row.id,
                                    (rowWithSeats.seats.size + delta).coerceAtLeast(0)
                                )
                            },
                            onDelete = { viewModel.deleteRow(rowWithSeats.row.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowCard(
    rowWithSeats: SeatRowWithSeats,
    onResize: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Text(rowWithSeats.row.label, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Text("${rowWithSeats.seats.size} seats", Modifier.weight(1f))
            FilledTonalIconButton(onClick = { onResize(-1) }, enabled = rowWithSeats.seats.isNotEmpty()) {
                Icon(Icons.Filled.Remove, contentDescription = "Remove seat")
            }
            Spacer(Modifier.width(4.dp))
            FilledTonalIconButton(onClick = { onResize(1) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add seat")
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete row")
            }
        }
    }
}
