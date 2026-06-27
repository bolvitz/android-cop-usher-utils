package com.eventmonitor.app.presentation.screens.areas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.SoftCard
import com.eventmonitor.core.common.ui.SoftIconButton
import com.eventmonitor.core.common.ui.SoftSection
import com.eventmonitor.core.common.ui.ZoneChip
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.AreaTemplateEntity
import com.eventmonitor.core.domain.models.AreaType

// ═════════════════════════════════════════════════════════════════════════════
// ZONE LEDGER — Manage Areas screen.
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AreaManagementScreen(
    viewModel: AreaManagementViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onCreateZone: () -> Unit = {},
    onBatchCreateZone: () -> Unit = {},
    onEditZone: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = rememberHapticFeedback()
    var selectedFilter by remember { mutableStateOf<AreaType?>(null) }
    var rowMenuFor by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SoftAppBar(
                title = "Zones",
                subtitle = uiState.branchName.ifBlank { "Venue" },
                onBack = { haptic.light(); onNavigateBack() },
                trailing = {
                    SoftIconButton(glyph = "+", onClick = { haptic.medium(); onCreateZone() })
                },
            )
        },
        bottomBar = {
            ActionRail(
                onAdd = { haptic.medium(); onCreateZone() },
                onQuick = { haptic.medium(); onBatchCreateZone() },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ArcadeBackground(modifier = Modifier.matchParentSize())

            when {
                uiState.isLoading -> LoadingPanel()
                uiState.areas.isEmpty() -> EmptyLedger(onAdd = {
                    haptic.medium(); onCreateZone()
                })

                else -> LedgerContent(
                    areas = uiState.areas,
                    selectedFilter = selectedFilter,
                    onFilter = { selectedFilter = it },
                    rowMenuFor = rowMenuFor,
                    onOpenRowMenu = { rowMenuFor = it },
                    onDismissRowMenu = { rowMenuFor = null },
                    onEdit = { haptic.light(); onEditZone(it.id) },
                    onDelete = { haptic.strong(); viewModel.deleteArea(it) },
                )
            }

            AnimatedVisibility(
                visible = uiState.error != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 72.dp),
            ) {
                uiState.error?.let { err ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                        action = {
                            TextButton(onClick = { haptic.light(); viewModel.clearError() }) {
                                Text(
                                    "OK",
                                    color = MaterialTheme.colorScheme.background,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        },
                    ) {
                        Text(err, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

}

// ---------------------------------------------------------------------------
// Ledger content
// ---------------------------------------------------------------------------

@Composable
private fun LedgerContent(
    areas: List<AreaTemplateEntity>,
    selectedFilter: AreaType?,
    onFilter: (AreaType?) -> Unit,
    rowMenuFor: String?,
    onOpenRowMenu: (String) -> Unit,
    onDismissRowMenu: () -> Unit,
    onEdit: (AreaTemplateEntity) -> Unit,
    onDelete: (String) -> Unit,
) {
    val totalCap = areas.sumOf { if (it.capacity > 0) it.capacity else 0 }
    val distribution = areas.groupBy { AreaType.fromString(it.type) }
    val typeOrder = distribution.keys.sortedBy { it.displayName }
    val visibleAreas = areas
        .filter { selectedFilter == null || AreaType.fromString(it.type) == selectedFilter }
        .sortedWith(compareBy({ AreaType.fromString(it.type).displayName }, { it.displayOrder }))
    val visibleGroups = visibleAreas.groupBy { AreaType.fromString(it.type) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "headline") { LedgerHeadline(zoneCount = areas.size) }
        item(key = "strip") {
            LedgerStrip(
                zones = areas.size,
                capacity = totalCap,
                types = distribution.size,
            )
        }
        item(key = "filter") {
            TypeFilterRail(
                selected = selectedFilter,
                types = typeOrder,
                distribution = distribution.mapValues { it.value.size },
                onSelect = onFilter,
            )
        }

        visibleGroups.entries.forEachIndexed { idx, (type, group) ->
            item(key = "sect-${type.name}") {
                SectionHeader(
                    type = type,
                    count = group.size,
                    capacity = group.sumOf { if (it.capacity > 0) it.capacity else 0 },
                    sectionIndex = idx + 1,
                )
            }
            group.forEachIndexed { rowIdx, area ->
                item(key = "row-${area.id}") {
                    ZoneLedgerRow(
                        area = area,
                        rowIndex = rowIdx + 1,
                        totalInSection = group.size,
                        menuOpen = rowMenuFor == area.id,
                        onOpenMenu = { onOpenRowMenu(area.id) },
                        onDismissMenu = onDismissRowMenu,
                        onEdit = { onDismissRowMenu(); onEdit(area) },
                        onDelete = { onDismissRowMenu(); onDelete(area.id) },
                    )
                }
            }
        }

        item(key = "colophon") { LedgerColophon(total = areas.size, capacity = totalCap) }
    }
}

// ---------------------------------------------------------------------------
// Headline + strip + filter rail
// ---------------------------------------------------------------------------

@Composable
private fun LedgerHeadline(zoneCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 10.dp),
    ) {
        Text(
            "§ ZONE LEDGER",
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
                text = "Plan the floor.",
                style = softHeadline(28),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = zoneCount.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Zones, capacity and type — tap a row to edit. Quick +N fills a series in one shot.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LedgerStrip(zones: Int, capacity: Int, types: Int) {
    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        StripCell(label = "ZONES", value = zones.toString(), modifier = Modifier.weight(1f))
        StripDivider()
        StripCell(
            label = "CAPACITY",
            value = if (capacity > 0) capacity.groupedThousands() else "—",
            modifier = Modifier.weight(1f),
        )
        StripDivider()
        StripCell(label = "TYPES", value = types.toString(), modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun StripCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MonoTiny, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun StripDivider() {
    Box(
        Modifier
            .width(FieldTokens.Hair)
            .height(34.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

@Composable
private fun TypeFilterRail(
    selected: AreaType?,
    types: List<AreaType>,
    distribution: Map<AreaType, Int>,
    onSelect: (AreaType?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ZoneChip(
            label = "ALL",
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        types.forEach { type ->
            val count = distribution[type] ?: 0
            ZoneChip(
                label = "${type.shortTag()} · $count",
                selected = selected == type,
                onClick = { onSelect(if (selected == type) null else type) },
            )
        }
    }
    Spacer(Modifier.height(20.dp))
}

// ---------------------------------------------------------------------------
// Section header + zone row
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(
    type: AreaType,
    count: Int,
    capacity: Int,
    sectionIndex: Int,
) {
    SoftSection(
        title = type.displayName,
        eyebrow = "SECTION ${
            sectionIndex.toString().padStart(2, '0')
        } · ${type.displayName.uppercase()}",
        hint = "${
            count.toString().padStart(2, '0')
        } zones · CAP ${if (capacity > 0) capacity.groupedThousands() else "—"}",
        modifier = Modifier.padding(top = 22.dp),
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ZoneLedgerRow(
    area: AreaTemplateEntity,
    rowIndex: Int,
    totalInSection: Int,
    menuOpen: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp),
    ) {
        SoftCard(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Index column — tabular fraction index
                Column(modifier = Modifier.width(52.dp)) {
                    Text(
                        text = rowIndex.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = MonoTiny.fontFamily,
                        ),
                        color = ink,
                    )
                    Text(
                        text = "/ ${totalInSection.toString().padStart(2, '0')}",
                        style = MonoTiny,
                        color = muted,
                    )
                }

                // Name + type caption
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = area.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = ink,
                    )
                    Text(
                        text = "${AreaType.fromString(area.type).shortTag()} · ID ${
                            area.id.take(6).uppercase()
                        }",
                        style = MonoTiny,
                        color = muted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                // Capacity column
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Text(
                        text = "CAP",
                        style = MonoTiny,
                        color = muted,
                    )
                    Text(
                        text = area.capacity.toString().padStart(3, ' '),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = MonoTiny.fontFamily,
                        ),
                        color = ink,
                    )
                }

                // Overflow menu
                Box {
                    Text(
                        text = "⋯",
                        style = MaterialTheme.typography.headlineSmall,
                        color = ink,
                        modifier = Modifier
                            .padding(start = 10.dp)
                            .clickable { onOpenMenu() },
                    )
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = onDismissMenu,
                        containerColor = MaterialTheme.colorScheme.background,
                    ) {
                        DropdownMenuItem(
                            text = { Text("EDIT", style = MaterialTheme.typography.labelMedium) },
                            onClick = onEdit,
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "DELETE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Signal
                                )
                            },
                            onClick = onDelete,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom action rail
// ---------------------------------------------------------------------------

@Composable
private fun ActionRail(onAdd: () -> Unit, onQuick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InkButton(
                label = "+ ZONE",
                inverted = true,
                onClick = onAdd,
                modifier = Modifier.weight(1f),
            )
            InkButton(
                label = "QUICK +N",
                inverted = false,
                onClick = onQuick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InkButton(
    label: String,
    inverted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .height(FieldTokens.ToolHeight)
            .alpha(if (enabled) 1f else 0.35f)
            .border(FieldTokens.Hair, ink)
            .background(if (inverted) ink else paper)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (inverted) paper else ink,
        )
    }
}

// ---------------------------------------------------------------------------
// Empty state / loading / colophon
// ---------------------------------------------------------------------------

@Composable
private fun EmptyLedger(onAdd: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            "§ ZONE LEDGER",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "No zones yet.",
            style = MaterialTheme.typography.displaySmall,
            color = ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "A zone is a countable slice of the venue — a bay, a section, a door. Add one to start planning the floor.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ink)
                .clickable { onAdd() }
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("START", style = MonoTiny, color = paper.copy(alpha = 0.55f))
                    Text(
                        "Add first zone",
                        style = softHeadline(20),
                        color = paper,
                    )
                }
                Text(
                    text = "+",
                    style = MaterialTheme.typography.displaySmall,
                    color = paper,
                )
            }
        }
    }
}

@Composable
private fun LoadingPanel() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onBackground,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "READING LEDGER…",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LedgerColophon(total: Int, capacity: Int) {
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
            Text("LEDGER CLOSED", style = MonoTiny, color = Sage)
            Text(
                "$total ZONES · CAP ${if (capacity > 0) capacity.groupedThousands() else "—"}",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "― FIELD · ZONES ―",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun AreaType.shortTag(): String = when (this) {
    AreaType.SEATING -> "SEATING"
    AreaType.STANDING -> "STANDING"
    AreaType.VIP -> "VIP"
    AreaType.GENERAL_ADMISSION -> "GA"
    AreaType.OVERFLOW -> "OVERFLOW"
    AreaType.PARKING -> "PARKING"
    AreaType.REGISTRATION -> "REG"
    AreaType.LOBBY -> "LOBBY"
    AreaType.OUTDOOR -> "OUTDOOR"
    AreaType.STAGE -> "STAGE"
    AreaType.BACKSTAGE -> "BACKSTAGE"
    AreaType.CARE_ROOM -> "CARE"
    AreaType.FOOD_AREA -> "F&B"
    AreaType.RESTROOMS -> "WC"
    AreaType.EMERGENCY_EXIT -> "EXIT"
    AreaType.OTHER -> "OTHER"
}

private fun Int.groupedThousands(): String =
    toString().reversed().chunked(3).joinToString(",").reversed()
