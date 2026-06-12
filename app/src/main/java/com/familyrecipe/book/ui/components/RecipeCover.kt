package com.familyrecipe.book.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * 菜谱封面缩略图。
 * 统一处理无封面/文件丢失时的占位展示，供列表页、随机选菜页等复用。
 *
 * @param coverImagePath 封面图片绝对路径，可为 null
 * @param contentDescription 无障碍描述
 * @param size 缩略图边长
 */
@Composable
fun RecipeCoverThumb(
    coverImagePath: String?,
    contentDescription: String?,
    size: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val fileExists = coverImagePath != null && File(coverImagePath).exists()

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (fileExists) {
            AsyncImage(
                model = File(coverImagePath!!),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(size / 2.2f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
