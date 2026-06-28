package com.eventmonitor.app.presentation.screens.venues

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.data.models.VenueDto
import com.eventmonitor.shared.presentation.venues.VenueListViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedVenueListScreen(
    onCount: (String) -> Unit,
    onAreas: (String) -> Unit,
    onHistory: (String) -> Unit,
    onIncidents: (String) -> Unit,
    onLostFound: (String) -> Unit,
    onEventTypes: () -> Unit,
    onReports: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: VenueListViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Monitor") },
                actions = {
                    TextButton(onClick = onReports) { Text("Reports") }
                    TextButton(onClick = onEventTypes) { Text("Event Types") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add venue")
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.isEmpty -> Text(
                    "No venues yet. Tap + to add one.",
                    Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.venues, key = { it.id }) { venue ->
                        VenueRow(
                            venue = venue,
                            onCount = { onCount(venue.id) },
                            onAreas = { onAreas(venue.id) },
                            onHistory = { onHistory(venue.id) },
                            onIncidents = { onIncidents(venue.id) },
                            onLostFound = { onLostFound(venue.id) },
                            onEdit = { onEdit(venue.id) },
                            onDelete = { viewModel.deleteVenue(venue.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddVenueDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, location, code ->
                viewModel.createVenue(name, location, code)
                showAdd = false
            }
        )
    }

    state.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } },
            title = { Text("Error") },
            text = { Text(msg) }
        )
    }
}

@Composable
private fun VenueRow(
    venue: VenueDto,
    onCount: () -> Unit,
    onAreas: () -> Unit,
    onHistory: () -> Unit,
    onIncidents: () -> Unit,
    onLostFound: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onCount).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VenueAvatar(venue)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(venue.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(venue.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Actions")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Head Count") }, onClick = { menu = false; onCount() })
                    DropdownMenuItem(text = { Text("Manage Areas") }, onClick = { menu = false; onAreas() })
                    DropdownMenuItem(text = { Text("History") }, onClick = { menu = false; onHistory() })
                    DropdownMenuItem(text = { Text("Incidents") }, onClick = { menu = false; onIncidents() })
                    DropdownMenuItem(text = { Text("Lost & Found") }, onClick = { menu = false; onLostFound() })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Edit Venue") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun VenueAvatar(venue: VenueDto) {
    val color = runCatching { Color(0xFF000000 or venue.color.removePrefix("#").toLong(16)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Text(
                venue.code.take(2).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun AddVenueDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, location: String, code: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, location, code) },
                enabled = name.isNotBlank() && code.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New Venue") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code (e.g. MC)") })
            }
        }
    )
}
