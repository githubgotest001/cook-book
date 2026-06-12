package com.familyrecipe.book.ui.screens.randomPick

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeCategory
import com.familyrecipe.book.domain.RandomWarning
import com.familyrecipe.book.ui.components.CategoryChip
import com.familyrecipe.book.ui.components.RecipeCoverThumb
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
                title = { Text("今天吃什么 🎲") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::refreshRandom,
                icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                text = { Text("换一批") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 分类过滤：想喝汤就只随机汤
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.setCategory(null) },
                    label = { Text("不限") }
                )
                RecipeCategory.entries.forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text("${category.emoji} ${category.label}") }
                    )
                }
            }

            // 数量选择器
            CountSelector(
                currentCount = uiState.currentCount,
                onCountChange = viewModel::setCount
            )

            // 警告提示
            uiState.warning?.let { warning ->
                val message = when (warning) {
                    RandomWarning.NO_RECIPES ->
                        if (uiState.selectedCategory != null) {
                            "这个分类下还没有菜谱，换个分类试试"
                        } else {
                            "还没有菜谱，快去添加吧！"
                        }
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "🎲", fontSize = 56.sp)
                        Text(
                            text = "还没有菜谱，快去添加吧！",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp
                    ),
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
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RecipeCoverThumb(
                coverImagePath = recipe.coverImagePath,
                contentDescription = "${recipe.name}封面",
                size = 80.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 80.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                StarRating(
                    rating = recipe.recommendationIndex,
                    starSize = 18.dp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryChip(category = recipe.recipeCategory)
                    if (recipe.cookingMinutes > 0) {
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
}
