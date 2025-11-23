package com.copheadcounter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.copheadcounter.model.CounterItem
import com.copheadcounter.model.LostFoundItem
import com.copheadcounter.ui.CountingScreen
import com.copheadcounter.ui.MainMenuScreen
import com.copheadcounter.ui.lostfound.AddEditItemScreen
import com.copheadcounter.ui.lostfound.ItemDetailsScreen
import com.copheadcounter.ui.lostfound.LostFoundListScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    counterItems: List<CounterItem>,
    onIncrementCount: (String) -> Unit,
    onDecrementCount: (String) -> Unit,
    onAddNewCounter: (String) -> Unit,
    lostFoundItems: List<LostFoundItem>,
    onAddItem: (LostFoundItem) -> Unit,
    onUpdateItem: (LostFoundItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClaimItem: (String, String, String) -> Unit,
    onUpdateStatus: (String, com.copheadcounter.model.ItemStatus) -> Unit,
    getItemById: (String) -> LostFoundItem?
) {
    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu.route
    ) {
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                onNavigateToCounter = {
                    navController.navigate(Screen.Counter.route)
                },
                onNavigateToLostFound = {
                    navController.navigate(Screen.LostFoundList.route)
                }
            )
        }

        composable(Screen.Counter.route) {
            CountingScreen(
                counterItems = counterItems,
                onIncrementCount = onIncrementCount,
                onDecrementCount = onDecrementCount,
                onAddNewCounter = onAddNewCounter,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.LostFoundList.route) {
            LostFoundListScreen(
                items = lostFoundItems,
                onItemClick = { itemId ->
                    navController.navigate(Screen.ItemDetails.createRoute(itemId))
                },
                onAddItem = {
                    navController.navigate(Screen.AddItem.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AddItem.route) {
            AddEditItemScreen(
                item = null,
                onSave = { item ->
                    onAddItem(item)
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ItemDetails.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val item = getItemById(itemId)

            if (item != null) {
                ItemDetailsScreen(
                    item = item,
                    onEdit = {
                        navController.navigate(Screen.EditItem.createRoute(itemId))
                    },
                    onDelete = {
                        onDeleteItem(itemId)
                        navController.popBackStack()
                    },
                    onClaim = { claimantName, claimantContact ->
                        onClaimItem(itemId, claimantName, claimantContact)
                    },
                    onUpdateStatus = { status ->
                        onUpdateStatus(itemId, status)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(
            route = Screen.EditItem.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val item = getItemById(itemId)

            if (item != null) {
                AddEditItemScreen(
                    item = item,
                    onSave = { updatedItem ->
                        onUpdateItem(updatedItem)
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
