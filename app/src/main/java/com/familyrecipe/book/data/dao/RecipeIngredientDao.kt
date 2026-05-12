package com.familyrecipe.book.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.familyrecipe.book.data.model.RecipeIngredient
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeIngredientDao {

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY displayOrder ASC, id ASC")
    fun getIngredientsForRecipe(recipeId: Long): Flow<List<RecipeIngredient>>

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId IN (:recipeIds) ORDER BY recipeId ASC, displayOrder ASC, id ASC")
    fun getIngredientsForRecipes(recipeIds: List<Long>): Flow<List<RecipeIngredient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<RecipeIngredient>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsForRecipe(recipeId: Long)
}
