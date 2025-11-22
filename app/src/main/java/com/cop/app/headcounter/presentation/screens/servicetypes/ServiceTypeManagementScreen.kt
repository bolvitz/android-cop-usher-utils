package com.cop.app.headcounter.presentation.screens.servicetypes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cop.app.headcounter.data.local.entities.ServiceTypeEntity
import com.cop.app.headcounter.presentation.viewmodels.ServiceTypeManagementViewModel
import com.cop.app.headcounter.presentation.utils.rememberHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceTypeManagementScreen(
    viewModel: ServiceTypeManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val serviceTypes by viewModel.serviceTypes.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedServiceType by remember { mutableStateOf<ServiceTypeEntity?>(null) }

    LaunchedEffect(uiState.message, uiState.error) {
        if (uiState.message != null || uiState.error != null) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Service Types") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.light()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                haptic.medium()
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, "Add Service Type")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            uiState.message?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (serviceTypes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "No service types configured",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Tap + to add your first service type",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(serviceTypes) { serviceType ->
                        ServiceTypeCard(
                            serviceType = serviceType,
                            onEdit = {
                                selectedServiceType = serviceType
                                showEditDialog = true
                            },
                            onDelete = {
                                selectedServiceType = serviceType
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddServiceTypeDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, dayType, time, description ->
                viewModel.createServiceType(name, dayType, time, description)
                showAddDialog = false
            }
        )
    }

    if (showEditDialog && selectedServiceType != null) {
        EditServiceTypeDialog(
            serviceType = selectedServiceType!!,
            onDismiss = {
                showEditDialog = false
                selectedServiceType = null
            },
            onConfirm = { updatedServiceType ->
                viewModel.updateServiceType(updatedServiceType)
                showEditDialog = false
                selectedServiceType = null
            }
        )
    }

    if (showDeleteDialog && selectedServiceType != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedServiceType = null
            },
            title = { Text("Delete Service Type") },
            text = {
                Text("Are you sure you want to delete \"${selectedServiceType?.name}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteServiceType(selectedServiceType!!.id)
                        showDeleteDialog = false
                        selectedServiceType = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedServiceType = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ServiceTypeCard(
    serviceType: ServiceTypeEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = serviceType.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = serviceType.dayType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = serviceType.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (serviceType.description.isNotEmpty()) {
                    Text(
                        text = serviceType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceTypeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dayType by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Service Type") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name") },
                    placeholder = { Text("e.g., Sunday Morning Service") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dayType,
                    onValueChange = { dayType = it },
                    label = { Text("Day") },
                    placeholder = { Text("e.g., Sunday, Wednesday") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time") },
                    placeholder = { Text("e.g., 9:00 AM, 7:00 PM") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Additional details...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && dayType.isNotBlank() && time.isNotBlank()) {
                        onConfirm(name.trim(), dayType.trim(), time.trim(), description.trim())
                    }
                },
                enabled = name.isNotBlank() && dayType.isNotBlank() && time.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServiceTypeDialog(
    serviceType: ServiceTypeEntity,
    onDismiss: () -> Unit,
    onConfirm: (ServiceTypeEntity) -> Unit
) {
    var name by remember { mutableStateOf(serviceType.name) }
    var dayType by remember { mutableStateOf(serviceType.dayType) }
    var time by remember { mutableStateOf(serviceType.time) }
    var description by remember { mutableStateOf(serviceType.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Service Type") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dayType,
                    onValueChange = { dayType = it },
                    label = { Text("Day") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && dayType.isNotBlank() && time.isNotBlank()) {
                        onConfirm(
                            serviceType.copy(
                                name = name.trim(),
                                dayType = dayType.trim(),
                                time = time.trim(),
                                description = description.trim()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && dayType.isNotBlank() && time.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
