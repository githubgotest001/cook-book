package com.familyrecipe.book.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("recipeId")]
)
data class RecipeIngredient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipeId: Long,
    val name: String,
    val amount: String = "1",
    val unit: String = "个",
    val note: String = "",
    val displayOrder: Int = 0
) {
    val displayText: String
        get() = listOf(name, amount + unit, note)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
}
