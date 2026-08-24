package com.familyrecipe.book.ui.screens.settings

import android.content.Context
import android.net.Uri
import com.familyrecipe.book.data.database.AppDatabase
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份/恢复工具：将数据库文件 + 图片目录打包为 zip。
 * 必须通过 Hilt 注入的 [AppDatabase] 实例操作，确保 checkpoint/close
 * 作用于运行时正在使用的数据库（Hilt 与备份共用同一单例）。
 *
 * 导入流程带回滚保护：
 * 1. 先解压到临时目录并校验（数据库文件存在且为合法 SQLite 文件）；
 * 2. 校验通过后把现有数据移入回滚目录；
 * 3. 用 rename 把新文件换入正式位置；
 * 4. 替换途中任何一步失败，都用回滚目录恢复原数据。
 */
object BackupHelper {

    private const val IMAGES_DIR_NAME = "recipe_images"
    private const val ZIP_DB_PREFIX = "db/"
    private const val ZIP_IMAGES_PREFIX = "images/"

    /** SQLite 数据库文件魔数（16 字节头） */
    private val SQLITE_MAGIC = "SQLite format 3\u0000".toByteArray(Charsets.ISO_8859_1)

    /**
     * 导出备份：先做 WAL checkpoint 把日志刷入主库文件，然后直接打包。
     * 不关闭数据库——Hilt 单例在导出后继续有效，应用可正常使用。
     */
    fun exportBackup(context: Context, database: AppDatabase, uri: Uri): Result<Unit> {
        return try {
            database.checkpointWal()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val imagesDir = File(context.filesDir, IMAGES_DIR_NAME)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                    if (dbFile.exists()) {
                        addFileToZip(zip, dbFile, ZIP_DB_PREFIX + dbFile.name)
                    }
                    if (imagesDir.exists() && imagesDir.isDirectory) {
                        imagesDir.listFiles()?.forEach { file ->
                            addFileToZip(zip, file, ZIP_IMAGES_PREFIX + file.name)
                        }
                    }
                }
            } ?: return Result.failure(IllegalStateException("无法写入备份文件"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 导入恢复。成功后调用方必须重启应用进程，让 Hilt 重新创建数据库实例。
     * 校验失败或替换失败时，原数据保持不变（或从回滚目录恢复）。
     */
    fun importBackup(context: Context, database: AppDatabase, uri: Uri): Result<Unit> {
        val stagingDir = File(context.cacheDir, "backup_import_staging")
        val rollbackDir = File(context.cacheDir, "backup_import_rollback")

        return try {
            // 第一阶段：解压到临时目录并校验，此阶段不触碰现有数据
            stagingDir.deleteRecursively()
            val extracted = context.contentResolver.openInputStream(uri)?.use { input ->
                extractBackupZip(input, stagingDir, AppDatabase.DATABASE_NAME)
            } ?: return Result.failure(IllegalStateException("无法读取备份文件"))

            val newDbFile = extracted.dbFile
                ?: return Result.failure(IllegalArgumentException("备份中缺少数据库文件，可能不是有效的备份包"))
            if (!isValidSqliteFile(newDbFile)) {
                return Result.failure(IllegalArgumentException("备份中的数据库文件已损坏或格式不正确"))
            }

            // 第二阶段：关闭数据库后原子替换，失败回滚
            database.checkpointWal()
            AppDatabase.closeAndClearInstance()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val imagesDir = File(context.filesDir, IMAGES_DIR_NAME)
            replaceWithRollback(
                dbFile = dbFile,
                imagesDir = imagesDir,
                newDbFile = newDbFile,
                newImagesDir = extracted.imagesDir,
                rollbackDir = rollbackDir
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            stagingDir.deleteRecursively()
            rollbackDir.deleteRecursively()
        }
    }

    internal data class ExtractedBackup(
        /** 解压出来的主数据库文件；备份包中没有则为 null */
        val dbFile: File?,
        /** 解压出来的图片目录（可能为空目录） */
        val imagesDir: File
    )

    /**
     * 把备份 zip 解压到 [stagingDir]（db/ 与 images/ 两个子目录）。
     * 对 zip 条目做路径穿越防护：只取文件名部分，并二次校验目标
     * 落在 staging 目录内，恶意条目（../ 等）直接跳过。
     */
    internal fun extractBackupZip(
        input: InputStream,
        stagingDir: File,
        dbFileName: String
    ): ExtractedBackup {
        val dbDir = File(stagingDir, "db").apply { mkdirs() }
        val imagesDir = File(stagingDir, "images").apply { mkdirs() }
        var dbFile: File? = null

        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val targetDir = when {
                    entry.isDirectory -> null
                    entry.name.startsWith(ZIP_DB_PREFIX) -> dbDir
                    entry.name.startsWith(ZIP_IMAGES_PREFIX) -> imagesDir
                    else -> null
                }
                if (targetDir != null) {
                    // 只取文件名部分，防止恶意 zip 的路径穿越（../）
                    val fileName = File(entry.name).name
                    if (fileName.isNotBlank() && fileName != "." && fileName != "..") {
                        val targetFile = File(targetDir, fileName)
                        // 双重防护：确认目标文件确实位于预期目录内
                        if (targetFile.canonicalPath.startsWith(targetDir.canonicalPath + File.separator)) {
                            FileOutputStream(targetFile).use { fos -> zip.copyTo(fos) }
                            if (targetDir == dbDir && fileName == dbFileName) {
                                dbFile = targetFile
                            }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return ExtractedBackup(dbFile = dbFile, imagesDir = imagesDir)
    }

    /**
     * 校验文件是否为合法 SQLite 数据库（检查 16 字节魔数头）。
     */
    internal fun isValidSqliteFile(file: File): Boolean {
        if (!file.isFile || file.length() < SQLITE_MAGIC.size) return false
        val header = ByteArray(SQLITE_MAGIC.size)
        return try {
            FileInputStream(file).use { fis ->
                var read = 0
                while (read < header.size) {
                    val n = fis.read(header, read, header.size - read)
                    if (n < 0) break
                    read += n
                }
                read == header.size && header.contentEquals(SQLITE_MAGIC)
            }
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 把现有数据移入 [rollbackDir]，再把新数据 rename 到正式位置；
     * 任何一步失败都尝试将原数据移回，保证不会留下半新半旧的状态。
     */
    private fun replaceWithRollback(
        dbFile: File,
        imagesDir: File,
        newDbFile: File,
        newImagesDir: File,
        rollbackDir: File
    ) {
        rollbackDir.deleteRecursively()
        rollbackDir.mkdirs()
        val rollbackDb = File(rollbackDir, dbFile.name)
        val rollbackImages = File(rollbackDir, IMAGES_DIR_NAME)

        // 清理残留 WAL/SHM：checkpoint(TRUNCATE) 后主库文件已完整，日志可安全删除
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()

        // 现有数据挪到回滚目录（同一内部存储分区，rename 是原子操作）
        val hadDb = dbFile.exists()
        val hadImages = imagesDir.exists()
        if (hadDb && !dbFile.renameTo(rollbackDb)) {
            throw IOException("无法备份现有数据库，已中止恢复")
        }
        if (hadImages && !imagesDir.renameTo(rollbackImages)) {
            // 把数据库移回去，保持原状
            if (hadDb) rollbackDb.renameTo(dbFile)
            throw IOException("无法备份现有图片目录，已中止恢复")
        }

        try {
            dbFile.parentFile?.mkdirs()
            if (!newDbFile.renameTo(dbFile)) {
                // 跨分区等 rename 失败场景退化为复制
                newDbFile.copyTo(dbFile, overwrite = true)
            }
            if (!newImagesDir.renameTo(imagesDir)) {
                newImagesDir.copyRecursively(imagesDir, overwrite = true)
            }
        } catch (e: Exception) {
            // 回滚：清掉半成品，把原数据移回
            dbFile.delete()
            imagesDir.deleteRecursively()
            if (hadDb) rollbackDb.renameTo(dbFile)
            if (hadImages) rollbackImages.renameTo(imagesDir)
            throw IOException("恢复备份失败，已还原原有数据", e)
        }
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { fis ->
            fis.copyTo(zip)
        }
        zip.closeEntry()
    }
}
