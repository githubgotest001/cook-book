package com.familyrecipe.book.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

enum class Preference {
    LIKE, NEUTRAL, DISLIKE
}

@Entity(
    tableName = "recipe_preferences",
    primaryKeys = ["recipeId", "memberId"],
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FamilyMember::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("memberId")]
)
data class RecipePreference(
    val recipeId: Long,
    val memberId: Long,
    val preference: Preference = Preference.NEUTRAL,
    val updatedAt: Long = System.currentTimeMillis()
)
