package com.eventmonitor.app.presentation.screens.areas

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.domain.models.AreaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaManagementScreen(
    viewModel: AreaManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Manage Areas")
                        if (uiState.branchName.isNotEmpty()) {
                            Text(
                                text = uiState.branchName,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleQuickAddDialog(true) }) {
                        Icon(Icons.Default.ViewWeek, "Quick Add")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.toggleAddDialog(true) }) {
                Icon(Icons.Default.Add, "Add Area")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.areas.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EventSeat,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No areas yet")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap + to add your first area",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.areas) { area ->
                            AreaCard(
                                area = area,
                                onEdit = { viewModel.setEditingArea(area) },
                                onDelete = { viewModel.deleteArea(area.id) }
                            )
                        }
                    }
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                LaunchedEffect(error) {
                    // Show error and clear it
                    viewModel.clearError()
                }
            }
        }

        // Add Area Dialog
        if (uiState.showAddDialog) {
            AddAreaDialog(
                onDismiss = { viewModel.toggleAddDialog(false) },
                onAdd = { name, type, capacity ->
                    viewModel.addArea(name, type, capacity)
                }
            )
        }

        // Quick Add Dialog
        if (uiState.showQuickAddDialog) {
            QuickAddAreasDialog(
                onDismiss = { viewModel.toggleQuickAddDialog(false) },
                onAdd = { type, count, startNumber ->
                    viewModel.createQuickAreas(type, count, startNumber)
                }
            )
        }

        // Edit Area Dialog
        uiState.editingArea?.let { area ->
            EditAreaDialog(
                area = area,
                onDismiss = { viewModel.setEditingArea(null) },
                onUpdate = { updatedArea ->
                    viewModel.updateArea(updatedArea)
                }
            )
        }
    }
}

@Composable
fun AreaCard(
    area: com.eventmonitor.core.data.local.entities.AreaTemplateEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = area.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AreaType.fromString(area.type).displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Capacity: ${area.capacity}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit")
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Area?") },
            text = { Text("Are you sure you want to delete ${area.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAreaDialog(
    onDismiss: () -> Unit,
    onAdd: (String, AreaType, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<AreaType>(AreaType.SEATING) }
    var capacity by remember { mutableStateOf("100") }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Area") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Area Name") },
                    placeholder = { Text("e.g., Bay 1, Baby Room, etc.") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Area Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        AreaType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it.filter { char -> char.isDigit() } },
                    label = { Text("Capacity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val capacityInt = capacity.toIntOrNull() ?: 100
                    onAdd(name, selectedType, capacityInt)
                },
                enabled = name.isNotBlank()
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
fun QuickAddAreasDialog(
    onDismiss: () -> Unit,
    onAdd: (AreaType, Int, Int) -> Unit
) {
    var selectedType by remember { mutableStateOf<AreaType>(AreaType.SEATING) }
    var count by remember { mutableStateOf("6") }
    var startNumber by remember { mutableStateOf("1") }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Areas") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Quickly create multiple areas of the same type with auto-numbered names",
                    style = MaterialTheme.typography.bodySmall
                )

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Area Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        AreaType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it.filter { char -> char.isDigit() } },
                    label = { Text("Number of Areas") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                OutlinedTextField(
                    value = startNumber,
                    onValueChange = { startNumber = it.filter { char -> char.isDigit() } },
                    label = { Text("Starting Number") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                val previewText = buildString {
                    val countInt = count.toIntOrNull() ?: 0
                    val startInt = startNumber.toIntOrNull() ?: 1
                    if (countInt > 0) {
                        append("Will create: ")
                        val examples = (0 until minOf(3, countInt)).map { index ->
                            val number = startInt + index
                            when (selectedType) {
                                AreaType.SEATING -> "Seating $number"
                                AreaType.STANDING -> "Standing $number"
                                AreaType.VIP -> "VIP $number"
                                AreaType.GENERAL_ADMISSION -> "General Admission $number"
                                AreaType.OVERFLOW -> "Overflow $number"
                                AreaType.PARKING -> "Parking $number"
                                AreaType.REGISTRATION -> "Registration $number"
                                AreaType.LOBBY -> "Lobby $number"
                                AreaType.OUTDOOR -> "Outdoor $number"
                                AreaType.STAGE -> "Stage $number"
                                AreaType.BACKSTAGE -> "Backstage $number"
                                AreaType.CARE_ROOM -> "Care Room $number"
                                AreaType.FOOD_AREA -> "Food Area $number"
                                AreaType.RESTROOMS -> "Restrooms $number"
                                AreaType.EMERGENCY_EXIT -> "Emergency Exit $number"
                                AreaType.OTHER -> "Area $number"
                            }
                        }
                        append(examples.joinToString(", "))
                        if (countInt > 3) append(", ...")
                    }
                }

                if (previewText.isNotEmpty()) {
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val countInt = count.toIntOrNull() ?: 0
                    val startInt = startNumber.toIntOrNull() ?: 1
                    if (countInt > 0) {
                        onAdd(selectedType, countInt, startInt)
                    }
                },
                enabled = (count.toIntOrNull() ?: 0) > 0
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditAreaDialog(
    area: com.eventmonitor.core.data.local.entities.AreaTemplateEntity,
    onDismiss: () -> Unit,
    onUpdate: (com.eventmonitor.core.data.local.entities.AreaTemplateEntity) -> Unit
) {
    var name by remember { mutableStateOf(area.name) }
    var capacity by remember { mutableStateOf(area.capacity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Area") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Area Name") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    )
                )

                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it.filter { char -> char.isDigit() } },
                    label = { Text("Capacity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                Text(
                    text = "Type: ${AreaType.fromString(area.type).displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val capacityInt = capacity.toIntOrNull() ?: area.capacity
                    onUpdate(area.copy(name = name, capacity = capacityInt))
                },
                enabled = name.isNotBlank()
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
