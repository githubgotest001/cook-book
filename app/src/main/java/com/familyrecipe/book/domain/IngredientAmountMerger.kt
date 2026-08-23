package com.familyrecipe.book.domain

import com.familyrecipe.book.data.model.RecipeIngredient

/**
 * 合并同一食材的数量文本，供购物清单等业务复用。
 */
object IngredientAmountMerger {

    fun mergeAmounts(group: List<RecipeIngredient>): String {
        val byUnit = group.groupBy { it.unit.trim() }
        val parts = byUnit.mapNotNull { (unit, items) ->
            val amounts = items.map { it.amount.trim() }.filter { it.isNotBlank() }
            if (amounts.isEmpty()) {
                return@mapNotNull unit.ifBlank { null }
            }
            val numbers = amounts.map { it.toDoubleOrNull() }
            if (numbers.all { it != null }) {
                formatNumber(numbers.filterNotNull().sum()) + unit
            } else {
                amounts.distinct().joinToString("、") { it + unit }
            }
        }
        return parts.joinToString(" + ")
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format("%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}
