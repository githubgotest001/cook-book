package com.familyrecipe.book.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 应用设置持久化存储，使用 DataStore Preferences 实现。
 * 当前管理随机选菜的默认数量配置。
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

    companion object {
        val DEFAULT_RANDOM_COUNT = intPreferencesKey("default_random_count")
    }
}
