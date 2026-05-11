package com.familyrecipe.book.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.familyrecipe.book.data.model.RecipeCategory

/**
 * 分类标签组件
 * 以 Chip 样式显示菜谱分类的中文标签
 *
 * @param category 菜谱分类枚举
 * @param modifier Modifier
 */
@Composable
fun CategoryChip(
    category: RecipeCategory,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { /* 仅展示用途，无点击行为 */ },
        label = {
            Text(
                text = category.label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier
    )
}
