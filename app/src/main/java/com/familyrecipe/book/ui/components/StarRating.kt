package com.familyrecipe.book.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * 5 星评分组件
 *
 * @param rating 当前评分（1-5）
 * @param onRatingChange 评分变更回调，为 null 时组件为只读模式
 * @param modifier Modifier
 */
@Composable
fun StarRating(
    rating: Int,
    onRatingChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clampedRating = rating.coerceIn(1, 5)

    Row(
        modifier = modifier.semantics {
            contentDescription = "评分 $clampedRating 星（共 5 星）"
        }
    ) {
        for (i in 1..5) {
            val isFilled = i <= clampedRating
            val starModifier = if (onRatingChange != null) {
                Modifier
                    .size(24.dp)
                    .clickable { onRatingChange(i) }
            } else {
                Modifier.size(24.dp)
            }

            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = if (isFilled) "已选第 $i 星" else "未选第 $i 星",
                tint = if (isFilled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = starModifier
            )
        }
    }
}
