package com.eventmonitor.feature.incidents.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.common.theme.Amber
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Navy
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.KeyTone
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.SoftBottomDock
import com.eventmonitor.core.common.ui.SoftCard
import com.eventmonitor.core.common.ui.SoftIconButton
import com.eventmonitor.core.common.ui.SoftKey
import com.eventmonitor.core.common.ui.SoftPrimaryButton
import com.eventmonitor.core.common.ui.SoftSection
import com.eventmonitor.core.common.ui.SoftToolButton
import com.eventmonitor.core.common.ui.Hairline
import com.eventmonitor.core.common.ui.HairlineSoft
import com.eventmonitor.core.common.ui.ZoneChip
import com.eventmonitor.core.common.ui.softEnter
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.IncidentEntity
import com.eventmonitor.core.domain.models.IncidentSeverity
import com.eventmonitor.core.domain.models.IncidentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// INCIDENT DESK — editorial redesign for the Incident Reports screen.
//   1. FieldAppBar     — [←] eyebrow "INCIDENTS" title "DESK" trailing [⌕]
//   2. Headline        — "§ INCIDENT DESK" · "Record what went wrong." · count
//   3. Severity strip  — CRIT / HIGH / MED / LOW colour counts
//   4. Filter rails    — STATUS rail, then SEVERITY rail, then optional inline search
//   5. Section heads   — grouped by status ("§ OPEN · 04" + oversize count)
//   6. Incident rows   — fractional index + severity stripe + title/meta + sev pill
//   7. Bottom rail     — [+ NEW INCIDENT] [⌕ FIND]
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IncidentListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddIncident: (String) -> Unit,
    onNavigateToIncidentDetail: (String) -> Unit,
    onNavigateToEditIncident: (String, String) -> Unit = { _, _ -> },
    venueId: String? = null,
    viewModel: IncidentListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val selectedSeverity by viewModel.selectedSeverity.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var searchOpen by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SoftAppBar(
                title = "DESK",
                subtitle = "§ Incidents",
                onBack = { haptic.light(); onNavigateBack() },
                trailing = {
                    SoftIconButton(
                        glyph = if (searchOpen) "×" else "⌕",
                        onClick = {
                            haptic.light()
                            if (searchOpen) viewModel.searchIncidents("")
                            searchOpen = !searchOpen
                        },
                    )
                },
            )
        },
        bottomBar = {
            if (venueId != null) {
                DeskActionRail(
                    onAdd = {
                        haptic.medium(); onNavigateToAddIncident(venueId)
                    },
                    onFind = {
                        haptic.light(); searchOpen = !searchOpen
                        if (!searchOpen) viewModel.searchIncidents("")
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ArcadeBackground(modifier = Modifier.matchParentSize())
            when (val state = uiState) {
                is IncidentListUiState.Loading -> LoadingPanel()
                is IncidentListUiState.Empty -> EmptyDesk(
                    hasFilter = selectedStatus != null || selectedSeverity != null || searchQuery.isNotBlank(),
                    onAdd = { venueId?.let { onNavigateToAddIncident(it) } },
                    onClearFilters = {
                        viewModel.filterByStatus(null)
                        viewModel.filterBySeverity(null)
                        viewModel.searchIncidents("")
                        searchOpen = false
                    },
                )

                is IncidentListUiState.Success -> IncidentDesk(
                    incidents = state.incidents,
                    selectedStatus = selectedStatus,
                    selectedSeverity = selectedSeverity,
                    searchQuery = searchQuery,
                    searchOpen = searchOpen,
                    onStatusFilter = { viewModel.filterByStatus(it) },
                    onSeverityFilter = { viewModel.filterBySeverity(it) },
                    onSearch = viewModel::searchIncidents,
                    onRowClick = onNavigateToIncidentDetail,
                    onEdit = { id -> venueId?.let { onNavigateToEditIncident(it, id) } },
                )

                is IncidentListUiState.Error -> ErrorPanel(state.message)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Desk body
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IncidentDesk(
    incidents: List<IncidentEntity>,
    selectedStatus: String?,
    selectedSeverity: String?,
    searchQuery: String,
    searchOpen: Boolean,
    onStatusFilter: (String?) -> Unit,
    onSeverityFilter: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onRowClick: (String) -> Unit,
    onEdit: (String) -> Unit,
) {
    val statusCounts = remember(incidents) {
        IncidentStatus.entries.associateWith { s -> incidents.count { it.status == s.name } }
    }
    val sevCounts = remember(incidents) {
        IncidentSeverity.entries.associateWith { s -> incidents.count { it.severity == s.name } }
    }

    val grouped = incidents
        .sortedByDescending { it.reportedAt }
        .groupBy { IncidentStatus.fromString(it.status) }
    val statusOrder = listOf(
        IncidentStatus.REPORTED,
        IncidentStatus.INVESTIGATING,
        IncidentStatus.IN_PROGRESS,
        IncidentStatus.RESOLVED,
        IncidentStatus.CLOSED,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "headline") {
            Box(modifier = Modifier.softEnter(index = 0)) {
                DeskHeadline(total = incidents.size)
            }
        }
        item(key = "strip") {
            Box(modifier = Modifier.softEnter(index = 1)) {
                SeverityStrip(counts = sevCounts)
            }
        }
        item(key = "status-rail") {
            Box(modifier = Modifier.softEnter(index = 2)) {
                StatusFilterRail(
                    selected = selectedStatus,
                    counts = statusCounts,
                    onSelect = onStatusFilter,
                )
            }
        }
        item(key = "sev-rail") {
            Box(modifier = Modifier.softEnter(index = 3)) {
                SeverityFilterRail(
                    selected = selectedSeverity,
                    counts = sevCounts,
                    onSelect = onSeverityFilter,
                )
            }
        }
        if (searchOpen) {
            item(key = "search") {
                InlineSearch(query = searchQuery, onChange = onSearch)
            }
        }

        statusOrder.forEach { status ->
            val group = grouped[status].orEmpty()
            if (group.isNotEmpty()) {
                item(key = "sect-${status.name}") {
                    StatusSectionHeader(status = status, count = group.size)
                }
                group.forEachIndexed { idx, inc ->
                    item(key = "row-${inc.id}") {
                        Box(modifier = Modifier.animateItem()) {
                            IncidentRow(
                                incident = inc,
                                rowIndex = idx + 1,
                                total = group.size,
                                onClick = { onRowClick(inc.id) },
                                onEdit = { onEdit(inc.id) },
                            )
                        }
                    }
                }
            }
        }

        item(key = "colophon") {
            DeskColophon(total = incidents.size, visible = incidents.size)
        }
    }
}

// ---------------------------------------------------------------------------
// Headline + severity strip
// ---------------------------------------------------------------------------

@Composable
private fun DeskHeadline(total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 10.dp),
    ) {
        Text(
            "§ INCIDENT DESK",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                "Record what went wrong.",
                style = softHeadline(28),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = total.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Every report lands here first. Triage by status, filter by severity, tap a row to read the file.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SeverityStrip(counts: Map<IncidentSeverity, Int>) {
    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        StripCell(
            "CRIT",
            (counts[IncidentSeverity.CRITICAL] ?: 0).toString(),
            Signal,
            Modifier.weight(1f)
        )
        StripDivider()
        StripCell(
            "HIGH",
            (counts[IncidentSeverity.HIGH] ?: 0).toString(),
            Amber,
            Modifier.weight(1f)
        )
        StripDivider()
        StripCell(
            "MED",
            (counts[IncidentSeverity.MEDIUM] ?: 0).toString(),
            Navy,
            Modifier.weight(1f)
        )
        StripDivider()
        StripCell("LOW", (counts[IncidentSeverity.LOW] ?: 0).toString(), Sage, Modifier.weight(1f))
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun StripCell(label: String, value: String, tone: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(6.dp)
                .background(tone))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = softHeadline(24),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun StripDivider() {
    Box(
        Modifier
            .width(FieldTokens.Hair)
            .height(30.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

// ---------------------------------------------------------------------------
// Filter rails
// ---------------------------------------------------------------------------

@Composable
private fun StatusFilterRail(
    selected: String?,
    counts: Map<IncidentStatus, Int>,
    onSelect: (String?) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 6.dp),
        ) {
            Text(
                "STATUS /",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val total = counts.values.sum()
            ZoneChip(
                label = "ALL · $total",
                selected = selected == null,
                onClick = { onSelect(null) },
            )
            IncidentStatus.entries.forEach { status ->
                val n = counts[status] ?: 0
                ZoneChip(
                    label = "${statusTag(status)} · $n",
                    selected = selected == status.name,
                    onClick = { onSelect(if (selected == status.name) null else status.name) },
                )
            }
        }
    }
}

@Composable
private fun SeverityFilterRail(
    selected: String?,
    counts: Map<IncidentSeverity, Int>,
    onSelect: (String?) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 6.dp),
        ) {
            Text(
                "SEVERITY /",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ZoneChip(
                label = "ANY",
                selected = selected == null,
                onClick = { onSelect(null) },
            )
            IncidentSeverity.entries.reversed().forEach { sev ->
                val n = counts[sev] ?: 0
                ZoneChip(
                    label = "${severityTag(sev)} · $n",
                    selected = selected == sev.name,
                    onClick = { onSelect(if (selected == sev.name) null else sev.name) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun InlineSearch(query: String, onChange: (String) -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 14.dp),
    ) {
        Text(
            "⌕  SEARCH INCIDENTS",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = softHeadline(20).copy(color = ink),
            cursorBrush = SolidColor(ink),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            decorationBox = { inner ->
                Column {
                    Box(Modifier.padding(vertical = 6.dp)) {
                        if (query.isEmpty()) {
                            Text(
                                "slip, fire alarm, medical…",
                                style = softHeadline(20),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(FieldTokens.Hair)
                            .background(ink),
                    )
                }
            },
        )
    }
    Spacer(Modifier.height(10.dp))
}

// ---------------------------------------------------------------------------
// Section header + incident row
// ---------------------------------------------------------------------------

@Composable
private fun StatusSectionHeader(status: IncidentStatus, count: Int) {
    val tone = statusTone(status)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier
                        .size(8.dp)
                        .background(tone))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "§ ${statusTag(status)} · ${count.toString().padStart(2, '0')}",
                        style = MonoTiny,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    sectionTitle(status),
                    style = softHeadline(20),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = count.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun IncidentRow(
    incident: IncidentEntity,
    rowIndex: Int,
    total: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val severity = IncidentSeverity.fromString(incident.severity)
    val status = IncidentStatus.fromString(incident.status)
    val sevTone = severityTone(severity)
    val statusT = statusTone(status)

    val dateFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val canEdit = status != IncidentStatus.RESOLVED && status != IncidentStatus.CLOSED

    SoftCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // Severity stripe — retained for severity signal
        Box(
            modifier = Modifier
                .width(FieldTokens.Lane)
                .fillMaxWidth()
                .background(sevTone),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 17.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.width(54.dp)) {
                    Text(
                        text = rowIndex.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = MonoTiny.fontFamily,
                        ),
                        color = ink,
                    )
                    Text(
                        text = "/ ${total.toString().padStart(2, '0')}",
                        style = MonoTiny,
                        color = muted,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = incident.title.ifBlank { "—" },
                        style = MaterialTheme.typography.titleLarge,
                        color = ink,
                    )
                    if (incident.description.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = incident.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = muted,
                            maxLines = 2,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = buildString {
                            append(incident.location.ifBlank { "—" }.uppercase())
                            append("   ·   ")
                            append(dateFmt.format(Date(incident.reportedAt)).uppercase())
                            append(" ")
                            append(timeFmt.format(Date(incident.reportedAt)))
                            if (incident.category.isNotBlank()) {
                                append("   ·   ")
                                append(incident.category.uppercase())
                            }
                        },
                        style = MonoTiny,
                        color = muted,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "FILED · ${if (incident.reportedBy.isBlank()) "ANON" else incident.reportedBy.uppercase()}",
                        style = MonoTiny,
                        color = muted,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(92.dp),
                ) {
                    SeverityPill(severity)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier
                            .size(8.dp)
                            .background(statusT))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = statusTag(status),
                            style = MonoTiny,
                            color = ink,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "ID ${incident.id.take(6).uppercase()}",
                        style = MonoTiny,
                        color = muted,
                    )
                }
            }

            // Action chips
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (canEdit) {
                    RowActionChip(label = "EDIT", primary = true, onClick = onEdit)
                }
                RowActionChip(label = "OPEN FILE", primary = false, onClick = onClick)
                if (status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED) {
                    incident.resolvedAt?.let { resolved ->
                        Spacer(Modifier.width(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp),
                        ) {
                            Text("↳ ", style = MonoTiny, color = Sage)
                            Text(
                                "RESOLVED ${dateFmt.format(Date(resolved)).uppercase()}",
                                style = MonoTiny,
                                color = Sage,
                            )
                        }
                    }
                }
            }
        }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun RowActionChip(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    SoftCard(
        modifier = Modifier.alpha(if (enabled) 1f else 0.35f),
        selected = primary,
        onClick = if (enabled) onClick else null,
        cornerRadius = 8,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (primary) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ---------------------------------------------------------------------------
// Bottom rail · empty · loading · error · colophon
// ---------------------------------------------------------------------------

@Composable
private fun DeskActionRail(onAdd: () -> Unit, onFind: () -> Unit) {
    SoftBottomDock(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SoftToolButton(
                label = "Find",
                glyph = "⌕",
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = onFind,
            )
            SoftPrimaryButton(
                label = "New Incident",
                onClick = onAdd,
                modifier = Modifier.weight(2f),
                trailingGlyph = "+",
            )
        }
    }
}

@Composable
private fun EmptyDesk(
    hasFilter: Boolean,
    onAdd: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            "§ INCIDENT DESK",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasFilter) "No matches." else "Desk is clear.",
            style = MaterialTheme.typography.displaySmall,
            color = ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasFilter) "Loosen the filters or widen the search window."
            else "Nothing has been reported. Tap + below to file the first incident.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))

        if (hasFilter) {
            SoftCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClearFilters,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 18.dp),
            ) {
                Text(
                    "CLEAR FILTERS",
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        } else {
            SoftCard(
                modifier = Modifier.fillMaxWidth(),
                selected = true,
                onClick = onAdd,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "START",
                            style = MonoTiny,
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                        )
                        Text(
                            "File first incident",
                            style = softHeadline(20),
                            color = MaterialTheme.colorScheme.background,
                        )
                    }
                    Text(
                        "+",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.background,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingPanel() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onBackground,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "OPENING INCIDENT DESK…",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorPanel(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("§ ERROR", style = MonoTiny, color = Signal)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeskColophon(total: Int, visible: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("DESK CLOSED", style = MonoTiny, color = Sage)
            Text(
                "$visible / $total REPORTS SHOWN",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "― FIELD · INCIDENTS ―",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Shared severity / status helpers
// ---------------------------------------------------------------------------

@Composable
internal fun SeverityPill(severity: IncidentSeverity) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val tone = severityTone(severity)
    val filled = severity == IncidentSeverity.CRITICAL || severity == IncidentSeverity.HIGH
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .background(if (filled) tone else ink.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = severityTag(severity),
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) paper else ink,
        )
    }
}

internal fun severityTag(sev: IncidentSeverity): String = when (sev) {
    IncidentSeverity.CRITICAL -> "CRIT"
    IncidentSeverity.HIGH -> "HIGH"
    IncidentSeverity.MEDIUM -> "MED"
    IncidentSeverity.LOW -> "LOW"
}

internal fun severityTone(sev: IncidentSeverity): Color = when (sev) {
    IncidentSeverity.CRITICAL -> Signal
    IncidentSeverity.HIGH -> Amber
    IncidentSeverity.MEDIUM -> Navy
    IncidentSeverity.LOW -> Sage
}

internal fun statusTag(status: IncidentStatus): String = when (status) {
    IncidentStatus.REPORTED -> "REP"
    IncidentStatus.INVESTIGATING -> "INV"
    IncidentStatus.IN_PROGRESS -> "PROG"
    IncidentStatus.RESOLVED -> "RES"
    IncidentStatus.CLOSED -> "CLS"
}

internal fun statusTone(status: IncidentStatus): Color = when (status) {
    IncidentStatus.REPORTED -> Signal
    IncidentStatus.INVESTIGATING -> Amber
    IncidentStatus.IN_PROGRESS -> Navy
    IncidentStatus.RESOLVED -> Sage
    IncidentStatus.CLOSED -> Color(0xFF78716C)
}

private fun sectionTitle(status: IncidentStatus): String = when (status) {
    IncidentStatus.REPORTED -> "Open reports"
    IncidentStatus.INVESTIGATING -> "Under investigation"
    IncidentStatus.IN_PROGRESS -> "In progress"
    IncidentStatus.RESOLVED -> "Resolved"
    IncidentStatus.CLOSED -> "Closed"
}
