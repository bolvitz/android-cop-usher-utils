package com.eventmonitor.feature.headcounter.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    val uiState by viewModel.uiState.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Attendance Trends")
                        if (uiState.venueName.isNotEmpty()) {
                            Text(
                                text = uiState.venueName,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
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

            uiState.isEmpty -> {
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
                            Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "No data yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Start counting to see trends",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Period selector
                    item {
                        PeriodSelector(
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = { viewModel.selectPeriod(it) }
                        )
                    }

                    // Summary stats row
                    item {
                        SummaryStatsRow(uiState)
                    }

                    // Attendance chart
                    item {
                        AttendanceChart(
                            dataPoints = uiState.attendanceOverTime,
                            averageAttendance = uiState.averageAttendance.toFloat()
                        )
                    }

                    // Growth indicator
                    item {
                        GrowthCard(
                            growthPercentage = uiState.growthPercentage,
                            avgCapacity = uiState.avgCapacityUtilization
                        )
                    }

                    // Event type breakdown
                    if (uiState.eventTypeBreakdown.isNotEmpty()) {
                        item {
                            Text(
                                "By Event Type",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(uiState.eventTypeBreakdown) { breakdown ->
                            EventTypeCard(breakdown)
                        }
                    }

                    // Bottom spacing
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun PeriodSelector(
    selectedPeriod: TrendPeriod,
    onPeriodSelected: (TrendPeriod) -> Unit
) {
    val haptic = rememberHapticFeedback()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TrendPeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selectedPeriod,
                onClick = {
                    haptic.selection()
                    onPeriodSelected(period)
                },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = TrendPeriod.entries.size
                )
            ) {
                Text(period.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun SummaryStatsRow(state: TrendsUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Events",
            value = state.totalEvents.toString(),
            icon = Icons.Default.Event,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Average",
            value = state.averageAttendance.toString(),
            icon = Icons.Default.People,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Peak",
            value = state.peakAttendance.toString(),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun AttendanceChart(
    dataPoints: List<TrendDataPoint>,
    averageAttendance: Float
) {
    if (dataPoints.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Attendance Over Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val maxValue = dataPoints.maxOf { it.value } * 1.15f
                val minValue = 0f
                val range = maxValue - minValue

                val leftPadding = 40f
                val bottomPadding = 30f
                val chartWidth = size.width - leftPadding
                val chartHeight = size.height - bottomPadding

                // Draw grid lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = chartHeight * (1 - i.toFloat() / gridLines)
                    drawLine(
                        color = surfaceVariant,
                        start = Offset(leftPadding, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    // Y-axis label
                    val labelValue = (minValue + range * i / gridLines).toInt()
                    drawText(
                        textMeasurer = textMeasurer,
                        text = labelValue.toString(),
                        topLeft = Offset(0f, y - 8f),
                        style = labelStyle.copy(color = onSurfaceVariant.copy(alpha = 0.6f))
                    )
                }

                // Draw average line (dashed)
                if (averageAttendance > 0) {
                    val avgY = chartHeight * (1 - (averageAttendance - minValue) / range)
                    drawLine(
                        color = tertiaryColor.copy(alpha = 0.5f),
                        start = Offset(leftPadding, avgY),
                        end = Offset(size.width, avgY),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }

                if (dataPoints.size == 1) {
                    // Single point - draw a dot
                    val x = leftPadding + chartWidth / 2
                    val y = chartHeight * (1 - (dataPoints[0].value - minValue) / range)
                    drawCircle(primaryColor, radius = 8f, center = Offset(x, y))
                } else {
                    // Draw line chart
                    val path = Path()
                    val stepX = chartWidth / (dataPoints.size - 1)

                    dataPoints.forEachIndexed { index, point ->
                        val x = leftPadding + stepX * index
                        val y = chartHeight * (1 - (point.value - minValue) / range)

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    // Draw the line
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )

                    // Draw dots at data points
                    dataPoints.forEachIndexed { index, point ->
                        val x = leftPadding + stepX * index
                        val y = chartHeight * (1 - (point.value - minValue) / range)
                        drawCircle(primaryColor, radius = 5f, center = Offset(x, y))
                        drawCircle(Color.White, radius = 2.5f, center = Offset(x, y))
                    }

                    // X-axis labels (show first, middle, last)
                    val labelIndices = when {
                        dataPoints.size <= 3 -> dataPoints.indices.toList()
                        dataPoints.size <= 7 -> listOf(0, dataPoints.size / 2, dataPoints.size - 1)
                        else -> listOf(
                            0,
                            dataPoints.size / 4,
                            dataPoints.size / 2,
                            dataPoints.size * 3 / 4,
                            dataPoints.size - 1
                        )
                    }
                    labelIndices.forEach { index ->
                        val x = leftPadding + stepX * index
                        drawText(
                            textMeasurer = textMeasurer,
                            text = dataPoints[index].label,
                            topLeft = Offset(x - 15f, chartHeight + 8f),
                            style = labelStyle.copy(color = onSurfaceVariant.copy(alpha = 0.6f))
                        )
                    }
                }
            }

            // Legend
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Text(
                        "Attendance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp, 2.dp)
                            .background(tertiaryColor.copy(alpha = 0.5f))
                    )
                    Text(
                        "Avg (${averageAttendance.toInt()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun GrowthCard(
    growthPercentage: Int,
    avgCapacity: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (growthPercentage >= 0)
                        Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (growthPercentage >= 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${if (growthPercentage >= 0) "+" else ""}$growthPercentage%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (growthPercentage >= 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                Text(
                    "Growth Trend",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$avgCapacity%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "Avg Capacity",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun EventTypeCard(breakdown: EventTypeBreakdown) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = breakdown.typeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${breakdown.count} events",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = breakdown.avgAttendance.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "avg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
