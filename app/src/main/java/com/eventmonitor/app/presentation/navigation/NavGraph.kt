package com.eventmonitor.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.eventmonitor.app.presentation.screens.areas.AreaManagementScreen
import com.eventmonitor.app.presentation.screens.venues.VenueListScreen
import com.eventmonitor.app.presentation.screens.venues.VenueManagementScreen
import com.eventmonitor.app.presentation.screens.venues.VenueSetupScreen
import com.eventmonitor.app.presentation.screens.reports.ReportsScreen
import com.eventmonitor.app.presentation.screens.eventtypes.ServiceTypeManagementScreen
import com.eventmonitor.app.presentation.screens.settings.SettingsScreen
import com.eventmonitor.feature.headcounter.screens.CountingScreen
import com.eventmonitor.feature.headcounter.screens.HistoryScreen
import com.eventmonitor.feature.lostandfound.screens.AddEditLostItemScreen
import com.eventmonitor.feature.lostandfound.screens.LostAndFoundScreen
import com.eventmonitor.feature.incidents.screens.AddEditIncidentScreen
import com.eventmonitor.feature.incidents.screens.IncidentDetailScreen
import com.eventmonitor.feature.incidents.screens.IncidentListScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.VenueList.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.VenueList.route) {
            VenueListScreen(
                onVenueClick = { venueId ->
                    navController.navigate(Screen.Counting.createRoute(venueId))
                },
                onManageAreas = { venueId ->
                    navController.navigate(Screen.AreaManagement.createRoute(venueId))
                },
                onEditVenue = { venueId ->
                    navController.navigate(Screen.VenueSetup.createRoute(venueId))
                },
                onVenueHistory = { venueId ->
                    navController.navigate(Screen.History.createRoute(venueId))
                },
                onVenueIncidents = { venueId ->
                    navController.navigate(Screen.IncidentList.createRoute(venueId))
                },
                onVenueLostAndFound = { venueId ->
                    navController.navigate(Screen.LostAndFound.createRoute(venueId))
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
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
        ) {
            AreaManagementScreen(
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
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onManageServiceTypes = {
                    navController.navigate(Screen.ServiceTypeManagement.route)
                },
                onManageVenues = {
                    navController.navigate(Screen.VenueManagement.route)
                }
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
    }
}
