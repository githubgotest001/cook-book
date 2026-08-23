package com.familyrecipe.book.ui.screens.recipeList

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.familyrecipe.book.data.model.Recipe
import com.familyrecipe.book.data.model.RecipeCategory
import com.familyrecipe.book.data.model.SortDimension
import com.familyrecipe.book.data.model.SortOrder
import com.familyrecipe.book.ui.components.CategoryChip
import com.familyrecipe.book.ui.components.EmptyStateBox
import com.familyrecipe.book.ui.components.LoadingBox
import com.familyrecipe.book.ui.components.RecipeCoverThumb
import com.familyrecipe.book.ui.components.StarRating
import com.familyrecipe.book.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeListScreen(
    onRecipeClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onEditRecipe: (Long) -> Unit,
    onMembersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onShoppingListClick: () -> Unit,
    onRandomPickClick: () -> Unit,
    viewModel: RecipeListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Recipe?>(null) }

    val filterActiveCount = uiState.recipeFilter.selectedMemberIds.size +
            (if (uiState.selectedCategory != null) 1 else 0)
    val hasAnyFilter = filterActiveCount > 0 || uiState.searchQuery.isNotBlank()

    // 删除确认对话框
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除菜谱") },
            text = { Text("确定要删除「${target.name}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecipe(target.id)
                    deleteTarget = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }

    // 筛选 BottomSheet
    if (showFilterSheet) {
        FilterBottomSheet(
            uiState = uiState,
            onMemberToggle = viewModel::onMemberFilterToggle,
            onCategoryChange = viewModel::onCategoryFilterChange,
            onReset = viewModel::clearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("家庭菜谱") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onShoppingListClick) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "购物清单")
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("家庭成员") },
                                leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onMembersClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("设置") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    onSettingsClick()
                                }
                            )
                        }
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
            // 搜索 + 排序 + 筛选 一行搞定
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("搜索菜名、食材...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除搜索")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge
                )

                SortMenuButton(
                    sortConfig = uiState.sortConfig,
                    onSortDimensionChange = viewModel::onSortDimensionChange
                )

                BadgedBox(
                    badge = {
                        if (filterActiveCount > 0) {
                            Badge { Text("$filterActiveCount") }
                        }
                    }
                ) {
                    FilledTonalIconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "筛选")
                    }
                }
            }

            // "今天吃什么"入口横幅
            RandomPickBanner(
                onClick = onRandomPickClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // 激活中的筛选条件展示行
            if (filterActiveCount > 0) {
                ActiveFilterRow(
                    uiState = uiState,
                    onMemberToggle = viewModel::onMemberFilterToggle,
                    onCategoryClear = { viewModel.onCategoryFilterChange(uiState.selectedCategory) },
                    onClearAll = viewModel::clearFilters
                )
            }

            // 内容区域
            when {
                uiState.isLoading -> {
                    LoadingBox()
                }

                uiState.recipes.isEmpty() -> {
                    EmptyStateBox(
                        emoji = if (hasAnyFilter) "🔍" else "🍳",
                        title = if (hasAnyFilter) "没有匹配的菜谱" else "还没有菜谱",
                        subtitle = if (hasAnyFilter) "试试调整筛选条件" else "记录第一道拿手菜吧",
                        actionLabel = if (hasAnyFilter) "清除筛选" else "添加菜谱",
                        onAction = if (hasAnyFilter) viewModel::clearFilters else onAddClick
                    )
                }

                else -> {
                    RecipeListContent(
                        recipes = uiState.recipes,
                        onRecipeClick = onRecipeClick,
                        onFavoriteClick = viewModel::toggleFavorite,
                        onEditClick = onEditRecipe,
                        onDeleteClick = { deleteTarget = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecipeListContent(
    recipes: List<Recipe>,
    onRecipeClick: (Long) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Recipe) -> Unit
) {
    val favorites = recipes.filter { it.isFavorite }
    val others = recipes.filter { !it.isFavorite }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (favorites.isNotEmpty()) {
            item(key = "header_favorites") {
                SectionHeader("收藏", Modifier.animateItemPlacement())
            }
            items(favorites, key = { it.id }) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    onClick = { onRecipeClick(recipe.id) },
                    onFavoriteClick = { onFavoriteClick(recipe.id) },
                    onEditClick = { onEditClick(recipe.id) },
                    onDeleteClick = { onDeleteClick(recipe) },
                    modifier = Modifier.animateItemPlacement()
                )
            }
            if (others.isNotEmpty()) {
                item(key = "header_all") {
                    SectionHeader("全部", Modifier.animateItemPlacement())
                }
            }
        }
        items(others, key = { it.id }) { recipe ->
            RecipeCard(
                recipe = recipe,
                onClick = { onRecipeClick(recipe.id) },
                onFavoriteClick = { onFavoriteClick(recipe.id) },
                onEditClick = { onEditClick(recipe.id) },
                onDeleteClick = { onDeleteClick(recipe) },
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

/**
 * "今天吃什么"渐变横幅，首页核心入口
 */
@Composable
private fun RandomPickBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = AppTheme.extendedColors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(listOf(extended.heroStart, extended.heroEnd))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🎲", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "今天吃什么？",
                    style = MaterialTheme.typography.titleMedium,
                    color = extended.onHero
                )
                Text(
                    text = "选择困难就交给骰子吧",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.onHero.copy(alpha = 0.85f)
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = extended.onHero
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SortMenuButton(
    sortConfig: com.familyrecipe.book.data.model.SortConfig,
    onSortDimensionChange: (SortDimension) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilledTonalIconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.SwapVert, contentDescription = "排序")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortDimension.entries.forEach { dimension ->
                val isSelected = sortConfig.dimension == dimension
                DropdownMenuItem(
                    text = { Text(dimension.label) },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = if (sortConfig.order == SortOrder.DESC) {
                                    Icons.Default.ArrowDownward
                                } else {
                                    Icons.Default.ArrowUpward
                                },
                                contentDescription = if (sortConfig.order == SortOrder.DESC) "降序" else "升序",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = { onSortDimensionChange(dimension) }
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterRow(
    uiState: RecipeListUiState,
    onMemberToggle: (Long) -> Unit,
    onCategoryClear: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uiState.familyMembers
                .filter { it.id in uiState.recipeFilter.selectedMemberIds }
                .forEach { member ->
                    InputChip(
                        selected = true,
                        onClick = { onMemberToggle(member.id) },
                        label = { Text(member.name) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "移除",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            uiState.selectedCategory?.let { category ->
                InputChip(
                    selected = true,
                    onClick = onCategoryClear,
                    label = { Text("${category.emoji} ${category.label}") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "移除",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
        TextButton(onClick = onClearAll) { Text("清除") }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheet(
    uiState: RecipeListUiState,
    onMemberToggle: (Long) -> Unit,
    onCategoryChange: (RecipeCategory?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("筛选", style = MaterialTheme.typography.titleLarge)

            if (uiState.familyMembers.isNotEmpty()) {
                Text(
                    "家人都喜欢",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.familyMembers.forEach { member ->
                        FilterChip(
                            selected = member.id in uiState.recipeFilter.selectedMemberIds,
                            onClick = { onMemberToggle(member.id) },
                            label = { Text(member.name) }
                        )
                    }
                }
            }

            Text(
                "分类",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.selectedCategory == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("全部") }
                )
                RecipeCategory.entries.forEach { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { onCategoryChange(category) },
                        label = { Text("${category.emoji} ${category.label}") }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                    Text("重置")
                }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("完成")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            )
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
                            AppTheme.extendedColors.favorite
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("编辑") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onEditClick()
                }
            )
            DropdownMenuItem(
                text = { Text("删除") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                }
            )
        }
    }
}
