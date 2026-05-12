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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * 关闭数据库（备份/恢复时需要先关闭）
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
