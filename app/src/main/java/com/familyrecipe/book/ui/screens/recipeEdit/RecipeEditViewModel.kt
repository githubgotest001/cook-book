package com.familyrecipe.book.ui.screens.recipeEdit

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeCategory
import com.familyrecipe.book.data.model.RecipeIngredient
import com.familyrecipe.book.data.repository.RecipeRepository
import com.familyrecipe.book.util.ImageUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeEditUiState(
    val name: String = "",
    val description: String = "",
    val steps: List<String> = listOf(""),
    val cookingMinutes: String = "15",
    val difficulty: Int = 2,
    val category: String = RecipeCategory.STIR_FRY.name,
    val coverImagePath: String? = null,
    val recommendationIndex: Int = 4,
    val ingredients: List<IngredientInput> = listOf(IngredientInput()),
    val selectedCategory: RecipeCategory? = RecipeCategory.STIR_FRY,
    val categoryError: Boolean = false,
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    /** 图片保存失败的提示消息，展示后由 UI 调用 onImageErrorShown() 清除 */
    val imageError: String? = null
)

data class IngredientInput(
    val name: String = "",
    val amount: String = "1",
    val unit: String = "个",
    val note: String = ""
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

    /** 数据库中已持久化的封面路径（编辑模式下加载），保存成功且被替换时才删除 */
    private var persistedCoverPath: String? = null

    /** 本次编辑会话中新生成的图片文件，未保存退出时全部清理 */
    private val sessionImages = mutableSetOf<String>()

    private var saved = false

    init {
        if (recipeId > 0) {
            _uiState.update { it.copy(isLoading = true, isEditMode = true) }
            viewModelScope.launch {
                val recipe = repository.getRecipeById(recipeId)
                if (recipe != null) {
                    persistedCoverPath = recipe.coverImagePath
                    val steps: List<String> = try {
                        gson.fromJson(recipe.stepsJson, object : TypeToken<List<String>>() {}.type)
                    } catch (e: Exception) {
                        listOf(recipe.stepsJson)
                    }
                    val ingredients = repository.getIngredientsForRecipe(recipeId).first()
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
                            ingredients = ingredients.map {
                                IngredientInput(
                                    name = it.name,
                                    amount = it.amount,
                                    unit = it.unit,
                                    note = it.note
                                )
                            }.ifEmpty { listOf(IngredientInput()) },
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
     * 处理图片选择（从相册或相机获取）。
     * 仅保存新图片并更新预览，旧图片的删除推迟到保存成功之后，
     * 避免用户放弃编辑时已持久化的封面被误删。
     */
    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                val newPath = ImageUtils.saveImage(application, uri)
                sessionImages.add(newPath)
                _uiState.update { it.copy(coverImagePath = newPath) }
            } catch (e: Exception) {
                Log.e(TAG, "保存封面图片失败: $uri", e)
                _uiState.update {
                    it.copy(imageError = "图片处理失败，请换一张试试")
                }
            }
        }
    }

    /** UI 展示过错误提示后调用，清除错误状态 */
    fun onImageErrorShown() {
        _uiState.update { it.copy(imageError = null) }
    }

    /**
     * 移除当前封面图片（仅更新状态，文件清理在保存/退出时统一处理）
     */
    fun onImageRemoved() {
        _uiState.update { it.copy(coverImagePath = null) }
    }

    /**
     * 保存成功后清理无用图片文件：
     * - 本次会话产生但最终未采用的新图
     * - 被替换或移除的旧封面
     */
    private fun cleanupImagesAfterSave(finalPath: String?) {
        sessionImages.filter { it != finalPath }.forEach { ImageUtils.deleteImage(it) }
        sessionImages.clear()
        persistedCoverPath?.let { old ->
            if (old != finalPath) {
                ImageUtils.deleteImage(old)
            }
        }
        persistedCoverPath = finalPath
    }

    override fun onCleared() {
        // 未保存就退出：清理本次会话产生的所有新图片
        if (!saved) {
            sessionImages.forEach { ImageUtils.deleteImage(it) }
        }
        super.onCleared()
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

    fun onIngredientNameChange(index: Int, value: String) {
        updateIngredient(index) { it.copy(name = value) }
    }

    fun onIngredientAmountChange(index: Int, value: String) {
        updateIngredient(index) { it.copy(amount = value) }
    }

    fun onIngredientUnitChange(index: Int, value: String) {
        updateIngredient(index) { it.copy(unit = value) }
    }

    fun onIngredientNoteChange(index: Int, value: String) {
        updateIngredient(index) { it.copy(note = value) }
    }

    fun addIngredient() {
        _uiState.update { it.copy(ingredients = it.ingredients + IngredientInput()) }
    }

    fun removeIngredient(index: Int) {
        _uiState.update {
            if (it.ingredients.size > 1) {
                it.copy(ingredients = it.ingredients.toMutableList().apply { removeAt(index) })
            } else {
                it.copy(ingredients = listOf(IngredientInput()))
            }
        }
    }

    private fun updateIngredient(index: Int, transform: (IngredientInput) -> IngredientInput) {
        _uiState.update {
            val ingredients = it.ingredients.toMutableList()
            ingredients[index] = transform(ingredients[index])
            it.copy(ingredients = ingredients)
        }
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
            val ingredients = state.ingredients
                .filter { it.name.isNotBlank() }
                .mapIndexed { index, ingredient ->
                    RecipeIngredient(
                        recipeId = recipeId,
                        name = ingredient.name,
                        amount = ingredient.amount,
                        unit = ingredient.unit,
                        note = ingredient.note,
                        displayOrder = index
                    )
                }

            if (recipeId > 0) {
                val existing = repository.getRecipeById(recipeId) ?: return@launch
                repository.saveRecipeWithIngredients(
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
                    ),
                    ingredients
                )
            } else {
                repository.saveRecipeWithIngredients(
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
                    ),
                    ingredients
                )
            }
            saved = true
            cleanupImagesAfterSave(state.coverImagePath)
            onDone()
        }
    }

    companion object {
        private const val TAG = "RecipeEditViewModel"
    }
}
