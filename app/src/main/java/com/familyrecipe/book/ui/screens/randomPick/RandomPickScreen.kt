package com.familyrecipe.book.ui.screens.randomPick

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.domain.RandomWarning
import com.familyrecipe.book.ui.components.CategoryChip
import com.familyrecipe.book.ui.components.StarRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomPickScreen(
    onRecipeClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: RandomPickViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今天吃什么") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refreshRandom) {
                        Icon(Icons.Default.Refresh, contentDescription = "换一批")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 数量选择器
            CountSelector(
                currentCount = uiState.currentCount,
                onCountChange = viewModel::setCount
            )

            // 警告提示
            uiState.warning?.let { warning ->
                val message = when (warning) {
                    RandomWarning.NO_RECIPES -> "还没有菜谱，快去添加吧！"
                    RandomWarning.INSUFFICIENT_RECIPES -> "可用菜谱不足，已显示全部"
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.selectedRecipes.isEmpty() && uiState.warning == null) {
                // 空状态提示（无菜谱时，且没有已显示的警告）
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "还没有菜谱，快去添加吧！",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.selectedRecipes, key = { it.id }) { recipe ->
                        RandomRecipeCard(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountSelector(
    currentCount: Int,
    onCountChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "选择数量：$currentCount",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.width(16.dp))
        Slider(
            value = currentCount.toFloat(),
            onValueChange = { onCountChange(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RandomRecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 封面缩略图
            AsyncImage(
                model = recipe.coverImagePath,
                contentDescription = "${recipe.name}封面",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            // 菜谱信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 72.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 标题
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 推荐指数星形评分
                StarRating(
                    rating = recipe.recommendationIndex,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // 底部信息行：分类标签 + 烹饪时间
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryChip(category = recipe.recipeCategory)
                    Text(
                        text = "⏱ ${recipe.cookingMinutes}分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
