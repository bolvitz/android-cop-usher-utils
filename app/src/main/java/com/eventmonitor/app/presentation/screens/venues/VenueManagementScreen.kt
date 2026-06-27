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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.eventmonitor.core.common.theme.MonoTiny
import com.eventmonitor.core.common.theme.Sage
import com.eventmonitor.core.common.theme.Signal
import com.eventmonitor.core.common.ui.ArcadeBackground
import com.eventmonitor.core.common.ui.FieldTokens
import com.eventmonitor.core.common.ui.SoftAppBar
import com.eventmonitor.core.common.ui.KeyTone
import com.eventmonitor.core.common.ui.SoftAlertDialog
import com.eventmonitor.core.common.ui.SoftButtonTone
import com.eventmonitor.core.common.ui.SoftLivePulseDot
import com.eventmonitor.core.common.ui.SoftCard
import com.eventmonitor.core.common.ui.SoftIconButton
import com.eventmonitor.core.common.ui.SevPill
import com.eventmonitor.core.common.ui.Severity
import com.eventmonitor.core.common.ui.softHeadline
import com.eventmonitor.core.common.utils.rememberHapticFeedback
import com.eventmonitor.core.data.local.entities.VenueEntity

private enum class RosterFilter { ALL, LIVE, IDLE }

@Composable
fun VenueManagementScreen(
    viewModel: VenueManagementViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onAddVenue: () -> Unit,
    onEditVenue: (String) -> Unit,
) {
    val haptic = rememberHapticFeedback()
    val venues by viewModel.venues.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<VenueEntity?>(null) }
    var filter by remember { mutableStateOf(RosterFilter.ALL) }

    LaunchedEffect(uiState.message, uiState.error) {
        if (uiState.message != null || uiState.error != null) {
            kotlinx.coroutines.delay(2400)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SoftAppBar(
                title = "Branches",
                subtitle = "§ Registry",
                onBack = { haptic.light(); onNavigateBack() },
                trailing = {
                    SoftIconButton(glyph = "+", onClick = { haptic.medium(); onAddVenue() })
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ArcadeBackground(modifier = Modifier.matchParentSize())

            val active = venues.count { it.isActive }
            val dormant = venues.size - active
            val visible = when (filter) {
                RosterFilter.ALL -> venues
                RosterFilter.LIVE -> venues.filter { it.isActive }
                RosterFilter.IDLE -> venues.filter { !it.isActive }
            }.sortedBy { it.name.lowercase() }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item(key = "dateline") {
                    Dateline(total = venues.size, live = active)
                }

                item(key = "stats") {
                    RegistryStats(
                        total = venues.size,
                        active = active,
                        dormant = dormant,
                    )
                }

                item(key = "cta") {
                    FileNewBranchSlab(
                        onClick = { haptic.medium(); onAddVenue() },
                    )
                }

                item(key = "filters") {
                    FilterStrip(
                        current = filter,
                        totals = Triple(venues.size, active, dormant),
                        onSelect = { haptic.light(); filter = it },
                    )
                }

                item(key = "roster-head") {
                    RosterHeader(
                        heading = when (filter) {
                            RosterFilter.ALL -> "All filed. A → Z."
                            RosterFilter.LIVE -> "On the air."
                            RosterFilter.IDLE -> "At rest."
                        },
                        count = visible.size,
                    )
                }

                if (visible.isEmpty()) {
                    item(key = "empty") {
                        EmptyRosterNote(filter = filter, onAdd = {
                            haptic.medium(); onAddVenue()
                        })
                    }
                } else {
                    items(items = visible, key = { it.id }) { v ->
                        val index = visible.indexOf(v) + 1
                        RegistryRow(
                            index = index,
                            venue = v,
                            onEdit = { haptic.light(); onEditVenue(v.id) },
                            onToggle = {
                                haptic.medium()
                                viewModel.toggleVenueStatus(v.id, !v.isActive)
                            },
                            onDelete = {
                                haptic.medium(); showDeleteDialog = v
                            },
                        )
                    }
                }

                item(key = "colophon") {
                    Colophon(total = venues.size, live = active)
                }
            }

            showDeleteDialog?.let { v ->
                SoftAlertDialog(
                    onDismiss = { haptic.light(); showDeleteDialog = null },
                    eyebrow = "Delete · Branch",
                    title = "Strike “${v.name}” from the register?",
                    message = "This branch and its entry in the registry will be removed. The action cannot be undone.",
                    confirmLabel = "Strike",
                    dismissLabel = "Keep",
                    confirmTone = SoftButtonTone.Destructive,
                    onConfirm = {
                        haptic.strong()
                        viewModel.deleteVenue(v.id)
                        showDeleteDialog = null
                    },
                )
            }

            AnimatedVisibility(
                visible = uiState.message != null || uiState.error != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                val flash = uiState.error ?: uiState.message
                val isError = uiState.error != null
                flash?.let { msg ->
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = if (isError) Signal else MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                        action = {
                            TextButton(onClick = {
                                haptic.light(); viewModel.clearMessage()
                            }) {
                                Text(
                                    "OK",
                                    color = MaterialTheme.colorScheme.background,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        },
                    ) {
                        Column {
                            Text(
                                text = if (isError) "ERROR · REGISTRY" else "FILED · REGISTRY",
                                style = MonoTiny,
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(msg, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Dateline — thin editorial kicker under the app bar
// ---------------------------------------------------------------------------

@Composable
private fun Dateline(total: Int, live: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "DIRECTORY OF BRANCHES",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "The Register",
                style = softHeadline(28),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (live > 0) {
                    LivePulse()
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = if (live > 0) "$live ON AIR" else "ALL STATIONS IDLE",
                    style = MonoTiny,
                    color = if (live > 0) Signal else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "$total FILED",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
    Spacer(Modifier.height(20.dp))
}

// ---------------------------------------------------------------------------
// Stat strip — INDEX / LIVE / DORMANT
// ---------------------------------------------------------------------------

@Composable
private fun RegistryStats(total: Int, active: Int, dormant: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        StatCell(label = "INDEX", value = total.toString())
        StatDivider()
        StatCell(label = "LIVE", value = active.toString(), accent = active > 0)
        StatDivider()
        StatCell(label = "DORMANT", value = dormant.toString())
    }
    Spacer(Modifier.height(20.dp))
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
// Primary CTA — classified-ad ink slab replacing the FAB
// ---------------------------------------------------------------------------

@Composable
private fun FileNewBranchSlab(onClick: () -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "OPEN FOR FILING",
                style = MaterialTheme.typography.labelMedium,
                color = Sage,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ink)
                .clickable { onClick() }
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "File a New",
                            style = softHeadline(24),
                            color = paper,
                        )
                        Text(
                            text = "Branch.",
                            style = softHeadline(28),
                            color = paper,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "NAME · CODE · LOCATION · FEATURES",
                            style = MonoTiny,
                            color = paper.copy(alpha = 0.6f),
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PEN",
                            style = MonoTiny,
                            color = paper.copy(alpha = 0.6f),
                        )
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.displayMedium,
                            color = paper,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Filter strip — ALL · LIVE · IDLE
// ---------------------------------------------------------------------------

@Composable
private fun FilterStrip(
    current: RosterFilter,
    totals: Triple<Int, Int, Int>,
    onSelect: (RosterFilter) -> Unit,
) {
    val (all, live, idle) = totals
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterTab(
            label = "ALL",
            count = all,
            selected = current == RosterFilter.ALL,
            onClick = { onSelect(RosterFilter.ALL) },
        )
        FilterTab(
            label = "LIVE",
            count = live,
            selected = current == RosterFilter.LIVE,
            onClick = { onSelect(RosterFilter.LIVE) },
            accent = Signal,
        )
        FilterTab(
            label = "IDLE",
            count = idle,
            selected = current == RosterFilter.IDLE,
            onClick = { onSelect(RosterFilter.IDLE) },
        )
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun FilterTab(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color? = null,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val borderColor = accent ?: ink
    Box(
        modifier = Modifier
            .border(FieldTokens.Hair, if (selected) borderColor else ink.copy(alpha = 0.45f))
            .background(if (selected) ink else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "· $label",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) paper else ink,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = count.toString().padStart(2, '0'),
                style = MonoTiny,
                color = if (selected) paper.copy(alpha = 0.65f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Roster header
// ---------------------------------------------------------------------------

@Composable
private fun RosterHeader(heading: String, count: Int) {
    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "§ THE ROSTER",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = heading,
                style = softHeadline(20),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = "$count ENTRIES",
            style = MonoTiny,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(10.dp))
}

// ---------------------------------------------------------------------------
// Registry row
// ---------------------------------------------------------------------------

@Composable
private fun RegistryRow(
    index: Int,
    venue: VenueEntity,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val isActive = venue.isActive

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .alpha(if (isActive) 1f else 0.55f),
    ) {
        SoftCard(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                // Large index number, like a newspaper column number.
                Box(
                    modifier = Modifier.width(44.dp),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Text(
                        text = index.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (isActive) ink else muted,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = venue.name,
                        style = softHeadline(20),
                        color = ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(venue.code.ifBlank { "—" })
                            append(" · ")
                            append(venue.location.ifBlank { "LOCATION UNSET" })
                            if (venue.timezone.isNotBlank() && venue.timezone != "UTC") {
                                append(" · ")
                                append(venue.timezone)
                            }
                        }.uppercase(),
                        style = MonoTiny,
                        color = muted,
                    )

                    if (venue.contactPerson.isNotBlank() || venue.contactPhone.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = buildList {
                                if (venue.contactPerson.isNotBlank()) add(venue.contactPerson)
                                if (venue.contactPhone.isNotBlank()) add(venue.contactPhone)
                            }.joinToString(" · ").uppercase(),
                            style = MonoTiny,
                            color = muted,
                        )
                    }

                    // Feature glyphs
                    val features = buildList {
                        if (venue.isHeadCountEnabled) add("CNT")
                        if (venue.isLostAndFoundEnabled) add("L&F")
                        if (venue.isIncidentReportingEnabled) add("INC")
                    }
                    if (features.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            features.forEach { FeatureTag(it, enabled = isActive) }
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(horizontalAlignment = Alignment.End) {
                    if (isActive) {
                        SevPill(severity = Severity.LOW, label = "LIVE")
                    } else {
                        SevPill(severity = Severity.NEUTRAL, label = "IDLE")
                    }
                    Spacer(Modifier.height(8.dp))
                    Box {
                        Text(
                            text = "⋯",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ink,
                            modifier = Modifier
                                .clickable { menuOpen = true }
                                .padding(start = 6.dp),
                        )
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = MaterialTheme.colorScheme.background,
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("EDIT", style = MaterialTheme.typography.labelMedium)
                                },
                                onClick = { menuOpen = false; onEdit() },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isActive) "SEND TO IDLE" else "BRING LIVE",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                onClick = { menuOpen = false; onToggle() },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "STRIKE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Signal,
                                    )
                                },
                                onClick = { menuOpen = false; onDelete() },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureTag(label: String, enabled: Boolean) {
    val ink = MaterialTheme.colorScheme.onBackground
    val alpha = if (enabled) 1f else 0.4f
    Box(
        modifier = Modifier
            .border(FieldTokens.Hair, ink.copy(alpha = alpha))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ink.copy(alpha = alpha),
        )
    }
}

// ---------------------------------------------------------------------------
// Empty roster note (inline within filtered views)
// ---------------------------------------------------------------------------

@Composable
private fun EmptyRosterNote(filter: RosterFilter, onAdd: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp),
    ) {
        Text(
            text = when (filter) {
                RosterFilter.ALL -> "The register is blank."
                RosterFilter.LIVE -> "No branch on the air."
                RosterFilter.IDLE -> "No dormant entries."
            },
            style = softHeadline(20),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = when (filter) {
                RosterFilter.ALL -> "FILE THE FIRST BRANCH ABOVE TO OPEN THE REGISTER."
                RosterFilter.LIVE -> "TOGGLE AN IDLE ENTRY, OR FILE A NEW ONE."
                RosterFilter.IDLE -> "EVERY BRANCH IS CURRENTLY LIVE."
            },
            style = MonoTiny,
            color = muted,
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(14.dp))
        SoftCard(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "＋ FILE A BRANCH",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Colophon
// ---------------------------------------------------------------------------

@Composable
private fun Colophon(total: Int, live: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("REGISTRAR · UP TO DATE", style = MonoTiny, color = Sage)
            Text(
                "$total FILED · $live LIVE",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "com.eventmonitor.app · registry",
                style = MonoTiny,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Live pulse (shared visual with VenueListScreen)
// ---------------------------------------------------------------------------

@Composable
private fun LivePulse() {
    SoftLivePulseDot(size = 8)
}
