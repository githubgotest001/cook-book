package com.familyrecipe.book.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.familyrecipe.book.data.dao.FamilyMemberDao
import com.familyrecipe.book.data.dao.RecipeDao
import com.familyrecipe.book.data.dao.RecipeIngredientDao
import com.familyrecipe.book.data.dao.RecipePreferenceDao
import com.familyrecipe.book.data.model.FamilyMember
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeIngredient
import com.familyrecipe.book.data.model.RecipePreference

@Database(
    entities = [Recipe::class, FamilyMember::class, RecipePreference::class, RecipeIngredient::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun recipePreferenceDao(): RecipePreferenceDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao

    companion object {
        const val DATABASE_NAME = "family_recipe.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { INSTANCE = it }
            }
        }

        /**
         * 关闭当前实例并清空单例引用（备份/恢复前调用）。
         */
        fun closeAndClearInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }

    /**
     * 执行 WAL checkpoint（TRUNCATE 模式），把日志全部刷入主库文件并清空 WAL，
     * 保证单独复制主库文件即可得到完整数据。
     */
    fun checkpointWal() {
        openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
    }
}
