package com.familyrecipe.book.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 应用设置持久化存储，使用 DataStore Preferences 实现。
 * 管理随机选菜的默认数量配置，以及购物清单的会话状态
 * （所选菜谱、手动添加项、已购勾选），保证退出页面/重启应用后不丢失。
 */
class SettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    /**
     * 随机选菜的默认数量，默认值为 3，有效范围 1-10。
     */
    val defaultRandomCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[DEFAULT_RANDOM_COUNT] ?: 3
    }

    /**
     * 设置随机选菜的默认数量。
     * @param count 数量，必须在 1-10 范围内
     * @throws IllegalArgumentException 如果 count 不在有效范围内
     */
    suspend fun setDefaultRandomCount(count: Int) {
        require(count in 1..10) { "Default random count must be between 1 and 10" }
        dataStore.edit { prefs ->
            prefs[DEFAULT_RANDOM_COUNT] = count
        }
    }

    // ===== 购物清单状态 =====

    /** 购物清单中勾选的菜谱 ID 集合 */
    val shoppingSelectedRecipeIds: Flow<Set<Long>> = dataStore.data.map { prefs ->
        prefs[SHOPPING_SELECTED_RECIPE_IDS]
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    suspend fun setShoppingSelectedRecipeIds(ids: Set<Long>) {
        dataStore.edit { prefs ->
            prefs[SHOPPING_SELECTED_RECIPE_IDS] = ids.map { it.toString() }.toSet()
        }
    }

    /** 手动添加的临时采购项 */
    val shoppingManualItems: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[SHOPPING_MANUAL_ITEMS] ?: emptySet()
    }

    suspend fun setShoppingManualItems(items: Set<String>) {
        dataStore.edit { prefs ->
            prefs[SHOPPING_MANUAL_ITEMS] = items
        }
    }

    /** 已购买项的唯一键集合 */
    val shoppingPurchasedKeys: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[SHOPPING_PURCHASED_KEYS] ?: emptySet()
    }

    suspend fun setShoppingPurchasedKeys(keys: Set<String>) {
        dataStore.edit { prefs ->
            prefs[SHOPPING_PURCHASED_KEYS] = keys
        }
    }

    companion object {
        val DEFAULT_RANDOM_COUNT = intPreferencesKey("default_random_count")
        val SHOPPING_SELECTED_RECIPE_IDS = stringSetPreferencesKey("shopping_selected_recipe_ids")
        val SHOPPING_MANUAL_ITEMS = stringSetPreferencesKey("shopping_manual_items")
        val SHOPPING_PURCHASED_KEYS = stringSetPreferencesKey("shopping_purchased_keys")
    }
}
