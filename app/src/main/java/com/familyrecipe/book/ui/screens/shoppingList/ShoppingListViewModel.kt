package com.familyrecipe.book.ui.screens.shoppingList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyrecipe.book.data.datastore.SettingsStore
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeIngredient
import com.familyrecipe.book.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 购物清单条目。
 *
 * @property key 唯一键（用于持久化已购状态）
 * @property name 食材/采购项名称
 * @property amountText 合并后的数量文本，如 "5个"、"2个 + 500克"
 * @property noteText 合并后的备注
 * @property recipeNames 来源菜谱名（手动项为空）
 * @property isManual 是否为手动添加的临时项
 * @property purchased 是否已购买
 */
data class ShoppingListItem(
    val key: String,
    val name: String,
    val amountText: String = "",
    val noteText: String = "",
    val recipeNames: List<String> = emptyList(),
    val isManual: Boolean = false,
    val purchased: Boolean = false
)

data class ShoppingListUiState(
    val recipes: List<Recipe> = emptyList(),
    val selectedRecipeIds: Set<Long> = emptySet(),
    val shoppingItems: List<ShoppingListItem> = emptyList(),
    val isLoading: Boolean = true
) {
    val pendingCount: Int get() = shoppingItems.count { !it.purchased }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private data class Inputs(
        val recipes: List<Recipe>,
        val selectedIds: Set<Long>,
        val manualItems: Set<String>,
        val purchasedKeys: Set<String>
    )

    val uiState: StateFlow<ShoppingListUiState> = combine(
        recipeRepository.getAllRecipes(),
        settingsStore.shoppingSelectedRecipeIds,
        settingsStore.shoppingManualItems,
        settingsStore.shoppingPurchasedKeys
    ) { recipes, selectedIds, manualItems, purchasedKeys ->
        val validIds = selectedIds.intersect(recipes.map { it.id }.toSet())
        Inputs(recipes, validIds, manualItems, purchasedKeys)
    }.flatMapLatest { inputs ->
        recipeRepository.getIngredientsForRecipes(inputs.selectedIds.toList())
            .map { ingredients ->
                ShoppingListUiState(
                    recipes = inputs.recipes,
                    selectedRecipeIds = inputs.selectedIds,
                    shoppingItems = buildItems(
                        ingredients = ingredients,
                        recipes = inputs.recipes,
                        manualItems = inputs.manualItems,
                        purchasedKeys = inputs.purchasedKeys
                    ),
                    isLoading = false
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShoppingListUiState()
    )

    fun toggleRecipe(recipeId: Long) {
        viewModelScope.launch {
            val current = settingsStore.shoppingSelectedRecipeIds.first()
            val updated = if (recipeId in current) current - recipeId else current + recipeId
            settingsStore.setShoppingSelectedRecipeIds(updated)
        }
    }

    fun addManualItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = settingsStore.shoppingManualItems.first()
            settingsStore.setShoppingManualItems(current + trimmed)
        }
    }

    fun removeManualItem(item: ShoppingListItem) {
        if (!item.isManual) return
        viewModelScope.launch {
            val current = settingsStore.shoppingManualItems.first()
            settingsStore.setShoppingManualItems(current - item.name)
            val purchased = settingsStore.shoppingPurchasedKeys.first()
            settingsStore.setShoppingPurchasedKeys(purchased - item.key)
        }
    }

    fun togglePurchased(item: ShoppingListItem) {
        viewModelScope.launch {
            val current = settingsStore.shoppingPurchasedKeys.first()
            val updated = if (item.key in current) current - item.key else current + item.key
            settingsStore.setShoppingPurchasedKeys(updated)
        }
    }

    /** 清空整个清单：所选菜谱、手动项、已购状态 */
    fun clearAll() {
        viewModelScope.launch {
            settingsStore.setShoppingSelectedRecipeIds(emptySet())
            settingsStore.setShoppingManualItems(emptySet())
            settingsStore.setShoppingPurchasedKeys(emptySet())
        }
    }

    private fun buildItems(
        ingredients: List<RecipeIngredient>,
        recipes: List<Recipe>,
        manualItems: Set<String>,
        purchasedKeys: Set<String>
    ): List<ShoppingListItem> {
        val recipeNamesById = recipes.associate { it.id to it.name }

        val ingredientItems = ingredients
            .filter { it.name.isNotBlank() }
            .groupBy { it.name.trim().lowercase() }
            .map { (nameKey, group) ->
                val key = "r:$nameKey"
                ShoppingListItem(
                    key = key,
                    name = group.first().name.trim(),
                    amountText = mergeAmounts(group),
                    noteText = group.map { it.note.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString("；"),
                    recipeNames = group.mapNotNull { recipeNamesById[it.recipeId] }.distinct(),
                    isManual = false,
                    purchased = key in purchasedKeys
                )
            }

        val manual = manualItems.map { text ->
            val key = "m:$text"
            ShoppingListItem(
                key = key,
                name = text,
                isManual = true,
                purchased = key in purchasedKeys
            )
        }

        // 未购在前、已购在后，组内按名称排序
        return (ingredientItems + manual).sortedWith(
            compareBy({ it.purchased }, { it.name })
        )
    }

    /**
     * 合并同一食材的数量：
     * - 同单位且数量均为数字时做数值累加（2个 + 3个 = 5个）
     * - 无法解析时降级为并列展示（少许、适量等）
     * - 不同单位之间用 " + " 连接（2个 + 500克）
     */
    private fun mergeAmounts(group: List<RecipeIngredient>): String {
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
            // 保留最多两位小数并去除末尾的 0
            String.format("%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}
