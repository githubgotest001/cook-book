# 实现计划：家庭菜谱 App v2 迭代

## 概述

基于 MVVM + Hilt DI 架构，将现有 MVP 版本升级为 v2。实现顺序为：架构基础设施 → 数据模型 → 核心功能 → UI 更新 → 应用图标 → 测试。所有代码使用 Kotlin，UI 使用 Jetpack Compose + Material 3。

## Tasks

- [x] 1. 架构基础设施搭建（Hilt DI + DataStore + Room Schema 导出）
  - [x] 1.1 添加 Hilt 和 DataStore 依赖到构建配置
    - 修改 `gradle/libs.versions.toml`：添加 hilt = "2.51"、hilt-navigation-compose、datastore-preferences 版本和库声明
    - 修改 `build.gradle.kts`（项目级）：添加 hilt 插件声明
    - 修改 `app/build.gradle.kts`：应用 hilt 插件、添加 hilt 依赖、配置 Room schema 导出目录（`ksp { arg("room.schemaLocation", ...) }`）、设置 `exportSchema = true`
    - _需求: 8.1, 8.2, 9.1_

  - [x] 1.2 创建 Hilt DI 模块
    - 创建 `app/src/main/java/com/familyrecipe/book/di/DatabaseModule.kt`：提供 AppDatabase（@Singleton）、RecipeDao、FamilyMemberDao、RecipePreferenceDao
    - 创建 `app/src/main/java/com/familyrecipe/book/di/RepositoryModule.kt`：提供 RecipeRepository、FamilyMemberRepository（@Singleton）
    - 创建 `app/src/main/java/com/familyrecipe/book/di/DataStoreModule.kt`：提供 DataStore<Preferences> 和 SettingsStore（@Singleton）
    - _需求: 8.2, 8.3_

  - [x] 1.3 迁移 Application 和 Activity 到 Hilt 注解
    - 修改 `RecipeApplication.kt`：添加 @HiltAndroidApp 注解，移除 AppContainer 引用
    - 修改 `MainActivity.kt`：添加 @AndroidEntryPoint 注解
    - 删除 `AppContainer.kt`
    - _需求: 8.1, 8.5_

  - [x] 1.4 迁移所有 ViewModel 到 @HiltViewModel
    - 修改 `RecipeListViewModel.kt`：添加 @HiltViewModel + @Inject constructor，移除 Factory 伴生类
    - 修改 `RecipeEditViewModel.kt`：添加 @HiltViewModel + @Inject constructor + SavedStateHandle 获取 recipeId，移除 Factory
    - 修改 `RecipeDetailViewModel.kt`：添加 @HiltViewModel + @Inject constructor + SavedStateHandle，移除 Factory
    - 修改 `MemberListViewModel.kt`：添加 @HiltViewModel + @Inject constructor，移除 Factory
    - 修改 `MemberEditViewModel.kt`：添加 @HiltViewModel + @Inject constructor + SavedStateHandle，移除 Factory
    - 更新所有 Screen composable 中的 ViewModel 获取方式为 `hiltViewModel()`
    - _需求: 8.4, 8.5, 8.6_

  - [x] 1.5 创建 SettingsStore（DataStore）
    - 创建 `app/src/main/java/com/familyrecipe/book/data/datastore/SettingsStore.kt`
    - 实现 `defaultRandomCount` Flow 属性（默认值 3，范围 1-10）
    - 实现 `setDefaultRandomCount(count: Int)` 方法
    - _需求: 5.7_

- [x] 2. 检查点 - 确保 Hilt 迁移编译通过
  - 确保所有测试通过，如有问题请询问用户。

- [x] 3. 数据模型与数据库迁移
  - [x] 3.1 创建 RecipeCategory 枚举
    - 创建 `app/src/main/java/com/familyrecipe/book/data/model/RecipeCategory.kt`
    - 定义枚举值：STIR_FRY("炒菜")、SOUP("煲汤")、QUICK_MEAL("速食")、STAPLE("主食")、COLD_DISH("凉菜")、DESSERT("甜品")、BEVERAGE("饮品")、OTHER("其他")
    - 实现 `fromLabel(label: String)` 和 `fromLegacyText(text: String)` 伴生方法
    - _需求: 4.1, 4.3, 4.4_

  - [x] 3.2 更新 Recipe 实体
    - 修改 `app/src/main/java/com/familyrecipe/book/data/model/Recipe.kt`
    - 新增字段：`recommendationIndex: Int = 3`、`isFavorite: Boolean = false`
    - 修改 `category` 默认值为 `"OTHER"`
    - 添加扩展属性 `val recipeCategory: RecipeCategory get() = ...`
    - _需求: 7.1, 6.4, 4.1_

  - [x] 3.3 创建排序与筛选数据模型
    - 创建 `app/src/main/java/com/familyrecipe/book/data/model/SortConfig.kt`
    - 定义 `SortDimension` 枚举（UPDATED_AT、CREATED_AT、COOKING_MINUTES、DIFFICULTY、RECOMMENDATION）
    - 定义 `SortOrder` 枚举（ASC、DESC）
    - 定义 `SortConfig` data class（dimension + order）
    - 定义 `RecipeFilter` data class（searchQuery、selectedMemberIds、selectedCategory、allFamilyLoved）
    - _需求: 6.1, 3.1_

  - [x] 3.4 创建数据库迁移 Migration_1_2
    - 创建 `app/src/main/java/com/familyrecipe/book/data/database/Migration_1_2.kt`
    - ALTER TABLE 添加 `recommendationIndex INTEGER NOT NULL DEFAULT 3`
    - ALTER TABLE 添加 `isFavorite INTEGER NOT NULL DEFAULT 0`
    - UPDATE 语句将 category 自由文本映射为枚举字符串（炒菜→STIR_FRY 等）
    - 未匹配的设为 OTHER
    - _需求: 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_

  - [x] 3.5 更新 AppDatabase 配置
    - 修改 `app/src/main/java/com/familyrecipe/book/data/database/AppDatabase.kt`
    - 版本号从 1 升级到 2
    - 设置 `exportSchema = true`
    - 在 DatabaseModule 中注册 Migration_1_2
    - _需求: 9.1, 9.2_

- [x] 4. 增强 DAO 层查询能力
  - [x] 4.1 增强 RecipeDao
    - 修改 `app/src/main/java/com/familyrecipe/book/data/dao/RecipeDao.kt`
    - 新增 `getRecipesByCategory(category: String): Flow<List<Recipe>>`
    - 新增 `getRecipesByIds(ids: List<Long>): Flow<List<Recipe>>`
    - 新增 `updateFavoriteStatus(id: Long, isFavorite: Boolean)`
    - 新增 `getAllRecipesWithFavoriteFirst(): Flow<List<Recipe>>`
    - 增强 `searchRecipes` 支持 ingredients 字段搜索
    - _需求: 4.6, 3.2, 6.4, 6.5_

  - [x] 4.2 增强 RecipePreferenceDao
    - 修改 `app/src/main/java/com/familyrecipe/book/data/dao/RecipePreferenceDao.kt`
    - 新增 `getRecipeIdsLikedByAllMembers(memberIds: List<Long>, memberCount: Int): Flow<List<Long>>`
    - _需求: 3.2, 3.3, 3.5_

- [x] 5. 核心功能实现
  - [x] 5.1 增强 RecipeRepository
    - 修改 `app/src/main/java/com/familyrecipe/book/data/repository/RecipeRepository.kt`
    - 新增 `getRecipesByCategory(category: RecipeCategory)` 方法
    - 新增 `updateFavoriteStatus(id: Long, isFavorite: Boolean)` 方法
    - 新增 `getRecipesLikedByMembers(memberIds: List<Long>)` 方法
    - 新增 `getFilteredAndSortedRecipes(filter: RecipeFilter, sortConfig: SortConfig)` 组合查询逻辑
    - _需求: 3.2, 3.3, 3.6, 4.6, 6.1, 6.5_

  - [x] 5.2 创建 RandomSelector 领域逻辑
    - 创建 `app/src/main/java/com/familyrecipe/book/domain/RandomSelector.kt`
    - 实现 `selectRandom(recipes: List<Recipe>, count: Int, random: Random): RandomSelectionResult`
    - 定义 `RandomSelectionResult` data class 和 `RandomWarning` 枚举
    - 确保无重复、数量正确、空列表处理
    - _需求: 5.1, 5.2, 5.4, 5.5, 5.8_

  - [x] 5.3 创建 ImageUtils 图片处理工具
    - 创建 `app/src/main/java/com/familyrecipe/book/util/ImageUtils.kt`
    - 实现 `saveImage(context: Context, sourceUri: Uri): String`（缩放至最长边 ≤ 1920px，保存到内部存储）
    - 实现 `deleteImage(path: String)` 删除旧图片
    - 实现 `scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap` 保持宽高比缩放
    - _需求: 2.3, 2.6, 2.7_

  - [x]* 5.4 编写 RandomSelector 属性测试
    - **Property 6: 随机选择无重复**
    - **Property 7: 随机选择数量正确**
    - **Property 8: 随机选择均匀分布**
    - **验证: 需求 5.1, 5.2, 5.4, 5.5**

  - [x]* 5.5 编写 RecipeCategory.fromLegacyText 属性测试
    - **Property 4: 分类映射正确性**
    - **验证: 需求 4.3, 4.4**

- [x] 6. 检查点 - 确保核心逻辑测试通过
  - 确保所有测试通过，如有问题请询问用户。

- [x] 7. UI 层更新 - 菜谱列表页
  - [x] 7.1 更新 RecipeListViewModel 支持筛选与排序
    - 修改 `app/src/main/java/com/familyrecipe/book/ui/screens/recipeList/RecipeListViewModel.kt`
    - 注入 RecipeRepository、FamilyMemberRepository、SettingsStore、RandomSelector
    - 新增 UiState 字段：sortConfig、recipeFilter、selectedCategory、familyMembers、randomResult
    - 实现排序切换逻辑（维度选择 + 升降序切换）
    - 实现成员偏好筛选逻辑
    - 实现分类筛选逻辑
    - 实现收藏置顶排序（isFavorite 优先）
    - 实现随机选菜触发与"换一批"
    - _需求: 3.1, 3.2, 3.3, 3.6, 4.6, 5.1, 5.6, 6.1, 6.2, 6.3, 6.5, 6.6, 6.8_

  - [x] 7.2 更新 RecipeListScreen UI
    - 修改 `app/src/main/java/com/familyrecipe/book/ui/screens/recipeList/RecipeListScreen.kt`
    - 添加家庭成员筛选标签行（水平滚动 FilterChip）
    - 添加分类筛选下拉/标签
    - 添加排序维度选择器（带升降序指示）
    - 添加"今天吃什么"随机选菜按钮
    - 菜谱卡片显示封面缩略图（Coil AsyncImage）
    - 菜谱卡片显示推荐指数星形图标
    - 菜谱卡片显示分类标签
    - 菜谱卡片显示收藏图标
    - 空状态提示（无匹配结果时）
    - _需求: 2.4, 3.1, 3.7, 4.5, 4.7, 5.1, 6.1, 7.3_

- [x] 8. UI 层更新 - 菜谱编辑页
  - [x] 8.1 更新 RecipeEditViewModel
    - 修改 `app/src/main/java/com/familyrecipe/book/ui/screens/recipeEdit/RecipeEditViewModel.kt`
    - 新增 UiState 字段：coverImagePath、recommendationIndex、selectedCategory（RecipeCategory 类型）
    - 实现图片选择/替换/移除逻辑（调用 ImageUtils）
    - 实现分类选择逻辑（必填验证）
    - 实现推荐指数设置逻辑
    - 保存时包含新字段
    - _需求: 2.1, 2.3, 2.6, 2.7, 4.2, 4.8, 7.1, 7.2_

  - [x] 8.2 更新 RecipeEditScreen UI
    - 修改 `app/src/main/java/com/familyrecipe/book/ui/screens/recipeEdit/RecipeEditScreen.kt`
    - 添加图片选择区域（占位图标/当前封面预览，点击触发选择器）
    - 添加相册/相机选择对话框
    - 添加分类下拉选择器（RecipeCategory 枚举，必填标记）
    - 添加推荐指数星形评分组件（5 星可点击）
    - 处理权限请求（相机/存储）
    - _需求: 2.1, 2.2, 2.6, 2.9, 4.2, 4.8, 7.2_

- [x] 9. UI 层更新 - 菜谱详情页
  - [x] 9.1 更新 RecipeDetailViewModel
    - 修改 `app/src/main/java/com/familyrecipe/book/ui/screens/recipeDetail/RecipeDetailViewModel.kt`
    - 新增收藏切换方法 `toggleFavorite()`
    - 暴露 isFavorite 状态
    - _需求: 6.4, 6.7_

  - [x] 9.2 更新 RecipeDetailScreen UI
    - 修改 `app/src/main/java/com/familyrecipe/book/ui/screens/recipeDetail/RecipeDetailScreen.kt`
    - 顶部全宽显示封面图片（Coil AsyncImage，缺失时显示占位）
    - 显示推荐指数星形图标（头部区域）
    - 显示分类标签
    - 添加收藏切换按钮（实心/空心心形图标）
    - _需求: 2.5, 2.8, 6.7, 7.4_

- [x] 10. UI 层更新 - 随机选菜页面
  - [x] 10.1 创建 RandomPickViewModel
    - 创建 `app/src/main/java/com/familyrecipe/book/ui/screens/randomPick/RandomPickViewModel.kt`
    - 注入 RecipeRepository、SettingsStore、RandomSelector
    - 实现初始随机选择（使用默认数量）
    - 实现"换一批"功能
    - 实现自定义数量选择（1-10）
    - _需求: 5.1, 5.2, 5.3, 5.6, 5.7, 5.8_

  - [x] 10.2 创建 RandomPickScreen UI
    - 创建 `app/src/main/java/com/familyrecipe/book/ui/screens/randomPick/RandomPickScreen.kt`
    - 显示随机选中的菜谱卡片列表
    - 数量选择器（1-10 滑块或数字选择）
    - "换一批"按钮
    - 空状态提示（无菜谱时）
    - 不足提示（可用菜谱少于请求数量时）
    - 点击菜谱卡片导航到详情页
    - _需求: 5.1, 5.3, 5.4, 5.6, 5.8_

- [x] 11. UI 层更新 - 设置页与导航
  - [x] 11.1 更新设置页面
    - 修改 `app/src/main/java/com/familyrecipe/book/ui/screens/settings/SettingsScreen.kt`
    - 添加"随机选菜默认数量"设置项（滑块或数字选择器，范围 1-10）
    - 注入 SettingsStore，读取和保存默认数量
    - _需求: 5.7_

  - [x] 11.2 更新导航图
    - 修改 `app/src/main/java/com/familyrecipe/book/ui/navigation/NavRoutes.kt`：添加 RANDOM_PICK 路由
    - 修改 `app/src/main/java/com/familyrecipe/book/ui/navigation/AppNavGraph.kt`：注册 RandomPickScreen 路由
    - 在 RecipeListScreen 添加导航到随机选菜页面的入口
    - _需求: 5.1_

- [x] 12. UI 共享组件
  - [x] 12.1 创建共享 UI 组件
    - 创建 `app/src/main/java/com/familyrecipe/book/ui/components/StarRating.kt`：5 星评分组件（可点击/只读两种模式）
    - 创建 `app/src/main/java/com/familyrecipe/book/ui/components/CategoryChip.kt`：分类标签组件
    - 创建 `app/src/main/java/com/familyrecipe/book/ui/components/ImagePicker.kt`：图片选择器组件（相册/相机选择对话框）
    - _需求: 2.1, 4.5, 7.2, 7.3_

- [x] 13. 检查点 - 确保 UI 编译通过且功能可用
  - 确保所有测试通过，如有问题请询问用户。

- [x] 14. 应用图标
  - [x] 14.1 创建自定义应用图标资源
    - 创建/替换 `app/src/main/res/mipmap-mdpi/ic_launcher.png`（48×48px）
    - 创建/替换 `app/src/main/res/mipmap-hdpi/ic_launcher.png`（72×72px）
    - 创建/替换 `app/src/main/res/mipmap-xhdpi/ic_launcher.png`（96×96px）
    - 创建/替换 `app/src/main/res/mipmap-xxhdpi/ic_launcher.png`（144×144px）
    - 创建/替换 `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`（192×192px）
    - 更新 `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`：自适应图标（前景层 + 背景层）
    - 更新 `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`：圆形变体
    - 确保 AndroidManifest 中 `android:icon` 和 `android:roundIcon` 正确引用
    - _需求: 1.1, 1.2, 1.3, 1.4_

- [x] 15. 测试基础设施与单元测试
  - [x] 15.1 添加测试依赖
    - 修改 `app/build.gradle.kts`：添加 JUnit、MockK、Coroutines Test、Room Testing、Compose UI Test、Espresso 依赖
    - _需求: 10.1, 10.2_

  - [x] 15.2 编写 RecipeRepository 单元测试
    - 创建 `app/src/test/java/com/familyrecipe/book/data/repository/RecipeRepositoryTest.kt`
    - 测试插入、按 ID 查询、更新、删除操作
    - 测试偏好查询（getPreferencesForRecipe、getRecipeIdsByMemberPreference、setPreference、removePreference）
    - 使用 MockK mock DAO 层
    - _需求: 10.3_

  - [x] 15.3 编写 RandomSelector 单元测试
    - 创建 `app/src/test/java/com/familyrecipe/book/domain/RandomSelectorTest.kt`
    - 验证单次结果无重复
    - 验证结果数量等于请求数量（有足够菜谱时）
    - 验证 1000 次重复选择中每个菜谱至少被选中一次（≤5 个菜谱池）
    - 验证空列表和不足情况
    - _需求: 10.4_

  - [x] 15.4 编写数据库迁移单元测试
    - 创建 `app/src/androidTest/java/com/familyrecipe/book/data/database/MigrationTest.kt`
    - 使用 MigrationTestHelper 验证 Migration_1_2
    - 验证 recommendationIndex 默认为 3
    - 验证 isFavorite 默认为 false（0）
    - 验证分类文本映射正确（炒菜→STIR_FRY 等）
    - 验证不匹配分类映射为 OTHER
    - _需求: 10.5_

  - [x]* 15.5 编写排序与收藏置顶属性测试
    - **Property 9: 排序正确性**
    - **Property 10: 收藏置顶不变量**
    - **验证: 需求 6.1, 6.2, 6.5, 7.5**

  - [x]* 15.6 编写偏好筛选属性测试
    - **Property 2: 成员偏好筛选返回交集**
    - **Property 3: 文本搜索与偏好筛选取交集**
    - **Property 5: 分类筛选仅返回匹配分类**
    - **验证: 需求 3.2, 3.3, 3.5, 3.6, 3.8, 4.6**

  - [x]* 15.7 编写 ImageUtils 属性测试
    - **Property 1: 图片缩放不超过最大尺寸**
    - **验证: 需求 2.3**

- [x] 16. UI 测试
  - [x]* 16.1 编写菜谱创建流程 UI 测试
    - 创建 `app/src/androidTest/java/com/familyrecipe/book/ui/RecipeCreateFlowTest.kt`
    - 验证名称、描述、分类选择器、烹饪时间字段可填写
    - 验证点击保存后菜谱被持久化并导航回列表
    - _需求: 10.6_

  - [x]* 16.2 编写搜索和筛选流程 UI 测试
    - 创建 `app/src/androidTest/java/com/familyrecipe/book/ui/SearchFilterFlowTest.kt`
    - 验证选择成员筛选标签后仅显示该成员"喜欢"的菜谱
    - 验证文本搜索与成员筛选组合后仅显示同时匹配两个条件的菜谱
    - _需求: 10.7_

- [x] 17. 最终检查点 - 确保所有测试通过
  - 确保所有测试通过，如有问题请询问用户。

## Notes

- 标记 `*` 的任务为可选任务，可跳过以加速 MVP 交付
- 每个任务引用了具体的需求编号，确保可追溯性
- 检查点任务确保增量验证，避免问题累积
- 属性测试验证设计文档中定义的正确性属性
- 单元测试验证具体示例和边界情况
- Hilt 迁移是最高优先级，因为后续所有 ViewModel 和 Repository 变更都依赖 DI 框架
- Room Migration 必须在数据模型变更之前完成，确保现有数据不丢失

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.5", "3.1"] },
    { "id": 2, "tasks": ["1.3", "3.2", "3.3"] },
    { "id": 3, "tasks": ["1.4", "3.4"] },
    { "id": 4, "tasks": ["3.5", "4.1", "4.2"] },
    { "id": 5, "tasks": ["5.1", "5.2", "5.3"] },
    { "id": 6, "tasks": ["5.4", "5.5", "12.1"] },
    { "id": 7, "tasks": ["7.1", "8.1", "9.1", "10.1"] },
    { "id": 8, "tasks": ["7.2", "8.2", "9.2", "10.2", "11.1", "11.2"] },
    { "id": 9, "tasks": ["14.1", "15.1"] },
    { "id": 10, "tasks": ["15.2", "15.3", "15.4"] },
    { "id": 11, "tasks": ["15.5", "15.6", "15.7"] },
    { "id": 12, "tasks": ["16.1", "16.2"] }
  ]
}
```
