package com.familyrecipe.book.data.model

/**
 * 排序维度枚举
 * 定义菜谱列表可用的排序字段
 */
enum class SortDimension(val label: String) {
    UPDATED_AT("更新时间"),
    CREATED_AT("创建时间"),
    COOKING_MINUTES("烹饪时间"),
    DIFFICULTY("难度"),
    RECOMMENDATION("推荐指数")
}

/**
 * 排序方向枚举
 */
enum class SortOrder {
    ASC, DESC
}

/**
 * 排序配置
 * @param dimension 排序维度，默认按更新时间
 * @param order 排序方向，默认降序（最新在前）
 */
data class SortConfig(
    val dimension: SortDimension = SortDimension.UPDATED_AT,
    val order: SortOrder = SortOrder.DESC
)

/**
 * 菜谱筛选条件
 * @param searchQuery 文本搜索关键词
 * @param selectedMemberIds 选中的家庭成员 ID 集合（用于偏好筛选）
 * @param selectedCategory 选中的菜谱分类（null 表示不筛选分类）
 * @param allFamilyLoved 是否仅显示全家都喜欢的菜谱
 */
data class RecipeFilter(
    val searchQuery: String = "",
    val selectedMemberIds: Set<Long> = emptySet(),
    val selectedCategory: RecipeCategory? = null,
    val allFamilyLoved: Boolean = false
)
