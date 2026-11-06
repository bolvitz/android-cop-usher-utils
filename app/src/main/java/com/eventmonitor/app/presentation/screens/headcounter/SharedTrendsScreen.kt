package com.eventmonitor.app.presentation.screens.headcounter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eventmonitor.shared.presentation.headcounter.TrendDataPoint
import com.eventmonitor.shared.presentation.headcounter.TrendPeriod
import com.eventmonitor.shared.presentation.headcounter.TrendsUiState
import com.eventmonitor.shared.presentation.headcounter.TrendsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Attendance trends for a period: average, best and lowest, a bar per count, and a breakdown by event type. Matches iOS TrendsView. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTrendsScreen(
    venueId: String?,
    onBack: () -> Unit,
    viewModel: TrendsViewModel = koinViewModel { parametersOf(venueId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reduceMotion = rememberReduceMotion()

    Scaffold(
        containerColor = CounterPalette.ground,
        topBar = {
            TopAppBar(
                title = { Text("Trends", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CounterPalette.ground),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            PeriodPicker(state.period, viewModel::selectPeriod)
            when {
                state.isLoading -> Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    CircularProgressIndicator(color = CounterPalette.chalk)
                }
                state.isEmpty -> EmptyState(
                    Icons.Filled.BarChart,
                    "Not enough counts",
                    "No counts were taken in this period. Try a longer one.",
                    Modifier.fillMaxWidth(),
                )
                else -> {
                    Hero(state)
                    Stats(state)
                    AttendanceChart(state, reduceMotion)
                    if (state.eventTypeBreakdown.isNotEmpty()) Breakdown(state)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodPicker(selected: TrendPeriod, onSelect: (TrendPeriod) -> Unit) {
    val periods = TrendPeriod.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        periods.forEachIndexed { i, period ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(i, periods.size),
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = CounterPalette.plateHigh,
                    activeContentColor = CounterPalette.chalk,
                    activeBorderColor = CounterPalette.line,
                    inactiveContainerColor = CounterPalette.plate.copy(alpha = 0.5f),
                    inactiveContentColor = CounterPalette.pew,
                    inactiveBorderColor = CounterPalette.line,
                ),
            ) { Text(period.label, maxLines = 1) }
        }
    }
}

@Composable
private fun Hero(state: TrendsUiState) {
    val growth = state.growthPercentage
    Column(
        Modifier.semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Average per count", style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${state.averageAttendance}", style = digitStyle(76.dp), color = CounterPalette.chalk)
            Text(
                "${if (growth >= 0) "+" else ""}$growth%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (growth >= 0) CounterPalette.brass else CounterPalette.pew,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }
        Text(
            "Growth compares the second half of this period with the first.",
            style = MaterialTheme.typography.bodySmall,
            color = CounterPalette.pew,
        )
    }
}

@Composable
private fun Stats(state: TrendsUiState) {
    Column {
        HorizontalDivider(color = CounterPalette.line)
        Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Counts", state.totalEvents, CounterPalette.chalk, Modifier.weight(1f))
            Stat("Best turnout", state.peakAttendance, CounterPalette.brass, Modifier.weight(1f))
            Stat("Lowest", state.lowestAttendance, CounterPalette.chalk, Modifier.weight(1f))
        }
        HorizontalDivider(color = CounterPalette.line)
    }
}

@Composable
private fun Stat(label: String, value: Int, color: Color, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew)
        Text("$value", style = digitStyle(32.dp), color = color)
    }
}

/** Round axis steps (1, 2 or 5 times a power of ten) so tick labels read cleanly. */
internal fun niceStep(range: Float, ticks: Int = 3): Float {
    if (range <= 0f) return 1f
    val raw = range / ticks
    val magnitude = 10f.pow(floor(log10(raw)))
    val residual = raw / magnitude
    val nice = when {
        residual <= 1f -> 1f
        residual <= 2f -> 2f
        residual <= 5f -> 5f
        else -> 10f
    }
    return maxOf(1f, nice * magnitude)
}

@Composable
private fun AttendanceChart(state: TrendsUiState, reduceMotion: Boolean) {
    val points = state.attendanceOverTime
    val grow = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(points.map(TrendDataPoint::date)) {
        if (reduceMotion) return@LaunchedEffect
        grow.snapTo(0f)
        grow.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 110f))
    }
    val peak = state.peakAttendance.toFloat()
    val domain = maxOf(peak, 1f) * 1.08f
    val ticks = remember(domain) {
        val step = niceStep(domain)
        (0..ceil(domain / step).toInt()).map { it * step }.filter { it <= domain }
    }
    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(color = CounterPalette.pew, fontSize = 11.sp)
    val brass = CounterPalette.brass
    val bar = CounterPalette.plateHigh
    val line = CounterPalette.line
    val pew = CounterPalette.pew
    val avg = state.averageAttendance.toFloat()
    // Up to four date labels, evenly spread, so they never collide.
    val labelIndexes = remember(points.size) {
        if (points.size <= 4) points.indices.toList()
        else listOf(0, points.size / 3, 2 * points.size / 3, points.size - 1).distinct()
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Attendance over time",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = CounterPalette.chalk,
            modifier = Modifier.semantics { heading() },
        )
        if (state.peakEventName.isNotEmpty()) {
            Text("Best turnout: ${state.peakEventName}", style = MaterialTheme.typography.bodySmall, color = CounterPalette.pew)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(top = 10.dp)
                .semantics {
                    contentDescription = "Bar chart of ${points.size} counts. " +
                        points.joinToString(". ") { "${it.eventName}, ${it.value.toInt()} on ${it.label}" }
                },
        ) {
            val axisW = 32.dp.toPx()
            val labelH = 18.dp.toPx()
            val plotW = size.width - axisW
            val plotH = size.height - labelH
            fun y(v: Float) = plotH - plotH * (v / domain)

            ticks.forEach { t ->
                drawLine(line, Offset(axisW, y(t)), Offset(size.width, y(t)), 1.dp.toPx())
                val text = measurer.measure("${t.toInt()}", axisStyle)
                drawText(text, topLeft = Offset(axisW - text.size.width - 6.dp.toPx(), y(t) - text.size.height / 2f))
            }

            val slot = plotW / maxOf(points.size, 1)
            val barW = minOf(slot * 0.6f, 28.dp.toPx())
            val radius = minOf(4.dp.toPx(), barW / 2)
            points.forEachIndexed { i, p ->
                val h = plotH * (p.value / domain) * grow.value
                if (h <= 0f) return@forEachIndexed
                val left = axisW + slot * i + (slot - barW) / 2
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left, plotH - h, left + barW, plotH,
                            topLeftCornerRadius = CornerRadius(radius), topRightCornerRadius = CornerRadius(radius),
                        ),
                    )
                }
                drawPath(path, if (p.value == peak) brass else bar)
            }

            val avgY = y(avg * grow.value)
            drawLine(
                pew, Offset(axisW, avgY), Offset(size.width, avgY), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
            )

            labelIndexes.forEach { i ->
                val text = measurer.measure(points[i].label, axisStyle)
                val center = axisW + slot * i + slot / 2
                val x = (center - text.size.width / 2f).coerceIn(axisW, size.width - text.size.width)
                drawText(text, topLeft = Offset(x, plotH + 4.dp.toPx()))
            }
        }
        Text("Dashed line: average", style = MaterialTheme.typography.labelSmall, color = CounterPalette.pew)
    }
}

@Composable
private fun Breakdown(state: TrendsUiState) {
    Column {
        Text(
            "By event type",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = CounterPalette.chalk,
            modifier = Modifier.padding(bottom = 6.dp).semantics { heading() },
        )
        state.eventTypeBreakdown.forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics(mergeDescendants = true) { },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(row.typeName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = CounterPalette.chalk)
                    Text(
                        if (row.count == 1) "1 count, average" else "${row.count} counts, average",
                        style = MaterialTheme.typography.bodySmall,
                        color = CounterPalette.pew,
                    )
                }
                Text("${row.avgAttendance}", style = digitStyle(26.dp), color = CounterPalette.chalk)
            }
            HorizontalDivider(color = CounterPalette.line)
        }
    }
}
