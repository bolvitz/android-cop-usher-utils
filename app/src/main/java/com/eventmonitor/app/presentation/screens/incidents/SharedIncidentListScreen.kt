package com.eventmonitor.app.presentation.screens.incidents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.data.models.IncidentDto
import com.eventmonitor.shared.domain.models.IncidentSeverity
import com.eventmonitor.shared.domain.models.IncidentStatus
import com.eventmonitor.shared.presentation.incidents.IncidentListViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedIncidentListScreen(
    venueId: String?,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: IncidentListViewModel = koinViewModel { parametersOf(venueId) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incidents") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (venueId != null) {
                FloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Report incident")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            StatusFilterRow(
                selected = state.filters.status,
                onSelect = viewModel::filterByStatus
            )
            Box(Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.isEmpty -> Text(
                        "No incidents reported.",
                        Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.incidents, key = { it.id }) { incident ->
                            IncidentCard(
                                incident = incident,
                                onOpen = { onOpen(incident.id) },
                                onAdvance = { next -> viewModel.updateStatus(incident.id, next) },
                                onDelete = { viewModel.delete(incident.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddIncidentDialog(
            onDismiss = { showAdd = false },
            onConfirm = { title, desc, severity ->
                viewModel.createIncident(title = title, description = desc, severity = severity)
                showAdd = false
            }
        )
    }
}

@Composable
private fun StatusFilterRow(selected: String?, onSelect: (String?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") })
        IncidentStatus.entries.take(3).forEach { status ->
            FilterChip(
                selected = selected == status.name,
                onClick = { onSelect(status.name) },
                label = { Text(status.displayName) }
            )
        }
    }
}

@Composable
private fun IncidentCard(
    incident: IncidentDto,
    onOpen: () -> Unit,
    onAdvance: (String) -> Unit,
    onDelete: () -> Unit
) {
    val severity = IncidentSeverity.fromString(incident.severity)
    val status = IncidentStatus.fromString(incident.status)
    Card(onClick = onOpen) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(incident.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                SeverityBadge(severity)
            }
            if (incident.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    incident.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = { Text(status.displayName) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = Color(parseHex(status.color))
                    )
                )
                Spacer(Modifier.weight(1f))
                val next = nextStatus(status)
                if (next != null) {
                    TextButton(onClick = { onAdvance(next.name) }) { Text("→ ${next.displayName}") }
                }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: IncidentSeverity) {
    Surface(color = Color(parseHex(severity.color)), shape = MaterialTheme.shapes.small) {
        Text(
            severity.displayName,
            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun AddIncidentDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, severity: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(IncidentSeverity.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, description, severity.name) },
                enabled = title.isNotBlank()
            ) { Text("Report") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Report Incident") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IncidentSeverity.entries.forEach { s ->
                        FilterChip(
                            selected = severity == s,
                            onClick = { severity = s },
                            label = { Text(s.displayName) }
                        )
                    }
                }
            }
        }
    )
}

private fun nextStatus(status: IncidentStatus): IncidentStatus? = when (status) {
    IncidentStatus.REPORTED -> IncidentStatus.INVESTIGATING
    IncidentStatus.INVESTIGATING -> IncidentStatus.IN_PROGRESS
    IncidentStatus.IN_PROGRESS -> IncidentStatus.RESOLVED
    IncidentStatus.RESOLVED -> IncidentStatus.CLOSED
    IncidentStatus.CLOSED -> null
}

private fun parseHex(hex: String): Long {
    val clean = hex.removePrefix("#")
    return 0xFF000000 or clean.toLong(16)
}
