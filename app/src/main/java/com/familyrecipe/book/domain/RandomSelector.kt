package com.familyrecipe.book.domain

import com.familyrecipe.book.data.model.Recipe
import javax.inject.Inject
import kotlin.random.Random

/**
 * 随机选菜领域逻辑。
 * 从菜谱列表中随机选择指定数量的菜谱，确保无重复。
 */
class RandomSelector @Inject constructor() {

    /**
     * 从菜谱列表中随机选择指定数量的菜谱。
     *
     * @param recipes 可选菜谱列表
     * @param count 请求选择的数量，范围 1-10
     * @param random 随机数生成器，可注入以支持测试
     * @return 随机选择结果，包含选中的菜谱列表和可能的警告
     * @throws IllegalArgumentException 当 count 不在 1-10 范围内时
     */
    fun selectRandom(
        recipes: List<Recipe>,
        count: Int,
        random: Random = Random
    ): RandomSelectionResult {
        require(count in 1..10) { "Count must be between 1 and 10" }

        if (recipes.isEmpty()) {
            return RandomSelectionResult(
                selected = emptyList(),
                warning = RandomWarning.NO_RECIPES
            )
        }

        val actualCount = minOf(count, recipes.size)
        val selected = recipes.shuffled(random).take(actualCount)

        val warning = if (recipes.size < count) {
            RandomWarning.INSUFFICIENT_RECIPES
        } else null

        return RandomSelectionResult(selected = selected, warning = warning)
    }
}

/**
 * 随机选择结果。
 *
 * @property selected 选中的菜谱列表（无重复）
 * @property warning 可能的警告信息
 */
data class RandomSelectionResult(
    val selected: List<Recipe>,
    val warning: RandomWarning? = null
)

/**
 * 随机选择警告类型。
 */
enum class RandomWarning {
    /** 菜谱列表为空，无法选择 */
    NO_RECIPES,
    /** 可用菜谱数量少于请求数量 */
    INSUFFICIENT_RECIPES
}
