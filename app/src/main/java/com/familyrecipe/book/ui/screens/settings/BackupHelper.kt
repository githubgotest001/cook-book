package com.familyrecipe.book.ui.screens.settings

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.familyrecipe.book.data.database.AppDatabase
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份/恢复工具：将数据库文件 + 图片目录打包为 zip。
 * 导入时先解压到临时目录并校验，再原子替换；失败则回滚，避免半覆盖损坏数据。
 */
object BackupHelper {

    private const val IMAGES_DIR = "recipe_images"
    private const val STAGING_DIR = "backup_staging"
    private const val ROLLBACK_DIR = "backup_rollback"

    fun exportBackup(context: Context, database: AppDatabase, uri: Uri): Result<Unit> {
        return try {
            database.checkpointWal()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val imagesDir = File(context.filesDir, IMAGES_DIR)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                    if (dbFile.exists()) {
                        addFileToZip(zip, dbFile, "db/${dbFile.name}")
                    }
                    if (imagesDir.exists() && imagesDir.isDirectory) {
                        imagesDir.listFiles()?.forEach { file ->
                            if (file.isFile) {
                                addFileToZip(zip, file, "images/${file.name}")
                            }
                        }
                    }
                }
            } ?: return Result.failure(IllegalStateException("无法写入备份文件"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importBackup(context: Context, database: AppDatabase, uri: Uri): Result<Unit> {
        val stagingRoot = File(context.cacheDir, STAGING_DIR)
        val rollbackRoot = File(context.cacheDir, ROLLBACK_DIR)
        return try {
            database.checkpointWal()
            AppDatabase.closeAndClearInstance()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val imagesDir = File(context.filesDir, IMAGES_DIR)

            stagingRoot.deleteRecursively()
            rollbackRoot.deleteRecursively()
            stagingRoot.mkdirs()
            rollbackRoot.mkdirs()

            // 先快照现有数据，失败时可回滚
            snapshotCurrentData(dbFile, imagesDir, rollbackRoot)

            val stagingDbDir = File(stagingRoot, "db").apply { mkdirs() }
            val stagingImagesDir = File(stagingRoot, "images").apply { mkdirs() }

            val input = context.contentResolver.openInputStream(uri)
            if (input == null) {
                restoreSnapshot(dbFile, imagesDir, rollbackRoot)
                return Result.failure(IllegalStateException("无法读取备份文件"))
            }
            input.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        val current = entry!!
                        val safeName = sanitizeZipEntryName(current.name)
                        if (safeName != null && !current.isDirectory) {
                            when {
                                current.name.startsWith("db/") -> {
                                    FileOutputStream(File(stagingDbDir, safeName)).use { fos ->
                                        zip.copyTo(fos)
                                    }
                                }
                                current.name.startsWith("images/") -> {
                                    FileOutputStream(File(stagingImagesDir, safeName)).use { fos ->
                                        zip.copyTo(fos)
                                    }
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }

            val stagedDb = File(stagingDbDir, AppDatabase.DATABASE_NAME)
            if (!stagedDb.exists()) {
                // 兼容仅含单个 db 文件名不同的情况：取 stagingDbDir 下第一个文件
                val fallback = stagingDbDir.listFiles()?.firstOrNull { it.isFile }
                if (fallback == null) {
                    restoreSnapshot(dbFile, imagesDir, rollbackRoot)
                    return Result.failure(IllegalStateException("备份中缺少数据库文件"))
                }
                fallback.copyTo(stagedDb, overwrite = true)
            }

            validateSqliteFile(stagedDb).getOrElse { err ->
                restoreSnapshot(dbFile, imagesDir, rollbackRoot)
                return Result.failure(err)
            }

            // 原子替换：先清 WAL，再覆盖主库与图片
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            dbFile.parentFile?.mkdirs()
            stagedDb.copyTo(dbFile, overwrite = true)

            if (imagesDir.exists()) {
                imagesDir.listFiles()?.forEach { it.deleteRecursively() }
            } else {
                imagesDir.mkdirs()
            }
            stagingImagesDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.copyTo(File(imagesDir, file.name), overwrite = true)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            runCatching {
                val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
                val imagesDir = File(context.filesDir, IMAGES_DIR)
                restoreSnapshot(dbFile, imagesDir, rollbackRoot)
            }
            Result.failure(e)
        } finally {
            stagingRoot.deleteRecursively()
            rollbackRoot.deleteRecursively()
        }
    }

    /**
     * 仅保留 zip 条目的文件名，拒绝空名与路径穿越。
     * 返回 null 表示应跳过该条目。
     */
    fun sanitizeZipEntryName(entryName: String): String? {
        val name = File(entryName).name
        if (name.isBlank() || name == "." || name == "..") return null
        if (entryName.contains("..")) return null
        return name
    }

    private fun validateSqliteFile(file: File): Result<Unit> {
        return try {
            SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                db.rawQuery("SELECT count(*) FROM sqlite_master", null).use { cursor ->
                    if (!cursor.moveToFirst()) {
                        return Result.failure(IllegalStateException("备份数据库无法读取"))
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(IllegalStateException("备份数据库无效: ${e.message}", e))
        }
    }

    private fun snapshotCurrentData(dbFile: File, imagesDir: File, rollbackRoot: File) {
        if (dbFile.exists()) {
            dbFile.copyTo(File(rollbackRoot, dbFile.name), overwrite = true)
        }
        val wal = File(dbFile.path + "-wal")
        val shm = File(dbFile.path + "-shm")
        if (wal.exists()) wal.copyTo(File(rollbackRoot, wal.name), overwrite = true)
        if (shm.exists()) shm.copyTo(File(rollbackRoot, shm.name), overwrite = true)

        val imagesBackup = File(rollbackRoot, "images")
        imagesBackup.mkdirs()
        if (imagesDir.exists()) {
            imagesDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.copyTo(File(imagesBackup, file.name), overwrite = true)
                }
            }
        }
        File(rollbackRoot, ".ok").writeText("1")
    }

    private fun restoreSnapshot(dbFile: File, imagesDir: File, rollbackRoot: File) {
        if (!File(rollbackRoot, ".ok").exists()) return
        val snapDb = File(rollbackRoot, dbFile.name)
        if (snapDb.exists()) {
            dbFile.parentFile?.mkdirs()
            snapDb.copyTo(dbFile, overwrite = true)
        }
        val walSnap = File(rollbackRoot, dbFile.name + "-wal")
        val shmSnap = File(rollbackRoot, dbFile.name + "-shm")
        val wal = File(dbFile.path + "-wal")
        val shm = File(dbFile.path + "-shm")
        if (walSnap.exists()) walSnap.copyTo(wal, overwrite = true) else wal.delete()
        if (shmSnap.exists()) shmSnap.copyTo(shm, overwrite = true) else shm.delete()

        if (imagesDir.exists()) {
            imagesDir.listFiles()?.forEach { it.deleteRecursively() }
        } else {
            imagesDir.mkdirs()
        }
        File(rollbackRoot, "images").listFiles()?.forEach { file ->
            if (file.isFile) {
                file.copyTo(File(imagesDir, file.name), overwrite = true)
            }
        }
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { fis -> fis.copyTo(zip) }
        zip.closeEntry()
    }
}
