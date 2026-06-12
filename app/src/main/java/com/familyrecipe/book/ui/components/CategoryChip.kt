package com.familyrecipe.book.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.familyrecipe.book.data.model.RecipeCategory

/**
 * 各分类的专属配色（容器色 / 文字色），深浅主题各一套。
 * 用颜色 + emoji 区分分类，让列表更有"食欲感"。
 */
private fun categoryColors(category: RecipeCategory, darkTheme: Boolean): Pair<Color, Color> {
    return if (!darkTheme) {
        when (category) {
            RecipeCategory.STIR_FRY -> Color(0xFFFFE3CC) to Color(0xFF8A3C00)
            RecipeCategory.SOUP -> Color(0xFFF3E2CD) to Color(0xFF6B4A2B)
            RecipeCategory.QUICK_MEAL -> Color(0xFFFFF1BF) to Color(0xFF775C00)
            RecipeCategory.STAPLE -> Color(0xFFF0E8CE) to Color(0xFF615425)
            RecipeCategory.COLD_DISH -> Color(0xFFDDF0D6) to Color(0xFF2F6B33)
            RecipeCategory.DESSERT -> Color(0xFFFFDFE7) to Color(0xFF99344E)
            RecipeCategory.BEVERAGE -> Color(0xFFD6EDF2) to Color(0xFF1F6470)
            RecipeCategory.OTHER -> Color(0xFFE9E2DA) to Color(0xFF5C5249)
        }
    } else {
        when (category) {
            RecipeCategory.STIR_FRY -> Color(0xFF5A3315) to Color(0xFFFFC899)
            RecipeCategory.SOUP -> Color(0xFF4C3A26) to Color(0xFFE8CCA8)
            RecipeCategory.QUICK_MEAL -> Color(0xFF534718) to Color(0xFFF2DD8E)
            RecipeCategory.STAPLE -> Color(0xFF494120) to Color(0xFFDCD09A)
            RecipeCategory.COLD_DISH -> Color(0xFF2C4528) to Color(0xFFB5DCAC)
            RecipeCategory.DESSERT -> Color(0xFF55293A) to Color(0xFFFFB7C9)
            RecipeCategory.BEVERAGE -> Color(0xFF1F424A) to Color(0xFFA8D8E2)
            RecipeCategory.OTHER -> Color(0xFF44403A) to Color(0xFFD3CCC4)
        }
    }
}

/**
 * 分类标签组件
 * emoji + 中文标签，按分类着色的小圆角胶囊
 *
 * @param category 菜谱分类枚举
 * @param modifier Modifier
 */
@Composable
fun CategoryChip(
    category: RecipeCategory,
    modifier: Modifier = Modifier
) {
    val (container, content) = categoryColors(category, isSystemInDarkTheme())

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = container,
        modifier = modifier
    ) {
        Text(
            text = "${category.emoji} ${category.label}",
            style = MaterialTheme.typography.labelMedium,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
