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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.FieldAppBar
import com.eventmonitor.core.common.ui.FieldAppBarIcon
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.Hairline
import com.eventmonitor.core.common.ui.HairlineSoft
import com.eventmonitor.core.common.ui.ZoneChip
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.AreaTemplateEntity
import com.eventmonitor.core.domain.models.AreaType

// ═════════════════════════════════════════════════════════════════════════════
// ZONE LEDGER — editorial/industrial redesign for the "Manage Areas" screen.
// Layout beats:
//   1. FieldAppBar    — venue eyebrow + ZONES title + inline [+] add glyph
//   2. Section head   — serif count · monotype headline ("§ ZONE LEDGER")
//   3. Ledger strip   — ZONES / CAPACITY / TYPES metrics
//   4. Type filter    — horizontally scrolling ZoneChips (all, seating, vip…)
//   5. Grouped rows   — each type becomes a ledger page with oversize count
//   6. Sticky rail    — [+ ZONE]   [QUICK +N]    (replaces FAB)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AreaManagementScreen(
    viewModel: AreaManagementViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = rememberHapticFeedback()
    var selectedFilter by remember { mutableStateOf<AreaType?>(null) }
    var rowMenuFor by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            FieldAppBar(
                title = "ZONES",
                eyebrow = uiState.branchName.ifBlank { "VENUE" },
                leading = {
                    FieldAppBarIcon(glyph = "←", onClick = { haptic.light(); onNavigateBack() })
                },
                trailing = {
                    FieldAppBarIcon(
                        glyph = "+",
                        onClick = { haptic.medium(); viewModel.toggleAddDialog(true) },
                    )
                },
            )
        },
        bottomBar = {
            ActionRail(
                onAdd = { haptic.medium(); viewModel.toggleAddDialog(true) },
                onQuick = { haptic.medium(); viewModel.toggleQuickAddDialog(true) },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.isLoading -> LoadingPanel()
                uiState.areas.isEmpty() -> EmptyLedger(onAdd = {
                    haptic.medium(); viewModel.toggleAddDialog(
                    true
                )
                })

                else -> LedgerContent(
                    areas = uiState.areas,
                    selectedFilter = selectedFilter,
                    onFilter = { selectedFilter = it },
                    rowMenuFor = rowMenuFor,
                    onOpenRowMenu = { rowMenuFor = it },
                    onDismissRowMenu = { rowMenuFor = null },
                    onEdit = { haptic.light(); viewModel.setEditingArea(it) },
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

    if (uiState.showAddDialog) {
        AddAreaDialog(
            onDismiss = { viewModel.toggleAddDialog(false) },
            onAdd = { name, type, capacity -> viewModel.addArea(name, type, capacity) },
        )
    }
    if (uiState.showQuickAddDialog) {
        QuickAddAreasDialog(
            onDismiss = { viewModel.toggleQuickAddDialog(false) },
            onAdd = { type, count, start -> viewModel.createQuickAreas(type, count, start) },
        )
    }
    uiState.editingArea?.let { area ->
        EditAreaDialog(
            area = area,
            onDismiss = { viewModel.setEditingArea(null) },
            onUpdate = { viewModel.updateArea(it) },
        )
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
                style = MaterialTheme.typography.headlineLarge,
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
    Hairline()
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
    Hairline()
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
    HairlineSoft()
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 22.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SECTION ${
                        sectionIndex.toString().padStart(2, '0')
                    } · ${type.displayName.uppercase()}",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = count.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (capacity > 0) "CAP ${capacity.groupedThousands()}" else "CAP —",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Hairline()
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
                        Text("DELETE", style = MaterialTheme.typography.labelMedium, color = Signal)
                    },
                    onClick = onDelete,
                )
            }
        }
    }
    HairlineSoft()
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
        Hairline()
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
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .height(FieldTokens.ToolHeight)
            .border(FieldTokens.Hair, ink)
            .background(if (inverted) ink else paper)
            .clickable { onClick() },
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
        Hairline()
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
                        style = MaterialTheme.typography.headlineSmall,
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
        Hairline(color = MaterialTheme.colorScheme.outline)
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
// Dialogs — FIELD styling (sharp corners come from theme; we set paper bg)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAreaDialog(
    onDismiss: () -> Unit,
    onAdd: (String, AreaType, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<AreaType>(AreaType.SEATING) }
    var capacity by remember { mutableStateOf("100") }
    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Column {
                Text(
                    "NEW · ZONE",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text("Add zone", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ZONE NAME", style = MaterialTheme.typography.labelMedium) },
                    placeholder = { Text("Bay 1, Baby Room, …") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = fieldFieldColors(),
                )
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("TYPE", style = MaterialTheme.typography.labelMedium) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = fieldFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                        containerColor = MaterialTheme.colorScheme.background,
                    ) {
                        AreaType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        type.displayName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = { selectedType = type; typeExpanded = false },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it.filter { c -> c.isDigit() } },
                    label = { Text("CAPACITY", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldFieldColors(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val c = capacity.toIntOrNull() ?: 100
                    onAdd(name, selectedType, c)
                },
                enabled = name.isNotBlank(),
            ) {
                Text("ADD", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelMedium)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddAreasDialog(
    onDismiss: () -> Unit,
    onAdd: (AreaType, Int, Int) -> Unit,
) {
    var selectedType by remember { mutableStateOf<AreaType>(AreaType.SEATING) }
    var count by remember { mutableStateOf("6") }
    var startNumber by remember { mutableStateOf("1") }
    var typeExpanded by remember { mutableStateOf(false) }

    val previewText = remember(selectedType, count, startNumber) {
        val c = count.toIntOrNull() ?: 0
        val s = startNumber.toIntOrNull() ?: 1
        if (c <= 0) "" else buildString {
            append("Creates: ")
            append((0 until minOf(3, c)).joinToString(", ") { i ->
                quickNameFor(
                    selectedType,
                    s + i
                )
            })
            if (c > 3) append(", …")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Column {
                Text(
                    "QUICK · +N",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text("Batch zones", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Auto-number a run of same-typed zones in one step.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("TYPE", style = MaterialTheme.typography.labelMedium) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = fieldFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                        containerColor = MaterialTheme.colorScheme.background,
                    ) {
                        AreaType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        type.displayName,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = { selectedType = type; typeExpanded = false },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = count,
                        onValueChange = { count = it.filter { c -> c.isDigit() } },
                        label = { Text("COUNT", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldFieldColors(),
                    )
                    OutlinedTextField(
                        value = startNumber,
                        onValueChange = { startNumber = it.filter { c -> c.isDigit() } },
                        label = { Text("START #", style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldFieldColors(),
                    )
                }
                if (previewText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(FieldTokens.Hair, MaterialTheme.colorScheme.onBackground)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Start,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val c = count.toIntOrNull() ?: 0
                    val s = startNumber.toIntOrNull() ?: 1
                    if (c > 0) onAdd(selectedType, c, s)
                },
                enabled = (count.toIntOrNull() ?: 0) > 0,
            ) {
                Text("CREATE", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelMedium)
            }
        },
    )
}

@Composable
fun EditAreaDialog(
    area: AreaTemplateEntity,
    onDismiss: () -> Unit,
    onUpdate: (AreaTemplateEntity) -> Unit,
) {
    var name by remember { mutableStateOf(area.name) }
    var capacity by remember { mutableStateOf(area.capacity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Column {
                Text(
                    "EDIT · ${AreaType.fromString(area.type).shortTag()}",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text("Edit zone", style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ZONE NAME", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = fieldFieldColors(),
                )
                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it.filter { c -> c.isDigit() } },
                    label = { Text("CAPACITY", style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldFieldColors(),
                )
                Text(
                    text = "TYPE · ${AreaType.fromString(area.type).displayName.uppercase()}",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val c = capacity.toIntOrNull() ?: area.capacity
                    onUpdate(area.copy(name = name, capacity = c))
                },
                enabled = name.isNotBlank(),
            ) {
                Text("UPDATE", style = MaterialTheme.typography.labelMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelMedium)
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun fieldFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = MaterialTheme.colorScheme.onBackground,
    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.onBackground,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.onBackground,
)

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

private fun quickNameFor(type: AreaType, number: Int): String = when (type) {
    AreaType.SEATING -> "Seating $number"
    AreaType.STANDING -> "Standing $number"
    AreaType.VIP -> "VIP $number"
    AreaType.GENERAL_ADMISSION -> "General Admission $number"
    AreaType.OVERFLOW -> "Overflow $number"
    AreaType.PARKING -> "Parking $number"
    AreaType.REGISTRATION -> "Registration $number"
    AreaType.LOBBY -> "Lobby $number"
    AreaType.OUTDOOR -> "Outdoor $number"
    AreaType.STAGE -> "Stage $number"
    AreaType.BACKSTAGE -> "Backstage $number"
    AreaType.CARE_ROOM -> "Care Room $number"
    AreaType.FOOD_AREA -> "Food Area $number"
    AreaType.RESTROOMS -> "Restrooms $number"
    AreaType.EMERGENCY_EXIT -> "Emergency Exit $number"
    AreaType.OTHER -> "Area $number"
}

private fun Int.groupedThousands(): String =
    toString().reversed().chunked(3).joinToString(",").reversed()