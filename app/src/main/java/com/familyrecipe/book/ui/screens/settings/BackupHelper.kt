package com.familyrecipe.book.ui.screens.settings

import android.content.Context
import android.net.Uri
import com.familyrecipe.book.data.database.AppDatabase
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份/恢复工具：将数据库文件 + 图片目录打包为 zip
 */
object BackupHelper {

    /**
     * 导出备份到用户选择的 URI
     */
    fun exportBackup(context: Context, uri: Uri): Result<Unit> {
        return try {
            // 先关闭数据库确保数据完整
            AppDatabase.closeDatabase()

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val dbWalFile = File(dbFile.path + "-wal")
            val dbShmFile = File(dbFile.path + "-shm")
            val imagesDir = File(context.filesDir, "recipe_images")

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                    // 打包数据库文件
                    if (dbFile.exists()) {
                        addFileToZip(zip, dbFile, "db/${dbFile.name}")
                    }
                    if (dbWalFile.exists()) {
                        addFileToZip(zip, dbWalFile, "db/${dbWalFile.name}")
                    }
                    if (dbShmFile.exists()) {
                        addFileToZip(zip, dbShmFile, "db/${dbShmFile.name}")
                    }

                    // 打包图片目录
                    if (imagesDir.exists() && imagesDir.isDirectory) {
                        imagesDir.listFiles()?.forEach { file ->
                            addFileToZip(zip, file, "images/${file.name}")
                        }
                    }
                }
            }

            // 重新打开数据库
            AppDatabase.getInstance(context)
            Result.success(Unit)
        } catch (e: Exception) {
            // 确保数据库重新打开
            AppDatabase.getInstance(context)
            Result.failure(e)
        }
    }

    /**
     * 从用户选择的 URI 导入备份
     */
    fun importBackup(context: Context, uri: Uri): Result<Unit> {
        return try {
            // 关闭数据库
            AppDatabase.closeDatabase()

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
            }

            // 重新打开数据库
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
