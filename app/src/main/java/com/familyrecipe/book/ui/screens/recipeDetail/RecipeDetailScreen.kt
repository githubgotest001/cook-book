package com.familyrecipe.book.ui.screens.recipeDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.familyrecipe.book.data.model.Preference
import com.familyrecipe.book.ui.components.CategoryChip
import com.familyrecipe.book.ui.components.StarRating
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onEditClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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
                // 封面图片区域
                val imagePath = recipe.coverImagePath
                val imageFileExists = imagePath != null && File(imagePath).exists()

                if (imageFileExists) {
                    AsyncImage(
                        model = File(imagePath!!),
                        contentDescription = "菜谱封面图片",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // 占位图：灰色背景 + 食物图标
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = "无封面图片",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 推荐指数 + 分类标签 + 收藏按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 推荐指数星形（只读）
                    StarRating(
                        rating = recipe.recommendationIndex,
                        onRatingChange = null
                    )

                    // 分类标签
                    CategoryChip(category = recipe.recipeCategory)

                    Spacer(modifier = Modifier.weight(1f))

                    // 收藏切换按钮
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (recipe.isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = if (recipe.isFavorite) "取消收藏" else "收藏",
                            tint = if (recipe.isFavorite) Color(0xFFE91E63) else Color.Gray
                        )
                    }
                }

                // 基本信息
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text("⏱ ${recipe.cookingMinutes}分钟") }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("难度 ${"★".repeat(recipe.difficulty)}") }
                    )
                }

                if (recipe.description.isNotBlank()) {
                    Text(
                        recipe.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 步骤
                if (uiState.steps.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Text(
                        "烹饪步骤",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    uiState.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.width(28.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = step, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
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
                                        tint = if (currentPref == Preference.LIKE) Color(0xFF4CAF50) else Color.Gray
                                    )
                                }
                                IconButton(onClick = {
                                    val newPref = if (currentPref == Preference.DISLIKE) Preference.NEUTRAL else Preference.DISLIKE
                                    viewModel.setPreference(member.id, newPref)
                                }) {
                                    Icon(
                                        if (currentPref == Preference.DISLIKE) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                        contentDescription = "不喜欢",
                                        tint = if (currentPref == Preference.DISLIKE) Color(0xFFF44336) else Color.Gray
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
