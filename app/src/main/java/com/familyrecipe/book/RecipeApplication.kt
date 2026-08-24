package com.familyrecipe.book

import android.app.Application
import com.familyrecipe.book.util.ImageUtils
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class RecipeApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // 兜底清理上次会话残留的相机拍照/图片导入临时文件
        applicationScope.launch {
            ImageUtils.cleanupTempCacheFiles(cacheDir)
        }
    }
}
