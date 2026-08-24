package com.familyrecipe.book.data.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.familyrecipe.book.data.dao.RecipeDao
import com.familyrecipe.book.data.dao.RecipeIngredientDao
import com.familyrecipe.book.data.dao.RecipePreferenceDao
import com.familyrecipe.book.data.model.Preference
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeCategory
import com.familyrecipe.book.data.model.RecipeFilter
import com.familyrecipe.book.data.model.RecipeIngredient
import com.familyrecipe.book.data.model.RecipePreference
import com.familyrecipe.book.data.model.SortConfig
import com.familyrecipe.book.data.model.SortDimension
import com.familyrecipe.book.data.model.SortOrder
import com.familyrecipe.book.util.ImageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RecipeRepository(
    private val database: RoomDatabase,
    private val recipeDao: RecipeDao,
    private val preferenceDao: RecipePreferenceDao,
    private val ingredientDao: RecipeIngredientDao
) {
    fun getAllRecipes(): Flow<List<Recipe>> = recipeDao.getAllRecipes()

    fun searchRecipes(query: String): Flow<List<Recipe>> = recipeDao.searchRecipes(query)

    suspend fun getRecipeById(id: Long): Recipe? = recipeDao.getRecipeById(id)

    fun getRecipeByIdFlow(id: Long): Flow<Recipe?> = recipeDao.getRecipeByIdFlow(id)

    suspend fun insertRecipe(recipe: Recipe): Long = recipeDao.insertRecipe(recipe)

    suspend fun updateRecipe(recipe: Recipe) = recipeDao.updateRecipe(recipe)

    fun getIngredientsForRecipe(recipeId: Long): Flow<List<RecipeIngredient>> =
        ingredientDao.getIngredientsForRecipe(recipeId)

    fun getIngredientsForRecipes(recipeIds: List<Long>): Flow<List<RecipeIngredient>> =
        if (recipeIds.isEmpty()) flowOf(emptyList()) else ingredientDao.getIngredientsForRecipes(recipeIds)

    /**
     * 菜谱主体与食材在同一个数据库事务内写入：
     * 任一步骤失败时整体回滚，不会出现"菜谱已更新但食材被清空"的中间状态。
     */
    suspend fun saveRecipeWithIngredients(recipe: Recipe, ingredients: List<RecipeIngredient>): Long {
        return database.withTransaction {
            val recipeId = if (recipe.id == 0L) {
                recipeDao.insertRecipe(recipe)
            } else {
                recipeDao.updateRecipe(recipe)
                recipe.id
            }
            ingredientDao.deleteIngredientsForRecipe(recipeId)
            val normalized = normalizeIngredients(ingredients, recipeId)
            if (normalized.isNotEmpty()) {
                ingredientDao.insertIngredients(normalized)
            }
            recipeId
        }
    }

    /**
     * 过滤空白食材并统一整理字段（trim、重排 displayOrder、绑定 recipeId）。
     */
    internal fun normalizeIngredients(
        ingredients: List<RecipeIngredient>,
        recipeId: Long
    ): List<RecipeIngredient> = ingredients
        .filter { it.name.isNotBlank() }
        .mapIndexed { index, ingredient ->
            ingredient.copy(
                id = 0,
                recipeId = recipeId,
                name = ingredient.name.trim(),
                amount = ingredient.amount.trim(),
                unit = ingredient.unit.trim(),
                note = ingredient.note.trim(),
                displayOrder = index
            )
        }

    /**
     * 删除菜谱，同时清理其封面图片文件，避免遗留孤儿文件
     */
    suspend fun deleteRecipeById(id: Long) {
        val coverPath = recipeDao.getRecipeById(id)?.coverImagePath
        recipeDao.deleteRecipeById(id)
        coverPath?.let { ImageUtils.deleteImage(it) }
    }

    // 喜好相关
    fun getPreferencesForRecipe(recipeId: Long): Flow<List<RecipePreference>> =
        preferenceDao.getPreferencesForRecipe(recipeId)

    fun getRecipeIdsByMemberPreference(memberId: Long, preference: Preference): Flow<List<Long>> =
        preferenceDao.getRecipeIdsByMemberPreference(memberId, preference)

    suspend fun setPreference(recipeId: Long, memberId: Long, preference: Preference) {
        preferenceDao.upsertPreference(
            RecipePreference(
                recipeId = recipeId,
                memberId = memberId,
                preference = preference,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removePreference(recipeId: Long, memberId: Long) {
        preferenceDao.deletePreference(recipeId, memberId)
    }

    /**
     * 按分类获取菜谱列表
     */
    fun getRecipesByCategory(category: RecipeCategory): Flow<List<Recipe>> =
        recipeDao.getRecipesByCategory(category.name)

    /**
     * 原子翻转收藏状态（单条 SQL，无读写竞态）
     */
    suspend fun toggleFavorite(id: Long) {
        recipeDao.toggleFavorite(id)
    }

    /**
     * 获取所有指定成员都喜欢的菜谱 ID 列表
     * 返回的是被所有 memberIds 中的成员都标记为 LIKE 的菜谱 ID
     */
    fun getRecipesLikedByMembers(memberIds: List<Long>): Flow<List<Long>> {
        if (memberIds.isEmpty()) return flowOf(emptyList())
        return preferenceDao.getRecipeIdsLikedByAllMembers(memberIds, memberIds.size)
    }

    /**
     * 组合筛选与排序查询
     * 根据 RecipeFilter 和 SortConfig 返回经过筛选和排序的菜谱列表
     * 收藏菜谱始终置顶（isFavorite = true 的排在前面）
     */
    @Suppress("OPT_IN_USAGE")
    fun getFilteredAndSortedRecipes(
        filter: RecipeFilter,
        sortConfig: SortConfig
    ): Flow<List<Recipe>> {
        // 1. 基础数据源：根据是否有搜索关键词决定
        val baseRecipesFlow: Flow<List<Recipe>> = if (filter.searchQuery.isBlank()) {
            recipeDao.getAllRecipes()
        } else {
            recipeDao.searchRecipes(filter.searchQuery.trim())
        }

        // 2. 获取成员偏好筛选的菜谱 ID 集合
        val likedIdsFlow: Flow<List<Long>?> = if (filter.selectedMemberIds.isNotEmpty()) {
            getRecipesLikedByMembers(filter.selectedMemberIds.toList()).map { it as List<Long>? }
        } else {
            flowOf(null) // null 表示不进行成员偏好筛选
        }

        // 3. 组合基础数据源和偏好筛选
        return combine(baseRecipesFlow, likedIdsFlow) { recipes, likedIds ->
            var result = recipes

            // 按成员偏好筛选（交集）
            if (likedIds != null) {
                val likedIdSet = likedIds.toSet()
                result = result.filter { it.id in likedIdSet }
            }

            // 按分类筛选
            if (filter.selectedCategory != null) {
                result = result.filter { it.category == filter.selectedCategory.name }
            }

            // 排序：收藏置顶 + 维度排序
            result = applySortingWithFavoriteFirst(result, sortConfig)

            result
        }
    }

    /**
     * 应用排序逻辑：收藏菜谱始终置顶，组内按指定维度和方向排序
     */
    private fun applySortingWithFavoriteFirst(
        recipes: List<Recipe>,
        sortConfig: SortConfig
    ): List<Recipe> {
        val comparator = buildSortComparator(sortConfig)

        // 分为收藏组和非收藏组，各自排序后合并
        val favorites = recipes.filter { it.isFavorite }.sortedWith(comparator)
        val nonFavorites = recipes.filter { !it.isFavorite }.sortedWith(comparator)

        return favorites + nonFavorites
    }

    /**
     * 根据排序配置构建比较器
     */
    private fun buildSortComparator(sortConfig: SortConfig): Comparator<Recipe> {
        val baseComparator: Comparator<Recipe> = when (sortConfig.dimension) {
            SortDimension.UPDATED_AT -> compareBy { it.updatedAt }
            SortDimension.CREATED_AT -> compareBy { it.createdAt }
            SortDimension.COOKING_MINUTES -> compareBy { it.cookingMinutes }
            SortDimension.DIFFICULTY -> compareBy { it.difficulty }
            SortDimension.RECOMMENDATION -> compareBy { it.recommendationIndex }
        }

        return if (sortConfig.order == SortOrder.DESC) {
            baseComparator.reversed()
        } else {
            baseComparator
        }
    }
}
