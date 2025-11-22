package com.cop.app.headcounter.presentation.navigation

sealed class Screen(val route: String) {
    object BranchList : Screen("branch_list")
    object BranchSetup : Screen("branch_setup/{branchId}") {
        fun createRoute(branchId: String = "new") = "branch_setup/$branchId"
    }
    object Counting : Screen("counting/{branchId}") {
        fun createRoute(branchId: String) = "counting/$branchId"
    }
    object History : Screen("history")
    object HistoryDetail : Screen("history/{serviceId}") {
        fun createRoute(serviceId: String) = "history/$serviceId"
    }
    object Reports : Screen("reports")
    object Settings : Screen("settings")
    object AreaManagement : Screen("area_management/{branchId}") {
        fun createRoute(branchId: String) = "area_management/$branchId"
    }
}
