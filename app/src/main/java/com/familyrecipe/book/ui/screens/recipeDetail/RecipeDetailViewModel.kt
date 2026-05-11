package com.familyrecipe.book.ui.screens.recipeDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.model.FamilyMember
import com.familyrecipe.book.data.model.Preference
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipePreference
import com.familyrecipe.book.data.repository.FamilyMemberRepository
import com.familyrecipe.book.data.repository.RecipeRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val steps: List<String> = emptyList(),
    val members: List<FamilyMember> = emptyList(),
    val preferences: List<RecipePreference> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipeRepository: RecipeRepository,
    private val memberRepository: FamilyMemberRepository
) : ViewModel() {

    private val recipeId: Long = savedStateHandle.get<Long>("recipeId") ?: 0L

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val recipe = recipeRepository.getRecipeById(recipeId)
            if (recipe != null) {
                val steps: List<String> = try {
                    gson.fromJson(recipe.stepsJson, object : TypeToken<List<String>>() {}.type)
                } catch (e: Exception) {
                    if (recipe.stepsJson.isNotBlank()) listOf(recipe.stepsJson) else emptyList()
                }
                _uiState.update { it.copy(recipe = recipe, steps = steps) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }

        viewModelScope.launch {
            memberRepository.getAllMembers().collect { members ->
                _uiState.update { it.copy(members = members) }
            }
        }

        viewModelScope.launch {
            recipeRepository.getPreferencesForRecipe(recipeId).collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    fun setPreference(memberId: Long, preference: Preference) {
        viewModelScope.launch {
            recipeRepository.setPreference(recipeId, memberId, preference)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentRecipe = _uiState.value.recipe ?: return@launch
            val newFavorite = !currentRecipe.isFavorite
            recipeRepository.updateFavoriteStatus(currentRecipe.id, newFavorite)
            _uiState.update { it.copy(recipe = it.recipe?.copy(isFavorite = newFavorite)) }
        }
    }

    fun deleteRecipe(onDone: () -> Unit) {
        viewModelScope.launch {
            recipeRepository.deleteRecipeById(recipeId)
            onDone()
        }
    }
}
