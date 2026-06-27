package com.eventmonitor.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.eventmonitor.app.presentation.screens.areas.AreaManagementScreen
import com.eventmonitor.app.presentation.screens.areas.ZoneEditorScreen
import com.eventmonitor.app.presentation.screens.areas.SharedAreaManagementScreen
import com.eventmonitor.app.presentation.screens.areas.SharedSeatMapEditorScreen
import com.eventmonitor.app.presentation.screens.eventtypes.ServiceTypeManagementScreen
import com.eventmonitor.app.presentation.screens.eventtypes.SharedEventTypeManagementScreen
import com.eventmonitor.app.presentation.screens.headcounter.SharedCountingScreen
import com.eventmonitor.app.presentation.screens.venues.SharedVenueListScreen
import com.eventmonitor.app.presentation.screens.headcounter.SharedHistoryScreen
import com.eventmonitor.app.presentation.screens.headcounter.SharedTrendsScreen
import com.eventmonitor.app.presentation.screens.incidents.SharedIncidentDetailScreen
import com.eventmonitor.app.presentation.screens.incidents.SharedIncidentListScreen
import com.eventmonitor.app.presentation.screens.lostandfound.SharedLostAndFoundScreen
import com.eventmonitor.app.presentation.screens.lostandfound.SharedLostItemDetailScreen
import com.eventmonitor.app.presentation.screens.venues.SharedVenueEditScreen
import com.eventmonitor.app.presentation.screens.reports.ReportsScreen
import com.eventmonitor.app.presentation.screens.reports.SharedReportsScreen
import com.eventmonitor.app.presentation.screens.venues.VenueListScreen
import com.eventmonitor.app.presentation.screens.venues.VenueManagementScreen
import com.eventmonitor.app.presentation.screens.venues.VenueSetupScreen
import com.eventmonitor.feature.headcounter.screens.CountingScreen
import com.eventmonitor.feature.headcounter.screens.HistoryScreen
import com.eventmonitor.feature.headcounter.screens.TrendsScreen
import com.eventmonitor.feature.headcounter.seatmap.SeatMapDemoScreen
import com.eventmonitor.feature.incidents.screens.AddEditIncidentScreen
import com.eventmonitor.feature.incidents.screens.IncidentDetailScreen
import com.eventmonitor.feature.incidents.screens.IncidentListScreen
import com.eventmonitor.feature.lostandfound.screens.AddEditLostItemScreen
import com.eventmonitor.feature.lostandfound.screens.LostAndFoundScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.SharedVenueList.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.SharedVenueList.route) {
            SharedVenueListScreen(
                onCount = { venueId -> navController.navigate(Screen.SharedCounting.createRoute(venueId)) },
                onAreas = { venueId -> navController.navigate(Screen.SharedAreaManagement.createRoute(venueId)) },
                onHistory = { venueId -> navController.navigate(Screen.SharedHistory.createRoute(venueId)) },
                onIncidents = { venueId -> navController.navigate(Screen.SharedIncidents.createRoute(venueId)) },
                onLostFound = { venueId -> navController.navigate(Screen.SharedLostAndFound.createRoute(venueId)) },
                onEventTypes = { navController.navigate(Screen.SharedEventTypes.route) },
                onReports = { navController.navigate(Screen.SharedReports.route) },
                onEdit = { venueId -> navController.navigate(Screen.SharedVenueEdit.createRoute(venueId)) }
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

        composable(Screen.VenueList.route) {
            VenueListScreen(
                onVenueClick = { venueId ->
                    // Head counting now runs on the shared KMP ViewModel.
                    navController.navigate(Screen.SharedCounting.createRoute(venueId))
                },
                onManageAreas = { venueId ->
                    navController.navigate(Screen.AreaManagement.createRoute(venueId))
                },
                onEditVenue = { venueId ->
                    navController.navigate(Screen.VenueSetup.createRoute(venueId))
                },
                onVenueHistory = { venueId ->
                    // History now reads the shared KMP database.
                    navController.navigate(Screen.SharedHistory.createRoute(venueId))
                },
                onVenueIncidents = { venueId ->
                    // Incidents now run on the shared KMP ViewModel.
                    navController.navigate(Screen.SharedIncidents.createRoute(venueId))
                },
                onVenueLostAndFound = { venueId ->
                    // Lost & found now runs on the shared KMP ViewModel.
                    navController.navigate(Screen.SharedLostAndFound.createRoute(venueId))
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onManageVenues = {
                    navController.navigate(Screen.VenueManagement.route)
                },
                onManageServiceTypes = {
                    navController.navigate(Screen.ServiceTypeManagement.route)
                },
                onSeatMapDemo = {
                    navController.navigate(Screen.SeatMapDemo.route)
                },
            )
        }

        composable(
            route = Screen.VenueSetup.route,
            arguments = listOf(navArgument("venueId") { type = NavType.StringType })
        ) {
            VenueSetupScreen(
                onNavigateBack = { navController.popBackStack() },
                onManageAreas = { venueId ->
                    navController.navigate(Screen.AreaManagement.createRoute(venueId))
                }
            )
        }

        composable(
            route = Screen.AreaManagement.route,
            arguments = listOf(navArgument("venueId") { type = NavType.StringType })
        ) { backStackEntry ->
            val venueId = backStackEntry.arguments?.getString("venueId") ?: ""
            AreaManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateZone = {
                    navController.navigate(
                        Screen.ZoneEditor.createRoute(venueId, mode = Screen.ZoneEditor.MODE_SOLO)
                    )
                },
                onBatchCreateZone = {
                    navController.navigate(
                        Screen.ZoneEditor.createRoute(venueId, mode = Screen.ZoneEditor.MODE_BATCH)
                    )
                },
                onEditZone = { zoneId ->
                    navController.navigate(
                        Screen.ZoneEditor.createRoute(venueId, zoneId = zoneId)
                    )
                },
            )
        }

        composable(
            route = Screen.ZoneEditor.route,
            arguments = listOf(
                navArgument("venueId") { type = NavType.StringType },
                navArgument("zoneId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("mode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = Screen.ZoneEditor.MODE_SOLO
                },
            )
        ) {
            ZoneEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.ServiceTypeManagement.route) {
            ServiceTypeManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.VenueManagement.route) {
            VenueManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddVenue = {
                    navController.navigate(Screen.VenueSetup.createRoute())
                },
                onEditVenue = { venueId ->
                    navController.navigate(Screen.VenueSetup.createRoute(venueId))
                }
            )
        }

        composable(
            route = Screen.Counting.route,
            arguments = listOf(
                navArgument("venueId") { type = NavType.StringType },
                navArgument("serviceId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            CountingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SharedCounting.route,
            arguments = listOf(navArgument("venueId") { type = NavType.StringType })
        ) { backStackEntry ->
            val venueId = backStackEntry.arguments?.getString("venueId") ?: ""
            SharedCountingScreen(
                venueId = venueId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SharedIncidents.route,
            arguments = listOf(
                navArgument("venueId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
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
            arguments = listOf(
                navArgument("locationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
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

        composable(
            route = Screen.SharedHistory.route,
            arguments = listOf(
                navArgument("venueId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val venueId = backStackEntry.arguments?.getString("venueId")
            SharedHistoryScreen(
                venueId = venueId,
                onBack = { navController.popBackStack() },
                onOpenTrends = { navController.navigate(Screen.SharedTrends.createRoute(venueId)) }
            )
        }

        composable(
            route = Screen.SharedTrends.route,
            arguments = listOf(
                navArgument("venueId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            SharedTrendsScreen(
                venueId = backStackEntry.arguments?.getString("venueId"),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.History.route,
            arguments = listOf(
                navArgument("venueId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            HistoryScreen(
                onServiceClick = { venueId, serviceId ->
                    navController.navigate(Screen.Counting.createRoute(venueId, serviceId))
                },
                onStartNewCount = { venueId ->
                    navController.navigate(Screen.Counting.createRoute(venueId))
                },
                onViewTrends = { venueId ->
                    navController.navigate(Screen.Trends.createRoute(venueId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Trends.route,
            arguments = listOf(
                navArgument("venueId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            TrendsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LostAndFound.route,
            arguments = listOf(
                navArgument("locationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val locationId = backStackEntry.arguments?.getString("locationId")
            LostAndFoundScreen(
                locationId = locationId,
                onNavigateToAddItem = { locId ->
                    navController.navigate(Screen.AddEditLostItem.createRoute(locId))
                },
                onNavigateToItemDetail = { itemId ->
                    navController.navigate(Screen.LostItemDetail.createRoute(itemId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddEditLostItem.route,
            arguments = listOf(
                navArgument("locationId") { type = NavType.StringType },
                navArgument("itemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val locationId = backStackEntry.arguments?.getString("locationId") ?: ""
            val itemId = backStackEntry.arguments?.getString("itemId")
            AddEditLostItemScreen(
                locationId = locationId,
                itemId = itemId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.LostItemDetail.route,
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            com.eventmonitor.feature.lostandfound.screens.LostItemDetailScreen(
                itemId = itemId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { locId, itmId ->
                    navController.navigate(Screen.AddEditLostItem.createRoute(locId, itmId))
                }
            )
        }

        composable(
            route = Screen.IncidentList.route,
            arguments = listOf(
                navArgument("venueId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val venueId = backStackEntry.arguments?.getString("venueId")
            IncidentListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddIncident = { vnId ->
                    navController.navigate(Screen.AddEditIncident.createRoute(vnId))
                },
                onNavigateToIncidentDetail = { incidentId ->
                    navController.navigate(Screen.IncidentDetail.createRoute(incidentId))
                },
                onNavigateToEditIncident = { vnId, incidentId ->
                    navController.navigate(Screen.AddEditIncident.createRoute(vnId, incidentId))
                },
                venueId = venueId
            )
        }

        composable(
            route = Screen.AddEditIncident.route,
            arguments = listOf(
                navArgument("venueId") { type = NavType.StringType },
                navArgument("incidentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val venueId = backStackEntry.arguments?.getString("venueId") ?: ""
            val incidentId = backStackEntry.arguments?.getString("incidentId")
            AddEditIncidentScreen(
                venueId = venueId,
                incidentId = incidentId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.IncidentDetail.route,
            arguments = listOf(
                navArgument("incidentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val incidentId = backStackEntry.arguments?.getString("incidentId") ?: ""
            IncidentDetailScreen(
                incidentId = incidentId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { incId ->
                    // We need to get the branchId from the incident, for now navigate back
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.SeatMapDemo.route) {
            SeatMapDemoScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
