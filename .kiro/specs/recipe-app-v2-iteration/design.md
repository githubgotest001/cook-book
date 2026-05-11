# 设计文档：家庭菜谱 App v2 迭代

## 概述

本设计文档描述家庭菜谱 App 从 v1（MVP）到 v2 的迭代升级方案。核心变更包括：

1. **数据模型增强**：Recipe 实体新增 `recommendationIndex`、`isFavorite` 字段，`category` 从自由文本迁移为 `RecipeCategory` 枚举
2. **图片支持**：集成图片选择器（相册/相机），使用 Coil 加载封面图片
3. **搜索增强**：在文本搜索基础上增加家庭成员偏好筛选（Filter Chips）
4. **随机选菜**：新增"今天吃什么"功能，支持可配置数量
5. **多维排序与收藏**：支持多种排序维度，收藏菜谱始终置顶
6. **架构升级**：从手动 DI（AppContainer）迁移到 Hilt，Room schema 导出，测试基础设施
7. **应用图标**：自定义自适应图标资源

技术栈保持不变：Kotlin 2.1.0 + Jetpack Compose + Material 3 + Room 2.6.1 + Navigation Compose 2.7.7 + Coil 2.7.0。新增 Hilt 2.51 和 DataStore Preferences。

## 架构

### 整体架构（MVVM + Clean Architecture 简化版）

```mermaid
graph TB
    subgraph UI Layer
        Screens[Compose Screens]
        ViewModels[ViewModels<br/>@HiltViewModel]
    end
    
    subgraph Domain Layer
        Repos[Repositories<br/>@Singleton]
        UseCases[Use Cases<br/>RandomSelector等]
    end
    
    subgraph Data Layer
        DAOs[Room DAOs]
        DB[AppDatabase v2]
        DataStore[DataStore Preferences]
        FileStorage[Internal File Storage]
    end
    
    subgraph DI
        HiltModules[Hilt Modules<br/>DatabaseModule<br/>RepositoryModule]
    end
    
    Screens --> ViewModels
    ViewModels --> Repos
    ViewModels --> UseCases
    Repos --> DAOs
    DAOs --> DB
    ViewModels --> DataStore
    Screens --> FileStorage
    HiltModules -.-> Repos
    HiltModules -.-> DAOs
    HiltModules -.-> DB
```

### Hilt DI 架构

迁移后的依赖注入结构：

```mermaid
graph LR
    subgraph Hilt Modules
        DBModule[DatabaseModule<br/>@Module @InstallIn Singleton]
        RepoModule[RepositoryModule<br/>@Module @InstallIn Singleton]
        DataStoreModule[DataStoreModule<br/>@Module @InstallIn Singleton]
    end
    
    DBModule -->|provides| AppDatabase
    DBModule -->|provides| RecipeDao
    DBModule -->|provides| FamilyMemberDao
    DBModule -->|provides| RecipePreferenceDao
    RepoModule -->|provides| RecipeRepository
    RepoModule -->|provides| FamilyMemberRepository
    DataStoreModule -->|provides| SettingsStore
```

### 包结构变更

```
com.familyrecipe.book/
├── RecipeApplication.kt          // @HiltAndroidApp
├── MainActivity.kt               // @AndroidEntryPoint
├── data/
│   ├── model/
│   │   ├── Recipe.kt             // 新增字段
│   │   ├── RecipeCategory.kt     // 新增枚举
│   │   ├── FamilyMember.kt
│   │   └── RecipePreference.kt
│   ├── dao/
│   │   ├── RecipeDao.kt          // 新增查询方法
│   │   ├── FamilyMemberDao.kt
│   │   └── RecipePreferenceDao.kt
│   ├── database/
│   │   ├── AppDatabase.kt        // version=2, exportSchema=true
│   │   └── Migration_1_2.kt      // 新增迁移
│   ├── repository/
│   │   ├── RecipeRepository.kt   // 增强搜索/排序
│   │   └── FamilyMemberRepository.kt
│   └── datastore/
│       └── SettingsStore.kt       // 新增 DataStore
├── di/                            // 新增
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   └── DataStoreModule.kt
├── domain/                        // 新增
│   └── RandomSelector.kt
├── ui/
│   ├── navigation/
│   │   ├── AppNavGraph.kt        // 新增路由
│   │   └── NavRoutes.kt
│   ├── screens/
│   │   ├── recipeList/           // 增强搜索/排序/筛选
│   │   ├── recipeDetail/         // 收藏按钮、推荐指数
│   │   ├── recipeEdit/           // 图片选择、分类枚举、推荐指数
│   │   ├── randomPick/           // 新增
│   │   ├── memberList/
│   │   ├── memberEdit/
│   │   └── settings/             // 新增随机选菜默认数量设置
│   ├── components/                // 新增共享组件
│   │   ├── StarRating.kt
│   │   ├── CategoryChip.kt
│   │   └── ImagePicker.kt
│   └── theme/
└── util/
    └── ImageUtils.kt              // 新增图片处理工具
```

## 组件与接口

### 1. RecipeCategory 枚举

```kotlin
enum class RecipeCategory(val label: String) {
    STIR_FRY("炒菜"),
    SOUP("煲汤"),
    QUICK_MEAL("速食"),
    STAPLE("主食"),
    COLD_DISH("凉菜"),
    DESSERT("甜品"),
    BEVERAGE("饮品"),
    OTHER("其他");

    companion object {
        fun fromLabel(label: String): RecipeCategory {
            return entries.find { 
                it.label == label || it.name.equals(label, ignoreCase = true) 
            } ?: OTHER
        }
        
        fun fromLegacyText(text: String): RecipeCategory {
            return entries.find { 
                text.contains(it.label) || it.name.equals(text, ignoreCase = true)
            } ?: OTHER
        }
    }
}
```

### 2. SettingsStore（DataStore）

```kotlin
class SettingsStore(private val dataStore: DataStore<Preferences>) {
    
    val defaultRandomCount: Flow<Int> = dataStore.data.map { prefs ->
        prefs[DEFAULT_RANDOM_COUNT] ?: 3
    }
    
    suspend fun setDefaultRandomCount(count: Int) {
        require(count in 1..10)
        dataStore.edit { prefs ->
            prefs[DEFAULT_RANDOM_COUNT] = count
        }
    }
    
    companion object {
        val DEFAULT_RANDOM_COUNT = intPreferencesKey("default_random_count")
    }
}
```

### 3. RandomSelector

```kotlin
class RandomSelector {
    
    fun selectRandom(
        recipes: List<Recipe>,
        count: Int,
        random: Random = Random
    ): RandomSelectionResult {
        require(count in 1..10) { "Count must be between 1 and 10" }
        
        if (recipes.isEmpty()) {
            return RandomSelectionResult(
                selected = emptyList(),
                warning = RandomWarning.NO_RECIPES
            )
        }
        
        val actualCount = minOf(count, recipes.size)
        val selected = recipes.shuffled(random).take(actualCount)
        
        val warning = if (recipes.size < count) {
            RandomWarning.INSUFFICIENT_RECIPES
        } else null
        
        return RandomSelectionResult(selected = selected, warning = warning)
    }
}

data class RandomSelectionResult(
    val selected: List<Recipe>,
    val warning: RandomWarning? = null
)

enum class RandomWarning {
    NO_RECIPES,
    INSUFFICIENT_RECIPES
}
```

### 4. 增强的 RecipeDao

新增查询方法：

```kotlin
@Dao
interface RecipeDao {
    // 现有方法保留...
    
    @Query("SELECT * FROM recipes WHERE category = :category ORDER BY updatedAt DESC")
    fun getRecipesByCategory(category: String): Flow<List<Recipe>>
    
    @Query("""
        SELECT * FROM recipes 
        WHERE id IN (:ids)
        ORDER BY updatedAt DESC
    """)
    fun getRecipesByIds(ids: List<Long>): Flow<List<Recipe>>
    
    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)
    
    @Query("SELECT * FROM recipes ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllRecipesWithFavoriteFirst(): Flow<List<Recipe>>
}
```

### 5. 增强的 RecipePreferenceDao

新增联合查询：

```kotlin
@Dao
interface RecipePreferenceDao {
    // 现有方法保留...
    
    @Query("""
        SELECT DISTINCT rp.recipeId FROM recipe_preferences rp
        WHERE rp.memberId IN (:memberIds) AND rp.preference = 'LIKE'
        GROUP BY rp.recipeId
        HAVING COUNT(DISTINCT rp.memberId) = :memberCount
    """)
    fun getRecipeIdsLikedByAllMembers(
        memberIds: List<Long>, 
        memberCount: Int
    ): Flow<List<Long>>
}
```

### 6. Hilt Modules

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(Migration_1_2)
            .build()
    }
    
    @Provides
    fun provideRecipeDao(db: AppDatabase): RecipeDao = db.recipeDao()
    
    @Provides
    fun provideFamilyMemberDao(db: AppDatabase): FamilyMemberDao = db.familyMemberDao()
    
    @Provides
    fun provideRecipePreferenceDao(db: AppDatabase): RecipePreferenceDao = db.recipePreferenceDao()
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideRecipeRepository(
        recipeDao: RecipeDao,
        preferenceDao: RecipePreferenceDao
    ): RecipeRepository = RecipeRepository(recipeDao, preferenceDao)
    
    @Provides
    @Singleton
    fun provideFamilyMemberRepository(
        memberDao: FamilyMemberDao
    ): FamilyMemberRepository = FamilyMemberRepository(memberDao)
}
```

### 7. ImageUtils

```kotlin
object ImageUtils {
    
    private const val MAX_DIMENSION = 1920
    private const val IMAGE_DIR = "recipe_images"
    
    suspend fun saveImage(
        context: Context, 
        sourceUri: Uri
    ): String = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }
        val fileName = "recipe_${System.currentTimeMillis()}.jpg"
        val destFile = File(dir, fileName)
        
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            val scaled = scaleBitmap(bitmap, MAX_DIMENSION)
            FileOutputStream(destFile).use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, output)
            }
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
        }
        
        destFile.absolutePath
    }
    
    fun deleteImage(path: String) {
        File(path).delete()
    }
    
    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxSide
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
```

### 8. Database Migration

```kotlin
val Migration_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 添加 recommendationIndex 列
        db.execSQL("ALTER TABLE recipes ADD COLUMN recommendationIndex INTEGER NOT NULL DEFAULT 3")
        
        // 添加 isFavorite 列
        db.execSQL("ALTER TABLE recipes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
        
        // 转换 category 文本为枚举值
        db.execSQL("UPDATE recipes SET category = 'STIR_FRY' WHERE category LIKE '%炒菜%'")
        db.execSQL("UPDATE recipes SET category = 'SOUP' WHERE category LIKE '%煲汤%'")
        db.execSQL("UPDATE recipes SET category = 'QUICK_MEAL' WHERE category LIKE '%速食%'")
        db.execSQL("UPDATE recipes SET category = 'STAPLE' WHERE category LIKE '%主食%'")
        db.execSQL("UPDATE recipes SET category = 'COLD_DISH' WHERE category LIKE '%凉菜%'")
        db.execSQL("UPDATE recipes SET category = 'DESSERT' WHERE category LIKE '%甜品%'")
        db.execSQL("UPDATE recipes SET category = 'BEVERAGE' WHERE category LIKE '%饮品%'")
        // 未匹配的设为 OTHER
        db.execSQL("""
            UPDATE recipes SET category = 'OTHER' 
            WHERE category NOT IN ('STIR_FRY','SOUP','QUICK_MEAL','STAPLE','COLD_DISH','DESSERT','BEVERAGE')
        """)
    }
}
```

## 数据模型

### Recipe（v2）

```kotlin
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val stepsJson: String = "[]",
    val cookingMinutes: Int = 0,
    val difficulty: Int = 1,              // 1-5
    val category: String = "OTHER",       // 变更：存储 RecipeCategory.name
    val coverImagePath: String? = null,
    val recommendationIndex: Int = 3,     // 新增：1-5 推荐指数
    val isFavorite: Boolean = false,      // 新增：收藏标记
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val recipeCategory: RecipeCategory
        get() = RecipeCategory.entries.find { it.name == category } ?: RecipeCategory.OTHER
}
```

**变更说明：**
- `category`：类型保持 `String`（Room 存储），但值从自由文本变为 `RecipeCategory.name` 枚举字符串。通过扩展属性 `recipeCategory` 获取枚举值。
- `recommendationIndex`：新增，整数 1-5，默认 3
- `isFavorite`：新增，布尔值，默认 false

### RecipeCategory（新增）

| 枚举值 | 中文标签 |
|--------|---------|
| STIR_FRY | 炒菜 |
| SOUP | 煲汤 |
| QUICK_MEAL | 速食 |
| STAPLE | 主食 |
| COLD_DISH | 凉菜 |
| DESSERT | 甜品 |
| BEVERAGE | 饮品 |
| OTHER | 其他 |

### FamilyMember（无变更）

保持现有结构不变。

### RecipePreference（无变更）

保持现有结构不变。

### 数据库版本变更

| 版本 | 变更内容 |
|------|---------|
| v1 | 初始 schema：recipes, family_members, recipe_preferences |
| v2 | recipes 表新增 recommendationIndex(INT, DEFAULT 3)、isFavorite(INT, DEFAULT 0)；category 列值从自由文本转换为枚举字符串 |

### 排序模型

```kotlin
enum class SortDimension(val label: String) {
    UPDATED_AT("更新时间"),
    CREATED_AT("创建时间"),
    COOKING_MINUTES("烹饪时间"),
    DIFFICULTY("难度"),
    RECOMMENDATION("推荐指数")
}

enum class SortOrder {
    ASC, DESC
}

data class SortConfig(
    val dimension: SortDimension = SortDimension.UPDATED_AT,
    val order: SortOrder = SortOrder.DESC
)
```

### 搜索筛选模型

```kotlin
data class RecipeFilter(
    val searchQuery: String = "",
    val selectedMemberIds: Set<Long> = emptySet(),
    val selectedCategory: RecipeCategory? = null,
    val allFamilyLoved: Boolean = false
)
```

## 正确性属性

*属性（Property）是指在系统所有有效执行中都应保持为真的特征或行为——本质上是对系统应做什么的形式化陈述。属性是人类可读规格说明与机器可验证正确性保证之间的桥梁。*

### Property 1: 图片缩放不超过最大尺寸

*对于任意*宽度和高度的图片，经过 `scaleBitmap` 缩放后，输出图片的最长边应不超过 1920px，且宽高比应与原始图片保持一致（误差 ≤ 1px 取整）。

**Validates: Requirements 2.3**

### Property 2: 成员偏好筛选返回交集

*对于任意*菜谱集合、家庭成员集合和偏好记录集合，当选中 N 个成员进行筛选时，返回的菜谱集合应恰好等于所有选中成员都明确标记为"喜欢"的菜谱集合。未被任何选中成员标记为"喜欢"的菜谱不应出现在结果中。

**Validates: Requirements 3.2, 3.3, 3.5, 3.8**

### Property 3: 文本搜索与偏好筛选取交集

*对于任意*菜谱集合和筛选条件（文本查询 + 成员偏好），组合筛选的结果应等于文本搜索结果集与偏好筛选结果集的交集。

**Validates: Requirements 3.6**

### Property 4: 分类映射正确性

*对于任意*字符串，`RecipeCategory.fromLegacyText` 函数应满足：若字符串包含某个 RecipeCategory 的中文标签子串，则映射到该分类；若不包含任何已知标签，则映射到 OTHER。

**Validates: Requirements 4.3, 4.4**

### Property 5: 分类筛选仅返回匹配分类

*对于任意*菜谱集合和选定的 RecipeCategory 值，按分类筛选后返回的所有菜谱的 category 字段应等于所选分类的枚举名称。

**Validates: Requirements 4.6**

### Property 6: 随机选择无重复

*对于任意*非空菜谱列表和有效数量（1-10），单次随机选择的结果中不应包含重复的菜谱（按 id 判断唯一性）。

**Validates: Requirements 5.5**

### Property 7: 随机选择数量正确

*对于任意*菜谱列表和请求数量 count（1-10），随机选择结果的数量应等于 `min(count, recipes.size)`。

**Validates: Requirements 5.2, 5.4**

### Property 8: 随机选择均匀分布

*对于任意*包含 N 个菜谱（N ≤ 5）的列表，在 1000 次独立随机选择（每次选 1 个）中，每个菜谱应至少被选中一次。

**Validates: Requirements 5.1**

### Property 9: 排序正确性

*对于任意*菜谱列表和排序配置（维度 + 升降序），排序后的列表中相邻元素应满足所选维度的排序约束（降序时前一个 ≥ 后一个，升序时前一个 ≤ 后一个）。

**Validates: Requirements 6.1, 6.2, 7.5**

### Property 10: 收藏置顶不变量

*对于任意*菜谱列表（包含收藏和非收藏菜谱）和任意排序配置，排序后的列表中所有 `isFavorite = true` 的菜谱应出现在所有 `isFavorite = false` 的菜谱之前。

**Validates: Requirements 6.5, 6.8**

## 错误处理

### 图片相关

| 场景 | 处理方式 |
|------|---------|
| 图片文件缺失/损坏 | Coil 显示占位图，不崩溃 |
| 图片保存失败（磁盘满） | Toast 提示用户，不保存图片路径 |
| 权限被拒绝 | 显示 Snackbar 说明需要权限，返回编辑页 |
| 相机不可用 | 隐藏相机选项，仅显示相册选项 |

### 数据库迁移

| 场景 | 处理方式 |
|------|---------|
| 迁移失败 | Room 标准回退机制，不损坏数据 |
| 分类文本无法匹配 | 映射为 OTHER |
| 数据库文件损坏 | 依赖 Room 的 fallbackToDestructiveMigration（仅在无法恢复时） |

### 随机选菜

| 场景 | 处理方式 |
|------|---------|
| 菜谱列表为空 | 显示空状态提示，不执行选择 |
| 可用菜谱少于请求数量 | 返回所有可用菜谱 + 提示信息 |
| DataStore 读取失败 | 使用默认值 3 |

### 搜索筛选

| 场景 | 处理方式 |
|------|---------|
| 无匹配结果 | 显示空状态 + 建议调整筛选条件 |
| 成员被删除后偏好数据 | 外键 CASCADE 自动清理 |

## 测试策略

### 测试框架与依赖

| 类型 | 框架 |
|------|------|
| 单元测试 | JUnit 5 + MockK + Kotlinx Coroutines Test |
| 属性测试 | Kotest Property Testing (io.kotest:kotest-property:5.8.0) |
| 数据库测试 | Room Testing (androidx.room:room-testing) |
| UI 测试 | Compose UI Test + Espresso |

### 属性测试配置

- 每个属性测试最少运行 **100 次迭代**
- 使用 Kotest 的 `forAll` / `checkAll` 生成器
- 每个测试标注对应的设计属性：`// Feature: recipe-app-v2-iteration, Property N: {description}`

### 单元测试覆盖

| 模块 | 测试内容 |
|------|---------|
| RecipeRepository | 插入、查询、更新、删除、偏好操作 |
| RandomSelector | 无重复、数量正确、均匀分布、边界情况 |
| RecipeCategory.fromLegacyText | 各中文标签映射、未知文本映射 OTHER |
| ImageUtils.scaleBitmap | 各种尺寸缩放、宽高比保持 |
| 排序逻辑 | 各维度排序正确性、收藏置顶 |
| 筛选逻辑 | 成员偏好筛选、分类筛选、组合筛选 |

### 属性测试覆盖

| Property | 测试目标 | 生成器 |
|----------|---------|--------|
| Property 1 | scaleBitmap | 随机宽高 (1..10000) |
| Property 2 | 偏好筛选 | 随机菜谱/成员/偏好组合 |
| Property 3 | 组合筛选 | 随机文本 + 随机成员选择 |
| Property 4 | fromLegacyText | 随机字符串（含/不含标签） |
| Property 5 | 分类筛选 | 随机菜谱列表 + 随机分类 |
| Property 6 | RandomSelector 无重复 | 随机菜谱列表 + 随机数量 |
| Property 7 | RandomSelector 数量 | 随机列表大小 + 随机请求数量 |
| Property 8 | RandomSelector 均匀性 | 小列表 + 1000 次选择 |
| Property 9 | 排序 | 随机菜谱列表 + 随机排序配置 |
| Property 10 | 收藏置顶 | 随机菜谱列表（混合收藏状态） |

### 集成测试

| 测试 | 内容 |
|------|------|
| Migration_1_2 | 使用 MigrationTestHelper 验证列添加、默认值、分类转换 |
| Hilt DI | 验证依赖图完整性，所有组件可注入 |

### UI 测试

| 流程 | 验证点 |
|------|--------|
| 菜谱创建 | 填写表单 → 选择分类 → 保存 → 导航回列表 |
| 搜索筛选 | 选择成员标签 → 验证结果 → 组合文本搜索 → 验证交集 |
| 随机选菜 | 触发随机 → 验证数量 → 换一批 → 验证新结果 |
| 收藏操作 | 标记收藏 → 验证置顶 → 取消收藏 → 验证恢复 |

