package com.familyrecipe.book.ui.screens.randomPick

import com.familyrecipe.book.data.datastore.SettingsStore
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeCategory
import com.familyrecipe.book.data.repository.RecipeRepository
import com.familyrecipe.book.domain.RandomSelector
import com.familyrecipe.book.domain.RandomWarning
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * RandomPickViewModel 单元测试。
 * 覆盖默认数量加载、数量调整、分类过滤和菜谱不足警告。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RandomPickViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: RecipeRepository
    private lateinit var settingsStore: SettingsStore

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        settingsStore = mockk()
        every { settingsStore.defaultRandomCount } returns flowOf(4)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        RandomPickViewModel(repository, settingsStore, RandomSelector())

    @Test
    fun `initial selection uses default count from settings`() = runTest {
        every { repository.getAllRecipes() } returns flowOf(createRecipes(10))

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(4, state.currentCount)
        assertEquals(4, state.selectedRecipes.size)
        assertNull(state.warning)
    }

    @Test
    fun `setCount reselects with new count and clamps to valid range`() = runTest {
        every { repository.getAllRecipes() } returns flowOf(createRecipes(20))

        val viewModel = createViewModel()
        viewModel.setCount(6)
        assertEquals(6, viewModel.uiState.value.selectedRecipes.size)

        viewModel.setCount(99)
        assertEquals(10, viewModel.uiState.value.currentCount)
        assertEquals(10, viewModel.uiState.value.selectedRecipes.size)
    }

    @Test
    fun `setCategory filters pool and toggles off when tapped again`() = runTest {
        val soups = createRecipes(5, category = RecipeCategory.SOUP)
        val stirFry = createRecipes(5, category = RecipeCategory.STIR_FRY, idOffset = 100)
        every { repository.getAllRecipes() } returns flowOf(soups + stirFry)

        val viewModel = createViewModel()
        viewModel.setCategory(RecipeCategory.SOUP)

        val filtered = viewModel.uiState.value
        assertEquals(RecipeCategory.SOUP, filtered.selectedCategory)
        assertTrue(filtered.selectedRecipes.all { it.category == RecipeCategory.SOUP.name })

        // 再点一次同分类 → 取消过滤
        viewModel.setCategory(RecipeCategory.SOUP)
        assertNull(viewModel.uiState.value.selectedCategory)
    }

    @Test
    fun `insufficient recipes returns all with warning`() = runTest {
        every { repository.getAllRecipes() } returns flowOf(createRecipes(2))

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(2, state.selectedRecipes.size)
        assertEquals(RandomWarning.INSUFFICIENT_RECIPES, state.warning)
    }

    @Test
    fun `empty recipe list returns NO_RECIPES warning`() = runTest {
        every { repository.getAllRecipes() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.selectedRecipes.isEmpty())
        assertEquals(RandomWarning.NO_RECIPES, state.warning)
    }

    @Test
    fun `refreshRandom keeps current count`() = runTest {
        every { repository.getAllRecipes() } returns flowOf(createRecipes(10))

        val viewModel = createViewModel()
        viewModel.setCount(5)
        viewModel.refreshRandom()

        assertEquals(5, viewModel.uiState.value.currentCount)
        assertEquals(5, viewModel.uiState.value.selectedRecipes.size)
    }

    private fun createRecipes(
        count: Int,
        category: RecipeCategory = RecipeCategory.OTHER,
        idOffset: Int = 0
    ): List<Recipe> = (1..count).map { i ->
        Recipe(
            id = (idOffset + i).toLong(),
            name = "菜谱${idOffset + i}",
            description = "",
            category = category.name,
            createdAt = 1000L,
            updatedAt = 1000L
        )
    }
}
