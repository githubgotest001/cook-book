package com.familyrecipe.book.ui.screens.shoppingList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeIngredient
import com.familyrecipe.book.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShoppingListItem(
    val name: String,
    val details: List<String>,
    val recipeNames: List<String>
)

data class ShoppingListUiState(
    val recipes: List<Recipe> = emptyList(),
    val selectedRecipeIds: Set<Long> = emptySet(),
    val shoppingItems: List<ShoppingListItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    private var ingredientJob: Job? = null

    init {
        viewModelScope.launch {
            recipeRepository.getAllRecipes().collect { recipes ->
                _uiState.update {
                    it.copy(
                        recipes = recipes,
                        selectedRecipeIds = it.selectedRecipeIds.intersect(recipes.map { recipe -> recipe.id }.toSet()),
                        isLoading = false
                    )
                }
                rebuildShoppingList()
            }
        }
    }

    fun toggleRecipe(recipeId: Long) {
        _uiState.update { state ->
            val selected = state.selectedRecipeIds.toMutableSet()
            if (recipeId in selected) selected.remove(recipeId) else selected.add(recipeId)
            state.copy(selectedRecipeIds = selected)
        }
        rebuildShoppingList()
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedRecipeIds = emptySet(), shoppingItems = emptyList()) }
        ingredientJob?.cancel()
    }

    private fun rebuildShoppingList() {
        ingredientJob?.cancel()
        val state = _uiState.value
        val selectedIds = state.selectedRecipeIds.toList()
        if (selectedIds.isEmpty()) {
            _uiState.update { it.copy(shoppingItems = emptyList()) }
            return
        }

        ingredientJob = viewModelScope.launch {
            recipeRepository.getIngredientsForRecipes(selectedIds).collect { ingredients ->
                val recipeNamesById = _uiState.value.recipes.associate { it.id to it.name }
                _uiState.update {
                    it.copy(shoppingItems = ingredients.toShoppingItems(recipeNamesById))
                }
            }
        }
    }

    private fun List<RecipeIngredient>.toShoppingItems(recipeNamesById: Map<Long, String>): List<ShoppingListItem> {
        return filter { it.name.isNotBlank() }
            .groupBy { it.name.trim().lowercase() }
            .map { (_, group) ->
                val first = group.first()
                ShoppingListItem(
                    name = first.name.trim(),
                    details = group.map { ingredient ->
                        listOf(ingredient.amount + ingredient.unit, ingredient.note)
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                    }.filter { it.isNotBlank() }.distinct(),
                    recipeNames = group.mapNotNull { recipeNamesById[it.recipeId] }.distinct()
                )
            }
            .sortedBy { it.name }
    }
}
