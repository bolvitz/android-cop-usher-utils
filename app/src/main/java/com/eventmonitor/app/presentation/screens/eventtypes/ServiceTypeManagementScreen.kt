package com.eventmonitor.app.presentation.screens.eventtypes

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.data.local.entities.EventTypeEntity
import com.eventmonitor.app.presentation.viewmodels.EventTypeManagementViewModel
import com.eventmonitor.core.common.utils.rememberHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceTypeManagementScreen(
    viewModel: EventTypeManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val eventTypes by viewModel.eventTypes.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedServiceType by remember { mutableStateOf<EventTypeEntity?>(null) }

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

            if (eventTypes.isEmpty()) {
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
                    items(eventTypes) { eventType ->
                        ServiceTypeCard(
                            eventType = eventType,
                            onEdit = {
                                selectedServiceType = eventType
                                showEditDialog = true
                            },
                            onDelete = {
                                selectedServiceType = eventType
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
            eventType = selectedServiceType!!,
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
    eventType: EventTypeEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = rememberHapticFeedback()

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
                    text = eventType.name,
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
                        text = eventType.dayType,
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
                        text = eventType.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (eventType.description.isNotEmpty()) {
                    Text(
                        text = eventType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = {
                    haptic.light()
                    onEdit()
                }) {
                    Icon(Icons.Default.Edit, "Edit")
                }
                IconButton(onClick = {
                    haptic.medium()
                    onDelete()
                }) {
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
    val haptic = rememberHapticFeedback()
    val daysOfWeek = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    var name by remember { mutableStateOf("") }
    var dayType by remember { mutableStateOf(daysOfWeek[0]) }
    var dayExpanded by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(9) }
    var selectedMinute by remember { mutableStateOf(0) }
    var description by remember { mutableStateOf("") }

    val timeString = remember(selectedHour, selectedMinute) {
        val period = if (selectedHour >= 12) "PM" else "AM"
        val displayHour = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
        String.format("%d:%02d %s", displayHour, selectedMinute, period)
    }

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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    )
                )

                // Day dropdown
                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = it }
                ) {
                    OutlinedTextField(
                        value = dayType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }
                    ) {
                        daysOfWeek.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    haptic.selection()
                                    dayType = day
                                    dayExpanded = false
                                }
                            )
                        }
                    }
                }

                // Time picker button
                OutlinedTextField(
                    value = timeString,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time") },
                    trailingIcon = {
                        IconButton(onClick = {
                            haptic.light()
                            showTimePicker = true
                        }) {
                            Icon(Icons.Default.Schedule, "Select time")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Additional details...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.success()
                    onConfirm(name.trim(), dayType, timeString, description.trim())
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptic.light()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                haptic.selection()
                selectedHour = hour
                selectedMinute = minute
                showTimePicker = false
            },
            initialHour = selectedHour,
            initialMinute = selectedMinute
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServiceTypeDialog(
    eventType: EventTypeEntity,
    onDismiss: () -> Unit,
    onConfirm: (EventTypeEntity) -> Unit
) {
    val haptic = rememberHapticFeedback()
    val daysOfWeek = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    var name by remember { mutableStateOf(eventType.name) }
    var dayType by remember { mutableStateOf(eventType.dayType) }
    var dayExpanded by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Parse existing time
    val (initialHour, initialMinute) = remember(eventType.time) {
        parseTime(eventType.time)
    }
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var description by remember { mutableStateOf(eventType.description) }

    val timeString = remember(selectedHour, selectedMinute) {
        val period = if (selectedHour >= 12) "PM" else "AM"
        val displayHour = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
        String.format("%d:%02d %s", displayHour, selectedMinute, period)
    }

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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    )
                )

                // Day dropdown
                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = it }
                ) {
                    OutlinedTextField(
                        value = dayType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }
                    ) {
                        daysOfWeek.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    haptic.selection()
                                    dayType = day
                                    dayExpanded = false
                                }
                            )
                        }
                    }
                }

                // Time picker button
                OutlinedTextField(
                    value = timeString,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time") },
                    trailingIcon = {
                        IconButton(onClick = {
                            haptic.light()
                            showTimePicker = true
                        }) {
                            Icon(Icons.Default.Schedule, "Select time")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.success()
                    onConfirm(
                        eventType.copy(
                            name = name.trim(),
                            dayType = dayType,
                            time = timeString,
                            description = description.trim()
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                haptic.light()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                haptic.selection()
                selectedHour = hour
                selectedMinute = minute
                showTimePicker = false
            },
            initialHour = selectedHour,
            initialMinute = selectedMinute
        )
    }
}

/**
 * Parse time string like "9:00 AM" or "7:30 PM" to hour and minute
 */
private fun parseTime(timeString: String): Pair<Int, Int> {
    return try {
        val parts = timeString.trim().split(" ")
        val timeParts = parts[0].split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts.getOrNull(1)?.toInt() ?: 0
        val isPM = parts.getOrNull(1)?.uppercase() == "PM"

        val hour24 = when {
            isPM && hour != 12 -> hour + 12
            !isPM && hour == 12 -> 0
            else -> hour
        }

        Pair(hour24, minute)
    } catch (e: Exception) {
        Pair(9, 0) // Default to 9:00 AM if parsing fails
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int = 9,
    initialMinute: Int = 0
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute)
            }) {
                Text("OK")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}
