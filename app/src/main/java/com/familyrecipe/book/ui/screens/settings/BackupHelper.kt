package com.familyrecipe.book.ui.screens.settings

import android.content.Context
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
 * 必须通过 Hilt 注入的 [AppDatabase] 实例操作，确保 checkpoint/close
 * 作用于运行时正在使用的数据库（Hilt 与备份共用同一单例）。
 */
object BackupHelper {

    /**
     * 导出备份：先做 WAL checkpoint 把日志刷入主库文件，然后直接打包。
     * 不关闭数据库——Hilt 单例在导出后继续有效，应用可正常使用。
     */
    fun exportBackup(context: Context, database: AppDatabase, uri: Uri): Result<Unit> {
        return try {
            database.checkpointWal()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val imagesDir = File(context.filesDir, "recipe_images")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                    if (dbFile.exists()) {
                        addFileToZip(zip, dbFile, "db/${dbFile.name}")
                    }
                    if (imagesDir.exists() && imagesDir.isDirectory) {
                        imagesDir.listFiles()?.forEach { file ->
                            addFileToZip(zip, file, "images/${file.name}")
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
     * 导入恢复：checkpoint 后关闭数据库，再用备份内容覆盖本地文件。
     * 调用方必须在成功后重启应用进程，让 Hilt 重新创建数据库实例。
     */
    fun importBackup(context: Context, database: AppDatabase, uri: Uri): Result<Unit> {
        return try {
            database.checkpointWal()
            AppDatabase.closeAndClearInstance()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val imagesDir = File(context.filesDir, "recipe_images")

            // 删除残留的 WAL/SHM，避免旧日志混入恢复后的主库文件
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        // 只取文件名部分，防止恶意 zip 的路径穿越（../）
                        val fileName = File(entry.name).name
                        when {
                            entry.isDirectory || fileName.isBlank() -> Unit
                            entry.name.startsWith("db/") -> {
                                val targetFile = File(dbFile.parentFile, fileName)
                                targetFile.parentFile?.mkdirs()
                                FileOutputStream(targetFile).use { fos ->
                                    zip.copyTo(fos)
                                }
                            }
                            entry.name.startsWith("images/") -> {
                                imagesDir.mkdirs()
                                val targetFile = File(imagesDir, fileName)
                                FileOutputStream(targetFile).use { fos ->
                                    zip.copyTo(fos)
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return Result.failure(IllegalStateException("无法读取备份文件"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
