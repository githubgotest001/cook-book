package com.familyrecipe.book.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImageUtilsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // ===== cleanupTempCacheFiles =====

    @Test
    fun `cleanupTempCacheFiles removes only temp prefixed files`() {
        val cacheDir = tmp.newFolder("cache")
        val cameraFile = File(cacheDir, "camera_photo_123.jpg").apply { writeText("x") }
        val importFile = File(cacheDir, "recipe_import_456.png").apply { writeText("x") }
        val otherFile = File(cacheDir, "unrelated.txt").apply { writeText("x") }

        val deleted = ImageUtils.cleanupTempCacheFiles(cacheDir)

        assertEquals(2, deleted)
        assertFalse(cameraFile.exists())
        assertFalse(importFile.exists())
        assertTrue(otherFile.exists())
    }

    @Test
    fun `cleanupTempCacheFiles skips directories even with matching prefix`() {
        val cacheDir = tmp.newFolder("cache")
        val dir = File(cacheDir, "camera_photo_dir").apply { mkdirs() }
        File(dir, "inner.jpg").writeText("x")

        val deleted = ImageUtils.cleanupTempCacheFiles(cacheDir)

        assertEquals(0, deleted)
        assertTrue(dir.exists())
    }

    @Test
    fun `cleanupTempCacheFiles returns zero for empty or missing dir`() {
        val emptyDir = tmp.newFolder("empty")
        assertEquals(0, ImageUtils.cleanupTempCacheFiles(emptyDir))
        assertEquals(0, ImageUtils.cleanupTempCacheFiles(File(tmp.root, "missing")))
    }

    // ===== calculateInSampleSize =====

    @Test
    fun `calculateInSampleSize returns 1 when image within max dimension`() {
        assertEquals(1, ImageUtils.calculateInSampleSize(1000, 800, 1920))
    }

    @Test
    fun `calculateInSampleSize halves large images by power of two`() {
        assertEquals(2, ImageUtils.calculateInSampleSize(4000, 3000, 1920))
        assertEquals(4, ImageUtils.calculateInSampleSize(8000, 6000, 1920))
    }
}
