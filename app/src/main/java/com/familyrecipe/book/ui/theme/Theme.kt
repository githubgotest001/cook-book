package com.familyrecipe.book.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE85D04),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB3),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF6F5B40),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBDEBC),
    onSecondaryContainer = Color(0xFF271904),
    tertiary = Color(0xFF51643F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4EABA),
    onTertiaryContainer = Color(0xFF102003),
    background = Color(0xFFFFF8F1),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFF8F1),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFF1E0D0),
    onSurfaceVariant = Color(0xFF50453A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDF1E5),
    surfaceContainer = Color(0xFFF8EBDD),
    surfaceContainerHigh = Color(0xFFF2E5D6),
    surfaceContainerHighest = Color(0xFFECDFD0),
    outline = Color(0xFF827568),
    outlineVariant = Color(0xFFD5C3B5),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB77C),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6A3C00),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = Color(0xFFDDC2A1),
    onSecondary = Color(0xFF3E2D16),
    secondaryContainer = Color(0xFF56442A),
    onSecondaryContainer = Color(0xFFFBDEBC),
    tertiary = Color(0xFFB8CDA0),
    onTertiary = Color(0xFF243515),
    tertiaryContainer = Color(0xFF3A4C29),
    onTertiaryContainer = Color(0xFFD4EABA),
    background = Color(0xFF17120D),
    onBackground = Color(0xFFEBE1D8),
    surface = Color(0xFF17120D),
    onSurface = Color(0xFFEBE1D8),
    surfaceVariant = Color(0xFF50453A),
    onSurfaceVariant = Color(0xFFD5C3B5),
    surfaceContainerLowest = Color(0xFF120D08),
    surfaceContainerLow = Color(0xFF1F1A14),
    surfaceContainer = Color(0xFF241E17),
    surfaceContainerHigh = Color(0xFF2F2820),
    surfaceContainerHighest = Color(0xFF3A322A),
    outline = Color(0xFF9D8E80),
    outlineVariant = Color(0xFF50453A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

/**
 * 语义化扩展色：喜欢/不喜欢/收藏/评分星/主页横幅渐变。
 * 收敛各页面散落的硬编码颜色，并跟随深浅主题自动调整。
 */
@Immutable
data class ExtendedColors(
    val like: Color,
    val dislike: Color,
    val favorite: Color,
    /** 评分星星的琥珀色（食欲色系，区别于主色） */
    val star: Color,
    /** "今天吃什么"横幅渐变起止色及其上的文字色 */
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color
)

private val LightExtendedColors = ExtendedColors(
    like = Color(0xFF3E8948),
    dislike = Color(0xFFC23B2E),
    favorite = Color(0xFFD8385E),
    star = Color(0xFFF5A623),
    heroStart = Color(0xFFF2620F),
    heroEnd = Color(0xFFFF9D45),
    onHero = Color.White
)

private val DarkExtendedColors = ExtendedColors(
    like = Color(0xFF8FD694),
    dislike = Color(0xFFFF9B8D),
    favorite = Color(0xFFFF8FAC),
    star = Color(0xFFFFC95C),
    heroStart = Color(0xFF93481A),
    heroEnd = Color(0xFF6B3410),
    onHero = Color(0xFFFFE9D6)
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** 便捷访问扩展色：AppTheme.extendedColors.like 等 */
object AppTheme {
    val extendedColors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}

@Composable
fun FamilyRecipeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
