package com.familyrecipe.book.data.dao

import androidx.room.*
import com.familyrecipe.book.data.model.Preference
import com.familyrecipe.book.data.model.RecipePreference
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipePreferenceDao {

    @Query("SELECT * FROM recipe_preferences WHERE recipeId = :recipeId")
    fun getPreferencesForRecipe(recipeId: Long): Flow<List<RecipePreference>>

    @Query("SELECT * FROM recipe_preferences WHERE memberId = :memberId")
    fun getPreferencesForMember(memberId: Long): Flow<List<RecipePreference>>

    @Query(
        """
        SELECT DISTINCT recipeId FROM recipe_preferences 
        WHERE memberId = :memberId AND preference = :preference
        """
    )
    fun getRecipeIdsByMemberPreference(memberId: Long, preference: Preference): Flow<List<Long>>

    @Query(
        """
        SELECT DISTINCT rp.recipeId FROM recipe_preferences rp
        WHERE rp.memberId IN (:memberIds) AND rp.preference = 'LIKE'
        GROUP BY rp.recipeId
        HAVING COUNT(DISTINCT rp.memberId) = :memberCount
        """
    )
    fun getRecipeIdsLikedByAllMembers(memberIds: List<Long>, memberCount: Int): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreference(preference: RecipePreference)

    @Query("DELETE FROM recipe_preferences WHERE recipeId = :recipeId AND memberId = :memberId")
    suspend fun deletePreference(recipeId: Long, memberId: Long)
}
