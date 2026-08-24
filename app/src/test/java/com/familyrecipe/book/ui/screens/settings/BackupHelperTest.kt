package com.familyrecipe.book.ui.screens.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupHelperTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val dbFileName = "family_recipe.db"

    /** 合法 SQLite 文件的 16 字节魔数头 */
    private val sqliteHeader = "SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)

    // ===== extractBackupZip：正常解压 =====

    @Test
    fun `extractBackupZip extracts db file and images`() {
        val zipBytes = buildZip(
            "db/$dbFileName" to sqliteHeader + "data".toByteArray(),
            "images/cover1.jpg" to "img1".toByteArray(),
            "images/cover2.jpg" to "img2".toByteArray()
        )
        val staging = tmp.newFolder("staging")

        val result = BackupHelper.extractBackupZip(
            ByteArrayInputStream(zipBytes), staging, dbFileName
        )

        assertNotNull(result.dbFile)
        assertTrue(result.dbFile!!.exists())
        assertEquals(dbFileName, result.dbFile!!.name)
        val imageNames = result.imagesDir.listFiles()!!.map { it.name }.sorted()
        assertEquals(listOf("cover1.jpg", "cover2.jpg"), imageNames)
    }

    @Test
    fun `extractBackupZip returns null dbFile when backup has no database`() {
        val zipBytes = buildZip("images/cover.jpg" to "img".toByteArray())
        val staging = tmp.newFolder("staging")

        val result = BackupHelper.extractBackupZip(
            ByteArrayInputStream(zipBytes), staging, dbFileName
        )

        assertNull(result.dbFile)
    }

    @Test
    fun `extractBackupZip ignores unrelated entries`() {
        val zipBytes = buildZip(
            "readme.txt" to "hello".toByteArray(),
            "other/file.bin" to "bin".toByteArray(),
            "db/$dbFileName" to sqliteHeader
        )
        val staging = tmp.newFolder("staging")

        val result = BackupHelper.extractBackupZip(
            ByteArrayInputStream(zipBytes), staging, dbFileName
        )

        assertNotNull(result.dbFile)
        // 只应存在 db/ 与 images/ 两个子目录，无关文件不落盘
        val allFiles = staging.walkTopDown().filter { it.isFile }.toList()
        assertEquals(listOf(result.dbFile), allFiles)
    }

    // ===== extractBackupZip：路径穿越防护 =====

    @Test
    fun `extractBackupZip prevents path traversal outside staging dir`() {
        val zipBytes = buildZip(
            "db/../../evil.txt" to "evil".toByteArray(),
            "images/../../../evil2.txt" to "evil".toByteArray(),
            "../evil3.txt" to "evil".toByteArray(),
            "db/$dbFileName" to sqliteHeader
        )
        val parent = tmp.newFolder("parent")
        val staging = File(parent, "staging")

        val result = BackupHelper.extractBackupZip(
            ByteArrayInputStream(zipBytes), staging, dbFileName
        )

        assertNotNull(result.dbFile)
        // staging 之外不能出现任何解压产物
        val escaped = parent.walkTopDown()
            .filter { it.isFile && !it.canonicalPath.startsWith(staging.canonicalPath + File.separator) }
            .toList()
        assertEquals(emptyList<File>(), escaped)
        // 恶意条目即使被降级为纯文件名，也只会落在预期子目录内
        staging.walkTopDown().filter { it.isFile }.forEach { file ->
            assertTrue(
                "文件 ${file.path} 不在预期目录",
                file.canonicalPath.startsWith(File(staging, "db").canonicalPath) ||
                    file.canonicalPath.startsWith(File(staging, "images").canonicalPath)
            )
        }
    }

    @Test
    fun `extractBackupZip skips directory and blank name entries`() {
        val bytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zip ->
                zip.putNextEntry(ZipEntry("db/"))
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("images/"))
                zip.closeEntry()
            }
            baos.toByteArray()
        }
        val staging = tmp.newFolder("staging")

        val result = BackupHelper.extractBackupZip(
            ByteArrayInputStream(bytes), staging, dbFileName
        )

        assertNull(result.dbFile)
        assertEquals(0, staging.walkTopDown().count { it.isFile })
    }

    // ===== isValidSqliteFile =====

    @Test
    fun `isValidSqliteFile accepts file with sqlite magic header`() {
        val file = tmp.newFile("valid.db")
        file.writeBytes(sqliteHeader + ByteArray(100))

        assertTrue(BackupHelper.isValidSqliteFile(file))
    }

    @Test
    fun `isValidSqliteFile rejects file with wrong header`() {
        val file = tmp.newFile("fake.db")
        file.writeBytes("this is definitely not a database".toByteArray())

        assertFalse(BackupHelper.isValidSqliteFile(file))
    }

    @Test
    fun `isValidSqliteFile rejects file shorter than header`() {
        val file = tmp.newFile("short.db")
        file.writeBytes("SQLite".toByteArray())

        assertFalse(BackupHelper.isValidSqliteFile(file))
    }

    @Test
    fun `isValidSqliteFile rejects empty and missing files`() {
        val empty = tmp.newFile("empty.db")
        assertFalse(BackupHelper.isValidSqliteFile(empty))
        assertFalse(BackupHelper.isValidSqliteFile(File(tmp.root, "missing.db")))
    }

    // ===== 辅助方法 =====

    private fun buildZip(vararg entries: Pair<String, ByteArray>): ByteArray {
        return ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zip ->
                entries.forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content)
                    zip.closeEntry()
                }
            }
            baos.toByteArray()
        }
    }
}
