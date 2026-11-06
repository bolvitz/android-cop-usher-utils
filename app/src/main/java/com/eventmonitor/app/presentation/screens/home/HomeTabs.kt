package com.eventmonitor.app.presentation.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eventmonitor.app.presentation.navigation.LocalFloatingBarInset
import com.eventmonitor.shared.presentation.scope.AppShellUiState
import com.eventmonitor.shared.presentation.scope.VenueScopeRow

/** Live tab: all-venues overview, or the selected venue's hub. */
@Composable
fun LiveTab(
    state: AppShellUiState,
    onSelectVenue: (String) -> Unit,
    onCount: (String) -> Unit,
    onAreas: (String) -> Unit,
    onHistory: (String) -> Unit,
    onOpenIncidents: () -> Unit,
    onOpenLostFound: () -> Unit,
    onSetUpVenues: () -> Unit
) {
    val venue = state.selectedVenue
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = tabContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TabTitle("Live") }
        when {
            state.isLoading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.venues.isEmpty() -> item { EmptyVenues(onSetUpVenues) }
            venue == null -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Open incidents", state.openIncidents, onOpenIncidents, Modifier.weight(1f))
                        StatCard("Unclaimed items", state.unclaimedItems, onOpenLostFound, Modifier.weight(1f))
                    }
                }
                item { SectionLabel("Venues") }
                items(state.venues, key = { it.id }) { row ->
                    VenueCard(row, onClick = { onSelectVenue(row.id) })
                }
            }
            else -> {
                item {
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(venue.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            if (venue.location.isNotBlank()) {
                                Text(venue.location, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { onCount(venue.id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Count")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onAreas(venue.id) }, Modifier.weight(1f).heightIn(min = 48.dp)) {
                                    Text("Areas")
                                }
                                OutlinedButton(onClick = { onHistory(venue.id) }, Modifier.weight(1f).heightIn(min = 48.dp)) {
                                    Text("History")
                                }
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("Open incidents", venue.openIncidents, onOpenIncidents, Modifier.weight(1f))
                        StatCard("Unclaimed items", venue.unclaimedItems, onOpenLostFound, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Count tab: pick the venue to count (only the scoped one when a venue is selected). */
@Composable
fun CountTab(state: AppShellUiState, onCount: (String) -> Unit, onSetUpVenues: () -> Unit) {
    val rows = state.selectedVenue?.let { listOf(it) } ?: state.venues
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = tabContentPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TabTitle("Count") }
        when {
            state.isLoading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            rows.isEmpty() -> item { EmptyVenues(onSetUpVenues) }
            else -> {
                if (state.isAllVenues) item { SectionLabel("Pick a venue") }
                items(rows, key = { it.id }) { row ->
                    ListRow(title = row.name, subtitle = row.location.ifBlank { null }, onClick = { onCount(row.id) })
                }
            }
        }
    }
}

/** More tab: records, coordinator setup and app settings. */
@Composable
fun MoreTab(
    state: AppShellUiState,
    onHistory: () -> Unit,
    onTrends: () -> Unit,
    onReports: () -> Unit,
    onVenues: () -> Unit,
    onAreas: (String) -> Unit,
    onEventTypes: () -> Unit
) {
    val venue = state.selectedVenue
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = tabContentPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { TabTitle("More") }
        item { SectionLabel("Records") }
        item { ListRow("History", onClick = onHistory) }
        item { ListRow("Trends", onClick = onTrends) }
        item { ListRow("Reports", onClick = onReports) }
        item { SectionLabel("Setup") }
        item { ListRow("Venues", onClick = onVenues) }
        item {
            ListRow(
                title = "Areas & seat maps",
                subtitle = venue?.name ?: "Pick a venue at the top first",
                enabled = venue != null,
                onClick = { venue?.let { onAreas(it.id) } }
            )
        }
        item { ListRow("Event types", onClick = onEventTypes) }
    }
}

/** Leaves room below the last item for the floating tab bar, so content can scroll beneath it. */
@Composable
private fun tabContentPadding() =
    PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + LocalFloatingBarInset.current)

@Composable
private fun TabTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun StatCard(label: String, value: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VenueCard(row: VenueScopeRow, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (row.location.isNotBlank()) {
                Text(row.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CountChip(row.openIncidents, "open incident", "open incidents")
                CountChip(row.unclaimedItems, "unclaimed item", "unclaimed items")
            }
        }
    }
}

@Composable
private fun CountChip(count: Int, one: String, many: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            "$count ${if (count == 1) one else many}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ListRow(title: String, subtitle: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val alpha = if (enabled) 1f else 0.5f
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
    }
}

@Composable
private fun EmptyVenues(onSetUp: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("No venues yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Add a venue, then its areas, to start counting.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onSetUp, Modifier.heightIn(min = 48.dp)) { Text("Set up your first venue") }
    }
}
