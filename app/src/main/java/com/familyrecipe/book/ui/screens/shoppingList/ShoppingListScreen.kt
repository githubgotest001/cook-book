package com.familyrecipe.book.ui.screens.shoppingList

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShoppingListScreen(
    onBack: () -> Unit,
    viewModel: ShoppingListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var manualInput by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空清单") },
            text = { Text("将取消所有菜谱勾选，并删除手动添加的采购项。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

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
                    if (uiState.shoppingItems.isNotEmpty()) {
                        IconButton(onClick = {
                            shareShoppingList(context, uiState.shoppingItems)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "分享清单")
                        }
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "清空清单")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ===== 菜谱选择 =====
                item(key = "header_recipes") {
                    Text("选择要采购的菜谱", style = MaterialTheme.typography.titleMedium)
                }

                if (uiState.recipes.isEmpty()) {
                    item(key = "empty_recipes") {
                        Text(
                            "还没有菜谱，先添加几道家常菜吧。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    item(key = "recipe_chips") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.recipes.forEach { recipe ->
                                FilterChip(
                                    selected = recipe.id in uiState.selectedRecipeIds,
                                    onClick = { viewModel.toggleRecipe(recipe.id) },
                                    label = {
                                        Text(
                                            recipe.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // ===== 手动添加临时项 =====
                item(key = "manual_input") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualInput,
                            onValueChange = { manualInput = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("添加临时采购项，如：酱油") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        FilledIconButton(
                            onClick = {
                                viewModel.addManualItem(manualInput)
                                manualInput = ""
                            },
                            enabled = manualInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "添加")
                        }
                    }
                }

                // ===== 采购清单 =====
                item(key = "header_items") {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "采购清单",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (uiState.shoppingItems.isNotEmpty()) {
                            Text(
                                "还差 ${uiState.pendingCount} 项",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (uiState.pendingCount == 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                if (uiState.shoppingItems.isEmpty()) {
                    item(key = "empty_items") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🛒", fontSize = 48.sp)
                            Text(
                                "勾选菜谱或手动添加，食材会自动合并到这里",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.shoppingItems, key = { it.key }) { item ->
                        ShoppingItemRow(
                            item = item,
                            onTogglePurchased = { viewModel.togglePurchased(item) },
                            onRemove = if (item.isManual) {
                                { viewModel.removeManualItem(item) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingListItem,
    onTogglePurchased: () -> Unit,
    onRemove: (() -> Unit)?
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.purchased,
                onCheckedChange = { onTogglePurchased() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        textDecoration = if (item.purchased) TextDecoration.LineThrough else null,
                        color = if (item.purchased) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (item.amountText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.amountText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.purchased) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
                if (item.noteText.isNotBlank()) {
                    Text(
                        text = item.noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.recipeNames.isNotEmpty()) {
                    Text(
                        text = "来自：${item.recipeNames.joinToString("、")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 将购物清单格式化为纯文本并调起系统分享（发给家人代买）
 */
private fun shareShoppingList(
    context: android.content.Context,
    items: List<ShoppingListItem>
) {
    val text = buildString {
        appendLine("🛒 购物清单")
        items.forEach { item ->
            val mark = if (item.purchased) "☑" else "☐"
            val amount = if (item.amountText.isNotBlank()) " ${item.amountText}" else ""
            appendLine("$mark ${item.name}$amount")
        }
        append("—— 来自「家庭菜谱」")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享购物清单"))
}
