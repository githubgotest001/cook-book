package com.familyrecipe.book

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class RecipeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        cleanupCameraCache()
    }

    /** 清理相机拍照遗留的临时文件，避免 cacheDir 持续膨胀。 */
    private fun cleanupCameraCache() {
        runCatching {
            cacheDir.listFiles()
                ?.filter { it.isFile && it.name.startsWith("camera_photo_") }
                ?.forEach { it.delete() }
        }
    }
}
