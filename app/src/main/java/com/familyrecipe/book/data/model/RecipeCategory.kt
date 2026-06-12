package com.familyrecipe.book.data.model

/**
 * 菜谱分类枚举
 * 用于替代原有的自由文本分类，提供类型安全的分类管理
 */
enum class RecipeCategory(val label: String, val emoji: String) {
    STIR_FRY("炒菜", "🍳"),
    SOUP("煲汤", "🍲"),
    QUICK_MEAL("速食", "⚡"),
    STAPLE("主食", "🍚"),
    COLD_DISH("凉菜", "🥗"),
    DESSERT("甜品", "🍰"),
    BEVERAGE("饮品", "🥤"),
    OTHER("其他", "🍴");

    companion object {
        /**
         * 根据中文标签或枚举名称查找对应的分类
         * 未匹配时返回 OTHER
         */
        fun fromLabel(label: String): RecipeCategory {
            return entries.find {
                it.label == label || it.name.equals(label, ignoreCase = true)
            } ?: OTHER
        }

        /**
         * 从旧版自由文本中匹配分类
         * 检查文本是否包含某个分类的中文标签子串，或与枚举名称完全匹配
         * 未匹配时返回 OTHER
         */
        fun fromLegacyText(text: String): RecipeCategory {
            return entries.find {
                text.contains(it.label) || it.name.equals(text, ignoreCase = true)
            } ?: OTHER
        }
    }
}
