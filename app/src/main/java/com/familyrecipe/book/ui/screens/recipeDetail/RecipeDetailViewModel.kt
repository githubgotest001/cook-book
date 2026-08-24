package com.familyrecipe.book.ui.screens.recipeDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.model.FamilyMember
import com.familyrecipe.book.data.model.Preference
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeIngredient
import com.familyrecipe.book.data.model.RecipePreference
import com.familyrecipe.book.data.repository.FamilyMemberRepository
import com.familyrecipe.book.data.repository.RecipeRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeDetailUiState(
    val recipe: Recipe? = null,
    val steps: List<String> = emptyList(),
    val ingredients: List<RecipeIngredient> = emptyList(),
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
    private val gson = Gson()

    /**
     * 观察式加载：编辑页返回后同一 ViewModel 仍在，Flow 会自动刷新菜谱主体。
     */
    val uiState: StateFlow<RecipeDetailUiState> = combine(
        recipeRepository.observeRecipeById(recipeId),
        recipeRepository.getIngredientsForRecipe(recipeId),
        recipeRepository.getPreferencesForRecipe(recipeId),
        memberRepository.getAllMembers()
    ) { recipe, ingredients, preferences, members ->
        val steps: List<String> = if (recipe == null) {
            emptyList()
        } else {
            try {
                gson.fromJson(recipe.stepsJson, object : TypeToken<List<String>>() {}.type)
                    ?: emptyList()
            } catch (_: Exception) {
                if (recipe.stepsJson.isNotBlank()) listOf(recipe.stepsJson) else emptyList()
            }
        }
        RecipeDetailUiState(
            recipe = recipe,
            steps = steps,
            ingredients = ingredients,
            members = members,
            preferences = preferences,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecipeDetailUiState()
    )

    fun setPreference(memberId: Long, preference: Preference) {
        viewModelScope.launch {
            recipeRepository.setPreference(recipeId, memberId, preference)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            if (uiState.value.recipe == null) return@launch
            recipeRepository.toggleFavoriteStatus(recipeId)
        }
    }

    fun deleteRecipe(onDone: () -> Unit) {
        viewModelScope.launch {
            recipeRepository.deleteRecipeById(recipeId)
            onDone()
        }
    }
}
