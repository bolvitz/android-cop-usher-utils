package com.eventmonitor.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.eventmonitor.app.presentation.screens.areas.SharedAreaManagementScreen
import com.eventmonitor.app.presentation.screens.areas.SharedSeatMapEditorScreen
import com.eventmonitor.app.presentation.screens.eventtypes.SharedEventTypeManagementScreen
import com.eventmonitor.app.presentation.screens.headcounter.SharedCountingScreen
import com.eventmonitor.app.presentation.screens.headcounter.SharedHistoryScreen
import com.eventmonitor.app.presentation.screens.headcounter.SharedTrendsScreen
import com.eventmonitor.app.presentation.screens.home.CountTab
import com.eventmonitor.app.presentation.screens.home.LiveTab
import com.eventmonitor.app.presentation.screens.home.MoreTab
import com.eventmonitor.app.presentation.screens.incidents.SharedIncidentDetailScreen
import com.eventmonitor.app.presentation.screens.incidents.SharedIncidentListScreen
import com.eventmonitor.app.presentation.screens.lostandfound.SharedLostAndFoundScreen
import com.eventmonitor.app.presentation.screens.lostandfound.SharedLostItemDetailScreen
import com.eventmonitor.app.presentation.screens.reports.SharedReportsScreen
import com.eventmonitor.app.presentation.screens.venues.SharedVenueEditScreen
import com.eventmonitor.app.presentation.screens.venues.SharedVenueListScreen
import com.eventmonitor.shared.presentation.scope.AppShellUiState
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NavGraph(
    navController: NavHostController,
    shellState: AppShellUiState,
    onSelectVenue: (String?) -> Unit,
    onOpenTab: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.TabLive.route
) {
    val toCount: (String) -> Unit = { navController.navigate(Screen.SharedCounting.createRoute(it)) }
    val toAreas: (String) -> Unit = { navController.navigate(Screen.SharedAreaManagement.createRoute(it)) }
    val toHistory: (String?) -> Unit = { navController.navigate(Screen.SharedHistory.createRoute(it)) }
    val toVenues: () -> Unit = { navController.navigate(Screen.SharedVenueList.route) }
    // Lists keyed by scope so switching venue builds a fresh, correctly filtered ViewModel.
    val scopeKey = shellState.selectedVenueId ?: "all"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.TabLive.route) {
            LiveTab(
                state = shellState,
                onSelectVenue = onSelectVenue,
                onCount = toCount,
                onAreas = toAreas,
                onHistory = toHistory,
                onOpenIncidents = { onOpenTab(Screen.TabIncidents) },
                onOpenLostFound = { onOpenTab(Screen.TabLostFound) },
                onSetUpVenues = toVenues
            )
        }

        composable(Screen.TabCount.route) {
            CountTab(state = shellState, onCount = toCount, onSetUpVenues = toVenues)
        }

        composable(Screen.TabIncidents.route) {
            if (!shellState.isLoading) BelowFloatingBar {
                val venueId = shellState.selectedVenueId
                SharedIncidentListScreen(
                    venueId = venueId,
                    onBack = null,
                    onOpen = { navController.navigate(Screen.SharedIncidentDetail.createRoute(it)) },
                    viewModel = koinViewModel(key = "incidents_$scopeKey") { parametersOf(venueId) }
                )
            }
        }

        composable(Screen.TabLostFound.route) {
            if (!shellState.isLoading) BelowFloatingBar {
                val venueId = shellState.selectedVenueId
                SharedLostAndFoundScreen(
                    locationId = venueId,
                    onBack = null,
                    onOpen = { navController.navigate(Screen.SharedLostItemDetail.createRoute(it)) },
                    viewModel = koinViewModel(key = "lost_$scopeKey") { parametersOf(venueId) }
                )
            }
        }

        composable(Screen.TabMore.route) {
            MoreTab(
                state = shellState,
                onHistory = { toHistory(shellState.selectedVenueId) },
                onTrends = { navController.navigate(Screen.SharedTrends.createRoute(shellState.selectedVenueId)) },
                onReports = { navController.navigate(Screen.SharedReports.route) },
                onVenues = toVenues,
                onAreas = toAreas,
                onEventTypes = { navController.navigate(Screen.SharedEventTypes.route) }
            )
        }

        composable(Screen.SharedVenueList.route) {
            SharedVenueListScreen(
                onCount = { venueId -> navController.navigate(Screen.SharedCounting.createRoute(venueId)) },
                onAreas = { venueId -> navController.navigate(Screen.SharedAreaManagement.createRoute(venueId)) },
                onHistory = { venueId -> navController.navigate(Screen.SharedHistory.createRoute(venueId)) },
                onIncidents = { venueId -> navController.navigate(Screen.SharedIncidents.createRoute(venueId)) },
                onLostFound = { venueId -> navController.navigate(Screen.SharedLostAndFound.createRoute(venueId)) },
                onEventTypes = { navController.navigate(Screen.SharedEventTypes.route) },
                onReports = { navController.navigate(Screen.SharedReports.route) },
                onEdit = { venueId -> navController.navigate(Screen.SharedVenueEdit.createRoute(venueId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SharedVenueEdit.route,
            arguments = listOf(navArgument("venueId") { type = NavType.StringType })
        ) { backStackEntry ->
            SharedVenueEditScreen(
                venueId = backStackEntry.arguments?.getString("venueId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SharedAreaManagement.route,
            arguments = listOf(navArgument("venueId") { type = NavType.StringType })
        ) { backStackEntry ->
            SharedAreaManagementScreen(
                venueId = backStackEntry.arguments?.getString("venueId") ?: "",
                onBack = { navController.popBackStack() },
                onSeatMap = { areaId -> navController.navigate(Screen.SharedSeatMapEditor.createRoute(areaId)) }
            )
        }

        composable(
            route = Screen.SharedSeatMapEditor.route,
            arguments = listOf(navArgument("areaId") { type = NavType.StringType })
        ) { backStackEntry ->
            SharedSeatMapEditorScreen(
                areaTemplateId = backStackEntry.arguments?.getString("areaId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SharedEventTypes.route) {
            SharedEventTypeManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.SharedReports.route) {
            SharedReportsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.SharedCounting.route,
            arguments = listOf(
                navArgument("venueId") { type = NavType.StringType },
                navArgument("eventId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            SharedCountingScreen(
                venueId = backStackEntry.arguments?.getString("venueId") ?: "",
                eventId = backStackEntry.arguments?.getString("eventId"),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SharedHistory.route,
            arguments = listOf(navArgument("venueId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) { backStackEntry ->
            val venueId = backStackEntry.arguments?.getString("venueId")
            SharedHistoryScreen(
                venueId = venueId,
                onBack = { navController.popBackStack() },
                onOpenTrends = { navController.navigate(Screen.SharedTrends.createRoute(venueId)) },
                onOpenCount = { eventVenueId, eventId ->
                    navController.navigate(Screen.SharedCounting.createRoute(eventVenueId, eventId))
                }
            )
        }

        composable(
            route = Screen.SharedTrends.route,
            arguments = listOf(navArgument("venueId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) { backStackEntry ->
            SharedTrendsScreen(
                venueId = backStackEntry.arguments?.getString("venueId"),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SharedIncidents.route,
            arguments = listOf(navArgument("venueId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) { backStackEntry ->
            SharedIncidentListScreen(
                venueId = backStackEntry.arguments?.getString("venueId"),
                onBack = { navController.popBackStack() },
                onOpen = { incidentId -> navController.navigate(Screen.SharedIncidentDetail.createRoute(incidentId)) }
            )
        }

        composable(
            route = Screen.SharedIncidentDetail.route,
            arguments = listOf(navArgument("incidentId") { type = NavType.StringType })
        ) { backStackEntry ->
            SharedIncidentDetailScreen(
                incidentId = backStackEntry.arguments?.getString("incidentId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SharedLostAndFound.route,
            arguments = listOf(navArgument("locationId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) { backStackEntry ->
            SharedLostAndFoundScreen(
                locationId = backStackEntry.arguments?.getString("locationId"),
                onBack = { navController.popBackStack() },
                onOpen = { itemId -> navController.navigate(Screen.SharedLostItemDetail.createRoute(itemId)) }
            )
        }

        composable(
            route = Screen.SharedLostItemDetail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            SharedLostItemDetailScreen(
                itemId = backStackEntry.arguments?.getString("itemId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/** Lifts a tab screen with its own Scaffold (FAB, snackbar) clear of the floating tab bar. */
@Composable
private fun BelowFloatingBar(content: @Composable () -> Unit) {
    val inset = PaddingValues(bottom = LocalFloatingBarInset.current)
    Box(Modifier.padding(inset).consumeWindowInsets(inset)) { content() }
}
