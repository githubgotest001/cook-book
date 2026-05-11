package com.familyrecipe.book.ui.screens.recipeEdit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeCategory
import com.familyrecipe.book.data.repository.RecipeRepository
import com.familyrecipe.book.util.ImageUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeEditUiState(
    val name: String = "",
    val description: String = "",
    val steps: List<String> = listOf(""),
    val cookingMinutes: String = "",
    val difficulty: Int = 3,
    val category: String = "",
    val coverImagePath: String? = null,
    val recommendationIndex: Int = 3,
    val selectedCategory: RecipeCategory? = null,
    val categoryError: Boolean = false,
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false
)

@HiltViewModel
class RecipeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
    private val application: Application
) : ViewModel() {

    private val recipeId: Long = savedStateHandle.get<Long>("recipeId") ?: 0L

    private val _uiState = MutableStateFlow(RecipeEditUiState())
    val uiState: StateFlow<RecipeEditUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        if (recipeId > 0) {
            _uiState.update { it.copy(isLoading = true, isEditMode = true) }
            viewModelScope.launch {
                val recipe = repository.getRecipeById(recipeId)
                if (recipe != null) {
                    val steps: List<String> = try {
                        gson.fromJson(recipe.stepsJson, object : TypeToken<List<String>>() {}.type)
                    } catch (e: Exception) {
                        listOf(recipe.stepsJson)
                    }
                    _uiState.update {
                        it.copy(
                            name = recipe.name,
                            description = recipe.description,
                            steps = steps.ifEmpty { listOf("") },
                            cookingMinutes = recipe.cookingMinutes.toString(),
                            difficulty = recipe.difficulty,
                            category = recipe.category,
                            coverImagePath = recipe.coverImagePath,
                            recommendationIndex = recipe.recommendationIndex,
                            selectedCategory = recipe.recipeCategory,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onCookingMinutesChange(value: String) = _uiState.update { it.copy(cookingMinutes = value) }
    fun onDifficultyChange(value: Int) = _uiState.update { it.copy(difficulty = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }

    /**
     * 处理图片选择（从相册或相机获取）
     * 如果已有封面图片，先删除旧图片再保存新图片
     */
    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            // 删除旧图片
            val oldPath = _uiState.value.coverImagePath
            if (oldPath != null) {
                ImageUtils.deleteImage(oldPath)
            }
            // 保存新图片
            try {
                val newPath = ImageUtils.saveImage(application, uri)
                _uiState.update { it.copy(coverImagePath = newPath) }
            } catch (e: Exception) {
                // 图片保存失败，不更新路径
            }
        }
    }

    /**
     * 移除当前封面图片
     */
    fun onImageRemoved() {
        val oldPath = _uiState.value.coverImagePath
        if (oldPath != null) {
            ImageUtils.deleteImage(oldPath)
        }
        _uiState.update { it.copy(coverImagePath = null) }
    }

    /**
     * 选择菜谱分类
     */
    fun onCategorySelected(category: RecipeCategory) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                category = category.name,
                categoryError = false
            )
        }
    }

    /**
     * 设置推荐指数（1-5）
     */
    fun onRecommendationChange(index: Int) {
        val clampedIndex = index.coerceIn(1, 5)
        _uiState.update { it.copy(recommendationIndex = clampedIndex) }
    }

    fun onStepChange(index: Int, value: String) {
        _uiState.update {
            val newSteps = it.steps.toMutableList()
            newSteps[index] = value
            it.copy(steps = newSteps)
        }
    }

    fun addStep() {
        _uiState.update { it.copy(steps = it.steps + "") }
    }

    fun removeStep(index: Int) {
        _uiState.update {
            if (it.steps.size > 1) {
                it.copy(steps = it.steps.toMutableList().apply { removeAt(index) })
            } else {
                it
            }
        }
    }

    fun save(onDone: () -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank()) return

        // 分类必填验证
        if (state.selectedCategory == null) {
            _uiState.update { it.copy(categoryError = true) }
            return
        }

        viewModelScope.launch {
            val stepsJson = gson.toJson(state.steps.filter { it.isNotBlank() })
            val minutes = state.cookingMinutes.toIntOrNull() ?: 0
            val now = System.currentTimeMillis()

            if (recipeId > 0) {
                val existing = repository.getRecipeById(recipeId) ?: return@launch
                repository.updateRecipe(
                    existing.copy(
                        name = state.name,
                        description = state.description,
                        stepsJson = stepsJson,
                        cookingMinutes = minutes,
                        difficulty = state.difficulty,
                        category = state.selectedCategory.name,
                        coverImagePath = state.coverImagePath,
                        recommendationIndex = state.recommendationIndex,
                        updatedAt = now
                    )
                )
            } else {
                repository.insertRecipe(
                    Recipe(
                        name = state.name,
                        description = state.description,
                        stepsJson = stepsJson,
                        cookingMinutes = minutes,
                        difficulty = state.difficulty,
                        category = state.selectedCategory.name,
                        coverImagePath = state.coverImagePath,
                        recommendationIndex = state.recommendationIndex,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            onDone()
        }
    }
}
