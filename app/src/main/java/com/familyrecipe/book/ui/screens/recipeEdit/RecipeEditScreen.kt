package com.familyrecipe.book.ui.screens.recipeEdit

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.familyrecipe.book.data.model.RecipeCategory
import com.familyrecipe.book.ui.components.ImagePickerDialog
import com.familyrecipe.book.ui.components.StarRating
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecipeEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 图片选择器对话框状态
    var showImagePickerDialog by remember { mutableStateOf(false) }

    // 分类下拉菜单展开状态
    var categoryExpanded by remember { mutableStateOf(false) }

    // 相机拍照临时文件 URI
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // 相册选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    // 相机拍照
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraImageUri?.let { viewModel.onImageSelected(it) }
        }
    }

    // 相机权限请求
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 权限授予后启动相机
            val photoFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        }
    }

    // 处理相机点击
    fun launchCamera() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            val photoFile = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 处理相册点击
    fun launchGallery() {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // 图片选择器对话框
    ImagePickerDialog(
        showDialog = showImagePickerDialog,
        onDismiss = { showImagePickerDialog = false },
        onGalleryClick = { launchGallery() },
        onCameraClick = { launchCamera() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "编辑菜谱" else "新增菜谱") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onSaved) },
                        enabled = uiState.name.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 封面图片选择区域
                CoverImageSection(
                    coverImagePath = uiState.coverImagePath,
                    onImageClick = { showImagePickerDialog = true },
                    onRemoveImage = { viewModel.onImageRemoved() }
                )

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("菜名 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("简介") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // 分类下拉选择器（必填）
                CategoryDropdown(
                    selectedCategory = uiState.selectedCategory,
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    onCategorySelected = { category ->
                        viewModel.onCategorySelected(category)
                        categoryExpanded = false
                    },
                    isError = uiState.categoryError
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.cookingMinutes,
                        onValueChange = viewModel::onCookingMinutesChange,
                        label = { Text("烹饪时间(分钟)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("10", "15", "30", "60").forEach { minutes ->
                        FilterChip(
                            selected = uiState.cookingMinutes == minutes,
                            onClick = { viewModel.onCookingMinutesChange(minutes) },
                            label = { Text("${minutes}分钟") }
                        )
                    }
                }

                // 难度选择
                Text("难度", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { level ->
                        FilterChip(
                            selected = uiState.difficulty == level,
                            onClick = { viewModel.onDifficultyChange(level) },
                            label = { Text("$level") }
                        )
                    }
                }

                // 推荐指数星形评分
                Text("推荐指数", style = MaterialTheme.typography.labelLarge)
                StarRating(
                    rating = uiState.recommendationIndex,
                    onRatingChange = { viewModel.onRecommendationChange(it) }
                )

                // 食材
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("食材清单", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = viewModel::addIngredient) {
                        Icon(Icons.Default.Add, contentDescription = "添加食材")
                    }
                }

                uiState.ingredients.forEachIndexed { index, ingredient ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = ingredient.name,
                                onValueChange = { viewModel.onIngredientNameChange(index, it) },
                                label = { Text("食材 ${index + 1}") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(onClick = { viewModel.removeIngredient(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "删除食材")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ingredient.amount,
                                onValueChange = { viewModel.onIngredientAmountChange(index, it) },
                                label = { Text("数量") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = ingredient.unit,
                                onValueChange = { viewModel.onIngredientUnitChange(index, it) },
                                label = { Text("单位") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("个", "克", "勺", "把", "适量").forEach { unit ->
                                FilterChip(
                                    selected = ingredient.unit == unit,
                                    onClick = { viewModel.onIngredientUnitChange(index, unit) },
                                    label = { Text(unit) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = ingredient.note,
                            onValueChange = { viewModel.onIngredientNoteChange(index, it) },
                            label = { Text("备注") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                // 步骤
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("烹饪步骤", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = viewModel::addStep) {
                        Icon(Icons.Default.Add, contentDescription = "添加步骤")
                    }
                }

                uiState.steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        OutlinedTextField(
                            value = step,
                            onValueChange = { viewModel.onStepChange(index, it) },
                            label = { Text("步骤 ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            minLines = 2,
                            maxLines = 5
                        )
                        if (uiState.steps.size > 1) {
                            IconButton(onClick = { viewModel.removeStep(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "删除步骤")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * 封面图片选择区域
 * 显示当前封面预览或占位图标，点击触发图片选择器
 */
@Composable
private fun CoverImageSection(
    coverImagePath: String?,
    onImageClick: () -> Unit,
    onRemoveImage: () -> Unit
) {
    val imageFileExists = coverImagePath != null && File(coverImagePath).exists()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onImageClick() },
        contentAlignment = Alignment.Center
    ) {
        if (imageFileExists) {
            // 显示当前封面预览
            AsyncImage(
                model = File(coverImagePath!!),
                contentDescription = "菜谱封面预览",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // 移除图片按钮
            IconButton(
                onClick = onRemoveImage,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "移除封面图片",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            // 占位图标
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "添加封面图片",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击添加封面图片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 分类下拉选择器
 * 使用 ExposedDropdownMenuBox 展示所有 RecipeCategory 枚举值
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: RecipeCategory?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCategorySelected: (RecipeCategory) -> Unit,
    isError: Boolean
) {
    Column {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = selectedCategory?.label ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("分类 *") },
                placeholder = { Text("请选择分类") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                isError = isError,
                supportingText = if (isError) {
                    { Text("请选择菜谱分类", color = MaterialTheme.colorScheme.error) }
                } else null
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                RecipeCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.label) },
                        onClick = { onCategorySelected(category) },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}
