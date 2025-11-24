package com.eventmonitor.app.presentation.screens.reports

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.common.utils.HapticFeedbackHelper
import com.eventmonitor.core.common.utils.rememberHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val reportData by viewModel.reportData.collectAsState()
    val venues by viewModel.venues.collectAsState()
    val eventTypes by viewModel.eventTypes.collectAsState()
    val selectedVenue by viewModel.selectedVenue.collectAsState()
    val selectedServiceType by viewModel.selectedServiceType.collectAsState()

    var showPeriodMenu by remember { mutableStateOf(false) }
    var showBranchMenu by remember { mutableStateOf(false) }
    var showServiceTypeMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.light()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Period selector
                    Box {
                        TextButton(onClick = {
                            haptic.light()
                            showPeriodMenu = true
                        }) {
                            Text(selectedPeriod.displayName)
                            Icon(Icons.Default.ArrowDropDown, "Select period", modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = showPeriodMenu,
                            onDismissRequest = { showPeriodMenu = false }
                        ) {
                            ReportPeriod.entries.forEach { period ->
                                DropdownMenuItem(
                                    text = { Text(period.displayName) },
                                    onClick = {
                                        haptic.selection()
                                        viewModel.selectPeriod(period)
                                        showPeriodMenu = false
                                    },
                                    leadingIcon = if (period == selectedPeriod) {
                                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (reportData.totalEvents == 0) {
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
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Text(
                        "No data for selected period",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Try selecting a different time range",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Branch filter
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = {
                                    haptic.light()
                                    showBranchMenu = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedVenue?.let { branchId ->
                                        venues.find { it.id == branchId }?.name ?: "All Branches"
                                    } ?: "All Branches",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showBranchMenu,
                                onDismissRequest = { showBranchMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Branches") },
                                    onClick = {
                                        haptic.selection()
                                        viewModel.selectVenue(null)
                                        showBranchMenu = false
                                    },
                                    leadingIcon = if (selectedVenue == null) {
                                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null
                                )
                                venues.forEach { branch ->
                                    DropdownMenuItem(
                                        text = { Text(branch.name) },
                                        onClick = {
                                            haptic.selection()
                                            viewModel.selectVenue(branch.id)
                                            showBranchMenu = false
                                        },
                                        leadingIcon = if (selectedVenue == branch.id) {
                                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                }
                            }
                        }

                        // Service type filter
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = {
                                    haptic.light()
                                    showServiceTypeMenu = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedServiceType?.let { eventTypeId ->
                                        eventTypes.find { it.id == eventTypeId }?.name ?: "All Services"
                                    } ?: "All Services",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showServiceTypeMenu,
                                onDismissRequest = { showServiceTypeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Services") },
                                    onClick = {
                                        haptic.selection()
                                        viewModel.selectServiceType(null)
                                        showServiceTypeMenu = false
                                    },
                                    leadingIcon = if (selectedServiceType == null) {
                                        { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null
                                )
                                eventTypes.forEach { eventType ->
                                    DropdownMenuItem(
                                        text = { Text(eventType.name) },
                                        onClick = {
                                            haptic.selection()
                                            viewModel.selectServiceType(eventType.id)
                                            showServiceTypeMenu = false
                                        },
                                        leadingIcon = if (selectedServiceType == eventType.id) {
                                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }

                // Summary cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryCard(
                            title = "Total\nServices",
                            value = reportData.totalEvents.toString(),
                            icon = Icons.Default.Event,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Total\nAttendance",
                            value = reportData.totalAttendance.toString(),
                            icon = Icons.Default.People,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Average\nAttendance",
                            value = reportData.averageAttendance.toString(),
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Area breakdown section header
                item {
                    Text(
                        "Area Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Area statistics cards - EMPHASIZED
                items(reportData.areaStatistics) { areaStat ->
                    AreaStatisticsCard(
                        areaStatistics = areaStat,
                        haptic = haptic
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(170.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    fontWeight = FontWeight.Medium,
                    lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.3
                )
            }
        }
    }
}

@Composable
fun AreaStatisticsCard(
    areaStatistics: AreaStatistics,
    haptic: HapticFeedbackHelper
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Area name and counts in compact layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Area name
                Text(
                    text = areaStatistics.areaName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Average count - EMPHASIZED
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = areaStatistics.averageCount.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "avg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Total count - smaller
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = areaStatistics.totalCount.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Capacity utilization bar
            if (areaStatistics.capacity > 0) {
                val utilizationPercentage = (areaStatistics.averageCount.toFloat() / areaStatistics.capacity * 100).toInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = (utilizationPercentage / 100f).coerceIn(0f, 1f),
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp),
                        color = when {
                            utilizationPercentage < 50 -> MaterialTheme.colorScheme.tertiary
                            utilizationPercentage < 80 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "$utilizationPercentage%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            utilizationPercentage < 50 -> MaterialTheme.colorScheme.tertiary
                            utilizationPercentage < 80 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            // Expandable details section
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DetailRow("Services counted", areaStatistics.eventsCount.toString())
                    DetailRow("Highest count", areaStatistics.maxCount.toString())
                    DetailRow("Lowest count", areaStatistics.minCount.toString())
                    DetailRow("Capacity", areaStatistics.capacity.toString())
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Expand/collapse button
            TextButton(
                onClick = {
                    haptic.light()
                    expanded = !expanded
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    if (expanded) "Less" else "More",
                    style = MaterialTheme.typography.labelSmall
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
