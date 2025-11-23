package com.eventmonitor.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.eventmonitor.app.presentation.screens.areas.AreaManagementScreen
import com.eventmonitor.app.presentation.screens.branches.BranchListScreen
import com.eventmonitor.app.presentation.screens.branches.BranchSetupScreen
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
    startDestination: String = Screen.BranchList.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.BranchList.route) {
            BranchListScreen(
                onBranchClick = { branchId ->
                    navController.navigate(Screen.Counting.createRoute(branchId))
                },
                onManageAreas = { branchId ->
                    navController.navigate(Screen.AreaManagement.createRoute(branchId))
                },
                onEditBranch = { branchId ->
                    navController.navigate(Screen.BranchSetup.createRoute(branchId))
                },
                onBranchHistory = { branchId ->
                    navController.navigate(Screen.History.createRoute(branchId))
                },
                onBranchIncidents = { branchId ->
                    navController.navigate(Screen.IncidentList.createRoute(branchId))
                },
                onBranchLostAndFound = { branchId ->
                    navController.navigate(Screen.LostAndFound.createRoute(branchId))
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
            route = Screen.BranchSetup.route,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            BranchSetupScreen(
                onNavigateBack = { navController.popBackStack() },
                onManageAreas = { branchId ->
                    navController.navigate(Screen.AreaManagement.createRoute(branchId))
                }
            )
        }

        composable(
            route = Screen.AreaManagement.route,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
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

        composable(
            route = Screen.Counting.route,
            arguments = listOf(
                navArgument("branchId") { type = NavType.StringType },
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
                navArgument("branchId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            HistoryScreen(
                onServiceClick = { branchId, serviceId ->
                    navController.navigate(Screen.Counting.createRoute(branchId, serviceId))
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
                onAddBranch = {
                    navController.navigate(Screen.BranchSetup.createRoute())
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
            route = Screen.IncidentList.route,
            arguments = listOf(
                navArgument("branchId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val branchId = backStackEntry.arguments?.getString("branchId")
            IncidentListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddIncident = { brId ->
                    navController.navigate(Screen.AddEditIncident.createRoute(brId))
                },
                onNavigateToIncidentDetail = { incidentId ->
                    navController.navigate(Screen.IncidentDetail.createRoute(incidentId))
                },
                onNavigateToEditIncident = { brId, incidentId ->
                    navController.navigate(Screen.AddEditIncident.createRoute(brId, incidentId))
                },
                branchId = branchId
            )
        }

        composable(
            route = Screen.AddEditIncident.route,
            arguments = listOf(
                navArgument("branchId") { type = NavType.StringType },
                navArgument("incidentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val branchId = backStackEntry.arguments?.getString("branchId") ?: ""
            val incidentId = backStackEntry.arguments?.getString("incidentId")
            AddEditIncidentScreen(
                branchId = branchId,
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
