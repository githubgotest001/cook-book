package com.familyrecipe.book.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.familyrecipe.book.ui.screens.memberEdit.MemberEditScreen
import com.familyrecipe.book.ui.screens.memberList.MemberListScreen
import com.familyrecipe.book.ui.screens.recipeDetail.RecipeDetailScreen
import com.familyrecipe.book.ui.screens.recipeEdit.RecipeEditScreen
import com.familyrecipe.book.ui.screens.randomPick.RandomPickScreen
import com.familyrecipe.book.ui.screens.recipeList.RecipeListScreen
import com.familyrecipe.book.ui.screens.settings.SettingsScreen
import com.familyrecipe.book.ui.screens.shoppingList.ShoppingListScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = NavRoutes.RECIPE_LIST) {

        composable(NavRoutes.RECIPE_LIST) {
            RecipeListScreen(
                onRecipeClick = { id -> navController.navigate(NavRoutes.recipeDetail(id)) },
                onAddClick = { navController.navigate(NavRoutes.recipeEdit(0)) },
                onEditRecipe = { id -> navController.navigate(NavRoutes.recipeEdit(id)) },
                onMembersClick = { navController.navigate(NavRoutes.MEMBER_LIST) },
                onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) },
                onShoppingListClick = { navController.navigate(NavRoutes.SHOPPING_LIST) },
                onRandomPickClick = { navController.navigate(NavRoutes.RANDOM_PICK) }
            )
        }

        composable(
            route = NavRoutes.RECIPE_DETAIL,
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId") ?: 0L
            RecipeDetailScreen(
                onEditClick = { navController.navigate(NavRoutes.recipeEdit(recipeId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.RECIPE_EDIT,
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType })
        ) {
            RecipeEditScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.MEMBER_LIST) {
            MemberListScreen(
                onAddClick = { navController.navigate(NavRoutes.memberEdit(0)) },
                onMemberClick = { id -> navController.navigate(NavRoutes.memberEdit(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.MEMBER_EDIT,
            arguments = listOf(navArgument("memberId") { type = NavType.LongType })
        ) {
            MemberEditScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.RANDOM_PICK) {
            RandomPickScreen(
                onRecipeClick = { id -> navController.navigate(NavRoutes.recipeDetail(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.SHOPPING_LIST) {
            ShoppingListScreen(onBack = { navController.popBackStack() })
        }
    }
}
