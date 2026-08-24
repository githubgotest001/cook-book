package com.familyrecipe.book.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupHelperTest {

    @Test
    fun `sanitizeZipEntryName keeps plain file name`() {
        assertEquals("family_recipe.db", BackupHelper.sanitizeZipEntryName("db/family_recipe.db"))
        assertEquals("cover.jpg", BackupHelper.sanitizeZipEntryName("images/cover.jpg"))
    }

    @Test
    fun `sanitizeZipEntryName rejects path traversal`() {
        assertNull(BackupHelper.sanitizeZipEntryName("../evil.db"))
        assertNull(BackupHelper.sanitizeZipEntryName("db/../../evil.db"))
        assertNull(BackupHelper.sanitizeZipEntryName("images/../passwd"))
    }

    @Test
    fun `sanitizeZipEntryName rejects blank and dot names`() {
        assertNull(BackupHelper.sanitizeZipEntryName(""))
        assertNull(BackupHelper.sanitizeZipEntryName("db/."))
        assertNull(BackupHelper.sanitizeZipEntryName("db/.."))
    }
}
