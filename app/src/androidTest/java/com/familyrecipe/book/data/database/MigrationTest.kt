package com.familyrecipe.book.data.database

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 数据库迁移测试：验证 Migration_1_2 正确执行
 * - 验证 recommendationIndex 默认为 3
 * - 验证 isFavorite 默认为 false（0）
 * - 验证分类文本映射正确（炒菜→STIR_FRY 等）
 * - 验证不匹配分类映射为 OTHER
 *
 * Validates: Requirements 10.5
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDbName = "test_migration.db"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migration_1_2_addsNewColumnsWithCorrectDefaults() {
        // 创建 v1 数据库并插入测试数据
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (1, '测试菜谱', '描述', '[]', 30, 3, '炒菜', NULL, 1000, 2000)
                """.trimIndent()
            )
            close()
        }

        // 执行迁移到 v2
        val db = helper.runMigrationsAndValidate(testDbName, 2, true, Migration_1_2)

        // 验证新列默认值
        val cursor: Cursor = db.query("SELECT recommendationIndex, isFavorite FROM recipes WHERE id = 1")
        assertTrue("应该有查询结果", cursor.moveToFirst())

        val recommendationIndex = cursor.getInt(cursor.getColumnIndexOrThrow("recommendationIndex"))
        val isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow("isFavorite"))

        assertEquals("recommendationIndex 默认应为 3", 3, recommendationIndex)
        assertEquals("isFavorite 默认应为 0 (false)", 0, isFavorite)

        cursor.close()
        db.close()
    }

    @Test
    fun migration_1_2_mapsCategoryStirFryCorrectly() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (1, '番茄炒蛋', '经典炒菜', '[]', 15, 2, '炒菜', NULL, 1000, 2000)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 2, true, Migration_1_2)

        val cursor = db.query("SELECT category FROM recipes WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("STIR_FRY", cursor.getString(cursor.getColumnIndexOrThrow("category")))

        cursor.close()
        db.close()
    }

    @Test
    fun migration_1_2_mapsCategorySoupCorrectly() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (1, '排骨汤', '营养煲汤', '[]', 60, 3, '煲汤', NULL, 1000, 2000)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 2, true, Migration_1_2)

        val cursor = db.query("SELECT category FROM recipes WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("SOUP", cursor.getString(cursor.getColumnIndexOrThrow("category")))

        cursor.close()
        db.close()
    }

    @Test
    fun migration_1_2_mapsUnknownCategoryToOther() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (1, '未知菜', '未知分类', '[]', 20, 1, 'unknown_text', NULL, 1000, 2000)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 2, true, Migration_1_2)

        val cursor = db.query("SELECT category FROM recipes WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("OTHER", cursor.getString(cursor.getColumnIndexOrThrow("category")))

        cursor.close()
        db.close()
    }

    @Test
    fun migration_1_2_mapsAllCategoriesCorrectly() {
        // 插入多条不同分类的数据，一次性验证所有映射
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (1, '菜1', '', '[]', 10, 1, '炒菜', NULL, 1000, 2000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (2, '菜2', '', '[]', 10, 1, '煲汤', NULL, 1000, 2000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (3, '菜3', '', '[]', 10, 1, '速食', NULL, 1000, 2000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (4, '菜4', '', '[]', 10, 1, '主食', NULL, 1000, 2000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (5, '菜5', '', '[]', 10, 1, '凉菜', NULL, 1000, 2000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (6, '菜6', '', '[]', 10, 1, '甜品', NULL, 1000, 2000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (7, '菜7', '', '[]', 10, 1, '饮品', NULL, 1000, 2000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO recipes (id, name, description, stepsJson, cookingMinutes, difficulty, category, coverImagePath, createdAt, updatedAt)
                VALUES (8, '菜8', '', '[]', 10, 1, 'random_text', NULL, 1000, 2000)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 2, true, Migration_1_2)

        // 验证所有分类映射
        val expectedMappings = mapOf(
            1L to "STIR_FRY",
            2L to "SOUP",
            3L to "QUICK_MEAL",
            4L to "STAPLE",
            5L to "COLD_DISH",
            6L to "DESSERT",
            7L to "BEVERAGE",
            8L to "OTHER"
        )

        val cursor = db.query("SELECT id, category FROM recipes ORDER BY id")
        var count = 0
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
            val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
            assertEquals(
                "id=$id 的分类映射应为 ${expectedMappings[id]}",
                expectedMappings[id],
                category
            )
            count++
        }
        assertEquals("应有 8 条记录", 8, count)

        cursor.close()
        db.close()
    }
}
