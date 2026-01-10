package com.kroslabs.recipemanager.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kroslabs.recipemanager.R
import com.kroslabs.recipemanager.ui.screens.addrecipe.AddRecipeScreen
import com.kroslabs.recipemanager.ui.screens.mealplan.MealPlanScreen
import com.kroslabs.recipemanager.ui.screens.recipes.RecipeDetailScreen
import com.kroslabs.recipemanager.ui.screens.recipes.RecipeListScreen
import com.kroslabs.recipemanager.ui.screens.settings.LogViewerScreen
import com.kroslabs.recipemanager.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object RecipeList : Screen("recipes")
    data object RecipeDetail : Screen("recipe/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe/$recipeId"
    }
    data object AddRecipe : Screen("add_recipe")
    data object EditRecipe : Screen("edit_recipe/{recipeId}") {
        fun createRoute(recipeId: String) = "edit_recipe/$recipeId"
    }
    data object MealPlan : Screen("meal_plan")
    data object Settings : Screen("settings")
    data object LogViewer : Screen("log_viewer")
}

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int
) {
    data object Recipes : BottomNavItem(
        route = Screen.RecipeList.route,
        icon = Icons.Default.MenuBook,
        labelResId = R.string.nav_recipes
    )
    data object MealPlan : BottomNavItem(
        route = Screen.MealPlan.route,
        icon = Icons.Default.CalendarMonth,
        labelResId = R.string.nav_meal_plan
    )
    data object Settings : BottomNavItem(
        route = Screen.Settings.route,
        icon = Icons.Default.Settings,
        labelResId = R.string.nav_settings
    )
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val bottomNavItems = listOf(
        BottomNavItem.Recipes,
        BottomNavItem.MealPlan,
        BottomNavItem.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelResId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.RecipeList.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.RecipeList.route) {
                RecipeListScreen(
                    onRecipeClick = { recipeId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                    },
                    onAddRecipeClick = {
                        navController.navigate(Screen.AddRecipe.route)
                    }
                )
            }

            composable(
                route = Screen.RecipeDetail.route,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                RecipeDetailScreen(
                    recipeId = recipeId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditClick = { navController.navigate(Screen.EditRecipe.createRoute(recipeId)) }
                )
            }

            composable(Screen.AddRecipe.route) {
                AddRecipeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRecipeSaved = { recipeId ->
                        navController.popBackStack()
                        navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                    }
                )
            }

            composable(
                route = Screen.EditRecipe.route,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                AddRecipeScreen(
                    recipeId = recipeId,
                    onNavigateBack = { navController.popBackStack() },
                    onRecipeSaved = { navController.popBackStack() }
                )
            }

            composable(Screen.MealPlan.route) {
                MealPlanScreen(
                    onRecipeClick = { recipeId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToLogs = {
                        navController.navigate(Screen.LogViewer.route)
                    }
                )
            }

            composable(Screen.LogViewer.route) {
                LogViewerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
