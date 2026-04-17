package com.eventmonitor.app.presentation.screens.venues

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.FieldMasthead
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.Hairline
import com.eventmonitor.core.common.ui.SevPill
import com.eventmonitor.core.common.ui.Severity
import com.eventmonitor.core.common.ui.SparkBar
import com.eventmonitor.core.common.ui.capacityToneFor
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.VenueWithAreas
import kotlin.math.roundToInt

@Composable
fun VenueListScreen(
    viewModel: VenueListViewModel = hiltViewModel(),
    onVenueClick: (String) -> Unit,
    onManageAreas: (String) -> Unit = {},
    onEditVenue: (String) -> Unit = {},
    onVenueHistory: (String) -> Unit = {},
    onVenueIncidents: (String) -> Unit = {},
    onVenueLostAndFound: (String) -> Unit = {},
    onNavigateToReports: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val state = uiState) {
                is VenueListUiState.Loading -> LoadingPanel()

                is VenueListUiState.Empty -> EmptyState()

                is VenueListUiState.Success -> HomeContent(
                    venues = state.venues,
                    onEnterVenue = { id ->
                        haptic.medium(); onVenueHistory(id)
                    },
                    onHeadcount = { id ->
                        haptic.medium(); onVenueHistory(id)
                    },
                    onLostFound = { id ->
                        haptic.light(); onVenueLostAndFound(id)
                    },
                    onIncidents = { id ->
                        haptic.light(); onVenueIncidents(id)
                    },
                    onManageAreas = { id ->
                        haptic.light(); onManageAreas(id)
                    },
                    onEdit = { id ->
                        haptic.light(); onEditVenue(id)
                    },
                    onDelete = { id ->
                        haptic.medium(); showDeleteDialog = id
                    },
                    onReports = { haptic.light(); onNavigateToReports() },
                    onSettings = { haptic.light(); onNavigateToSettings() },
                )

                is VenueListUiState.Error -> ErrorPanel(state.message)
            }

            AnimatedVisibility(
                visible = showDeleteDialog != null,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
            ) {
                showDeleteDialog?.let { branchId ->
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = null },
                        containerColor = MaterialTheme.colorScheme.background,
                        title = {
                            Column {
                                Text(
                                    "DELETE · VENUE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Are you sure?",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        },
                        text = { Text("This venue will be removed. You can't undo this.") },
                        confirmButton = {
                            TextButton(onClick = {
                                haptic.strong()
                                viewModel.deleteVenue(branchId) { errorMessage = it }
                                showDeleteDialog = null
                            }) {
                                Text(
                                    "DELETE",
                                    color = Signal,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { haptic.light(); showDeleteDialog = null }) {
                                Text("CANCEL", style = MaterialTheme.typography.labelMedium)
                            }
                        },
                    )
                }
            }

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                errorMessage?.let { error ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                        action = {
                            TextButton(onClick = {
                                haptic.light()
                                errorMessage = null
                            }) {
                                Text(
                                    "OK",
                                    color = MaterialTheme.colorScheme.background,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        },
                    ) {
                        Text(error, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Home content
// ---------------------------------------------------------------------------

@Composable
private fun HomeContent(
    venues: List<VenueWithAreas>,
    onEnterVenue: (String) -> Unit,
    onHeadcount: (String) -> Unit,
    onLostFound: (String) -> Unit,
    onIncidents: (String) -> Unit,
    onManageAreas: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onReports: () -> Unit,
    onSettings: () -> Unit,
) {
    val hero = venues.firstOrNull { it.venue.isActive }
    val activeCount = venues.count { it.venue.isActive }
    val pendingCount = venues.size - activeCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // 1. Masthead
        item(key = "masthead") {
            FieldMasthead()
        }

        // 2. Stat strip — total / live / pending
        item(key = "stats") {
            StatStrip(
                total = venues.size,
                live = activeCount,
                pending = pendingCount,
            )
        }

        // 3. Live hero slab
        if (hero != null) {
            item(key = "hero-${hero.venue.id}") {
                LiveHero(
                    venue = hero,
                    onEnter = { onEnterVenue(hero.venue.id) },
                )
            }
        }

        // 4. Roster header
        item(key = "roster-head") {
            RosterHeader(count = venues.size)
        }

        // 5. Venue rows
        items(items = venues, key = { it.venue.id }) { venue ->
            VenueRow(
                venue = venue,
                onTap = { onEnterVenue(venue.venue.id) },
                onHeadcount = { onHeadcount(venue.venue.id) },
                onLostFound = { onLostFound(venue.venue.id) },
                onIncidents = { onIncidents(venue.venue.id) },
                onManageAreas = { onManageAreas(venue.venue.id) },
                onEdit = { onEdit(venue.venue.id) },
                onDelete = { onDelete(venue.venue.id) },
            )
        }

        // 6. Tool rail (Reports · Settings) + colophon
        item(key = "tools") {
            Spacer(Modifier.height(20.dp))
            ToolRail(onReports = onReports, onSettings = onSettings)
        }
        item(key = "colophon") {
            Colophon(total = venues.size, live = activeCount)
        }
    }
}

// ---------------------------------------------------------------------------
// Stat strip
// ---------------------------------------------------------------------------

@Composable
private fun StatStrip(total: Int, live: Int, pending: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        StatCell(label = "VENUES", value = total.toString())
        StatDivider()
        StatCell(label = "LIVE", value = live.toString(), accent = live > 0)
        StatDivider()
        StatCell(label = "PENDING", value = pending.toString())
    }
    Hairline(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun StatCell(label: String, value: String, accent: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (accent) {
                Spacer(Modifier.width(6.dp))
                LivePulse()
            }
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .width(FieldTokens.Hair)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

// ---------------------------------------------------------------------------
// Live hero slab
// ---------------------------------------------------------------------------

@Composable
private fun LiveHero(
    venue: VenueWithAreas,
    onEnter: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val capacity = venue.areas.sumOf { if (it.capacity > 0) it.capacity else 0 }
    val occupied = 0 // Without event data we can't compute live occupancy yet.
    val progress = if (capacity > 0) occupied.toFloat() / capacity else 0f

    Column(modifier = Modifier
        .padding(horizontal = 20.dp)
        .padding(top = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LivePulse()
            Spacer(Modifier.width(8.dp))
            Text(
                text = "LIVE NOW · READY TO COUNT",
                style = MaterialTheme.typography.labelMedium,
                color = Signal,
            )
        }
        Spacer(Modifier.height(8.dp))

        // Ink slab, tap to enter.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ink)
                .clickable { onEnter() }
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = venue.venue.name,
                            style = MaterialTheme.typography.headlineLarge,
                            color = paper,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${venue.venue.code.ifBlank { "—" }} · ${venue.venue.location.ifBlank { "LOCATION UNSET" }}".uppercase(),
                            style = MonoTiny,
                            color = paper.copy(alpha = 0.6f),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ENTER", style = MonoTiny, color = paper.copy(alpha = 0.6f))
                        Text(
                            text = "→",
                            style = MaterialTheme.typography.displayMedium,
                            color = paper,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    HeroMeta(label = "ZONES", value = venue.areas.size.toString())
                    HeroMeta(
                        label = "CAPACITY",
                        value = if (capacity > 0) capacity.toString() else "—"
                    )
                    HeroMeta(
                        label = "FEATURES",
                        value = buildList {
                            if (venue.venue.isHeadCountEnabled) add("CNT")
                            if (venue.venue.isLostAndFoundEnabled) add("LF")
                            if (venue.venue.isIncidentReportingEnabled) add("INC")
                        }.joinToString(" · ").ifEmpty { "—" },
                    )
                }

                Spacer(Modifier.height(14.dp))
                SparkBar(
                    progress = progress,
                    trackColor = Color(0xFF2E2A24),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (capacity > 0) {
                        "${(progress * 100).roundToInt()}% OF ${
                            capacity.toString().reversed().chunked(3).joinToString(",").reversed()
                        }"
                    } else {
                        "SET CAPACITY PER ZONE TO TRACK FILL"
                    },
                    style = MonoTiny,
                    color = paper.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun HeroMeta(label: String, value: String) {
    Column {
        Text(
            label,
            style = MonoTiny,
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f)
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.background,
        )
    }
}

// ---------------------------------------------------------------------------
// Roster header
// ---------------------------------------------------------------------------

@Composable
private fun RosterHeader(count: Int) {
    Spacer(Modifier.height(22.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("§ TONIGHT", style = MonoTiny, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "All venues.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = "$count TOTAL",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Hairline()
}

// ---------------------------------------------------------------------------
// Venue row
// ---------------------------------------------------------------------------

@Composable
private fun VenueRow(
    venue: VenueWithAreas,
    onTap: () -> Unit,
    onHeadcount: () -> Unit,
    onLostFound: () -> Unit,
    onIncidents: () -> Unit,
    onManageAreas: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val capacity = venue.areas.sumOf { if (it.capacity > 0) it.capacity else 0 }
    val isActive = venue.venue.isActive
    val progress = 0f // Awaiting live event data hook-in; 0 for now.
    var menuOpen by remember { mutableStateOf(false) }

    // Staggered enter animation.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isActive) { onTap() }
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .alpha(if (isActive) 1f else 0.55f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = venue.venue.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = "${venue.venue.code.ifBlank { "—" }} · ${venue.venue.location.ifBlank { "LOCATION UNSET" }} · CAP ${if (capacity > 0) capacity else "—"}".uppercase(),
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            if (isActive) {
                SevPill(severity = Severity.LOW, label = "READY")
            } else {
                SevPill(severity = Severity.NEUTRAL, label = "IDLE")
            }
            Box {
                Text(
                    text = "⋯",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clickable { menuOpen = true },
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "MANAGE ZONES",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        onClick = { menuOpen = false; onManageAreas() },
                    )
                    DropdownMenuItem(
                        text = { Text("EDIT VENUE", style = MaterialTheme.typography.labelMedium) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                "DELETE",
                                style = MaterialTheme.typography.labelMedium,
                                color = Signal
                            )
                        },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (capacity > 0 && isActive) {
            SparkBar(progress = progress)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${(progress * 100).roundToInt()}% FULL",
                    style = MonoTiny,
                    color = capacityToneFor(progress),
                )
                Text(
                    text = "${venue.areas.size} ZONES",
                    style = MonoTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
        }

        // Feature chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (venue.venue.isHeadCountEnabled) {
                FeatureChip(label = "HEADCOUNT", onClick = onHeadcount, enabled = isActive)
            }
            if (venue.venue.isLostAndFoundEnabled) {
                FeatureChip(label = "LOST & FND", onClick = onLostFound, enabled = isActive)
            }
            if (venue.venue.isIncidentReportingEnabled) {
                FeatureChip(label = "INCIDENTS", onClick = onIncidents, enabled = isActive)
            }
        }
    }
    Hairline(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun FeatureChip(label: String, onClick: () -> Unit, enabled: Boolean) {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .border(FieldTokens.Hair, ink.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = "· $label",
            style = MaterialTheme.typography.labelSmall,
            color = ink.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

// ---------------------------------------------------------------------------
// Tool rail + colophon
// ---------------------------------------------------------------------------

@Composable
private fun ToolRail(onReports: () -> Unit, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToolButton(label = "▲ REPORTS", onClick = onReports, modifier = Modifier.weight(1f))
        ToolButton(label = "⚙ SETTINGS", onClick = onSettings, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ToolButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = modifier
            .height(FieldTokens.ToolHeight)
            .border(FieldTokens.Hair, ink)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ink,
        )
    }
}

@Composable
private fun Colophon(total: Int, live: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Hairline(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("SYNC · UP TO DATE", style = MonoTiny, color = Sage)
            Text(
                "$total VENUES · $live LIVE",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "com.eventmonitor.app",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("― FIELD ―", style = MonoTiny, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------

@Composable
private fun LivePulse() {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "live-alpha",
    )
    Box(
        modifier = Modifier
            .alpha(alpha)
            .background(Signal)
            .width(7.dp)
            .height(7.dp),
    )
}

@Composable
private fun LoadingPanel() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "LOADING · PLEASE STAND BY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FieldMasthead(modifier = Modifier.padding(bottom = 24.dp))
        Text(
            text = "No venues on the roster.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Add a venue from Settings to begin counting.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorPanel(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "ERROR · $message",
            style = MaterialTheme.typography.labelMedium,
            color = Signal,
        )
    }
}
