package com.copheadcounter.navigation

sealed class Screen(val route: String) {
    object MainMenu : Screen("main_menu")
    object Counter : Screen("counter")
    object LostFoundList : Screen("lost_found_list")
    object AddItem : Screen("add_item")
    object ItemDetails : Screen("item_details/{itemId}") {
        fun createRoute(itemId: String) = "item_details/$itemId"
    }
    object EditItem : Screen("edit_item/{itemId}") {
        fun createRoute(itemId: String) = "edit_item/$itemId"
    }
}
