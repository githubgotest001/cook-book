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

    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeRecipeById(id: Long): Flow<Recipe?>

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

    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE recipes SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavoriteStatus(id: Long)

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
