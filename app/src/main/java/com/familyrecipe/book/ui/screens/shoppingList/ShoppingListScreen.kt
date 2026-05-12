package com.familyrecipe.book.ui.screens.shoppingList

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onBack: () -> Unit,
    viewModel: ShoppingListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("购物清单") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::clearSelection) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "清空选择")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("选择要采购的菜谱", style = MaterialTheme.typography.titleMedium)
                }

                if (uiState.recipes.isEmpty()) {
                    item {
                        Text(
                            "还没有菜谱，先添加几道家常菜吧。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(uiState.recipes, key = { it.id }) { recipe ->
                        RecipeSelectionRow(
                            name = recipe.name,
                            checked = recipe.id in uiState.selectedRecipeIds,
                            onCheckedChange = { viewModel.toggleRecipe(recipe.id) }
                        )
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "采购项 (${uiState.shoppingItems.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (uiState.selectedRecipeIds.isEmpty()) {
                    item {
                        Text(
                            "勾选菜谱后会在这里合并食材。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (uiState.shoppingItems.isEmpty()) {
                    item {
                        Text(
                            "选中的菜谱还没有录入食材。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(uiState.shoppingItems, key = { it.name }) { item ->
                        ShoppingItemCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeSelectionRow(
    name: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ShoppingItemCard(item: ShoppingListItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(item.name, style = MaterialTheme.typography.titleSmall)
            if (item.details.isNotEmpty()) {
                Text(
                    item.details.joinToString(" / "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                "来自：${item.recipeNames.joinToString("、")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
