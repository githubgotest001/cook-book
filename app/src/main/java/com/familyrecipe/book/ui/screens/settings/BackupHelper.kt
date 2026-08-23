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
 * 必须通过 Hilt 注入的 [AppDatabase] 实例操作，确保关闭的是运行时正在使用的数据库。
 */
object BackupHelper {

    fun exportBackup(context: Context, database: AppDatabase, uri: Uri): Result<Unit> {
        return try {
            database.checkpointWal()
            AppDatabase.closeAndClearInstance()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val dbWalFile = File(dbFile.path + "-wal")
            val dbShmFile = File(dbFile.path + "-shm")
            val imagesDir = File(context.filesDir, "recipe_images")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                    if (dbFile.exists()) {
                        addFileToZip(zip, dbFile, "db/${dbFile.name}")
                    }
                    if (dbWalFile.exists()) {
                        addFileToZip(zip, dbWalFile, "db/${dbWalFile.name}")
                    }
                    if (dbShmFile.exists()) {
                        addFileToZip(zip, dbShmFile, "db/${dbShmFile.name}")
                    }
                    if (imagesDir.exists() && imagesDir.isDirectory) {
                        imagesDir.listFiles()?.forEach { file ->
                            addFileToZip(zip, file, "images/${file.name}")
                        }
                    }
                }
            } ?: return Result.failure(IllegalStateException("无法写入备份文件"))

            AppDatabase.getInstance(context)
            Result.success(Unit)
        } catch (e: Exception) {
            AppDatabase.getInstance(context)
            Result.failure(e)
        }
    }

    fun importBackup(context: Context, database: AppDatabase, uri: Uri): Result<Unit> {
        return try {
            database.checkpointWal()
            AppDatabase.closeAndClearInstance()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val imagesDir = File(context.filesDir, "recipe_images")

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        when {
                            entry.name.startsWith("db/") -> {
                                val fileName = entry.name.removePrefix("db/")
                                val targetFile = File(dbFile.parent, fileName)
                                targetFile.parentFile?.mkdirs()
                                FileOutputStream(targetFile).use { fos ->
                                    zip.copyTo(fos)
                                }
                            }
                            entry.name.startsWith("images/") -> {
                                val fileName = entry.name.removePrefix("images/")
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

            AppDatabase.getInstance(context)
            Result.success(Unit)
        } catch (e: Exception) {
            AppDatabase.getInstance(context)
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
