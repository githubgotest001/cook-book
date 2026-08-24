package com.familyrecipe.book.data.dao

import androidx.room.*
import com.familyrecipe.book.data.model.Recipe
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY updatedAt DESC")
    fun getAllRecipes(): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeById(id: Long): Recipe?

    /**
     * 以 Flow 形式观察单个菜谱，编辑保存后详情页可自动收到最新数据。
     */
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun getRecipeByIdFlow(id: Long): Flow<Recipe?>

    @Query(
        """
        SELECT * FROM recipes 
        WHERE name LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%'
        OR stepsJson LIKE '%' || :query || '%'
        OR id IN (
            SELECT recipeId FROM recipe_ingredients 
            WHERE name LIKE '%' || :query || '%'
        )
        ORDER BY updatedAt DESC
        """
    )
    fun searchRecipes(query: String): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE category = :category ORDER BY updatedAt DESC")
    fun getRecipesByCategory(category: String): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id IN (:ids) ORDER BY updatedAt DESC")
    fun getRecipesByIds(ids: List<Long>): Flow<List<Recipe>>

    /**
     * 单条 SQL 原子翻转收藏状态，避免"读取-取反-写回"竞态。
     */
    @Query("UPDATE recipes SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("SELECT * FROM recipes ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllRecipesWithFavoriteFirst(): Flow<List<Recipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Long)
}
