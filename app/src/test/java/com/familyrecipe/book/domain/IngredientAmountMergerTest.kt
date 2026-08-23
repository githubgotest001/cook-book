package com.familyrecipe.book.domain

import com.familyrecipe.book.data.model.RecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Test

class IngredientAmountMergerTest {

    @Test
    fun `merges same unit numeric amounts`() {
        val group = listOf(
            ingredient(amount = "2", unit = "个"),
            ingredient(amount = "3", unit = "个")
        )
        assertEquals("5个", IngredientAmountMerger.mergeAmounts(group))
    }

    @Test
    fun `joins different units with plus`() {
        val group = listOf(
            ingredient(amount = "2", unit = "个"),
            ingredient(amount = "500", unit = "克")
        )
        assertEquals("2个 + 500克", IngredientAmountMerger.mergeAmounts(group))
    }

    @Test
    fun `falls back to listing non numeric amounts`() {
        val group = listOf(
            ingredient(amount = "少许", unit = ""),
            ingredient(amount = "适量", unit = "")
        )
        assertEquals("少许、适量", IngredientAmountMerger.mergeAmounts(group))
    }

    @Test
    fun `sums decimal amounts`() {
        val group = listOf(
            ingredient(amount = "1.5", unit = "勺"),
            ingredient(amount = "0.5", unit = "勺")
        )
        assertEquals("2勺", IngredientAmountMerger.mergeAmounts(group))
    }

    private fun ingredient(amount: String, unit: String) = RecipeIngredient(
        id = 0L,
        recipeId = 1L,
        name = "盐",
        amount = amount,
        unit = unit,
        note = ""
    )
}
