package com.familyrecipe.book.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 图片处理工具类，负责菜谱封面图片的保存、缩放和删除。
 */
object ImageUtils {

    private const val MAX_DIMENSION = 1920
    private const val IMAGE_DIR = "recipe_images"
    private const val JPEG_QUALITY = 85

    /**
     * 将指定 Uri 的图片缩放后保存到应用内部存储。
     * 图片最长边不超过 1920px，保存为 JPEG 格式。
     *
     * @param context 应用上下文
     * @param sourceUri 源图片 Uri（来自相册或相机）
     * @return 保存后的文件绝对路径
     */
    suspend fun saveImage(
        context: Context,
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }
        val fileName = "recipe_${System.currentTimeMillis()}.jpg"
        val destFile = File(dir, fileName)

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            val scaled = scaleBitmap(bitmap, MAX_DIMENSION)
            FileOutputStream(destFile).use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
        }

        destFile.absolutePath
    }

    /**
     * 删除指定路径的图片文件。
     *
     * @param path 图片文件的绝对路径
     */
    fun deleteImage(path: String) {
        File(path).delete()
    }

    /**
     * 按比例缩放 Bitmap，使最长边不超过 maxDimension。
     * 如果图片已经小于等于 maxDimension，则返回原始 Bitmap。
     * 使用 internal 可见性以便属性测试可以直接测试此方法。
     *
     * @param bitmap 原始 Bitmap
     * @param maxDimension 最长边的最大像素值
     * @return 缩放后的 Bitmap（如果无需缩放则返回原始 Bitmap）
     */
    internal fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxSide
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
