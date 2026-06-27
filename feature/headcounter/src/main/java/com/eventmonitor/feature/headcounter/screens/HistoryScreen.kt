package com.eventmonitor.feature.headcounter.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.koin.androidx.compose.koinViewModel
import com.eventmonitor.core.common.theme.Amber
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.BrandBlue
import com.eventmonitor.core.common.ui.BrandRed
import com.eventmonitor.core.common.ui.KeyTone
import com.eventmonitor.core.common.ui.SoftAlertDialog
import com.eventmonitor.core.common.ui.SoftButtonTone
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.SoftCard
import com.eventmonitor.core.common.ui.SoftIconButton
import com.eventmonitor.core.common.ui.Hairline
import com.eventmonitor.core.common.ui.SparkBar
import com.eventmonitor.core.common.ui.capacityToneFor
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.EventWithDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = koinViewModel(),
    onServiceClick: (branchId: String, serviceId: String) -> Unit,
    onStartNewCount: (venueId: String) -> Unit,
    onViewTrends: (venueId: String?) -> Unit = {},
    onNavigateBack: () -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showUnlockDialog by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SoftAppBar(
            title = "Ledger",
            subtitle = "Event History",
            onBack = {
                haptic.light()
                onNavigateBack()
            },
            trailing = {
                SoftIconButton(glyph = "TR", onClick = {
                    haptic.light()
                    onViewTrends(viewModel.venueId)
                })
            },
        )

        Box(modifier = Modifier.weight(1f)) {
            ArcadeBackground(modifier = Modifier.matchParentSize())
            when (val state = uiState) {
                is HistoryUiState.Loading -> LoadingPanel()
                is HistoryUiState.Empty -> EmptyLedgerPanel()
                is HistoryUiState.Success -> {
                    val grouped = remember(state.events) { groupByMonth(state.events) }
                    val stats = remember(state.events) { computeStats(state.events) }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        item(key = "summary") { LedgerSummary(stats) }
                        grouped.forEach { (month, events) ->
                            stickyHeader(key = "h-$month") { MonthRule(month) }
                            items(events, key = { it.event.id }) { entry ->
                                LedgerRow(
                                    entry = entry,
                                    onResumeEdit = {
                                        haptic.medium()
                                        onServiceClick(entry.event.venueId, entry.event.id)
                                    },
                                    onUnlock = {
                                        haptic.light()
                                        showUnlockDialog = entry.event.id
                                    },
                                    onDelete = {
                                        haptic.light()
                                        showDeleteDialog = entry.event.id
                                    },
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            val railVenueId = viewModel.venueId
                ?: (uiState as? HistoryUiState.Success)?.events?.firstOrNull()?.event?.venueId
            railVenueId?.let { vId ->
                NewCountRail(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                    onClick = {
                        haptic.medium()
                        onStartNewCount(vId)
                    },
                )
            }
        }
    }

    showDeleteDialog?.let { serviceId ->
        SoftAlertDialog(
            onDismiss = { haptic.light(); showDeleteDialog = null },
            eyebrow = "Delete · Entry",
            title = "Delete this event?",
            message = "This removes the event and all of its counts. Cannot be undone.",
            confirmLabel = "Delete",
            confirmTone = SoftButtonTone.Destructive,
            onConfirm = {
                haptic.strong()
                viewModel.deleteEvent(serviceId)
                showDeleteDialog = null
            },
        )
    }

    showUnlockDialog?.let { serviceId ->
        SoftAlertDialog(
            onDismiss = { haptic.light(); showUnlockDialog = null },
            eyebrow = "Unlock · Entry",
            title = "Unlock this event?",
            message = "Unlocking lets you edit the counts for this event.",
            confirmLabel = "Unlock",
            onConfirm = {
                haptic.medium()
                viewModel.unlockEvent(serviceId)
                showUnlockDialog = null
            },
        )
    }

}

// region ── Ledger summary ────────────────────────────────────────────────

private data class LedgerStats(
    val total: Int,
    val avg: Int,
    val peak: Int,
    val cumulative: Int,
    val daysSinceLast: Long,
)

private fun computeStats(events: List<EventWithDetails>): LedgerStats {
    if (events.isEmpty()) return LedgerStats(0, 0, 0, 0, -1)
    val attendances = events.map { it.event.totalAttendance }
    val mostRecent = events.maxOfOrNull { it.event.date } ?: 0L
    val days = ((System.currentTimeMillis() - mostRecent) / (1000L * 60 * 60 * 24)).coerceAtLeast(0)
    return LedgerStats(
        total = events.size,
        avg = if (attendances.isNotEmpty()) attendances.average().toInt() else 0,
        peak = attendances.maxOrNull() ?: 0,
        cumulative = attendances.sum(),
        daysSinceLast = days,
    )
}

private fun groupByMonth(events: List<EventWithDetails>): LinkedHashMap<String, List<EventWithDetails>> {
    val fmt = SimpleDateFormat("MMMM · yyyy", Locale.getDefault())
    val result = LinkedHashMap<String, MutableList<EventWithDetails>>()
    events.forEach { e ->
        val key = fmt.format(Date(e.event.date)).uppercase()
        result.getOrPut(key) { mutableListOf() }.add(e)
    }
    @Suppress("UNCHECKED_CAST")
    return result as LinkedHashMap<String, List<EventWithDetails>>
}

@Composable
private fun LedgerSummary(stats: LedgerStats) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stats.total.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 92.sp,
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
                    style = softHeadline(24),
                    color = ink,
                )
                Text(
                    text = "on file".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell("Avg", stats.avg.toString(), modifier = Modifier.weight(1f))
            VerticalHair()
            StatCell("Peak", stats.peak.toString(), modifier = Modifier.weight(1f))
            VerticalHair()
            StatCell("Total", stats.cumulative.toString(), modifier = Modifier.weight(1f))
            VerticalHair()
            StatCell(
                label = "Latest",
                value = when {
                    stats.daysSinceLast < 0 -> "—"
                    stats.daysSinceLast == 0L -> "Today"
                    stats.daysSinceLast == 1L -> "1d"
                    else -> "${stats.daysSinceLast}d"
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 10.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun VerticalHair() {
    Box(
        Modifier
            .width(FieldTokens.Hair)
            .height(42.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

// endregion

// region ── Month sticky header ──────────────────────────────────────────

@Composable
private fun MonthRule(month: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = month,
                style = softHeadline(22),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "SECTION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

// endregion

// region ── Ledger row ───────────────────────────────────────────────────

@Composable
private fun LedgerRow(
    entry: EventWithDetails,
    onResumeEdit: () -> Unit,
    onUnlock: () -> Unit,
    onDelete: () -> Unit,
) {
    val dayFmt = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    val weekdayFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val rowInteraction = remember { MutableInteractionSource() }

    val date = Date(entry.event.date)
    val pct = if (entry.event.totalCapacity > 0) {
        (entry.event.totalAttendance.toFloat() / entry.event.totalCapacity).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = rowInteraction,
                indication = null,
                onClick = onResumeEdit,
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier.width(56.dp),
        ) {
            Text(
                text = dayFmt.format(date),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 44.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = ink,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = weekdayFmt.format(date).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = muted,
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val titleText =
                entry.event.eventName.ifEmpty { entry.event.eventType }.ifEmpty { "Untitled Event" }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                color = ink,
                maxLines = 2,
            )

            val subParts = buildList {
                add(entry.venue.name.uppercase())
                if (entry.event.eventName.isNotBlank() && entry.event.eventType.isNotBlank()) {
                    add(entry.event.eventType.uppercase())
                }
            }
            if (subParts.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subParts.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                    maxLines = 1,
                )
            }

            if (entry.event.totalCapacity > 0) {
                Spacer(Modifier.height(12.dp))
                SparkBar(progress = pct, height = 3.dp)
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeFmt.format(date),
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
                if (entry.event.countedBy.isNotBlank()) {
                    Text("  ·  ", style = MaterialTheme.typography.labelSmall, color = muted)
                    Text(
                        text = entry.event.countedBy.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = muted,
                        maxLines = 1,
                    )
                }
                Text("  ·  ", style = MaterialTheme.typography.labelSmall, color = muted)
                StatusBadge(locked = entry.event.isLocked)
            }

            Spacer(Modifier.height(12.dp))
            LedgerActionRow(
                locked = entry.event.isLocked,
                onUnlock = onUnlock,
                onDelete = onDelete,
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.widthIn(min = 62.dp),
        ) {
            Text(
                text = entry.event.totalAttendance.toString(),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 38.sp,
                    lineHeight = 38.sp,
                ),
                color = ink,
            )
            if (entry.event.totalCapacity > 0) {
                Text(
                    text = "/${entry.event.totalCapacity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${(pct * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = capacityToneFor(pct),
                )
            } else {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "TALLY",
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(locked: Boolean) {
    val color = if (locked) Sage else Amber
    val label = if (locked) "LOCKED" else "DRAFT"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier
            .size(6.dp)
            .background(color))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun LedgerActionRow(
    locked: Boolean,
    onUnlock: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (locked) {
            LedgerActionChip(label = "Unlock", onClick = onUnlock)
        }
        Spacer(Modifier.weight(1f))
        LedgerActionChip(label = "Del", danger = true, onClick = onDelete)
    }
}

@Composable
private fun LedgerActionChip(
    label: String,
    solid: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val fg = when {
        danger -> Signal
        solid -> MaterialTheme.colorScheme.background
        else -> MaterialTheme.colorScheme.onBackground
    }
    SoftCard(
        onClick = onClick,
        selected = solid,
        cornerRadius = 8,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 10.dp, vertical = 6.dp,
        ),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(color = fg),
        )
    }
}

// endregion

// region ── Bottom rail, empty, loading ──────────────────────────────────

@Composable
private fun NewCountRail(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        SoftCard(
            onClick = onClick,
            selected = true,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.background,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "NEW COUNT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.background,
                )
            }
        }
    }
}

@Composable
private fun EmptyLedgerPanel() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "—",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 140.sp,
                lineHeight = 120.sp,
                fontWeight = FontWeight.Light,
            ),
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "No records",
            style = softHeadline(28),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "on file.",
            style = softHeadline(28),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Start a count to file the ledger's first entry.".uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingPanel() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.onBackground,
            strokeWidth = 2.dp,
        )
    }
}

// endregion

// region ── Report dialog (unchanged presentation, retained) ─────────────

@Composable
fun ServiceReportDialog(
    report: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.82f),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Summary",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        item {
                            ServiceReportText(
                                report = report,
                                modifier = Modifier.fillMaxWidth(),
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var inAreaBreakdown = false

        lines.forEachIndexed { index, line ->
            when {
                line == "HEAD COUNT REPORT" -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                index == 1 && line == line.uppercase() -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                index == 2 -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                line in listOf("AREA BREAKDOWN", "AREA", "TOTAL", "EVENT NOTES") -> {
                    inAreaBreakdown = line == "AREA BREAKDOWN" || line == "AREA"
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = line,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 18.sp,
                            letterSpacing = 3.sp,
                        ),
                        fontWeight = FontWeight.Bold,
                        color = onSurface,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                inAreaBreakdown && line.trim().toIntOrNull() != null -> {
                    Text(
                        text = line.trim(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 24.sp,
                            letterSpacing = 3.sp,
                        ),
                        fontWeight = FontWeight.Black,
                        color = BrandBlue,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                inAreaBreakdown && line.contains(Regex("\\s{2,}")) &&
                        line.split(Regex("\\s{2,}")).lastOrNull()?.toIntOrNull() != null -> {
                    val parts = line.split(Regex("\\s{2,}"))
                    if (parts.size == 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = parts[0],
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.Normal,
                                color = onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                text = parts[1],
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 24.sp,
                                    letterSpacing = 3.sp,
                                ),
                                fontWeight = FontWeight.Black,
                                color = BrandBlue,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
                line.startsWith("_") -> {
                    inAreaBreakdown = false
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = onSurfaceVariant.copy(alpha = 0.3f),
                    )
                }
                line.startsWith(".") -> {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        color = onSurfaceVariant.copy(alpha = 0.2f),
                    )
                }
                line.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                line.contains(":") && !line.startsWith(" ") -> {
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        val isMetadata = parts[0] in listOf("Weather", "Generated", "ID")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Text(
                                text = parts[0] + ":",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                modifier = if (isMetadata) Modifier else Modifier.width(130.dp),
                            )
                            if (!isMetadata) {
                                Text(
                                    text = parts[1].trim(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = onSurface,
                                    fontFamily = FontFamily.Monospace,
                                )
                            } else {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = parts[1].trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onSurfaceVariant.copy(alpha = 0.7f),
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = onSurface,
                        )
                    }
                }
                inAreaBreakdown && line.trim().isNotEmpty() -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

// endregion
