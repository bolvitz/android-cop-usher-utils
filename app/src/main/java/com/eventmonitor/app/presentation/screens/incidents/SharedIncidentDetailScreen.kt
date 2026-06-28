package com.eventmonitor.app.presentation.screens.incidents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.data.models.IncidentDto
import com.eventmonitor.shared.domain.models.IncidentSeverity
import com.eventmonitor.shared.domain.models.IncidentStatus
import com.eventmonitor.shared.presentation.incidents.IncidentDetailViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedIncidentDetailScreen(
    incidentId: String,
    onBack: () -> Unit,
    viewModel: IncidentDetailViewModel = koinViewModel { parametersOf(incidentId) }
) {
    val incident by viewModel.incident.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.delete(); onBack() }) { Text("Delete") }
                }
            )
        }
    ) { padding ->
        val current = incident
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            IncidentForm(
                modifier = Modifier.padding(padding),
                incident = current,
                onSave = viewModel::save,
                onResolve = viewModel::resolve
            )
        }
    }
}

@Composable
private fun IncidentForm(
    modifier: Modifier,
    incident: IncidentDto,
    onSave: (IncidentDto) -> Unit,
    onResolve: (String) -> Unit
) {
    var title by remember(incident.id) { mutableStateOf(incident.title) }
    var description by remember(incident.id) { mutableStateOf(incident.description) }
    var severity by remember(incident.id) { mutableStateOf(IncidentSeverity.fromString(incident.severity)) }
    var category by remember(incident.id) { mutableStateOf(incident.category) }
    var location by remember(incident.id) { mutableStateOf(incident.location) }
    var assignedTo by remember(incident.id) { mutableStateOf(incident.assignedTo) }
    var showResolve by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AssistChip(
            onClick = {},
            label = { Text(IncidentStatus.fromString(incident.status).displayName) }
        )
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
        Text("Severity", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IncidentSeverity.entries.forEach { s ->
                FilterChip(selected = severity == s, onClick = { severity = s }, label = { Text(s.displayName) })
            }
        }
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = assignedTo, onValueChange = { assignedTo = it }, label = { Text("Assigned to") }, modifier = Modifier.fillMaxWidth())

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onSave(
                        incident.copy(
                            title = title,
                            description = description,
                            severity = severity.name,
                            category = category,
                            location = location,
                            assignedTo = assignedTo
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) { Text("Save") }
            OutlinedButton(onClick = { showResolve = true }, modifier = Modifier.weight(1f)) { Text("Resolve") }
        }
    }

    if (showResolve) {
        var actions by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResolve = false },
            confirmButton = {
                TextButton(onClick = { onResolve(actions); showResolve = false }) { Text("Resolve") }
            },
            dismissButton = { TextButton(onClick = { showResolve = false }) { Text("Cancel") } },
            title = { Text("Resolve Incident") },
            text = {
                OutlinedTextField(value = actions, onValueChange = { actions = it }, label = { Text("Actions taken") })
            }
        )
    }
}
