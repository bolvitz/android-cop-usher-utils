package com.eventmonitor.feature.headcounter.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.eventmonitor.core.common.theme.Amber
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.SoftCard
import com.eventmonitor.core.common.ui.SoftIconButton
import com.eventmonitor.core.common.ui.Hairline
import com.eventmonitor.core.common.ui.HairlineSoft
import com.eventmonitor.core.common.ui.SparkBar
import com.eventmonitor.core.common.ui.capacityToneFor
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val uiState by viewModel.uiState.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SoftAppBar(
            title = "Almanac",
            subtitle = if (uiState.venueName.isNotEmpty())
                "Trends · ${uiState.venueName}"
            else "Attendance Trends",
            onBack = {
                haptic.light()
                onNavigateBack()
            },
        )

        Box(modifier = Modifier.weight(1f)) {
            ArcadeBackground(modifier = Modifier.matchParentSize())
            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(strokeWidth = 2.dp) }

                uiState.isEmpty -> EmptyAlmanac()

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 48.dp),
                ) {
                    item(key = "mast") {
                        AlmanacMasthead(
                            state = uiState,
                            period = selectedPeriod,
                        )
                    }
                    item(key = "period") {
                        PeriodStrip(
                            selected = selectedPeriod,
                            onSelect = {
                                haptic.selection()
                                viewModel.selectPeriod(it)
                            },
                        )
                    }
                    item(key = "chart") {
                        ChartSection(
                            dataPoints = uiState.attendanceOverTime,
                            averageAttendance = uiState.averageAttendance.toFloat(),
                        )
                    }
                    item(key = "peak-low") {
                        PeakLowSpread(state = uiState)
                    }
                    item(key = "capacity") {
                        CapacitySection(
                            capacityPct = uiState.avgCapacityUtilization,
                            totalEvents = uiState.totalEvents,
                        )
                    }
                    if (uiState.eventTypeBreakdown.isNotEmpty()) {
                        item(key = "type-head") { SectionRule("By Event Type", "COLUMN IV") }
                        items(
                            items = uiState.eventTypeBreakdown.withIndex().toList(),
                            key = { it.value.typeName + it.index },
                        ) { (index, breakdown) ->
                            EventTypeRow(
                                rank = index + 1,
                                breakdown = breakdown,
                                maxAvg = uiState.eventTypeBreakdown.maxOf { it.avgAttendance }
                                    .coerceAtLeast(1),
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    item(key = "foot") { AlmanacFooter(uiState.totalEvents) }
                }
            }
        }
    }
}

// region ── Masthead ───────────────────────────────────────────────────────

@Composable
private fun AlmanacMasthead(state: TrendsUiState, period: TrendPeriod) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val rangeLabel = remember(period) { buildRangeLabel(period) }
    val volume = remember(period) { romanNumeral(period.ordinal + 1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "VOLUME $volume",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }

        Spacer(Modifier.height(10.dp))
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = state.averageAttendance.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 104.sp,
                    lineHeight = 92.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-5).sp,
                ),
                color = ink,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.padding(bottom = 14.dp)) {
                Text(
                    text = "Average",
                    style = softHeadline(24),
                    color = ink,
                )
                Text(
                    text = "attendance".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GrowthChip(pct = state.growthPercentage)
            Spacer(Modifier.width(10.dp))
            HairlineSoft(modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Text(
                text = "N = ${state.totalEvents}",
                style = MaterialTheme.typography.labelMedium,
                color = ink,
            )
        }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun GrowthChip(pct: Int) {
    val positive = pct >= 0
    val tone = when {
        pct > 0 -> Sage
        pct < 0 -> Signal
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val arrow = when {
        pct > 0 -> "▲"
        pct < 0 -> "▼"
        else -> "—"
    }
    SoftCard(
        cornerRadius = 8,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 8.dp, vertical = 4.dp,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(arrow, style = MaterialTheme.typography.labelSmall, color = tone)
            Text(
                text = "${if (positive && pct != 0) "+" else ""}${abs(pct)}%",
                style = MaterialTheme.typography.labelMedium,
                color = tone,
            )
            Text(
                text = "vs prior half".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// endregion

// region ── Period strip ──────────────────────────────────────────────────

@Composable
private fun PeriodStrip(
    selected: TrendPeriod,
    onSelect: (TrendPeriod) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            TrendPeriod.entries.forEachIndexed { index, period ->
                if (index > 0) {
                    Box(
                        Modifier
                            .width(FieldTokens.Hair)
                            .height(22.dp)
                            .background(MaterialTheme.colorScheme.outline)
                            .align(Alignment.CenterVertically),
                    )
                }
                PeriodTab(
                    label = period.strip,
                    selected = period == selected,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(period) },
                )
            }
        }
    }
}

@Composable
private fun PeriodTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) ink else muted,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth(if (selected) 0.6f else 0f)
                .height(FieldTokens.HairStrong)
                .background(ink),
        )
    }
}

// endregion

// region ── Chart ─────────────────────────────────────────────────────────

@Composable
private fun ChartSection(
    dataPoints: List<TrendDataPoint>,
    averageAttendance: Float,
) {
    if (dataPoints.isEmpty()) return

    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val outlineSoft = MaterialTheme.colorScheme.outlineVariant
    val paper = MaterialTheme.colorScheme.background
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)

    val max = dataPoints.maxOf { it.value }
    val peakIdx = dataPoints.indexOfFirst { it.value == max }
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700),
        label = "chartReveal",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionRule("Attendance over time", "COLUMN I")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
        ) {
            Text(
                text = "Tracked across ${dataPoints.size} event${if (dataPoints.size == 1) "" else "s"}.",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
            Spacer(Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
            ) {
                val topPad = 18f
                val leftPad = 52f
                val bottomPad = 32f
                val rightPad = 12f
                val chartW = size.width - leftPad - rightPad
                val chartH = size.height - topPad - bottomPad
                val yMax = max * 1.15f
                val range = if (yMax <= 0f) 1f else yMax

                // Grid rules (hairlines)
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = topPad + chartH * (1 - i.toFloat() / gridLines)
                    drawLine(
                        color = if (i == 0) ink else outlineSoft,
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = if (i == 0) 1.2f else 0.8f,
                    )
                    val value = (range * i / gridLines).toInt()
                    val layout = textMeasurer.measure(
                        text = value.toString(),
                        style = labelStyle.copy(color = muted),
                    )
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            leftPad - layout.size.width - 8f,
                            y - layout.size.height / 2f
                        ),
                    )
                }

                // Axis ticks on left
                drawLine(
                    color = ink,
                    start = Offset(leftPad, topPad),
                    end = Offset(leftPad, topPad + chartH),
                    strokeWidth = 1.2f,
                )

                // Average reference line
                if (averageAttendance > 0f) {
                    val avgY = topPad + chartH * (1 - (averageAttendance / range))
                    drawLine(
                        color = outline,
                        start = Offset(leftPad, avgY),
                        end = Offset(size.width - rightPad, avgY),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                    )
                    val avgLabel = textMeasurer.measure(
                        text = "AVG ${averageAttendance.toInt()}",
                        style = labelStyle.copy(color = muted),
                    )
                    drawRect(
                        color = paper,
                        topLeft = Offset(
                            size.width - rightPad - avgLabel.size.width - 6f,
                            avgY - avgLabel.size.height / 2f - 2f
                        ),
                        size = Size(avgLabel.size.width + 6f, avgLabel.size.height + 4f),
                    )
                    drawText(
                        textLayoutResult = avgLabel,
                        topLeft = Offset(
                            size.width - rightPad - avgLabel.size.width - 3f,
                            avgY - avgLabel.size.height / 2f
                        ),
                    )
                }

                // Build line path
                val pointCount = dataPoints.size
                val stepX = if (pointCount > 1) chartW / (pointCount - 1) else 0f
                val shownCount =
                    (pointCount * animProgress).toInt().coerceAtLeast(if (pointCount > 0) 1 else 0)

                if (pointCount == 1) {
                    val x = leftPad + chartW / 2f
                    val y = topPad + chartH * (1 - (dataPoints[0].value / range))
                    drawCircle(ink, radius = 6f, center = Offset(x, y))
                    drawCircle(paper, radius = 2.5f, center = Offset(x, y))
                } else {
                    val path = Path()
                    dataPoints.forEachIndexed { i, p ->
                        if (i > shownCount) return@forEachIndexed
                        val x = leftPad + stepX * i
                        val y = topPad + chartH * (1 - (p.value / range))
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = ink,
                        style = Stroke(width = 2.4f, cap = StrokeCap.Round),
                    )

                    // Dots
                    dataPoints.forEachIndexed { i, p ->
                        if (i > shownCount) return@forEachIndexed
                        val x = leftPad + stepX * i
                        val y = topPad + chartH * (1 - (p.value / range))
                        val isPeak = i == peakIdx
                        val r = if (isPeak) 6f else 3.5f
                        drawCircle(ink, radius = r, center = Offset(x, y))
                        if (isPeak) {
                            drawCircle(Signal, radius = 3f, center = Offset(x, y))
                        } else {
                            drawCircle(paper, radius = 1.6f, center = Offset(x, y))
                        }
                    }

                    // X-axis labels (sparse)
                    val labelIndices = when {
                        pointCount <= 3 -> dataPoints.indices.toList()
                        pointCount <= 7 -> listOf(0, pointCount / 2, pointCount - 1)
                        else -> listOf(
                            0,
                            pointCount / 4,
                            pointCount / 2,
                            pointCount * 3 / 4,
                            pointCount - 1
                        )
                    }
                    labelIndices.forEach { i ->
                        val x = leftPad + stepX * i
                        // Tiny tick
                        drawLine(
                            color = ink,
                            start = Offset(x, topPad + chartH),
                            end = Offset(x, topPad + chartH + 4f),
                            strokeWidth = 1f,
                        )
                        val layout = textMeasurer.measure(
                            text = dataPoints[i].label.uppercase(),
                            style = labelStyle.copy(color = muted),
                        )
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(x - layout.size.width / 2f, topPad + chartH + 8f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendSwatch("Series", ink, solid = true)
                LegendSwatch("Peak", Signal, solid = true)
                LegendSwatch("Average", outline, dashed = true)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun LegendSwatch(
    label: String,
    color: Color,
    solid: Boolean = false,
    dashed: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 2.dp)
                .background(if (dashed) Color.Transparent else color),
        ) {
            if (dashed) {
                Row(modifier = Modifier.fillMaxSize()) {
                    repeat(3) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 0.5.dp)
                                .background(color),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        if (solid) {
            Box(
                Modifier
                    .size(4.dp)
                    .background(color),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// endregion

// region ── Peak / Low spread ─────────────────────────────────────────────

@Composable
private fun PeakLowSpread(state: TrendsUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionRule("Dispatches", "COLUMN II")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 18.dp),
        ) {
            PeakLowCell(
                modifier = Modifier.weight(1f),
                kicker = "Peak",
                value = state.peakAttendance,
                subject = state.peakEventName.ifBlank { "—" },
                dateMs = state.peakEventDate,
                accent = Signal,
            )
            Box(
                Modifier
                    .width(FieldTokens.Hair)
                    .height(118.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
            PeakLowCell(
                modifier = Modifier.weight(1f),
                kicker = "Low",
                value = state.lowestAttendance,
                subject = "Lowest count",
                dateMs = 0L,
                accent = Sage,
                alignEnd = true,
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun PeakLowCell(
    modifier: Modifier = Modifier,
    kicker: String,
    value: Int,
    subject: String,
    dateMs: Long,
    accent: Color,
    alignEnd: Boolean = false,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val dateFmt = remember { SimpleDateFormat("MMM d · yyyy", Locale.getDefault()) }
    Column(
        modifier = modifier.padding(
            start = if (alignEnd) 14.dp else 0.dp,
            end = if (alignEnd) 0.dp else 14.dp,
        ),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(6.dp)
                .background(accent))
            Spacer(Modifier.width(6.dp))
            Text(
                text = kicker.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 56.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-2).sp,
            ),
            color = ink,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subject,
            style = MaterialTheme.typography.titleMedium.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
            color = ink,
            maxLines = 2,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
        )
        if (dateMs > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = dateFmt.format(Date(dateMs)).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
    }
}

// endregion

// region ── Capacity ──────────────────────────────────────────────────────

@Composable
private fun CapacitySection(capacityPct: Int, totalEvents: Int) {
    val tone = capacityToneFor(capacityPct / 100f)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onBackground

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionRule("Capacity utilization", "COLUMN III")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = capacityPct.toString(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 72.sp,
                            lineHeight = 66.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-3).sp,
                        ),
                        color = ink,
                    )
                    Text(
                        text = "%",
                        style = softHeadline(28),
                        color = tone,
                        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = totalEvents.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge,
                        color = ink,
                    )
                    Text(
                        text = "events sampled".uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            SparkBar(progress = capacityPct / 100f, height = 6.dp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0%", style = MaterialTheme.typography.labelSmall, color = muted)
                Text(
                    text = capacityDescriptor(capacityPct).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = tone,
                )
                Text("100%", style = MaterialTheme.typography.labelSmall, color = muted)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun capacityDescriptor(pct: Int): String = when {
    pct >= 95 -> "At capacity"
    pct >= 80 -> "Heavy load"
    pct >= 50 -> "Steady flow"
    pct > 0 -> "Light turnout"
    else -> "Uncharted"
}

// endregion

// region ── Event type rows ───────────────────────────────────────────────

@Composable
private fun EventTypeRow(rank: Int, breakdown: EventTypeBreakdown, maxAvg: Int) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val pct = (breakdown.avgAttendance.toFloat() / maxAvg).coerceIn(0f, 1f)
    val barTone = when (rank) {
        1 -> Signal
        2 -> Amber
        else -> ink
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = rank.toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelMedium,
                color = muted,
                modifier = Modifier.width(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = breakdown.typeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = ink,
                    maxLines = 1,
                )
                Text(
                    text = "${breakdown.count} event${if (breakdown.count == 1) "" else "s"}".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = breakdown.avgAttendance.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = ink,
                )
                Text(
                    text = "avg".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(28.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(pct)
                        .background(barTone),
                )
            }
        }
    }
}

// endregion

// region ── Helpers ───────────────────────────────────────────────────────

@Composable
private fun SectionRule(title: String, tag: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 22.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = softHeadline(22),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun EmptyAlmanac() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "—",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Light,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "No dispatches on file",
                style = softHeadline(20),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "start counting to populate the almanac".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AlmanacFooter(totalEvents: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "— END OF REPORT —",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$totalEvents REC",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun buildRangeLabel(period: TrendPeriod): String {
    val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
    val end = Calendar.getInstance()
    val start = (end.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -period.days)
    }
    return "${fmt.format(start.time).uppercase()} – ${fmt.format(end.time).uppercase()}"
}

private fun romanNumeral(n: Int): String {
    val table = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
    )
    var x = n
    val sb = StringBuilder()
    for ((v, s) in table) {
        while (x >= v) {
            sb.append(s); x -= v
        }
    }
    return sb.toString()
}

// endregion
