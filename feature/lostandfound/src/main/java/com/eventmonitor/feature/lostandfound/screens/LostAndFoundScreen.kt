package com.eventmonitor.feature.lostandfound.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.koin.androidx.compose.koinViewModel
import com.eventmonitor.core.common.theme.Amber
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Navy
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.SoftCard
import com.eventmonitor.core.common.ui.SoftIconButton
import com.eventmonitor.core.common.ui.ZoneChip
import com.eventmonitor.core.common.ui.softEnter
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.LostItemEntity
import com.eventmonitor.core.domain.models.ItemCategory
import com.eventmonitor.core.domain.models.ItemStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═════════════════════════════════════════════════════════════════════════════
// CASE LEDGER — editorial redesign for the Lost & Found main screen.
//   1. FieldAppBar   — venue eyebrow + "CASES" title, [←] / [⌕] / [+] glyphs
//   2. Headline      — "§ CASE LEDGER" + editorial lede + oversize case count
//   3. Status strip  — HOLDING / CLAIMED / DONATED / DISPOSED counts
//   4. Filter rail   — status chip row + category chip row + search inline
//   5. Section heads — "§ HOLDING · 08" with oversize count
//   6. Case rows     — fractional index, status dot, 180-day tick strip
//   7. Bottom rail   — [+ NEW CASE] [⌕ FIND]
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LostAndFoundScreen(
    locationId: String?,
    onNavigateToAddItem: (String) -> Unit,
    onNavigateToItemDetail: (String) -> Unit = {},
    onNavigateBack: () -> Unit,
    viewModel: LostAndFoundViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var claimTarget by remember { mutableStateOf<LostItemEntity?>(null) }
    val haptic = rememberHapticFeedback()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SoftAppBar(
                title = "CASES",
                subtitle = "Lost · Found",
                onBack = { haptic.light(); onNavigateBack() },
                trailing = {
                    SoftIconButton(
                        glyph = if (searchOpen) "×" else "⌕",
                        onClick = {
                            haptic.light()
                            if (searchOpen) viewModel.searchItems("")
                            searchOpen = !searchOpen
                        },
                    )
                },
            )
        },
        bottomBar = {
            if (locationId != null) {
                CaseActionRail(
                    onAdd = {
                        haptic.medium(); onNavigateToAddItem(locationId)
                    },
                    onFind = {
                        haptic.light(); searchOpen = !searchOpen
                        if (!searchOpen) viewModel.searchItems("")
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
                is LostAndFoundUiState.Loading -> LoadingPanel()
                is LostAndFoundUiState.Empty -> EmptyLedger(
                    hasFilter = selectedStatus != null || selectedCategory != null || searchQuery.isNotBlank(),
                    onAdd = { locationId?.let { onNavigateToAddItem(it) } },
                    onClearFilters = {
                        selectedCategory = null
                        viewModel.filterByStatus(null)
                        viewModel.searchItems("")
                        searchOpen = false
                    },
                )

                is LostAndFoundUiState.Success -> CaseLedger(
                    items = state.items,
                    selectedStatus = selectedStatus,
                    selectedCategory = selectedCategory,
                    searchQuery = searchQuery,
                    searchOpen = searchOpen,
                    onStatusFilter = { viewModel.filterByStatus(it) },
                    onCategoryFilter = { selectedCategory = it },
                    onSearch = viewModel::searchItems,
                    onItemClick = onNavigateToItemDetail,
                    onClaim = { claimTarget = it },
                    onDonate = { viewModel.updateItemStatus(it.id, ItemStatus.DONATED.name) },
                )

                is LostAndFoundUiState.Error -> ErrorPanel(state.message)
            }
        }
    }

    claimTarget?.let { item ->
        ClaimCaseSheet(
            item = item,
            onDismiss = { claimTarget = null },
            onClaim = { name, contact, notes ->
                viewModel.claimItem(item.id, name, contact, notes)
                claimTarget = null
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Ledger body
// ---------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CaseLedger(
    items: List<LostItemEntity>,
    selectedStatus: String?,
    selectedCategory: ItemCategory?,
    searchQuery: String,
    searchOpen: Boolean,
    onStatusFilter: (String?) -> Unit,
    onCategoryFilter: (ItemCategory?) -> Unit,
    onSearch: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onClaim: (LostItemEntity) -> Unit,
    onDonate: (LostItemEntity) -> Unit,
) {
    val counts = remember(items) {
        ItemStatus.entries.associateWith { s -> items.count { it.status == s.name } }
    }
    val catCounts = remember(items) {
        items.groupingBy { ItemCategory.fromString(it.category) }.eachCount()
    }
    val visible = items
        .filter { selectedCategory == null || ItemCategory.fromString(it.category) == selectedCategory }
    val grouped = visible
        .sortedByDescending { it.foundDate }
        .groupBy { ItemStatus.fromString(it.status) }
    val statusOrder =
        listOf(ItemStatus.PENDING, ItemStatus.CLAIMED, ItemStatus.DONATED, ItemStatus.DISPOSED)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item(key = "headline") {
            Box(modifier = Modifier.softEnter(index = 0)) {
                LedgerHeadline(total = items.size)
            }
        }
        item(key = "strip") {
            Box(modifier = Modifier.softEnter(index = 1)) {
                StatusStrip(counts = counts)
            }
        }
        item(key = "status-filter") {
            Box(modifier = Modifier.softEnter(index = 2)) {
                StatusFilterRail(
                    selected = selectedStatus,
                    counts = counts,
                    onSelect = onStatusFilter,
                )
            }
        }
        if (catCounts.isNotEmpty()) {
            item(key = "cat-filter") {
                Box(modifier = Modifier.softEnter(index = 3)) {
                    CategoryFilterRail(
                        selected = selectedCategory,
                        counts = catCounts,
                        onSelect = onCategoryFilter,
                    )
                }
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
                group.forEachIndexed { idx, item ->
                    item(key = "row-${item.id}") {
                        Box(modifier = Modifier.animateItem()) {
                            CaseLedgerRow(
                                item = item,
                                rowIndex = idx + 1,
                                total = group.size,
                                onClick = { onItemClick(item.id) },
                                onClaim = { onClaim(item) },
                                onDonate = { onDonate(item) },
                            )
                        }
                    }
                }
            }
        }

        item(key = "colophon") { LedgerColophon(total = items.size, visible = visible.size) }
    }
}

// ---------------------------------------------------------------------------
// Headline + status strip
// ---------------------------------------------------------------------------

@Composable
private fun LedgerHeadline(total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 10.dp),
    ) {
        Text(
            "§ CASE LEDGER",
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
                "Found on the floor.",
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
            "Every case enters as HOLDING — 180 days of custody before it tips to DONATE. Tap a row to read its file.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusStrip(counts: Map<ItemStatus, Int>) {
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        StripCell("HOLD", (counts[ItemStatus.PENDING] ?: 0).toString(), Amber, Modifier.weight(1f))
        StripDivider()
        StripCell("CLAIM", (counts[ItemStatus.CLAIMED] ?: 0).toString(), Sage, Modifier.weight(1f))
        StripDivider()
        StripCell("DONATE", (counts[ItemStatus.DONATED] ?: 0).toString(), Navy, Modifier.weight(1f))
        StripDivider()
        StripCell(
            "PURGE",
            (counts[ItemStatus.DISPOSED] ?: 0).toString(),
            Signal,
            Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun StripCell(label: String, value: String, tone: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(6.dp)
                    .background(tone),
            )
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
    counts: Map<ItemStatus, Int>,
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
            ItemStatus.entries.forEach { status ->
                val count = counts[status] ?: 0
                ZoneChip(
                    label = "${shortTag(status)} · $count",
                    selected = selected == status.name,
                    onClick = { onSelect(if (selected == status.name) null else status.name) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryFilterRail(
    selected: ItemCategory?,
    counts: Map<ItemCategory, Int>,
    onSelect: (ItemCategory?) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 6.dp, bottom = 6.dp),
        ) {
            Text(
                "KIND /",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ZoneChip(
                label = "ANY",
                selected = selected == null,
                onClick = { onSelect(null) },
            )
            counts.entries
                .sortedByDescending { it.value }
                .forEach { (cat, n) ->
                    ZoneChip(
                        label = "${cat.shortTag()} · $n",
                        selected = selected == cat,
                        onClick = { onSelect(if (selected == cat) null else cat) },
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
            "⌕  SEARCH CASES",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(color = ink),
            cursorBrush = SolidColor(ink),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            decorationBox = { inner ->
                Column {
                    Box(Modifier.padding(vertical = 6.dp)) {
                        if (query.isEmpty()) {
                            Text(
                                "wallet, black backpack, keys…",
                                style = MaterialTheme.typography.headlineSmall,
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
// Section header + case row
// ---------------------------------------------------------------------------

@Composable
private fun StatusSectionHeader(status: ItemStatus, count: Int) {
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
                        "§ ${shortTag(status)} · ${count.toString().padStart(2, '0')}",
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
private fun CaseLedgerRow(
    item: LostItemEntity,
    rowIndex: Int,
    total: Int,
    onClick: () -> Unit,
    onClaim: () -> Unit,
    onDonate: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val status = ItemStatus.fromString(item.status)
    val category = ItemCategory.fromString(item.category)
    val tone = statusTone(status)

    val dateFmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
    val yearFmt = remember { SimpleDateFormat("yyyy", Locale.getDefault()) }

    val daysElapsed =
        ((System.currentTimeMillis() - item.foundDate) / 86_400_000L).toInt().coerceAtLeast(0)
    val daysRemaining = (180 - daysElapsed).coerceAtLeast(0)
    val canDonate = daysElapsed >= 180

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // Fractional index column
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

            // Description + meta
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description.ifBlank { "—" },
                    style = MaterialTheme.typography.titleLarge,
                    color = ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(item.foundZone.ifBlank { "—" }.uppercase())
                        append("   ·   ")
                        append(dateFmt.format(Date(item.foundDate)).uppercase())
                        append("   ·   ")
                        append(category.shortTag())
                    },
                    style = MonoTiny,
                    color = muted,
                )
                if (item.color.isNotBlank() || item.brand.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = listOf(item.color, item.brand).filter { it.isNotBlank() }
                            .joinToString(" · ").uppercase(),
                        style = MonoTiny,
                        color = muted,
                    )
                }
            }

            // Status dot + case id
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(92.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier
                        .size(8.dp)
                        .background(tone))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = shortTag(status),
                        style = MonoTiny,
                        color = ink,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "ID ${item.id.take(6).uppercase()}",
                    style = MonoTiny,
                    color = muted,
                )
                Text(
                    text = yearFmt.format(Date(item.foundDate)),
                    style = MonoTiny,
                    color = muted,
                )
            }
        }

        // Custody tick-strip — only for HOLDING cases
        if (status == ItemStatus.PENDING) {
            Spacer(Modifier.height(12.dp))
            CustodyTickStrip(
                daysElapsed = daysElapsed,
                ready = canDonate,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (canDonate) "CUSTODY ELAPSED · READY TO DONATE" else "D+${
                        daysElapsed.toString().padStart(3, '0')
                    } · $daysRemaining DAY${if (daysRemaining == 1) "" else "S"} LEFT",
                    style = MonoTiny,
                    color = if (canDonate) Sage else muted,
                )
                Text(
                    text = "${daysElapsed.coerceAtMost(180).toString().padStart(3, '0')} / 180",
                    style = MonoTiny,
                    color = muted,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CaseActionChip(label = "CLAIM", primary = true, onClick = onClaim)
                CaseActionChip(
                    label = "DONATE",
                    primary = false,
                    enabled = canDonate,
                    onClick = onDonate,
                )
                CaseActionChip(label = "OPEN FILE", primary = false, onClick = onClick)
            }
        } else if (status == ItemStatus.CLAIMED && item.claimedBy.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "↳ CLAIMED BY ${item.claimedBy.uppercase()}",
                style = MonoTiny,
                color = Sage,
            )
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun CustodyTickStrip(daysElapsed: Int, ready: Boolean) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.outline
    val ticks = 30 // 180 days / 6 per tick
    val filled = ((daysElapsed / 6f).coerceIn(0f, 30f)).toInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .border(FieldTokens.Hair, ink)
            .padding(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(ticks) { i ->
            val isFilled = i < filled
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(
                        when {
                            ready && isFilled -> Sage
                            isFilled -> ink
                            else -> Color.Transparent
                        },
                    ),
            )
            if (i < ticks - 1) {
                Box(
                    Modifier
                        .width(FieldTokens.Hair)
                        .height(6.dp)
                        .background(muted),
                )
            }
        }
    }
}

@Composable
private fun CaseActionChip(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val fg =
        if (primary) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
    SoftCard(
        onClick = if (enabled) onClick else null,
        selected = primary,
        cornerRadius = 8,
        modifier = Modifier.alpha(if (enabled) 1f else 0.35f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 12.dp, vertical = 8.dp,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
        )
    }
}

// ---------------------------------------------------------------------------
// Bottom rail
// ---------------------------------------------------------------------------

@Composable
private fun CaseActionRail(onAdd: () -> Unit, onFind: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RailButton(
                label = "+  NEW CASE",
                inverted = true,
                onClick = onAdd,
                modifier = Modifier.weight(1f),
            )
            RailButton(
                label = "⌕  FIND",
                inverted = false,
                onClick = onFind,
                modifier = Modifier.width(120.dp),
            )
        }
    }
}

@Composable
private fun RailButton(
    label: String,
    inverted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fg =
        if (inverted) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
    SoftCard(
        onClick = onClick,
        selected = inverted,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ---------------------------------------------------------------------------
// Empty / loading / error / colophon
// ---------------------------------------------------------------------------

@Composable
private fun EmptyLedger(
    hasFilter: Boolean,
    onAdd: () -> Unit,
    onClearFilters: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            "§ CASE LEDGER",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasFilter) "No matches." else "No cases filed.",
            style = MaterialTheme.typography.displaySmall,
            color = ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (hasFilter) "Loosen the filters or widen the search window."
            else "A case opens the moment an item is found. Tap + below to file the first.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))
        Spacer(Modifier.height(22.dp))

        if (hasFilter) {
            SoftCard(
                onClick = onClearFilters,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 18.dp),
            ) {
                Text(
                    "CLEAR FILTERS",
                    style = MaterialTheme.typography.labelMedium,
                    color = ink,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            SoftCard(
                onClick = onAdd,
                selected = true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "START",
                            style = MonoTiny,
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
                        )
                        Text(
                            "File first case",
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
                "READING CASE FILES…",
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
            style = softHeadline(20),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LedgerColophon(total: Int, visible: Int) {
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
                "$visible / $total CASES SHOWN",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            "― FIELD · LOST & FOUND ―",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Claim sheet — ink-bordered bottom sheet in FIELD grammar
// ---------------------------------------------------------------------------

@Composable
private fun ClaimCaseSheet(
    item: LostItemEntity,
    onDismiss: () -> Unit,
    onClaim: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SoftCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "§ CLAIM · CASE",
                        style = MonoTiny,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Release custody",
                        style = softHeadline(24),
                        color = ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "ID ${item.id.take(6).uppercase()} · ${item.description.uppercase()}",
                        style = MonoTiny,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SoftCard(
                    onClick = onDismiss,
                    cornerRadius = 8,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                ) {
                    Text(
                        "×",
                        style = softHeadline(20),
                        color = ink,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            SheetStep(index = "01", label = "CLAIMER", hint = "Legal name on the record.") {
                SheetField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Full name",
                    capitalization = KeyboardCapitalization.Words,
                )
            }
            Spacer(Modifier.height(10.dp))
            SheetStep(index = "02", label = "CONTACT", hint = "Phone or email to verify later.") {
                SheetField(
                    value = contact,
                    onValueChange = { contact = it },
                    placeholder = "+1 555 · name@email",
                )
            }
            Spacer(Modifier.height(10.dp))
            SheetStep(index = "03", label = "VERIFY", hint = "What proof was shown?") {
                SheetField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = "Photo ID · described scratch · receipt",
                    capitalization = KeyboardCapitalization.Sentences,
                    singleLine = false,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RailButton(
                    label = "CANCEL",
                    inverted = false,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                SoftCard(
                    onClick = if (name.isNotBlank()) {
                        { onClaim(name, contact, notes) }
                    } else null,
                    selected = true,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (name.isNotBlank()) 1f else 0.35f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
                ) {
                    Text(
                        "RELEASE CASE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetStep(
    index: String,
    label: String,
    hint: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = index,
                style = softHeadline(24),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "/ $label",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    singleLine: Boolean = true,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.titleLarge.copy(color = ink),
        cursorBrush = SolidColor(ink),
        keyboardOptions = KeyboardOptions(capitalization = capitalization),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (singleLine) 40.dp else 72.dp),
        decorationBox = { inner ->
            Column {
                Box(Modifier.padding(vertical = 4.dp)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.titleLarge,
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

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun shortTag(status: ItemStatus): String = when (status) {
    ItemStatus.PENDING -> "HOLD"
    ItemStatus.CLAIMED -> "CLAIM"
    ItemStatus.DONATED -> "GIVE"
    ItemStatus.DISPOSED -> "PURGE"
}

private fun sectionTitle(status: ItemStatus): String = when (status) {
    ItemStatus.PENDING -> "In custody"
    ItemStatus.CLAIMED -> "Released"
    ItemStatus.DONATED -> "Given away"
    ItemStatus.DISPOSED -> "Discarded"
}

private fun statusTone(status: ItemStatus): Color = when (status) {
    ItemStatus.PENDING -> Amber
    ItemStatus.CLAIMED -> Sage
    ItemStatus.DONATED -> Navy
    ItemStatus.DISPOSED -> Signal
}

internal fun ItemCategory.shortTag(): String = when (this) {
    ItemCategory.ELECTRONICS -> "TECH"
    ItemCategory.CLOTHING -> "WEAR"
    ItemCategory.DOCUMENTS -> "DOCS"
    ItemCategory.ACCESSORIES -> "ACC"
    ItemCategory.BAGS -> "BAGS"
    ItemCategory.PERSONAL_ITEMS -> "PERS"
    ItemCategory.KEYS -> "KEYS"
    ItemCategory.WALLETS -> "WLLT"
    ItemCategory.JEWELRY -> "JWLR"
    ItemCategory.TOYS -> "TOYS"
    ItemCategory.BOOKS -> "BOOK"
    ItemCategory.SPORTS_EQUIPMENT -> "SPRT"
    ItemCategory.OTHER -> "OTHR"
}
