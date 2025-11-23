package com.copheadcounter.navigation

sealed class Screen(val route: String) {
    object BranchList : Screen("branch_list")
    object AddBranch : Screen("add_branch")
    object EditBranch : Screen("edit_branch/{branchId}") {
        fun createRoute(branchId: String) = "edit_branch/$branchId"
    }
    object Counter : Screen("counter/{branchId}") {
        fun createRoute(branchId: String) = "counter/$branchId"
    }
    object LostFoundList : Screen("lost_found_list/{branchId}") {
        fun createRoute(branchId: String) = "lost_found_list/$branchId"
    }
    object AddItem : Screen("add_item/{branchId}") {
        fun createRoute(branchId: String) = "add_item/$branchId"
    }
    object ItemDetails : Screen("item_details/{itemId}") {
        fun createRoute(itemId: String) = "item_details/$itemId"
    }
    object EditItem : Screen("edit_item/{itemId}") {
        fun createRoute(itemId: String) = "edit_item/$itemId"
    }
}
