package com.cop.app.headcounter.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cop.app.headcounter.presentation.screens.areas.AreaManagementScreen
import com.cop.app.headcounter.presentation.screens.branches.BranchListScreen
import com.cop.app.headcounter.presentation.screens.branches.BranchSetupScreen
import com.cop.app.headcounter.presentation.screens.counting.CountingScreen
import com.cop.app.headcounter.presentation.screens.history.HistoryScreen
import com.cop.app.headcounter.presentation.screens.reports.ReportsScreen
import com.cop.app.headcounter.presentation.screens.servicetypes.ServiceTypeManagementScreen
import com.cop.app.headcounter.presentation.screens.settings.SettingsScreen

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
                onAddBranch = {
                    navController.navigate(Screen.BranchSetup.createRoute())
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.createRoute())
                },
                onBranchHistory = { branchId ->
                    navController.navigate(Screen.History.createRoute(branchId))
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onManageServiceTypes = {
                    navController.navigate(Screen.ServiceTypeManagement.route)
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
