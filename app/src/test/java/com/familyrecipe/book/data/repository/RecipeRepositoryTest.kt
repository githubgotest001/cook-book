package com.familyrecipe.book.data.repository

import com.familyrecipe.book.data.dao.RecipeDao
import com.familyrecipe.book.data.dao.RecipePreferenceDao
import com.familyrecipe.book.data.model.Preference
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipePreference
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RecipeRepositoryTest {

    private lateinit var recipeDao: RecipeDao
    private lateinit var preferenceDao: RecipePreferenceDao
    private lateinit var repository: RecipeRepository

    @Before
    fun setup() {
        recipeDao = mockk(relaxed = true)
        preferenceDao = mockk(relaxed = true)
        repository = RecipeRepository(recipeDao, preferenceDao)
    }

    // ===== 插入操作 =====

    @Test
    fun `insertRecipe delegates to RecipeDao and returns id`() = runTest {
        val recipe = createTestRecipe(name = "红烧肉")
        coEvery { recipeDao.insertRecipe(recipe) } returns 1L

        val result = repository.insertRecipe(recipe)

        assertEquals(1L, result)
        coVerify { recipeDao.insertRecipe(recipe) }
    }

    // ===== 按 ID 查询 =====

    @Test
    fun `getRecipeById returns recipe when exists`() = runTest {
        val recipe = createTestRecipe(id = 5L, name = "宫保鸡丁")
        coEvery { recipeDao.getRecipeById(5L) } returns recipe

        val result = repository.getRecipeById(5L)

        assertEquals(recipe, result)
        coVerify { recipeDao.getRecipeById(5L) }
    }

    @Test
    fun `getRecipeById returns null when not exists`() = runTest {
        coEvery { recipeDao.getRecipeById(99L) } returns null

        val result = repository.getRecipeById(99L)

        assertNull(result)
        coVerify { recipeDao.getRecipeById(99L) }
    }

    // ===== 更新操作 =====

    @Test
    fun `updateRecipe delegates to RecipeDao`() = runTest {
        val recipe = createTestRecipe(id = 3L, name = "番茄炒蛋")
        coEvery { recipeDao.updateRecipe(recipe) } returns Unit

        repository.updateRecipe(recipe)

        coVerify { recipeDao.updateRecipe(recipe) }
    }

    // ===== 删除操作 =====

    @Test
    fun `deleteRecipeById delegates to RecipeDao`() = runTest {
        coEvery { recipeDao.deleteRecipeById(7L) } returns Unit

        repository.deleteRecipeById(7L)

        coVerify { recipeDao.deleteRecipeById(7L) }
    }

    // ===== 偏好查询：getPreferencesForRecipe =====

    @Test
    fun `getPreferencesForRecipe returns preferences from dao`() = runTest {
        val preferences = listOf(
            RecipePreference(recipeId = 1L, memberId = 1L, preference = Preference.LIKE),
            RecipePreference(recipeId = 1L, memberId = 2L, preference = Preference.DISLIKE)
        )
        every { preferenceDao.getPreferencesForRecipe(1L) } returns flowOf(preferences)

        val result = repository.getPreferencesForRecipe(1L).first()

        assertEquals(preferences, result)
    }

    @Test
    fun `getPreferencesForRecipe returns empty list when no preferences`() = runTest {
        every { preferenceDao.getPreferencesForRecipe(99L) } returns flowOf(emptyList())

        val result = repository.getPreferencesForRecipe(99L).first()

        assertEquals(emptyList<RecipePreference>(), result)
    }

    // ===== 偏好查询：getRecipeIdsByMemberPreference =====

    @Test
    fun `getRecipeIdsByMemberPreference returns recipe ids from dao`() = runTest {
        val recipeIds = listOf(1L, 3L, 5L)
        every {
            preferenceDao.getRecipeIdsByMemberPreference(2L, Preference.LIKE)
        } returns flowOf(recipeIds)

        val result = repository.getRecipeIdsByMemberPreference(2L, Preference.LIKE).first()

        assertEquals(recipeIds, result)
    }

    @Test
    fun `getRecipeIdsByMemberPreference returns empty list when no matches`() = runTest {
        every {
            preferenceDao.getRecipeIdsByMemberPreference(5L, Preference.DISLIKE)
        } returns flowOf(emptyList())

        val result = repository.getRecipeIdsByMemberPreference(5L, Preference.DISLIKE).first()

        assertEquals(emptyList<Long>(), result)
    }

    // ===== 偏好操作：setPreference =====

    @Test
    fun `setPreference creates RecipePreference and delegates to dao`() = runTest {
        val preferenceSlot = slot<RecipePreference>()
        coEvery { preferenceDao.upsertPreference(capture(preferenceSlot)) } returns Unit

        repository.setPreference(recipeId = 1L, memberId = 2L, preference = Preference.LIKE)

        coVerify { preferenceDao.upsertPreference(any()) }
        val captured = preferenceSlot.captured
        assertEquals(1L, captured.recipeId)
        assertEquals(2L, captured.memberId)
        assertEquals(Preference.LIKE, captured.preference)
    }

    @Test
    fun `setPreference with DISLIKE preference`() = runTest {
        val preferenceSlot = slot<RecipePreference>()
        coEvery { preferenceDao.upsertPreference(capture(preferenceSlot)) } returns Unit

        repository.setPreference(recipeId = 3L, memberId = 4L, preference = Preference.DISLIKE)

        val captured = preferenceSlot.captured
        assertEquals(3L, captured.recipeId)
        assertEquals(4L, captured.memberId)
        assertEquals(Preference.DISLIKE, captured.preference)
    }

    // ===== 偏好操作：removePreference =====

    @Test
    fun `removePreference delegates to dao deletePreference`() = runTest {
        coEvery { preferenceDao.deletePreference(1L, 2L) } returns Unit

        repository.removePreference(recipeId = 1L, memberId = 2L)

        coVerify { preferenceDao.deletePreference(1L, 2L) }
    }

    // ===== 辅助方法 =====

    private fun createTestRecipe(
        id: Long = 0L,
        name: String = "测试菜谱",
        description: String = "测试描述",
        category: String = "OTHER",
        cookingMinutes: Int = 30,
        difficulty: Int = 3,
        recommendationIndex: Int = 3,
        isFavorite: Boolean = false
    ): Recipe = Recipe(
        id = id,
        name = name,
        description = description,
        category = category,
        cookingMinutes = cookingMinutes,
        difficulty = difficulty,
        recommendationIndex = recommendationIndex,
        isFavorite = isFavorite,
        createdAt = 1000L,
        updatedAt = 2000L
    )
}
