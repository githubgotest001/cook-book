package com.familyrecipe.book.ui.screens.shoppingList

import com.familyrecipe.book.data.datastore.SettingsStore
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeIngredient
import com.familyrecipe.book.data.repository.RecipeRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ShoppingListViewModel 单元测试。
 * 覆盖菜谱勾选、食材合并展示、手动项、已购状态和清空清单等核心交互。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: RecipeRepository
    private lateinit var settingsStore: SettingsStore

    // 用 MutableStateFlow 模拟 DataStore 持久化行为：setter 写入后 flow 立即发出新值
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val manualItems = MutableStateFlow<Set<String>>(emptySet())
    private val purchasedKeys = MutableStateFlow<Set<String>>(emptySet())

    private val recipes = listOf(
        recipe(id = 1L, name = "番茄炒蛋"),
        recipe(id = 2L, name = "蛋炒饭")
    )
    private val ingredients = listOf(
        ingredient(recipeId = 1L, name = "鸡蛋", amount = "2", unit = "个"),
        ingredient(recipeId = 2L, name = "鸡蛋", amount = "3", unit = "个"),
        ingredient(recipeId = 2L, name = "米饭", amount = "1", unit = "碗")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        selectedIds.value = emptySet()
        manualItems.value = emptySet()
        purchasedKeys.value = emptySet()

        repository = mockk()
        settingsStore = mockk()

        every { repository.getAllRecipes() } returns flowOf(recipes)
        every { repository.getIngredientsForRecipes(any()) } answers {
            val ids = firstArg<List<Long>>().toSet()
            flowOf(ingredients.filter { it.recipeId in ids })
        }

        every { settingsStore.shoppingSelectedRecipeIds } returns selectedIds
        every { settingsStore.shoppingManualItems } returns manualItems
        every { settingsStore.shoppingPurchasedKeys } returns purchasedKeys
        coEvery { settingsStore.setShoppingSelectedRecipeIds(any()) } answers {
            selectedIds.value = firstArg()
        }
        coEvery { settingsStore.setShoppingManualItems(any()) } answers {
            manualItems.value = firstArg()
        }
        coEvery { settingsStore.setShoppingPurchasedKeys(any()) } answers {
            purchasedKeys.value = firstArg()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ShoppingListViewModel(repository, settingsStore)

    /** 订阅 uiState 触发 stateIn 共享启动，返回收集任务供测试结束时取消 */
    private fun TestScope.subscribe(viewModel: ShoppingListViewModel): Job {
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        return job
    }

    /** combine/flatMapLatest 管道是异步发射的，断言前先推进调度器 */
    private fun TestScope.currentState(viewModel: ShoppingListViewModel): ShoppingListUiState {
        advanceUntilIdle()
        return viewModel.uiState.value
    }

    @Test
    fun `initial state has no selection and empty list`() = runTest {
        val viewModel = createViewModel()
        val job = subscribe(viewModel)

        val state = currentState(viewModel)
        assertFalse(state.isLoading)
        assertTrue(state.selectedRecipeIds.isEmpty())
        assertTrue(state.shoppingItems.isEmpty())
        assertEquals(recipes, state.recipes)

        job.cancel()
    }

    @Test
    fun `toggleRecipe selects recipes and merges same ingredient across recipes`() = runTest {
        val viewModel = createViewModel()
        val job = subscribe(viewModel)

        viewModel.toggleRecipe(1L)
        viewModel.toggleRecipe(2L)

        val state = currentState(viewModel)
        assertEquals(setOf(1L, 2L), state.selectedRecipeIds)

        val egg = state.shoppingItems.first { it.name == "鸡蛋" }
        assertEquals("5个", egg.amountText)
        assertEquals(listOf("番茄炒蛋", "蛋炒饭"), egg.recipeNames)

        val rice = state.shoppingItems.first { it.name == "米饭" }
        assertEquals("1碗", rice.amountText)

        job.cancel()
    }

    @Test
    fun `toggleRecipe twice deselects the recipe`() = runTest {
        val viewModel = createViewModel()
        val job = subscribe(viewModel)

        viewModel.toggleRecipe(1L)
        advanceUntilIdle()
        viewModel.toggleRecipe(1L)

        val state = currentState(viewModel)
        assertTrue(state.selectedRecipeIds.isEmpty())
        assertTrue(state.shoppingItems.isEmpty())

        job.cancel()
    }

    @Test
    fun `addManualItem trims input and ignores blank`() = runTest {
        val viewModel = createViewModel()
        val job = subscribe(viewModel)

        viewModel.addManualItem("  酱油  ")
        viewModel.addManualItem("   ")

        val items = currentState(viewModel).shoppingItems
        assertEquals(1, items.size)
        assertEquals("酱油", items.first().name)
        assertTrue(items.first().isManual)

        job.cancel()
    }

    @Test
    fun `togglePurchased moves item to bottom and updates pending count`() = runTest {
        val viewModel = createViewModel()
        val job = subscribe(viewModel)

        viewModel.toggleRecipe(2L) // 鸡蛋 + 米饭
        val target = currentState(viewModel).shoppingItems.first { it.name == "米饭" }

        viewModel.togglePurchased(target)

        val state = currentState(viewModel)
        assertEquals(1, state.pendingCount)
        // 已购项排在未购项之后
        assertEquals("米饭", state.shoppingItems.last().name)
        assertTrue(state.shoppingItems.last().purchased)

        job.cancel()
    }

    @Test
    fun `removeManualItem clears item and its purchased key`() = runTest {
        val viewModel = createViewModel()
        val job = subscribe(viewModel)

        viewModel.addManualItem("纸巾")
        val item = currentState(viewModel).shoppingItems.first()
        viewModel.togglePurchased(item)
        advanceUntilIdle()
        viewModel.removeManualItem(currentState(viewModel).shoppingItems.first())

        val state = currentState(viewModel)
        assertTrue(state.shoppingItems.isEmpty())
        assertTrue(purchasedKeys.value.isEmpty())

        job.cancel()
    }

    @Test
    fun `clearAll resets selection manual items and purchased keys`() = runTest {
        val viewModel = createViewModel()
        val job = subscribe(viewModel)

        viewModel.toggleRecipe(1L)
        viewModel.addManualItem("酱油")
        viewModel.togglePurchased(currentState(viewModel).shoppingItems.first())

        viewModel.clearAll()

        val state = currentState(viewModel)
        assertTrue(state.selectedRecipeIds.isEmpty())
        assertTrue(state.shoppingItems.isEmpty())
        assertTrue(purchasedKeys.value.isEmpty())

        job.cancel()
    }

    private fun recipe(id: Long, name: String) = Recipe(
        id = id,
        name = name,
        description = "",
        category = "OTHER",
        createdAt = 1000L,
        updatedAt = 1000L
    )

    private fun ingredient(recipeId: Long, name: String, amount: String, unit: String) =
        RecipeIngredient(
            id = 0L,
            recipeId = recipeId,
            name = name,
            amount = amount,
            unit = unit,
            note = ""
        )
}
