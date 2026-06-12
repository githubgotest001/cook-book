package com.familyrecipe.book.ui.screens.randomPick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.datastore.SettingsStore
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeCategory
import com.familyrecipe.book.data.repository.RecipeRepository
import com.familyrecipe.book.domain.RandomSelector
import com.familyrecipe.book.domain.RandomWarning
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 随机选菜页面的 ViewModel。
 * 管理随机选择状态，支持初始选择、换一批和自定义数量。
 */
@HiltViewModel
class RandomPickViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val settingsStore: SettingsStore,
    private val randomSelector: RandomSelector
) : ViewModel() {

    data class UiState(
        val selectedRecipes: List<Recipe> = emptyList(),
        val currentCount: Int = 3,
        val warning: RandomWarning? = null,
        val isLoading: Boolean = true,
        val allRecipes: List<Recipe> = emptyList(),
        val selectedCategory: RecipeCategory? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 获取默认随机数量
            val defaultCount = settingsStore.defaultRandomCount.first()
            // 获取所有菜谱
            recipeRepository.getAllRecipes().collect { recipes ->
                _uiState.update { state ->
                    val count = if (state.isLoading) defaultCount else state.currentCount
                    state.copy(
                        allRecipes = recipes,
                        currentCount = count
                    )
                }
                // 首次加载时执行初始随机选择
                if (_uiState.value.isLoading) {
                    performSelection()
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    /**
     * "换一批"：使用当前数量重新随机选择
     */
    fun refreshRandom() {
        performSelection()
    }

    /**
     * 设置自定义数量并重新选择。
     * @param count 数量，范围 1-10
     */
    fun setCount(count: Int) {
        val validCount = count.coerceIn(1, 10)
        _uiState.update { it.copy(currentCount = validCount) }
        performSelection()
    }

    /**
     * 设置分类过滤并重新选择。再次点击同一分类则取消过滤。
     * @param category 分类，null 表示不限分类
     */
    fun setCategory(category: RecipeCategory?) {
        _uiState.update {
            val newCategory = if (it.selectedCategory == category) null else category
            it.copy(selectedCategory = newCategory)
        }
        performSelection()
    }

    /**
     * 执行随机选择，更新 UI 状态
     */
    private fun performSelection() {
        val state = _uiState.value
        val pool = state.selectedCategory?.let { category ->
            state.allRecipes.filter { it.category == category.name }
        } ?: state.allRecipes
        val result = randomSelector.selectRandom(
            recipes = pool,
            count = state.currentCount
        )
        _uiState.update {
            it.copy(
                selectedRecipes = result.selected,
                warning = result.warning
            )
        }
    }
}
