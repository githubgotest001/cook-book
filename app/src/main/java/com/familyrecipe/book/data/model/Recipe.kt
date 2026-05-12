package com.familyrecipe.book.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val stepsJson: String = "[]", // JSON array of step strings
    val cookingMinutes: Int = 15,
    val difficulty: Int = 2, // 1-5
    val category: String = "STIR_FRY", // 存储 RecipeCategory.name
    val coverImagePath: String? = null,
    val recommendationIndex: Int = 4, // 1-5 推荐指数
    val isFavorite: Boolean = false, // 收藏标记
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** 将 category 字符串转换为 RecipeCategory 枚举值 */
    val recipeCategory: RecipeCategory
        get() = RecipeCategory.entries.find { it.name == category } ?: RecipeCategory.OTHER
}
