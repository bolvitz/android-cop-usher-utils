package com.eventmonitor.app.presentation.screens.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.SoftSection
import com.eventmonitor.core.common.ui.SparkBar
import com.eventmonitor.core.common.ui.capacityToneFor
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.utils.rememberHapticFeedback

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val reportData by viewModel.reportData.collectAsState()
    val venues by viewModel.venues.collectAsState()
    val eventTypes by viewModel.eventTypes.collectAsState()
    val selectedVenue by viewModel.selectedVenue.collectAsState()
    val selectedServiceType by viewModel.selectedServiceType.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ArcadeBackground(modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxSize()) {
            SoftAppBar(
                title = "Reports",
                subtitle = "Analytics · Dossier",
                onBack = { haptic.light(); onNavigateBack() },
            )

            // Period segmented strip — always visible, tap to switch
            PeriodStrip(
                selected = selectedPeriod,
                onSelect = {
                    haptic.selection()
                    viewModel.selectPeriod(it)
                },
            )
            Spacer(Modifier.height(20.dp))

            // Filter row: Venue + Type
            FilterRow(
                venueLabel = selectedVenue?.let { id ->
                    venues.find { it.id == id }?.name
                } ?: "All Venues",
                typeLabel = selectedServiceType?.let { id ->
                    eventTypes.find { it.id == id }?.name
                } ?: "All Types",
                venueMenu = {
                    DropdownMenuItem(
                        text = { Text("All Venues") },
                        onClick = {
                            haptic.selection()
                            viewModel.selectVenue(null)
                            it()
                        },
                    )
                    venues.forEach { v ->
                        DropdownMenuItem(
                            text = { Text(v.name) },
                            onClick = {
                                haptic.selection()
                                viewModel.selectVenue(v.id)
                                it()
                            },
                        )
                    }
                },
                typeMenu = {
                    DropdownMenuItem(
                        text = { Text("All Types") },
                        onClick = {
                            haptic.selection()
                            viewModel.selectServiceType(null)
                            it()
                        },
                    )
                    eventTypes.forEach { t ->
                        DropdownMenuItem(
                            text = { Text(t.name) },
                            onClick = {
                                haptic.selection()
                                viewModel.selectServiceType(t.id)
                                it()
                            },
                        )
                    }
                },
            )
            Spacer(Modifier.height(20.dp))

            if (reportData.totalEvents == 0) {
                EmptyReportPanel(selectedPeriod)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    item(key = "masthead") {
                        ReportMasthead(
                            period = selectedPeriod,
                            totalEvents = reportData.totalEvents,
                            totalAttendance = reportData.totalAttendance,
                            averageAttendance = reportData.averageAttendance,
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    if (reportData.areaStatistics.isNotEmpty()) {
                        item(key = "section-areas") {
                            SoftSection(
                                title = "Area Index",
                                eyebrow = "ANALYTICS",
                                hint = "By zone"
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        itemsIndexed(reportData.areaStatistics) { index, stat ->
                            AreaIndexRow(
                                index = index + 1,
                                stat = stat,
                                onToggle = { haptic.light() },
                            )
                            Spacer(Modifier.height(20.dp))
                        }

                        item(key = "area-footer") {
                            AreaIndexFooter(
                                count = reportData.areaStatistics.size,
                                totalEvents = reportData.totalEvents,
                            )
                        }
                    }
                }
            }
        }
    }
}

// region ── Period strip ─────────────────────────────────────────────────

private data class PeriodSpec(val period: ReportPeriod, val short: String, val long: String)

private val PeriodSpecs = listOf(
    PeriodSpec(ReportPeriod.LAST_7_DAYS, "7D", "Last 7 Days"),
    PeriodSpec(ReportPeriod.LAST_30_DAYS, "30D", "Last 30 Days"),
    PeriodSpec(ReportPeriod.LAST_90_DAYS, "90D", "Last 90 Days"),
    PeriodSpec(ReportPeriod.THIS_YEAR, "YTD", "This Year"),
    PeriodSpec(ReportPeriod.ALL_TIME, "ALL", "All Time"),
)

@Composable
private fun PeriodStrip(
    selected: ReportPeriod,
    onSelect: (ReportPeriod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "RANGE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
        ) {
            PeriodSpecs.forEachIndexed { i, spec ->
                PeriodPill(
                    label = spec.short,
                    selected = spec.period == selected,
                    onClick = { onSelect(spec.period) },
                )
                if (i < PeriodSpecs.lastIndex) Spacer(Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun PeriodPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val bg by animateColorAsState(if (selected) ink else paper, label = "periodBg")
    val fg by animateColorAsState(if (selected) paper else ink, label = "periodFg")
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .border(FieldTokens.Hair, ink)
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = fg),
        )
    }
}

// endregion

// region ── Filter row ───────────────────────────────────────────────────

@Composable
private fun FilterRow(
    venueLabel: String,
    typeLabel: String,
    venueMenu: @Composable ((dismiss: () -> Unit) -> Unit),
    typeMenu: @Composable ((dismiss: () -> Unit) -> Unit),
) {
    var showVenue by remember { mutableStateOf(false) }
    var showType by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterSlot(
            eyebrow = "VENUE",
            value = venueLabel,
            onClick = { showVenue = true },
            modifier = Modifier.weight(1f),
        ) {
            DropdownMenu(expanded = showVenue, onDismissRequest = { showVenue = false }) {
                venueMenu { showVenue = false }
            }
        }
        VerticalHair(heightDp = 36)
        FilterSlot(
            eyebrow = "TYPE",
            value = typeLabel,
            onClick = { showType = true },
            modifier = Modifier.weight(1f),
        ) {
            DropdownMenu(expanded = showType, onDismissRequest = { showType = false }) {
                typeMenu { showType = false }
            }
        }
    }
}

@Composable
private fun FilterSlot(
    eyebrow: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "▾",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        menu()
    }
}

// endregion

// region ── Masthead + stat strip ────────────────────────────────────────

@Composable
private fun ReportMasthead(
    period: ReportPeriod,
    totalEvents: Int,
    totalAttendance: Int,
    averageAttendance: Int,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val periodLabel = PeriodSpecs.firstOrNull { it.period == period }?.long ?: period.displayName
    val perEvent = if (totalEvents > 0) totalAttendance / totalEvents else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = totalEvents.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 96.sp,
                    lineHeight = 86.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-4).sp,
                ),
                color = ink,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(
                    text = "Records",
                    style = softHeadline(24).copy(fontStyle = FontStyle.Italic),
                    color = ink,
                )
                Text(
                    text = "in range".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "§ $periodLabel".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 6.dp)
                    .background(Signal),
            )
        }

        Spacer(Modifier.height(18.dp))
        Spacer(Modifier.height(20.dp))
        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                label = "Attendance",
                value = formatCompact(totalAttendance),
                modifier = Modifier.weight(1f),
            )
            VerticalHair()
            StatCell(
                label = "Avg / Event",
                value = averageAttendance.toString(),
                modifier = Modifier.weight(1f),
            )
            VerticalHair()
            StatCell(
                label = "Per-Event",
                value = perEvent.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 10.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = softHeadline(20),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun VerticalHair(heightDp: Int = 42) {
    Box(
        Modifier
            .width(FieldTokens.Hair)
            .height(heightDp.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

// endregion

// region ── Section rule ─────────────────────────────────────────────────

@Composable
private fun SectionRule(title: String, note: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = title,
                style = softHeadline(24).copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

// endregion

// region ── Area index row ───────────────────────────────────────────────

@Composable
private fun AreaIndexRow(
    index: Int,
    stat: AreaStatistics,
    onToggle: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val interaction = remember { MutableInteractionSource() }

    val pct = if (stat.capacity > 0) {
        (stat.averageCount.toFloat() / stat.capacity).coerceIn(0f, 1f)
    } else 0f
    val pctInt = (pct * 100).toInt()
    val pctTone = capacityToneFor(pct)

    val animatedPct by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(durationMillis = 560),
        label = "areaPct",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    expanded = !expanded
                    onToggle()
                },
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Index column
            Column(modifier = Modifier.width(40.dp)) {
                Text(
                    text = index.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelMedium,
                    color = muted,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(FieldTokens.HairStrong)
                        .background(ink),
                )
            }

            Spacer(Modifier.width(8.dp))

            // Name + capacity row
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.areaName,
                    style = MaterialTheme.typography.titleLarge,
                    color = ink,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("CAP ${stat.capacity}")
                        append("   ·   ")
                        append("${stat.eventsCount} EVT")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )

                if (stat.capacity > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SparkBar(
                            progress = animatedPct,
                            modifier = Modifier.weight(1f),
                            height = 3.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "$pctInt%".padStart(4, ' '),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = pctTone,
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Avg — big display digit kept as-is (>36sp numeric)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stat.averageCount.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 44.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = ink,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "AVG",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + expandVertically(tween(200)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(160)),
        ) {
            Column(modifier = Modifier.padding(start = 48.dp, top = 14.dp)) {
                Spacer(Modifier.height(20.dp))
                Spacer(Modifier.height(10.dp))
                DetailGrid(stat)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (expanded) "◂ COLLAPSE" else "EXPAND ▸",
                style = MoreAffordance,
                color = muted,
            )
            Text(
                text = "TOTAL ${formatCompact(stat.totalCount)}",
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }
    }
}

private val MoreAffordance = MonoTiny

@Composable
private fun DetailGrid(stat: AreaStatistics) {
    Column {
        DetailLine(
            "Peak count",
            stat.maxCount.toString(),
            hot = stat.capacity > 0 && stat.maxCount >= stat.capacity
        )
        Spacer(Modifier.height(20.dp))
        DetailLine("Low count", stat.minCount.toString())
        Spacer(Modifier.height(20.dp))
        DetailLine("Events logged", stat.eventsCount.toString())
        Spacer(Modifier.height(20.dp))
        DetailLine("Capacity", stat.capacity.toString())
        Spacer(Modifier.height(20.dp))
        DetailLine(
            label = "Headroom",
            value = (stat.capacity - stat.averageCount).let { if (it < 0) "—${-it}" else "$it" },
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String, hot: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (hot) Signal else MaterialTheme.colorScheme.onBackground,
            fontWeight = if (hot) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

// endregion

// region ── Index footer + empty ─────────────────────────────────────────

@Composable
private fun AreaIndexFooter(count: Int, totalEvents: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "END · ${count.toString().padStart(2, '0')} ZONES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${totalEvents.toString().padStart(2, '0')} EVENTS OBSERVED",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyReportPanel(period: ReportPeriod) {
    val periodLabel = PeriodSpecs.firstOrNull { it.period == period }?.long ?: period.displayName
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "No records\nin range.",
            style = MaterialTheme.typography.displaySmall.copy(
                fontStyle = FontStyle.Italic,
                fontSize = 40.sp,
                lineHeight = 42.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.height(20.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = "§ ${periodLabel.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Widen the range or change the venue · type filters to pull in more data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// endregion

// region ── Helpers ──────────────────────────────────────────────────────

private fun formatCompact(value: Int): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000f).removeSuffix(".0M").let {
        if (it.endsWith("M")) it else "${it}M"
    }

    value >= 10_000 -> "${value / 1_000}K"
    value >= 1_000 -> "%.1fK".format(value / 1_000f).removeSuffix(".0K").let {
        if (it.endsWith("K")) it else "${it}K"
    }

    else -> value.toString()
}

// endregion
