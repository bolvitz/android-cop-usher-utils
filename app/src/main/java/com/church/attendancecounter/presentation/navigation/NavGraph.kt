package com.church.attendancecounter.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.church.attendancecounter.presentation.screens.branches.BranchListScreen
import com.church.attendancecounter.presentation.screens.branches.BranchSetupScreen
import com.church.attendancecounter.presentation.screens.counting.CountingScreen
import com.church.attendancecounter.presentation.screens.history.HistoryScreen
import com.church.attendancecounter.presentation.screens.reports.ReportsScreen
import com.church.attendancecounter.presentation.screens.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.BranchList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.BranchList.route) {
            BranchListScreen(
                onBranchClick = { branchId ->
                    navController.navigate(Screen.Counting.createRoute(branchId))
                },
                onAddBranch = {
                    navController.navigate(Screen.BranchSetup.createRoute())
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
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
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Counting.route,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) {
            CountingScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onServiceClick = { serviceId ->
                    navController.navigate(Screen.HistoryDetail.createRoute(serviceId))
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
