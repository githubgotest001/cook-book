package com.familyrecipe.book.ui.screens.recipeList

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import com.familyrecipe.book.data.model.RecipeCategory
import com.familyrecipe.book.data.model.SortDimension
import com.familyrecipe.book.data.model.SortOrder
import com.familyrecipe.book.ui.components.CategoryChip
import com.familyrecipe.book.ui.components.StarRating

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    onRecipeClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onMembersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShoppingListClick: () -> Unit,
    onRandomPickClick: () -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("家庭菜谱") },
                actions = {
                    // "今天吃什么"随机选菜按钮
                    IconButton(onClick = onRandomPickClick) {
                        Icon(Icons.Default.Casino, contentDescription = "今天吃什么")
                    }
                    IconButton(onClick = onShoppingListClick) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "购物清单")
                    }
                    IconButton(onClick = onMembersClick) {
                        Icon(Icons.Default.People, contentDescription = "家庭成员")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "添加菜谱")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜索栏
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索菜谱...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            // 家庭成员筛选标签行
            if (uiState.familyMembers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.familyMembers.forEach { member ->
                        val isSelected = member.id in uiState.recipeFilter.selectedMemberIds
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onMemberFilterToggle(member.id) },
                            label = { Text(member.name) }
                        )
                    }
                }
            }

            // 分类筛选标签行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "全部"选项，用于清除分类筛选
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { viewModel.onCategoryFilterChange(null) },
                    label = { Text("全部") }
                )
                RecipeCategory.entries.forEach { category ->
                    val isSelected = uiState.selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategoryFilterChange(category) },
                        label = { Text(category.label) }
                    )
                }
            }

            // 排序维度选择器
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortDimension.entries.forEach { dimension ->
                    val isSelected = uiState.sortConfig.dimension == dimension
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onSortDimensionChange(dimension) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dimension.label)
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (uiState.sortConfig.order == SortOrder.DESC) {
                                            Icons.Default.ArrowDownward
                                        } else {
                                            Icons.Default.ArrowUpward
                                        },
                                        contentDescription = if (uiState.sortConfig.order == SortOrder.DESC) "降序" else "升序",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 内容区域
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.recipes.isEmpty()) {
                // 空状态提示
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val hasFilters = uiState.searchQuery.isNotBlank() ||
                            uiState.recipeFilter.selectedMemberIds.isNotEmpty() ||
                            uiState.selectedCategory != null
                    Text(
                        text = if (hasFilters) {
                            "没有匹配的菜谱，试试调整筛选条件"
                        } else {
                            "还没有菜谱，点击 + 添加"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.recipes, key = { it.id }) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(recipe.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
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
            if (recipe.coverImagePath != null) {
                AsyncImage(
                    model = recipe.coverImagePath,
                    contentDescription = "${recipe.name}封面",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 无封面时显示占位图标
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "无封面",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 菜谱信息
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 72.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 标题行
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 推荐指数星形图标
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
                    if (recipe.cookingMinutes > 0) {
                        Text(
                            text = "⏱ ${recipe.cookingMinutes}分钟",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 收藏图标
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.align(Alignment.Top)
            ) {
                Icon(
                    imageVector = if (recipe.isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = if (recipe.isFavorite) "取消收藏" else "收藏",
                    tint = if (recipe.isFavorite) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
