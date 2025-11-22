package com.cop.app.headcounter.presentation.screens.reports

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cop.app.headcounter.presentation.utils.HapticFeedbackHelper
import com.cop.app.headcounter.presentation.utils.rememberHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val reportData by viewModel.reportData.collectAsState()
    var showPeriodMenu by remember { mutableStateOf(false) }

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
        if (reportData.totalServices == 0) {
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
                // Summary cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Total Services",
                            value = reportData.totalServices.toString(),
                            icon = Icons.Default.Event,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Total Attendance",
                            value = reportData.totalAttendance.toString(),
                            icon = Icons.Default.People,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    SummaryCard(
                        title = "Average Attendance",
                        value = reportData.averageAttendance.toString(),
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.fillMaxWidth()
                    )
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
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
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
                    DetailRow("Services counted", areaStatistics.servicesCount.toString())
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
