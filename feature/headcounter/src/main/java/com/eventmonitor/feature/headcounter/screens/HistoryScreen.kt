package com.eventmonitor.feature.headcounter.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.data.local.entities.EventWithDetails
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onServiceClick: (branchId: String, serviceId: String) -> Unit,
    onStartNewCount: (venueId: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val uiState by viewModel.uiState.collectAsState()
    val selectedReport by viewModel.selectedServiceReport.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showUnlockDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Service History") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.light()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is HistoryUiState.Success || uiState is HistoryUiState.Empty) {
                val venueId = when (val state = uiState) {
                    is HistoryUiState.Success -> state.events.firstOrNull()?.event?.venueId
                    is HistoryUiState.Empty -> viewModel.venueId
                    else -> null
                }

                venueId?.let { vId ->
                    FloatingActionButton(
                        onClick = {
                            haptic.medium()
                            onStartNewCount(vId)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Start New Count")
                    }
                }
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is HistoryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is HistoryUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "No service history yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Start counting to create your first service",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is HistoryUiState.Success -> {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.events) { serviceWithDetails ->
                            ServiceHistoryCard(
                                service = serviceWithDetails,
                                onResumeEdit = {
                                    haptic.medium()
                                    onServiceClick(serviceWithDetails.event.venueId, serviceWithDetails.event.id)
                                },
                                onViewReport = {
                                    haptic.light()
                                    viewModel.generateReport(serviceWithDetails.event.id)
                                },
                                onUnlock = {
                                    haptic.light()
                                    showUnlockDialog = serviceWithDetails.event.id
                                },
                                onDelete = {
                                    haptic.light()
                                    showDeleteDialog = serviceWithDetails.event.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Report Dialog
    selectedReport?.let { report ->
        ServiceReportDialog(
            report = report,
            onDismiss = {
                haptic.light()
                viewModel.clearReport()
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { serviceId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Service") },
            text = { Text("Are you sure you want to delete this service? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.strong()
                        viewModel.deleteEvent(serviceId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.light()
                    showDeleteDialog = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Unlock Confirmation Dialog
    showUnlockDialog?.let { serviceId ->
        AlertDialog(
            onDismissRequest = { showUnlockDialog = null },
            title = { Text("Unlock Service") },
            text = { Text("Unlocking this service will allow you to edit the counts. Do you want to proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.medium()
                        viewModel.unlockEvent(serviceId)
                        showUnlockDialog = null
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic.light()
                    showUnlockDialog = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ServiceHistoryCard(
    service: EventWithDetails,
    onResumeEdit: () -> Unit,
    onViewReport: () -> Unit,
    onUnlock: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (service.event.isLocked)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header: Date and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormat.format(Date(service.event.date)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = service.event.eventName.ifEmpty {
                            service.event.eventType
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = service.venue.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Total Attendance Badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = service.event.totalAttendance.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Capacity Progress
            if (service.event.totalCapacity > 0) {
                val percentage = (service.event.totalAttendance.toFloat() / service.event.totalCapacity * 100).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { (percentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                        color = when {
                            percentage < 50 -> MaterialTheme.colorScheme.tertiary
                            percentage < 80 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        },
                    )
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (service.event.countedBy.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = service.event.countedBy,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (service.event.isLocked) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Locked",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divider to separate service info from actions
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Resume/Edit or View button
                FilledTonalButton(
                    onClick = onResumeEdit,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        if (service.event.isLocked) Icons.Default.Visibility else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (service.event.isLocked) "View" else "Edit",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // View Report button
                OutlinedButton(
                    onClick = onViewReport,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Report",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // More actions menu
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (service.event.isLocked) {
                            DropdownMenuItem(
                                text = { Text("Unlock") },
                                onClick = {
                                    onUnlock()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.LockOpen, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onDelete()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceReportDialog(
    report: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.82f),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Summary",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                HorizontalDivider()

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        item {
                            ServiceReportText(
                                report = report,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceReportText(report: String, modifier: Modifier = Modifier) {
    val lines = remember(report) { report.split("\n") }
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var inAreaBreakdown = false

        lines.forEachIndexed { index, line ->
            when {
                // Main title "HEAD COUNT REPORT"
                line == "HEAD COUNT REPORT" -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Venue name (uppercase, second line)
                index == 1 && line == line.uppercase() -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Location (third line, typically not all caps)
                index == 2 -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Section headers
                line in listOf("AREA BREAKDOWN", "AREA", "TOTAL", "EVENT NOTES") -> {
                    inAreaBreakdown = line == "AREA BREAKDOWN" || line == "AREA"
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = line,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 18.sp,
                            letterSpacing = 3.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Area count number (standalone number line in area breakdown)
                inAreaBreakdown && line.trim().toIntOrNull() != null -> {
                    Text(
                        text = line.trim(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 24.sp,
                            letterSpacing = 3.sp
                        ),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Area name with count (e.g., "Bay 1                  5")
                inAreaBreakdown && line.contains(Regex("\\s{2,}")) &&
                line.split(Regex("\\s{2,}")).lastOrNull()?.toIntOrNull() != null -> {
                    val parts = line.split(Regex("\\s{2,}"))
                    if (parts.size == 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = parts[0],
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 18.sp
                                ),
                                fontWeight = FontWeight.Normal,
                                color = onSurfaceVariant,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            Text(
                                text = parts[1],
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 24.sp,
                                    letterSpacing = 3.sp
                                ),
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
                // Divider lines (solid underscore)
                line.startsWith("_") -> {
                    inAreaBreakdown = false
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                // Dashed separator (dots)
                line.startsWith(".") -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        color = onSurfaceVariant.copy(alpha = 0.2f)
                    )
                }
                // Empty lines
                line.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Label: Value format (Event:, Date:, Time:, etc.)
                line.contains(":") && !line.startsWith(" ") -> {
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        // Special handling for Weather and Generated
                        val isMetadata = parts[0] in listOf("Weather", "Generated", "ID")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(
                                text = parts[0] + ":",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurfaceVariant,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                modifier = if (isMetadata) Modifier else Modifier.width(130.dp)
                            )
                            if (!isMetadata) {
                                Text(
                                    text = parts[1].trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = onSurface,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = parts[1].trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onSurfaceVariant.copy(alpha = 0.7f),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = onSurface
                        )
                    }
                }
                // Area names in area breakdown (non-numeric lines)
                inAreaBreakdown && line.trim().isNotEmpty() -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp
                        ),
                        color = onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Regular text
                else -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
