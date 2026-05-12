# 家庭菜谱 (Family Recipe Book)

一款面向家庭使用的 Android 菜谱管理应用，帮助你记录拿手菜的做法、家人的口味偏好、食材明细，并在做饭前快速生成购物清单。

## 功能介绍

### 菜谱管理
- 创建、编辑、删除菜谱
- 记录菜名、简介、分步骤做法、烹饪时间、难度（1-5 星）、分类
- 记录每道菜的食材、数量、单位和备注
- 封面图片支持（相册选择/相机拍摄，自动缩放至 1920px 内保存）
- 推荐指数评分（1-5 星）

### 菜谱分类
- 8 种预定义分类：炒菜、煲汤、速食、主食、凉菜、甜品、饮品、其他
- 按分类筛选菜谱
- 创建/编辑时必选分类

### 搜索与筛选
- 按名称/简介/步骤模糊搜索
- 按家庭成员偏好筛选（选中成员都喜欢的菜）
- 按分类筛选
- 多条件组合筛选（取交集）
- 空状态友好提示

### 多维排序与收藏
- 支持按更新时间、创建时间、烹饪时间、难度、推荐指数排序
- 升序/降序切换
- 收藏菜谱始终置顶显示
- 详情页一键收藏/取消收藏

### 随机选菜
- "今天吃什么"随机推荐功能
- 可配置默认数量（1-10，设置页调整）
- 支持自定义单次数量
- "换一批"重新随机
- 菜谱不足时友好提示

### 食材与购物清单
- 在菜谱编辑页维护食材清单
- 在菜谱详情页查看食材明细
- 从首页进入购物清单页面，勾选多道菜谱后自动合并食材
- 合并时保留数量、单位、备注和来源菜谱，便于采购前核对

### 家庭成员
- 添加家庭成员，设置姓名、代表色、备注（如忌口信息）
- 每位成员用独特颜色标识，一目了然

### 口味打标
- 在菜谱详情页为每位家人标记喜好：喜欢 👍 / 一般 / 不喜欢 👎
- 方便决定"今天做什么"时参考家人口味

### 数据备份与恢复
- 手动导出：将数据库和图片打包为 `.zip` 文件，保存到手机任意位置或云盘
- 手动导入：选择之前导出的 `.zip` 文件恢复数据
- 应用重装后数据不丢失，只需导入备份即可恢复
- 同时开启 Android Auto Backup 作为兜底保护

## 技术架构

### 技术栈

| 层级 | 技术选型 |
|------|----------|
| 语言 | Kotlin 2.1.0 |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构模式 | MVVM (ViewModel + StateFlow + Repository) |
| 依赖注入 | Hilt 2.51 |
| 数据库 | Room 2.6.1 (SQLite) |
| 设置存储 | DataStore Preferences |
| 页面导航 | Navigation Compose 2.7.7 |
| 图片加载 | Coil 2.7.0 |
| 异步处理 | Kotlin Coroutines + Flow |
| 构建工具 | Gradle Kotlin DSL + Version Catalogs + KSP |
| 最低版本 | Android 8.0 (API 26) |
| 目标版本 | Android 14 (API 34) |

### 项目结构

```
app/src/main/java/com/familyrecipe/book/
├── RecipeApplication.kt            # @HiltAndroidApp 入口
├── MainActivity.kt                 # @AndroidEntryPoint 主 Activity
├── data/
│   ├── model/                      # 数据实体 (Room Entity)
│   │   ├── Recipe.kt               # 菜谱（含推荐指数、收藏标记）
│   │   ├── RecipeCategory.kt       # 菜谱分类枚举
│   │   ├── RecipeIngredient.kt     # 菜谱食材
│   │   ├── SortConfig.kt           # 排序/筛选数据模型
│   │   ├── FamilyMember.kt         # 家庭成员
│   │   └── RecipePreference.kt     # 喜好关联（多对多）
│   ├── dao/                        # 数据访问对象 (Room DAO)
│   │   ├── RecipeDao.kt
│   │   ├── FamilyMemberDao.kt
│   │   ├── RecipePreferenceDao.kt
│   │   └── RecipeIngredientDao.kt
│   ├── database/
│   │   └── AppDatabase.kt          # Room 数据库定义
│   ├── datastore/
│   │   └── SettingsStore.kt        # DataStore 设置存储
│   └── repository/                 # 数据仓库层
│       ├── RecipeRepository.kt     # 含组合筛选/排序逻辑
│       └── FamilyMemberRepository.kt
├── di/                             # Hilt DI 模块
│   ├── DatabaseModule.kt
│   ├── RepositoryModule.kt
│   └── DataStoreModule.kt
├── domain/                         # 领域逻辑
│   └── RandomSelector.kt           # 随机选菜算法
├── util/
│   └── ImageUtils.kt               # 图片缩放/存储工具
└── ui/
    ├── theme/Theme.kt              # Material 3 主题配色
    ├── components/                 # 共享 UI 组件
    │   ├── StarRating.kt           # 5 星评分组件
    │   ├── CategoryChip.kt         # 分类标签组件
    │   └── ImagePicker.kt          # 图片选择器对话框
    ├── navigation/                 # 导航路由
    │   ├── NavRoutes.kt
    │   └── AppNavGraph.kt
    └── screens/
        ├── recipeList/             # 首页：菜谱列表 + 搜索 + 筛选 + 排序
        ├── recipeDetail/           # 菜谱详情 + 喜好打标 + 收藏
        ├── recipeEdit/             # 菜谱新增/编辑 + 图片 + 分类 + 评分
        ├── randomPick/             # 随机选菜页面
        ├── shoppingList/           # 购物清单生成页面
        ├── memberList/             # 家庭成员列表
        ├── memberEdit/             # 成员新增/编辑
        └── settings/               # 设置：随机数量/备份/恢复/关于
            ├── SettingsScreen.kt
            ├── SettingsViewModel.kt
            └── BackupHelper.kt
```

### 数据模型

```
Recipe (菜谱)
  id, name, description, stepsJson(JSON数组),
  cookingMinutes, difficulty(1-5), category(枚举字符串),
  coverImagePath, recommendationIndex(1-5), isFavorite,
  createdAt, updatedAt

RecipeCategory (分类枚举)
  STIR_FRY(炒菜), SOUP(煲汤), QUICK_MEAL(速食),
  STAPLE(主食), COLD_DISH(凉菜), DESSERT(甜品),
  BEVERAGE(饮品), OTHER(其他)

RecipeIngredient (菜谱食材)
  id, recipeId, name, amount, unit, note, displayOrder

FamilyMember (家庭成员)
  id, name, colorHex(代表色), note

RecipePreference (喜好关联，复合主键)
  recipeId, memberId, preference(LIKE/NEUTRAL/DISLIKE), updatedAt
```

### 架构分层

```
┌─────────────────────────────────┐
│           UI Layer              │
│  Compose Screen + ViewModel     │
│  (@HiltViewModel + hiltViewModel)│
├─────────────────────────────────┤
│        Domain Layer             │
│  RandomSelector                 │
├─────────────────────────────────┤
│         Repository Layer        │
│  RecipeRepository               │
│  FamilyMemberRepository         │
├─────────────────────────────────┤
│          Data Layer             │
│  Room Database + DAO            │
│  DataStore Preferences          │
├─────────────────────────────────┤
│           DI Layer              │
│  Hilt Modules (@Singleton)      │
└─────────────────────────────────┘
```

数据流向：`Screen` ← 观察 `ViewModel.StateFlow` ← 收集 `Repository.Flow` ← `Room DAO`

## 开发环境要求

- Android Studio Hedgehog (2023.1) 或更高版本
- JDK 17
- Android SDK，compileSdk 34
- Gradle 8.9（项目自带 wrapper 配置）

## 如何运行

1. 用 Android Studio 打开项目根目录 `cook-book/`
2. 等待 Gradle Sync 完成（首次会下载依赖，耐心等待）
3. 连接 Android 设备或启动模拟器（API 26+）
4. 点击 Run ▶ 运行 `app` 模块

## 如何打包 APK

### 方式一：Android Studio 图形界面

1. 菜单栏 → `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. 构建完成后，APK 位于 `app/build/outputs/apk/debug/app-debug.apk`

### 方式二：命令行打包

```bash
# Debug APK（无需签名，用于测试）
./gradlew assembleDebug

# 输出路径：app/build/outputs/apk/debug/app-debug.apk
```

### 方式三：签名 Release APK（正式发布）

1. 生成签名密钥（首次）：

```bash
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias family-recipe
```

2. 在项目根目录创建 `local.properties`（不要提交到 Git）：

```properties
RELEASE_STORE_FILE=../release-key.jks
RELEASE_STORE_PASSWORD=你的密码
RELEASE_KEY_ALIAS=family-recipe
RELEASE_KEY_PASSWORD=你的密码
```

3. 在 `app/build.gradle.kts` 中添加签名配置：

```kotlin
android {
    signingConfigs {
        create("release") {
            val props = rootProject.file("local.properties")
                .readLines().associate {
                    val (k, v) = it.split("=", limit = 2)
                    k.trim() to v.trim()
                }
            storeFile = file(props["RELEASE_STORE_FILE"]!!)
            storePassword = props["RELEASE_STORE_PASSWORD"]
            keyAlias = props["RELEASE_KEY_ALIAS"]
            keyPassword = props["RELEASE_KEY_PASSWORD"]
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

4. 打包：

```bash
./gradlew assembleRelease

# 输出路径：app/build/outputs/apk/release/app-release.apk
```

## 测试

```bash
# 运行单元测试
./gradlew test

# 编译 Android 仪器测试 APK（不需要连接设备）
./gradlew assembleDebugAndroidTest

# 运行 Android 仪器测试（需要连接设备/模拟器）
./gradlew connectedAndroidTest
```

测试覆盖：
- RecipeRepository 单元测试（MockK）
- RandomSelector 单元测试（Kotest）

## 备份与恢复说明

| 操作 | 步骤 |
|------|------|
| 导出备份 | 设置 → 导出备份 → 选择保存位置 → 生成 `family_recipe_backup.zip` |
| 导入恢复 | 设置 → 导入恢复 → 选择之前导出的 `.zip` 文件 → 重启应用生效 |

备份文件包含：
- `db/` — SQLite 数据库文件（含 WAL 日志）
- `images/` — 菜谱封面图片

建议将备份文件保存到云盘同步目录（如 OneDrive、Google Drive），这样换机或重装时随时可恢复。

## 后续规划

- [ ] 菜单规划（日历/周视图）
- [ ] 从随机选菜结果一键加入菜单计划
- [ ] 基于最近已吃记录优化随机推荐
- [ ] 步骤配图
- [ ] 自动定时备份
- [ ] 深色模式
- [ ] 菜谱分享（导出为图片/PDF）

## License

个人项目，仅供家庭使用。
