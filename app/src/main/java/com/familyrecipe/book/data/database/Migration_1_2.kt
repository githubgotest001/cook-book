package com.familyrecipe.book.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移 v1 → v2
 * - 添加 recommendationIndex 列（推荐指数，默认 3）
 * - 添加 isFavorite 列（收藏标记，默认 0/false）
 * - 将 category 自由文本映射为 RecipeCategory 枚举字符串
 */
val Migration_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 添加 recommendationIndex 列
        db.execSQL("ALTER TABLE recipes ADD COLUMN recommendationIndex INTEGER NOT NULL DEFAULT 3")

        // 添加 isFavorite 列
        db.execSQL("ALTER TABLE recipes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")

        // 转换 category 文本为枚举值
        db.execSQL("UPDATE recipes SET category = 'STIR_FRY' WHERE category LIKE '%炒菜%'")
        db.execSQL("UPDATE recipes SET category = 'SOUP' WHERE category LIKE '%煲汤%'")
        db.execSQL("UPDATE recipes SET category = 'QUICK_MEAL' WHERE category LIKE '%速食%'")
        db.execSQL("UPDATE recipes SET category = 'STAPLE' WHERE category LIKE '%主食%'")
        db.execSQL("UPDATE recipes SET category = 'COLD_DISH' WHERE category LIKE '%凉菜%'")
        db.execSQL("UPDATE recipes SET category = 'DESSERT' WHERE category LIKE '%甜品%'")
        db.execSQL("UPDATE recipes SET category = 'BEVERAGE' WHERE category LIKE '%饮品%'")
        // 未匹配的设为 OTHER
        db.execSQL(
            """
            UPDATE recipes SET category = 'OTHER' 
            WHERE category NOT IN ('STIR_FRY','SOUP','QUICK_MEAL','STAPLE','COLD_DISH','DESSERT','BEVERAGE')
            """.trimIndent()
        )
    }
}
