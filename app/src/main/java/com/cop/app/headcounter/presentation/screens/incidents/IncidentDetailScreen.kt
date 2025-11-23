package com.cop.app.headcounter.presentation.screens.incidents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cop.app.headcounter.data.local.entities.IncidentEntity
import com.cop.app.headcounter.domain.models.IncidentSeverity
import com.cop.app.headcounter.domain.models.IncidentStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDetailScreen(
    incidentId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: IncidentDetailViewModel = hiltViewModel()
) {
    val incident by viewModel.incident.collectAsState(initial = null)
    var showResolveDialog by remember { mutableStateOf(false) }
    var resolveNotes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { incident?.let { onNavigateToEdit(it.id) } }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                }
            )
        }
    ) { padding ->
        incident?.let { inc ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Photo
                if (inc.photoUri.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        AsyncImage(
                            model = inc.photoUri,
                            contentDescription = "Incident Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Title and Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = inc.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        SeverityBadge(IncidentSeverity.fromString(inc.severity))
                        Spacer(modifier = Modifier.height(4.dp))
                        StatusBadge(IncidentStatus.fromString(inc.status))
                    }
                }

                Divider()

                // Description
                DetailSection(
                    title = "Description",
                    content = inc.description
                )

                // Category and Location
                if (inc.category.isNotEmpty() || inc.location.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (inc.category.isNotEmpty()) {
                            Column(modifier = Modifier.weight(1f)) {
                                DetailLabel("Category")
                                DetailValue(inc.category)
                            }
                        }
                        if (inc.location.isNotEmpty()) {
                            Column(modifier = Modifier.weight(1f)) {
                                DetailLabel("Location")
                                DetailValue(inc.location)
                            }
                        }
                    }
                }

                // Reported Info
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

                DetailSection(
                    title = "Reported",
                    content = buildString {
                        append(dateFormat.format(Date(inc.reportedAt)))
                        if (inc.reportedBy.isNotEmpty()) {
                            append("\nBy: ${inc.reportedBy}")
                        }
                    }
                )

                // Resolved Info
                inc.resolvedAt?.let { resolvedAt ->
                    DetailSection(
                        title = "Resolved",
                        content = dateFormat.format(Date(resolvedAt))
                    )
                }

                // Assigned To
                if (inc.assignedTo.isNotEmpty()) {
                    DetailSection(
                        title = "Assigned To",
                        content = inc.assignedTo
                    )
                }

                // Actions Taken
                if (inc.actionsTaken.isNotEmpty()) {
                    DetailSection(
                        title = "Actions Taken",
                        content = inc.actionsTaken
                    )
                }

                // Notes
                if (inc.notes.isNotEmpty()) {
                    DetailSection(
                        title = "Additional Notes",
                        content = inc.notes
                    )
                }

                // Actions
                if (inc.status != IncidentStatus.RESOLVED.name && inc.status != IncidentStatus.CLOSED.name) {
                    Divider()

                    Button(
                        onClick = { showResolveDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, "Resolve")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark as Resolved")
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Resolve Dialog
    if (showResolveDialog) {
        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            title = { Text("Resolve Incident") },
            text = {
                Column {
                    Text("Please provide details of actions taken:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resolveNotes,
                        onValueChange = { resolveNotes = it },
                        label = { Text("Actions Taken") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resolveIncident(resolveNotes)
                        showResolveDialog = false
                    },
                    enabled = resolveNotes.isNotBlank()
                ) {
                    Text("Resolve")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailSection(title: String, content: String) {
    Column {
        DetailLabel(title)
        DetailValue(content)
    }
}

@Composable
fun DetailLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.outline,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun DetailValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge
    )
}
