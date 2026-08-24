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
import kotlinx.coroutines.flow.*
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

    private val _uiState = MutableStateFlow(RecipeDetailUiState())
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        loadData()
    }

    private fun loadData() {
        // 以 Flow 观察菜谱主体：编辑页保存后，详情页自动收到最新数据，无需手动刷新
        viewModelScope.launch {
            recipeRepository.getRecipeByIdFlow(recipeId).collect { recipe ->
                _uiState.update {
                    it.copy(recipe = recipe, steps = parseSteps(recipe), isLoading = false)
                }
            }
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

        viewModelScope.launch {
            recipeRepository.getIngredientsForRecipe(recipeId).collect { ingredients ->
                _uiState.update { it.copy(ingredients = ingredients) }
            }
        }
    }

    private fun parseSteps(recipe: Recipe?): List<String> {
        if (recipe == null) return emptyList()
        return try {
            gson.fromJson(recipe.stepsJson, object : TypeToken<List<String>>() {}.type)
        } catch (e: Exception) {
            if (recipe.stepsJson.isNotBlank()) listOf(recipe.stepsJson) else emptyList()
        }
    }

    fun setPreference(memberId: Long, preference: Preference) {
        viewModelScope.launch {
            recipeRepository.setPreference(recipeId, memberId, preference)
        }
    }

    /**
     * 数据库端原子翻转收藏状态；UI 通过 recipe Flow 自动刷新，无需手动同步状态。
     */
    fun toggleFavorite() {
        viewModelScope.launch {
            recipeRepository.toggleFavorite(recipeId)
        }
    }

    fun deleteRecipe(onDone: () -> Unit) {
        viewModelScope.launch {
            recipeRepository.deleteRecipeById(recipeId)
            onDone()
        }
    }
}
