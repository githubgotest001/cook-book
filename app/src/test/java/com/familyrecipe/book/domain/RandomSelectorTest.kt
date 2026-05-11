package com.familyrecipe.book.domain

import com.familyrecipe.book.data.model.Recipe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.random.Random

/**
 * RandomSelector 单元测试。
 * 验证随机选菜逻辑的正确性，包括无重复、数量正确、均匀分布和边界情况。
 *
 * Validates: Requirements 10.4
 */
class RandomSelectorTest : FunSpec({

    val selector = RandomSelector()

    // 辅助函数：创建测试用菜谱列表
    fun createRecipes(count: Int): List<Recipe> = (1..count).map { i ->
        Recipe(
            id = i.toLong(),
            name = "菜谱$i",
            description = "描述$i",
            category = "OTHER",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    test("单次选择结果无重复") {
        val recipes = createRecipes(10)
        val result = selector.selectRandom(recipes, 5, Random(42))

        val ids = result.selected.map { it.id }
        ids.distinct().size shouldBe ids.size
    }

    test("结果数量等于请求数量（有足够菜谱时）") {
        val recipes = createRecipes(20)

        for (count in 1..10) {
            val result = selector.selectRandom(recipes, count, Random(count))
            result.selected shouldHaveSize count
            result.warning shouldBe null
        }
    }

    test("结果数量等于 min(count, recipes.size)") {
        val recipes = createRecipes(3)
        val result = selector.selectRandom(recipes, 7, Random(42))

        result.selected shouldHaveSize 3
    }

    test("空列表返回空结果并带 NO_RECIPES 警告") {
        val result = selector.selectRandom(emptyList(), 3, Random(42))

        result.selected.shouldBeEmpty()
        result.warning shouldBe RandomWarning.NO_RECIPES
    }

    test("菜谱不足时返回所有可用菜谱并带 INSUFFICIENT_RECIPES 警告") {
        val recipes = createRecipes(2)
        val result = selector.selectRandom(recipes, 5, Random(42))

        result.selected shouldHaveSize 2
        result.selected.map { it.id } shouldContainExactlyInAnyOrder listOf(1L, 2L)
        result.warning shouldBe RandomWarning.INSUFFICIENT_RECIPES
    }

    test("1000 次重复选择中每个菜谱至少被选中一次（5 个菜谱池）") {
        val recipes = createRecipes(5)
        val selectedCounts = mutableMapOf<Long, Int>()

        repeat(1000) { iteration ->
            val result = selector.selectRandom(recipes, 1, Random(iteration))
            result.selected.forEach { recipe ->
                selectedCounts[recipe.id] = (selectedCounts[recipe.id] ?: 0) + 1
            }
        }

        // 每个菜谱至少被选中一次
        recipes.forEach { recipe ->
            val count = selectedCounts[recipe.id] ?: 0
            count shouldBeGreaterThan 0
        }
    }

    test("count 为 0 时抛出 IllegalArgumentException") {
        val recipes = createRecipes(5)

        shouldThrow<IllegalArgumentException> {
            selector.selectRandom(recipes, 0, Random(42))
        }
    }

    test("count 为 11 时抛出 IllegalArgumentException") {
        val recipes = createRecipes(5)

        shouldThrow<IllegalArgumentException> {
            selector.selectRandom(recipes, 11, Random(42))
        }
    }
})
