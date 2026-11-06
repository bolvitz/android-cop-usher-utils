package com.eventmonitor.app.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eventmonitor.shared.presentation.scope.AppShellUiState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.eventmonitor.shared.presentation.scope.AppShellViewModel
import org.koin.androidx.compose.koinViewModel

private data class Tab(val screen: Screen, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Screen.TabLive, "Live", Icons.Outlined.MonitorHeart),
    Tab(Screen.TabCount, "Count", Icons.Outlined.PersonAdd),
    Tab(Screen.TabIncidents, "Incidents", Icons.Outlined.ReportProblem),
    // "Lost & Found" truncates in a 5-item bar on phones.
    Tab(Screen.TabLostFound, "Lost Items", Icons.Outlined.Inventory2),
    Tab(Screen.TabMore, "More", Icons.Outlined.Menu)
)
private val TAB_ROUTES = TABS.map { it.screen.route }.toSet()

/**
 * Top-level shell: venue scope switcher on top, a floating glass tab bar at the bottom. Both hide on
 * pushed screens (counter, details, setup) so those keep their own full-screen layout.
 */
@Composable
fun AppShell(viewModel: AppShellViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val onTab = route in TAB_ROUTES
    var showScopeSheet by rememberSaveable { mutableStateOf(false) }

    val openTab: (Screen) -> Unit = { screen ->
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    var barHeight by remember { mutableStateOf(0.dp) }
    val selectedIndex = TABS.indexOfFirst { it.screen.route == route }

    Scaffold(
        // Only the scope bar pads the top; the tab bar floats over the content.
        contentWindowInsets = WindowInsets(0),
        topBar = { if (onTab) ScopeBar(state.scopeName, onClick = { showScopeSheet = true }) }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).consumeWindowInsets(inner)) {
            CompositionLocalProvider(LocalFloatingBarInset provides if (onTab) barHeight else 0.dp) {
                NavGraph(
                    navController = navController,
                    startDestination = Screen.TabLive.route,
                    shellState = state,
                    onSelectVenue = viewModel::selectVenue,
                    onOpenTab = openTab,
                    modifier = Modifier.fillMaxSize().haze(hazeState)
                )
            }
            if (onTab) {
                GlassTabs(
                    tabs = TABS.map { tab ->
                        val badge = when (tab.screen) {
                            Screen.TabIncidents -> state.openIncidents
                            Screen.TabLostFound -> state.unclaimedItems
                            else -> 0
                        }
                        GlassTabItem(tab.label, tab.icon, badge)
                    },
                    selectedIndex = selectedIndex,
                    onSelect = { openTab(TABS[it].screen) },
                    hazeState = hazeState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                )
            }
        }
    }

    if (showScopeSheet) {
        ScopeSheet(
            state = state,
            onPick = { venueId ->
                viewModel.selectVenue(venueId)
                showScopeSheet = false
            },
            onDismiss = { showScopeSheet = false }
        )
    }
}

@Composable
private fun ScopeBar(scopeName: String, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .clickable(role = Role.Button, onClickLabel = "Change venue", onClick = onClick)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Viewing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(scopeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScopeSheet(state: AppShellUiState, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Show",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                ScopeOption(AppShellUiState.ALL_VENUES, "${state.venues.size} venues", state.isAllVenues) { onPick(null) }
            }
            items(state.venues, key = { it.id }) { venue ->
                ScopeOption(venue.name, venue.location.ifBlank { null }, venue.id == state.selectedVenueId) { onPick(venue.id) }
            }
        }
    }
}

@Composable
private fun ScopeOption(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = if (selected) FontWeight.SemiBold else null) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary) }
        } else null,
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick).heightIn(min = 56.dp)
    )
}
