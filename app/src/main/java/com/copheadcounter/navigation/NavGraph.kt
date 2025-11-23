package com.copheadcounter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.copheadcounter.model.Branch
import com.copheadcounter.model.CounterItem
import com.copheadcounter.model.LostFoundItem
import com.copheadcounter.ui.AddEditBranchScreen
import com.copheadcounter.ui.BranchListScreen
import com.copheadcounter.ui.CountingScreen
import com.copheadcounter.ui.lostfound.AddEditItemScreen
import com.copheadcounter.ui.lostfound.ItemDetailsScreen
import com.copheadcounter.ui.lostfound.LostFoundListScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    branches: List<Branch>,
    onAddBranch: (Branch) -> Unit,
    onUpdateBranch: (Branch) -> Unit,
    getBranchById: (String) -> Branch?,
    getCountersForBranch: (String) -> List<CounterItem>,
    onIncrementCount: (String) -> Unit,
    onDecrementCount: (String) -> Unit,
    onAddNewCounter: (String, String) -> Unit,
    getItemsForBranch: (String) -> List<LostFoundItem>,
    onAddItem: (String, LostFoundItem) -> Unit,
    onUpdateItem: (LostFoundItem) -> Unit,
    onDeleteItem: (String) -> Unit,
    onClaimItem: (String, String, String) -> Unit,
    onUpdateStatus: (String, com.copheadcounter.model.ItemStatus) -> Unit,
    getItemById: (String) -> LostFoundItem?
) {
    NavHost(
        navController = navController,
        startDestination = Screen.BranchList.route
    ) {
        // Branch List Screen (Landing Page)
        composable(Screen.BranchList.route) {
            BranchListScreen(
                branches = branches,
                onBranchCounterClick = { branchId ->
                    navController.navigate(Screen.Counter.createRoute(branchId))
                },
                onBranchLostFoundClick = { branchId ->
                    navController.navigate(Screen.LostFoundList.createRoute(branchId))
                },
                onAddBranch = {
                    navController.navigate(Screen.AddBranch.route)
                },
                onEditBranch = { branchId ->
                    navController.navigate(Screen.EditBranch.createRoute(branchId))
                }
            )
        }

        // Add Branch Screen
        composable(Screen.AddBranch.route) {
            AddEditBranchScreen(
                branch = null,
                onSave = { branch ->
                    onAddBranch(branch)
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Branch Screen
        composable(
            route = Screen.EditBranch.route,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val branchId = backStackEntry.arguments?.getString("branchId") ?: ""
            val branch = getBranchById(branchId)

            if (branch != null) {
                AddEditBranchScreen(
                    branch = branch,
                    onSave = { updatedBranch ->
                        onUpdateBranch(updatedBranch)
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Counter Screen
        composable(
            route = Screen.Counter.route,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val branchId = backStackEntry.arguments?.getString("branchId") ?: ""
            val branch = getBranchById(branchId)
            val counters = getCountersForBranch(branchId)

            if (branch != null) {
                CountingScreen(
                    branchName = branch.name,
                    counterItems = counters,
                    onIncrementCount = onIncrementCount,
                    onDecrementCount = onDecrementCount,
                    onAddNewCounter = { name ->
                        onAddNewCounter(branchId, name)
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Lost & Found List Screen
        composable(
            route = Screen.LostFoundList.route,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val branchId = backStackEntry.arguments?.getString("branchId") ?: ""
            val branch = getBranchById(branchId)
            val items = getItemsForBranch(branchId)

            if (branch != null) {
                LostFoundListScreen(
                    branchName = branch.name,
                    items = items,
                    onItemClick = { itemId ->
                        navController.navigate(Screen.ItemDetails.createRoute(itemId))
                    },
                    onAddItem = {
                        navController.navigate(Screen.AddItem.createRoute(branchId))
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Add Item Screen
        composable(
            route = Screen.AddItem.route,
            arguments = listOf(navArgument("branchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val branchId = backStackEntry.arguments?.getString("branchId") ?: ""

            AddEditItemScreen(
                item = null,
                branchId = branchId,
                onSave = { item ->
                    onAddItem(branchId, item)
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        // Item Details Screen
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

        // Edit Item Screen
        composable(
            route = Screen.EditItem.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val item = getItemById(itemId)

            if (item != null) {
                AddEditItemScreen(
                    item = item,
                    branchId = item.branchId,
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
