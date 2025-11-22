package com.cop.app.headcounter.presentation.screens.counting

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cop.app.headcounter.data.local.entities.ServiceTypeEntity
import com.cop.app.headcounter.domain.models.ServiceType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountingScreen(
    viewModel: CountingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val serviceTypes by viewModel.serviceTypes.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    var showCreateDialog by remember { mutableStateOf(uiState.serviceId == null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.branchName)
                        Text(
                            text = ServiceType.fromString(uiState.serviceType.name).displayName,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                        Icon(Icons.Default.Undo, "Undo")
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                        Icon(Icons.Default.Redo, "Redo")
                    }
                    IconButton(onClick = { viewModel.shareReport() }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showCreateDialog) {
            CreateServiceDialog(
                serviceTypes = serviceTypes,
                onDismiss = { showCreateDialog = false; onNavigateBack() },
                onCreate = { serviceTypeId, serviceTypeName, date, countedBy ->
                    viewModel.createNewService(serviceTypeId, serviceTypeName, date, countedBy)
                    showCreateDialog = false
                }
            )
        }

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.serviceId != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    // Total attendance card
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Attendance", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.totalAttendance.toString(),
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (uiState.totalCapacity > 0) {
                                Text(
                                    text = "of ${uiState.totalCapacity} capacity",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                val percentage = (uiState.totalAttendance.toFloat() / uiState.totalCapacity * 100).toInt()
                                LinearProgressIndicator(
                                    progress = percentage / 100f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                                Text(
                                    text = "$percentage%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Area counts will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Creating service...")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServiceDialog(
    serviceTypes: List<ServiceTypeEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, String, Long, String) -> Unit
) {
    var selectedServiceType by remember { mutableStateOf<ServiceTypeEntity?>(serviceTypes.firstOrNull()) }
    var countedBy by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start New Service") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (serviceTypes.isEmpty()) {
                    Text(
                        "No service types configured. Please set up service types first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedServiceType?.let {
                                "${it.name} - ${it.dayType} ${it.time}"
                            } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Service Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            serviceTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(type.name, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${type.dayType} • ${type.time}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedServiceType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = countedBy,
                        onValueChange = { countedBy = it },
                        label = { Text("Counted By") },
                        placeholder = { Text("Your name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedServiceType?.let { type ->
                        onCreate(type.id, type.name, System.currentTimeMillis(), countedBy)
                    }
                },
                enabled = countedBy.isNotBlank() && selectedServiceType != null && serviceTypes.isNotEmpty()
            ) {
                Text("Start Counting")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
