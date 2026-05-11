package com.familyrecipe.book.ui.screens.recipeList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.datastore.SettingsStore
import com.familyrecipe.book.data.model.FamilyMember
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeCategory
import com.familyrecipe.book.data.model.RecipeFilter
import com.familyrecipe.book.data.model.SortConfig
import com.familyrecipe.book.data.model.SortDimension
import com.familyrecipe.book.data.model.SortOrder
import com.familyrecipe.book.data.repository.FamilyMemberRepository
import com.familyrecipe.book.data.repository.RecipeRepository
import com.familyrecipe.book.domain.RandomSelectionResult
import com.familyrecipe.book.domain.RandomSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeListUiState(
    val recipes: List<Recipe> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val sortConfig: SortConfig = SortConfig(),
    val recipeFilter: RecipeFilter = RecipeFilter(),
    val selectedCategory: RecipeCategory? = null,
    val familyMembers: List<FamilyMember> = emptyList(),
    val randomResult: RandomSelectionResult? = null,
    val showRandomResult: Boolean = false
)

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val repository: RecipeRepository,
    private val familyMemberRepository: FamilyMemberRepository,
    private val settingsStore: SettingsStore,
    private val randomSelector: RandomSelector
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeListUiState())
    val uiState: StateFlow<RecipeListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _sortConfig = MutableStateFlow(SortConfig())
    private val _recipeFilter = MutableStateFlow(RecipeFilter())

    init {
        // 加载家庭成员列表
        viewModelScope.launch {
            familyMemberRepository.getAllMembers().collect { members ->
                _uiState.update { it.copy(familyMembers = members) }
            }
        }

        // 组合筛选与排序：监听搜索、筛选条件和排序配置的变化
        viewModelScope.launch {
            combine(
                _searchQuery.debounce(300),
                _recipeFilter,
                _sortConfig
            ) { query, filter, sort ->
                // 将搜索关键词同步到 filter 中
                Triple(query, filter.copy(searchQuery = query), sort)
            }.collectLatest { (_, filter, sort) ->
                repository.getFilteredAndSortedRecipes(filter, sort).collect { recipes ->
                    _uiState.update {
                        it.copy(
                            recipes = recipes,
                            isLoading = false,
                            sortConfig = sort,
                            recipeFilter = filter,
                            selectedCategory = filter.selectedCategory
                        )
                    }
                }
            }
        }
    }

    /**
     * 更新搜索关键词
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * 切换排序维度
     * 如果选择的是当前已激活的维度，则切换升降序；否则切换到新维度并使用默认降序
     */
    fun onSortDimensionChange(dimension: SortDimension) {
        _sortConfig.update { current ->
            if (current.dimension == dimension) {
                // 同一维度：切换升降序
                current.copy(
                    order = if (current.order == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
                )
            } else {
                // 新维度：默认降序
                SortConfig(dimension = dimension, order = SortOrder.DESC)
            }
        }
    }

    /**
     * 切换当前排序方向（升序/降序）
     */
    fun onSortOrderToggle() {
        _sortConfig.update { current ->
            current.copy(
                order = if (current.order == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
            )
        }
    }

    /**
     * 切换成员偏好筛选
     * 如果成员已选中则取消选中，否则添加到选中集合
     */
    fun onMemberFilterToggle(memberId: Long) {
        _recipeFilter.update { current ->
            val updatedIds = current.selectedMemberIds.toMutableSet()
            if (memberId in updatedIds) {
                updatedIds.remove(memberId)
            } else {
                updatedIds.add(memberId)
            }
            current.copy(selectedMemberIds = updatedIds)
        }
    }

    /**
     * 切换分类筛选
     * 如果选择的是当前已激活的分类，则取消筛选；否则设置为新分类
     */
    fun onCategoryFilterChange(category: RecipeCategory?) {
        _recipeFilter.update { current ->
            if (current.selectedCategory == category) {
                // 再次点击同一分类：取消筛选
                current.copy(selectedCategory = null)
            } else {
                current.copy(selectedCategory = category)
            }
        }
    }

    /**
     * 清除所有筛选条件（搜索、成员偏好、分类）
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _recipeFilter.value = RecipeFilter()
        _uiState.update { it.copy(searchQuery = "") }
    }

    /**
     * 触发随机选菜
     * 使用 SettingsStore 中配置的默认数量
     */
    fun triggerRandomPick() {
        viewModelScope.launch {
            val count = settingsStore.defaultRandomCount.first()
            val currentRecipes = _uiState.value.recipes
            val result = randomSelector.selectRandom(currentRecipes, count)
            _uiState.update {
                it.copy(randomResult = result, showRandomResult = true)
            }
        }
    }

    /**
     * 换一批：以相同数量重新随机选择
     */
    fun refreshRandom() {
        viewModelScope.launch {
            val currentResult = _uiState.value.randomResult ?: return@launch
            val count = currentResult.selected.size.coerceIn(1, 10)
            val currentRecipes = _uiState.value.recipes
            val result = randomSelector.selectRandom(currentRecipes, count.coerceAtLeast(1))
            _uiState.update {
                it.copy(randomResult = result)
            }
        }
    }

    /**
     * 关闭随机选菜结果展示
     */
    fun dismissRandomResult() {
        _uiState.update {
            it.copy(showRandomResult = false, randomResult = null)
        }
    }

    /**
     * 切换菜谱收藏状态
     */
    fun toggleFavorite(recipeId: Long) {
        viewModelScope.launch {
            val recipe = repository.getRecipeById(recipeId) ?: return@launch
            repository.updateFavoriteStatus(recipeId, !recipe.isFavorite)
        }
    }

    /**
     * 删除菜谱
     */
    fun deleteRecipe(id: Long) {
        viewModelScope.launch {
            repository.deleteRecipeById(id)
        }
    }
}
