package com.cop.app.headcounter.presentation.screens.counting

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cop.app.headcounter.data.local.entities.ServiceTypeEntity
import com.cop.app.headcounter.domain.models.ServiceType
import com.cop.app.headcounter.presentation.utils.rememberHapticFeedback
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountingScreen(
    viewModel: CountingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val haptic = rememberHapticFeedback()
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
                        if (uiState.serviceName.isNotEmpty()) {
                            Text(
                                text = uiState.serviceName,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.light()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.medium()
                            viewModel.undo()
                        },
                        enabled = canUndo
                    ) {
                        Icon(Icons.Default.Undo, "Undo")
                    }
                    IconButton(
                        onClick = {
                            haptic.medium()
                            viewModel.redo()
                        },
                        enabled = canRedo
                    ) {
                        Icon(Icons.Default.Redo, "Redo")
                    }
                    IconButton(onClick = {
                        haptic.light()
                        viewModel.shareReport()
                    }) {
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
                    // Total attendance card - compact
                    val animatedAttendance by animateIntAsState(
                        targetValue = uiState.totalAttendance,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "attendanceAnimation"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Total Attendance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                if (uiState.totalCapacity > 0) {
                                    val percentage = (uiState.totalAttendance.toFloat() / uiState.totalCapacity * 100).toInt()
                                    Text(
                                        text = "$percentage% of ${uiState.totalCapacity}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Text(
                                text = animatedAttendance.toString(),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Area counting cards
                    if (uiState.areaCounts.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                "Loading areas...",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.areaCounts.forEach { areaCount ->
                                AreaCountCard(
                                    areaCount = areaCount,
                                    isLocked = uiState.isLocked,
                                    onIncrement = {
                                        haptic.counter()
                                        viewModel.incrementCount(areaCount.id)
                                    },
                                    onDecrement = {
                                        haptic.counter()
                                        viewModel.decrementCount(areaCount.id)
                                    },
                                    onSetCount = { newCount ->
                                        viewModel.setCount(areaCount.id, newCount)
                                    }
                                )
                            }
                        }
                    }
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
    val haptic = rememberHapticFeedback()
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
                                        haptic.selection()
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
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.success()
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
            TextButton(onClick = {
                haptic.light()
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AreaCountCard(
    areaCount: AreaCountState,
    isLocked: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetCount: (Int) -> Unit
) {
    val animatedCount by animateIntAsState(
        targetValue = areaCount.count,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "areaCountAnimation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Area name and count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = areaCount.template.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (areaCount.capacity > 0) {
                        val percentage = (areaCount.count.toFloat() / areaCount.capacity * 100).toInt()
                        Text(
                            text = "$percentage% · ${areaCount.capacity} capacity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Count display - smaller
                Text(
                    text = animatedCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar - inline
            if (areaCount.capacity > 0) {
                val progress = (areaCount.count.toFloat() / areaCount.capacity).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = when {
                        progress < 0.5f -> MaterialTheme.colorScheme.tertiary
                        progress < 0.8f -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Increment/Decrement buttons - compact
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Decrement button
                FilledTonalButton(
                    onClick = onDecrement,
                    modifier = Modifier.weight(1f),
                    enabled = !isLocked && areaCount.count > 0,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Remove, "Decrease", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("1", style = MaterialTheme.typography.labelLarge)
                }

                // Increment button
                FilledTonalButton(
                    onClick = onIncrement,
                    modifier = Modifier.weight(1f),
                    enabled = !isLocked,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Add, "Increase", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("1", style = MaterialTheme.typography.labelLarge)
                }

                // Quick add buttons in same row
                if (!isLocked) {
                    OutlinedButton(
                        onClick = { onSetCount(areaCount.count + 5) },
                        modifier = Modifier.weight(0.6f),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp)
                    ) {
                        Text("+5", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { onSetCount(areaCount.count + 10) },
                        modifier = Modifier.weight(0.6f),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp)
                    ) {
                        Text("+10", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Lock indicator
            if (isLocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Lock,
                        "Locked",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Service locked",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
