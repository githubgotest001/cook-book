package com.familyrecipe.book.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 图片处理工具类，负责菜谱封面图片的保存、缩放和删除。
 */
object ImageUtils {

    private const val MAX_DIMENSION = 1920
    private const val IMAGE_DIR = "recipe_images"
    private const val JPEG_QUALITY = 85

    /**
     * 将指定 Uri 的图片缩放后保存到应用内部存储。
     *
     * 采用两段式解码：先只读取图片尺寸计算采样率（inSampleSize），
     * 再按采样率解码，避免高像素照片整图解码导致 OOM。
     * 同时读取 EXIF 方向信息，矫正相机拍摄图片的旋转。
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
        val destFile = File(dir, "recipe_${System.currentTimeMillis()}.jpg")

        // 第一遍：只读尺寸
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: throw IOException("无法读取图片")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("图片格式不支持")
        }

        // 第二遍：按采样率解码
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        }
        var bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw IOException("图片解码失败")

        // 精确缩放到 MAX_DIMENSION 以内
        val scaled = scaleBitmap(bitmap, MAX_DIMENSION)
        if (scaled !== bitmap) {
            bitmap.recycle()
            bitmap = scaled
        }

        // EXIF 旋转矫正
        val rotationDegrees = readExifRotation(context, sourceUri)
        if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) {
                bitmap.recycle()
                bitmap = rotated
            }
        }

        try {
            FileOutputStream(destFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
        } finally {
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
     * 计算 BitmapFactory 采样率（2 的幂次），
     * 使解码后的最长边不小于但尽量接近 maxDimension。
     */
    internal fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        val maxSide = maxOf(width, height)
        while (maxSide / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun readExifRotation(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                when (ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
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
