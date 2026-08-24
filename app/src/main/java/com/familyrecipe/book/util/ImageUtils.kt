package com.familyrecipe.book.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
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

    /** 缓存目录中属于本应用的临时文件前缀（拍照原图、图片导入中转文件） */
    internal val TEMP_CACHE_PREFIXES = listOf("camera_photo_", "recipe_import_")

    /**
     * 清理缓存目录中残留的临时文件（拍照原图、图片导入中转文件）。
     * 进程被杀、拍照取消等场景会留下孤儿文件，应用启动时统一兜底清理。
     *
     * @return 删除的文件数
     */
    fun cleanupTempCacheFiles(cacheDir: File): Int {
        val files = cacheDir.listFiles() ?: return 0
        return files.count { file ->
            file.isFile &&
                TEMP_CACHE_PREFIXES.any { prefix -> file.name.startsWith(prefix) } &&
                file.delete()
        }
    }

    /**
     * 将指定 Uri 的图片缩放后保存到应用内部存储。
     *
     * 相册/相机返回的 content:// URI 不能直接用 [BitmapFactory.decodeStream] 解码
     * （流不可 seek，易返回 null）。先复制到临时文件，再用 ImageDecoder（API 28+）
     * 或 decodeFile 解码，兼容性更好。
     */
    suspend fun saveImage(
        context: Context,
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }
        val destFile = File(dir, "recipe_${System.currentTimeMillis()}.jpg")

        val temp = copyUriToTempFile(context, sourceUri)
        try {
            val decoded = decodeBitmap(temp.file)
            var bitmap = decoded.bitmap
            if (decoded.applyExifRotation) {
                val rotation = readExifRotation(temp.file)
                if (rotation != 0f) {
                    bitmap = rotateBitmap(bitmap, rotation)
                }
            }

            try {
                FileOutputStream(destFile).use { output ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                        throw IOException("图片写入失败")
                    }
                }
            } finally {
                bitmap.recycle()
            }
        } finally {
            if (temp.shouldDelete) {
                temp.file.delete()
            }
        }

        destFile.absolutePath
    }

    /**
     * 删除指定路径的图片文件。
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

    /**
     * 按比例缩放 Bitmap，使最长边不超过 maxDimension。
     */
    internal fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private data class DecodedBitmap(
        val bitmap: Bitmap,
        /** ImageDecoder 已自动处理 EXIF 方向，无需再旋转 */
        val applyExifRotation: Boolean
    )

    private data class TempImageFile(
        val file: File,
        val shouldDelete: Boolean
    )

    /**
     * 将 URI 内容复制到临时文件，便于多次读取尺寸/EXIF/解码。
     */
    private fun copyUriToTempFile(context: Context, uri: Uri): TempImageFile {
        // file:// 且可直接访问时跳过复制
        if (uri.scheme == ContentResolverScheme.FILE) {
            val path = uri.path
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    return TempImageFile(file = file, shouldDelete = false)
                }
            }
        }

        val suffix = guessImageSuffix(context, uri)
        val tempFile = File.createTempFile("recipe_import_", suffix, context.cacheDir)

        val copied = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            tempFile.length() > 0L
        } catch (_: Exception) {
            false
        }

        if (!copied) {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                tempFile.outputStream().use { output ->
                    java.io.FileInputStream(pfd.fileDescriptor).use { input ->
                        input.copyTo(output)
                    }
                }
            } ?: throw IOException("无法读取图片")
        }

        if (tempFile.length() <= 0L) {
            tempFile.delete()
            throw IOException("图片文件为空")
        }
        return TempImageFile(file = tempFile, shouldDelete = true)
    }

    private fun guessImageSuffix(context: Context, uri: Uri): String {
        return when (context.contentResolver.getType(uri)) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/heic", "image/heif" -> ".heic"
            else -> ".jpg"
        }
    }

    private fun decodeBitmap(tempFile: File): DecodedBitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return DecodedBitmap(
                    bitmap = decodeWithImageDecoder(tempFile),
                    applyExifRotation = false
                )
            } catch (_: Exception) {
                // 降级到 BitmapFactory
            }
        }
        return DecodedBitmap(
            bitmap = decodeWithBitmapFactory(tempFile),
            applyExifRotation = true
        )
    }

    private fun decodeWithImageDecoder(tempFile: File): Bitmap {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(tempFile)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
            val width = info.size.width
            val height = info.size.height
            val maxSide = maxOf(width, height)
            if (maxSide > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / maxSide
                decoder.setTargetSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
    }

    private fun decodeWithBitmapFactory(tempFile: File): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(tempFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("图片格式不支持")
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        }
        var bitmap = BitmapFactory.decodeFile(tempFile.absolutePath, decodeOptions)
            ?: throw IOException("图片解码失败")

        val scaled = scaleBitmap(bitmap, MAX_DIMENSION)
        if (scaled !== bitmap) {
            bitmap.recycle()
            bitmap = scaled
        }
        return bitmap
    }

    private fun readExifRotation(file: File): Float {
        return try {
            when (
                ExifInterface(file.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (_: Exception) {
            0f
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    private object ContentResolverScheme {
        const val FILE = "file"
    }
}
