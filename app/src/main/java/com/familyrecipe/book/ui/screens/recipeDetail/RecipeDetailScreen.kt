package com.familyrecipe.book.ui.screens.recipeDetail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.familyrecipe.book.data.model.Preference
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeIngredient
import com.familyrecipe.book.ui.components.StarRating
import com.familyrecipe.book.ui.theme.AppTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onEditClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这道菜谱吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecipe(onBack)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.recipe?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            uiState.recipe?.let { recipe ->
                                shareRecipe(context, recipe, uiState.ingredients, uiState.steps)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.recipe == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("菜谱不存在")
            }
        } else {
            val recipe = uiState.recipe!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoverImage(recipe)

                // 推荐指数 + 收藏按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StarRating(rating = recipe.recommendationIndex)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (recipe.isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = if (recipe.isFavorite) "取消收藏" else "收藏",
                            tint = if (recipe.isFavorite) {
                                AppTheme.extendedColors.favorite
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                // 难度
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "难度",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StarRating(rating = recipe.difficulty, starSize = 16.dp)
                }

                if (recipe.description.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            "💬 ${recipe.description}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (uiState.ingredients.isNotEmpty()) {
                    IngredientSection(ingredients = uiState.ingredients)
                }

                if (uiState.steps.isNotEmpty()) {
                    StepsSection(steps = uiState.steps)
                }

                // 家庭成员喜好
                if (uiState.members.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Text(
                        "家人口味",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    uiState.members.forEach { member ->
                        val pref = uiState.preferences.find { it.memberId == member.id }
                        val currentPref = pref?.preference ?: Preference.NEUTRAL

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(member.name, style = MaterialTheme.typography.bodyLarge)
                            Row {
                                IconButton(onClick = {
                                    val newPref = if (currentPref == Preference.LIKE) Preference.NEUTRAL else Preference.LIKE
                                    viewModel.setPreference(member.id, newPref)
                                }) {
                                    Icon(
                                        if (currentPref == Preference.LIKE) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                        contentDescription = "喜欢",
                                        tint = if (currentPref == Preference.LIKE) {
                                            AppTheme.extendedColors.like
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                IconButton(onClick = {
                                    val newPref = if (currentPref == Preference.DISLIKE) Preference.NEUTRAL else Preference.DISLIKE
                                    viewModel.setPreference(member.id, newPref)
                                }) {
                                    Icon(
                                        if (currentPref == Preference.DISLIKE) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                        contentDescription = "不喜欢",
                                        tint = if (currentPref == Preference.DISLIKE) {
                                            AppTheme.extendedColors.dislike
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * 封面区域：图片底部渐变压暗，叠加分类和烹饪时长信息（美食 App 常见的 hero 布局）。
 * 不在组合期做 File.exists()：占位层始终在底部，由 Coil 的加载状态（onState）
 * 决定是否展示渐变层与深色标签；文件缺失/解码失败时自然回退到占位图。
 */
@Composable
private fun CoverImage(recipe: Recipe) {
    val imagePath = recipe.coverImagePath
    var imageLoaded by remember(imagePath) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(text = recipe.recipeCategory.emoji, fontSize = 64.sp)
        }

        if (imagePath != null) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "菜谱封面图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    imageLoaded = state is AsyncImagePainter.State.Success
                }
            )
        }

        if (imageLoaded) {
            // 底部渐变压暗，保证叠加文字可读
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )
        }

        // 叠加信息：分类 + 烹饪时长
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OverlayTag(
                text = "${recipe.recipeCategory.emoji} ${recipe.recipeCategory.label}",
                onImage = imageLoaded
            )
            if (recipe.cookingMinutes > 0) {
                OverlayTag(text = "⏱ ${recipe.cookingMinutes}分钟", onImage = imageLoaded)
            }
        }
    }
}

@Composable
private fun OverlayTag(text: String, onImage: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (onImage) {
            Color.Black.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (onImage) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun IngredientSection(ingredients: List<RecipeIngredient>) {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    Text(
        "食材清单",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            ingredients.forEachIndexed { index, ingredient ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ingredient.name.trim(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (ingredient.note.isNotBlank()) {
                            Text(
                                text = ingredient.note.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = (ingredient.amount + ingredient.unit).trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (index < ingredients.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

/**
 * 烹饪步骤区域：
 * - 点击步骤可勾选完成，带进度展示
 * - 屏幕常亮开关，做饭时不熄屏
 */
@Composable
private fun StepsSection(steps: List<String>) {
    var doneSteps by remember { mutableStateOf(setOf<Int>()) }
    var keepScreenOn by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("烹饪步骤", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.width(8.dp))
        if (doneSteps.isNotEmpty()) {
            Text(
                "${doneSteps.size}/${steps.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "屏幕常亮",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Switch(
            checked = keepScreenOn,
            onCheckedChange = { keepScreenOn = it }
        )
    }

    if (doneSteps.isNotEmpty()) {
        LinearProgressIndicator(
            progress = { doneSteps.size.toFloat() / steps.size },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }

    steps.forEachIndexed { index, step ->
        val isDone = index in doneSteps
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    doneSteps = if (isDone) doneSteps - index else doneSteps + index
                }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isDone) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "已完成",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = step,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                color = if (isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * 将菜谱格式化为纯文本并调起系统分享
 */
private fun shareRecipe(
    context: android.content.Context,
    recipe: Recipe,
    ingredients: List<RecipeIngredient>,
    steps: List<String>
) {
    val text = buildString {
        appendLine("🍳 ${recipe.name}")
        if (recipe.description.isNotBlank()) {
            appendLine(recipe.description)
        }
        appendLine("分类：${recipe.recipeCategory.label}  烹饪时间：${recipe.cookingMinutes}分钟")
        if (ingredients.isNotEmpty()) {
            appendLine()
            appendLine("【食材】")
            ingredients.forEach { appendLine("· ${it.displayText}") }
        }
        if (steps.isNotEmpty()) {
            appendLine()
            appendLine("【步骤】")
            steps.forEachIndexed { index, step -> appendLine("${index + 1}. $step") }
        }
        appendLine()
        append("—— 来自「家庭菜谱」")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享菜谱"))
}
