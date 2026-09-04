# 家庭菜谱 (Family Recipe Book)

一款面向家庭使用的 Android 菜谱管理应用，帮助你记录拿手菜的做法、家人的口味偏好、食材明细，并在做饭前快速生成购物清单。

**当前版本：v1.2.0** — 统一设计系统、修复备份数据完整性、补充 CI 与测试、优化各页面 UI。

## 功能介绍

### 菜谱管理
- 创建、编辑、删除菜谱
- 记录菜名、简介、分步骤做法、烹饪时间、难度（1-5 星）、分类
- 记录每道菜的食材、数量、单位和备注
- 封面图片支持（相册选择/相机拍摄，自动缩放至 1920px 内保存）
- 推荐指数评分（1-5 星）

### 菜谱分类
- 8 种预定义分类，带 emoji 与专属配色标签：🍳 炒菜、🍲 煲汤、⚡ 速食、🍚 主食、🥗 凉菜、🍰 甜品、🥤 饮品、🍴 其他
- 按分类筛选菜谱
- 创建/编辑时必选分类

### 首页与列表
- 搜索框支持一键清除；排序收纳为下拉菜单；筛选收纳为底部面板（激活时显示角标）
- 首页「今天吃什么」渐变横幅，一键进入随机选菜
- 收藏菜谱分组置顶展示；列表项支持长按编辑/删除
- 滚动时 TopAppBar 自动收起，留出更多列表空间

### 搜索与筛选
- 按名称/简介/步骤/食材模糊搜索（"家里有鸡蛋能做什么"一搜便知）
- 按家庭成员偏好筛选（选中成员都喜欢的菜）
- 按分类筛选
- 多条件组合筛选（取交集），筛选条件收纳在底部面板，激活时显示角标
- 空状态友好提示，可一键清除筛选/添加菜谱

### 多维排序与收藏
- 支持按更新时间、创建时间、烹饪时间、难度、推荐指数排序
- 升序/降序切换
- 收藏菜谱始终置顶显示
- 详情页一键收藏/取消收藏

### 随机选菜
- 首页横幅或顶栏进入「今天吃什么」
- 支持按分类过滤（"随机一个汤"）
- 可配置默认数量（1-10，设置页调整）
- 支持自定义单次数量
- "换一批"重新随机
- 菜谱不足时友好提示

### 烹饪模式与分享
- 详情页封面 hero 布局：底部渐变压暗，叠加分类与烹饪时长
- 步骤可逐条勾选完成，带进度条与完成计数
- 屏幕常亮开关，做饭时不熄屏
- 菜谱一键分享为文本（含食材和步骤）

### 食材与购物清单
- 在菜谱编辑页维护食材清单
- 在菜谱详情页查看食材明细
- 从首页进入购物清单页面，勾选多道菜谱后自动合并食材
- 同食材同单位的数量自动数值累加（2个 + 3个 = 5个），不同单位并列展示
- 支持手动添加临时采购项（酱油、纸巾……）
- 采购时可逐项勾选"已购"，并显示剩余项数
- 清单可分享为文本发给家人代买
- 清单状态持久化，退出应用不丢失

### 家庭成员
- 添加家庭成员，设置姓名、代表色、备注（如忌口信息）
- 每位成员用独特颜色标识，一目了然

### 口味打标
- 在菜谱详情页为每位家人标记喜好：喜欢 👍 / 一般 / 不喜欢 👎
- 方便决定"今天做什么"时参考家人口味

### 数据备份与恢复
- 手动导出：将数据库和图片打包为 `.zip` 文件，保存到手机任意位置或云盘
- 手动导入：选择之前导出的 `.zip` 文件恢复数据，成功后一键重启应用生效
- 应用重装后数据不丢失，只需导入备份即可恢复
- 同时开启 Android Auto Backup 作为兜底保护

### 界面与视觉
- 跟随系统的深色模式（`values-night` 主题）
- 暖色奶油背景与语义化扩展色（喜欢 / 不喜欢 / 收藏 / 评分星）
- 分类彩色胶囊标签、琥珀色评分星、空状态 emoji 点缀

## 技术架构

### 技术栈

| 层级 | 技术选型 |
|------|----------|
| 语言 | Kotlin 2.1.0 |
| UI 框架 | Jetpack Compose + Material 3 |
| 架构模式 | MVVM (ViewModel + StateFlow + Repository) |
| 依赖注入 | Hilt 2.54 |
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
│   │   └── SettingsStore.kt        # 随机数量、购物清单状态等 DataStore 存储
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
│   └── ImageUtils.kt               # 图片采样缩放、EXIF 旋转矫正、存储与删除
└── ui/
    ├── theme/Theme.kt              # 深浅主题 + 语义化扩展色
    ├── components/                 # 共享 UI 组件
    │   ├── StarRating.kt           # 5 星评分组件
    │   ├── CategoryChip.kt         # emoji + 分类配色标签
    │   ├── RecipeCover.kt          # 封面缩略图（含占位）
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

RecipeCategory (分类枚举，含 emoji)
  STIR_FRY(🍳炒菜), SOUP(🍲煲汤), QUICK_MEAL(⚡速食),
  STAPLE(🍚主食), COLD_DISH(🥗凉菜), DESSERT(🍰甜品),
  BEVERAGE(🥤饮品), OTHER(🍴其他)

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
- Gradle 8.5（项目自带 wrapper，`./gradlew` 可直接构建）

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

签名配置已写在 `app/build.gradle.kts`：本地读 `local.properties`，CI 读环境变量。未配置密钥时 `assembleRelease` 不会签名。

1. 生成签名密钥（首次，密钥文件不要提交到 Git）：

```bash
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias family-recipe
```

2. 在项目根目录的 `local.properties` 中追加（不要提交到 Git）：

```properties
RELEASE_STORE_FILE=release-key.jks
RELEASE_STORE_PASSWORD=你的密码
RELEASE_KEY_ALIAS=family-recipe
RELEASE_KEY_PASSWORD=你的密码
```

3. 打包：

```bash
./gradlew assembleRelease

# 输出路径：app/build/outputs/apk/release/app-release.apk
```

## GitHub 自动发版

推送符合 `v*` 的 tag 后，[Android Release](.github/workflows/android-release.yml) 会跑测试、打包 APK，并创建 [GitHub Release](https://github.com/githubgotest001/cook-book/releases) 供他人下载。

```bash
# 1. 确认 versionName / versionCode 已更新，并已推到 main
# 2. 打 tag 并推送（会触发自动发版）
git tag v1.2.0
git push github v1.2.0
```

未配置 Release 密钥时，发布的是 debug 签名 APK（文件名带 `-debug`）。要让用户覆盖安装升级，请把同一份 `release-key.jks` 配进仓库 Secrets：

1. 生成纯 Base64（不要用 `certutil`，它会带证书头）：

```powershell
# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release-key.jks")) | Set-Clipboard
```

```bash
# Linux
base64 -w0 release-key.jks

# macOS
base64 -i release-key.jks | pbcopy
```
2. 仓库 **Settings → Secrets and variables → Actions** 添加：
   - `RELEASE_KEYSTORE_BASE64`
   - `RELEASE_STORE_PASSWORD`
   - `RELEASE_KEY_ALIAS`（如 `family-recipe`）
   - `RELEASE_KEY_PASSWORD`

之后再推 tag，Release 会挂上正式签名的 `family-recipe-vX.Y.Z.apk`。

## 测试

```bash
# 运行单元测试（JUnit4 + Kotest，统一跑在 JUnit Platform 上）
./gradlew test
```

测试覆盖：
- RecipeRepository 单元测试（MockK）
- RandomSelector 单元测试（Kotest）
- IngredientAmountMerger 购物清单数量合并测试
- ShoppingListViewModel 购物清单交互测试（勾选/合并/已购/清空）
- RandomPickViewModel 随机选菜测试（默认数量/分类过滤/不足警告）

每次 push / PR 会由 GitHub Actions 自动执行 `test` 与 `assembleDebug`（见 `.github/workflows/android-ci.yml`）。推送 `v*` tag 时由 `.github/workflows/android-release.yml` 打包 APK 并创建 GitHub Release。

## 备份与恢复说明

| 操作 | 步骤 |
|------|------|
| 导出备份 | 设置 → 导出备份 → 选择保存位置 → 生成 `family_recipe_backup.zip` |
| 导入恢复 | 设置 → 导入恢复 → 选择之前导出的 `.zip` 文件 → 点击「立即重启」生效 |

备份文件包含：
- `db/` — SQLite 主数据库文件（导出前执行 WAL checkpoint，日志已合并进主文件）
- `images/` — 菜谱封面图片

建议将备份文件保存到云盘同步目录（如 OneDrive、Google Drive），这样换机或重装时随时可恢复。

## 近期改进（v1.2）

- 统一 Material 3 设计系统：Typography、Shapes、语义化卡片与空状态组件
- 修复数据库双实例问题：Hilt 与备份共用单例；导出仅做 WAL checkpoint 不关库，导出后无需重启
- 购物清单新增采购进度条；各页面空状态与卡片视觉优化
- 补充单元测试：`IngredientAmountMerger`、`ShoppingListViewModel`、`RandomPickViewModel`；修复 JUnit4 测试在 JUnit Platform 下被跳过
- 添加 GitHub Actions CI；补齐 Unix `gradlew` 脚本；移除无源码支撑的 androidTest 依赖声明

## 近期改进（v1.1）

- 修复编辑页换图后未保存导致封面丢失的问题
- 大图两段式采样解码 + EXIF 旋转，避免 OOM 与照片方向错误
- 删除菜谱时同步清理封面图片文件
- 购物清单：已购勾选、数量合并、手动项、分享、状态持久化
- 首页 UI 重构：筛选面板、排序菜单、随机选菜横幅、收藏分组
- 详情页烹饪模式与文本分享；随机选菜支持分类过滤
- 搜索支持按食材匹配；导入备份后一键重启

## 后续规划

- [ ] 菜单规划（日历/周视图）
- [ ] 从随机选菜结果一键加入菜单计划
- [ ] 基于最近已吃记录优化随机推荐
- [ ] 步骤配图
- [ ] 自动定时备份
- [x] 深色模式（跟随系统）
- [x] 菜谱分享（文本格式）
- [ ] 菜谱分享（导出为图片/PDF）

## License

个人项目，仅供家庭使用。
