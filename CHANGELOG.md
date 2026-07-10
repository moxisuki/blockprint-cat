# 更新日志 (CHANGELOG)

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范。

## v1.4.0 · 2026-07-11

### 新增

- **从外部直接打开蓝图文件**：在文件管理器、聊天、浏览器等场景下点击 `.litematic` / `.schem` / `.schematic` / `.nbt` 文件，可直接用 BlockPrint Cat 打开；打开流程经过预览页面（显示文件名、作者、区域数、方块数、文件大小、格式），确认后才导入
- **首页内导入也走预览**：顶部「上传」图标选择本地蓝图文件后，同样先弹出预览确认面板，让用户在写入 SAF 文件夹前审视

### Intent / 权限

- `MainActivity` 加 `android:launchMode="singleTop"`，保证 ACTION_VIEW 经 `onNewIntent` 走并保留原 Activity 的 URI 授权
- 新增多个 `<intent-filter>`（V1 alias 同样复制）覆盖 content:// + file:// 两种 scheme，按扩展名 pathPattern + `*/*` MIME 兜底，所以旧文件浏览器也能匹配
- Uri 通过 `LocalPendingImportUri` CompositionLocal 透传到页面，全程不 stringify、不 `Uri.parse`，保留 ACTION_VIEW 自带的临时读权限
- 文件名通过 `OpenableColumns.DISPLAY_NAME` 查询（部分第三方 provider 用 docId 当 lastPathSegment，会退化为「随机数」），正确显示原始文件名 + 后缀

### UI

- 预览 UI 用 `ModalBottomSheet`，浮在主页/PC/社区任何 tab 之上，不离开当前页面；保留 scrim 让用户感知上下文
- 格式 chip 和 HomeBlueprintCard 完全一致：中文标签 + 按 `BadgeColor.Primary/Secondary/Outline` 上色

### 修复

- MT Manager / Solid Explorer / Google Drive 这类用 docId 当 lastPathSegment 的 provider，原先会显示一串 UUID；现在通过 `OpenableColumns.DISPLAY_NAME` 拿到真实文件名
- `Activity` 的 `enableEdgeToEdge` + `singleTop` 协同下，外部分享文件不再丢 ACTION_VIEW 授权导致 `SecurityException: Permission Denial`

### 重构

- 抽离 `ui/import_/ImportPreviewSheet.kt`：之前是 `Scaffold + TopAppBar` 整页面，现在改成 `ModalBottomSheet`，既支持 ACTION_VIEW 也支持 HomeScreen 内 picker 走同一条路径

## v1.3.0 · 2026-07-10

### 新增

- **分类管理 (Blueprint Categories)**
  - 首页顶部横向滑动的分类条，`HorizontalPager` 实现，每张卡片从 8 色调色板 × 8 种像素图案中选择封面
  - 长按蓝图卡片进入多选模式：底部弹起 Move / Delete 工具栏
  - 类别管理对话框：新建 / 重命名 / 改封面 / 删除（删除时蓝图自动归到「未分类」，零数据丢失）
  - 末页「管理分类」卡片，整合所有类别操作入口
- **分类条交互**
  - 滑动分页即时切换分类，bind 用 `rememberUpdatedState` 解决 stale capture 闭包陷阱
  - 点击分类卡 = 打开编辑，长按分类卡 = 通过「管理」对话框操作
  - 上滑列表自动收起分类条 + filter 面板，下滑自动展开，全程 200ms 缓动

### 动画 / UI 优化

- 分类切换过渡：内容区用 `slideVertically + fade` 替代纯 crossfade，模拟「新内容从下方推出」消除鬼影感
- 分类卡背景与 app 背景融为一体，仅靠主色文字 + SemiBold 字重区分选中态
- 筛选面板展开 / 收起改为 FastOutSlowIn 缓动 + expandVertically + fade
- 多选 AppBar 选中态统一 titleMedium / bodyMedium 字号阶梯

### 国际化

- `res/values/strings.xml` 是源语言（中文），`res/values-en/strings.xml` 是英文翻译
- 所有新功能字符串在 commit 时同时添加到这两个 locale
- 第三方（俄语、日语…）由 Crowdin 管理，本地不再编辑

### 依赖 / 数据层

- 数据库 schema 升到 v10，新增 `categories` 表 + `blueprints.categoryId` 外键，删除分类时 Room `ON DELETE SET NULL` 自动孤立化所属蓝图
- `CategoryManager`（`@Singleton`） + `CategoryDao`，hot StateFlow 暴露给 UI
- 149 个单元测试通过，覆盖 DAO sort/count/FK 行为与 Manager 状态合并

## [1.2.0] · 2026-07-10

### 新增

- **方块绘画 (BlockPaint)**：1×1 方块平面绘画工具
  - 支持画笔/橡皮/缩放(0.5x-6x)/拖拽；画布用真实方块纹理渲染
  - 方块调色板：按 BlockGroup 分类、横向滚动的 BlockPickerStrip
  - 图画列表：Room 持久化，支持新建/切换/重命名/删除
  - AppBar 标题显示当前图画名；导出按钮在 AppBar
  - 导出：PNG（16px/cell 真实纹理）+ BlueprintPreviewContent（MC 命令/蓝图/NBT）
  - blockIds v3 直出：跳过 PixelArtConverter 颜色匹配，画什么块就是什么块
- **文字转蓝图画 (TTB) 重写**：
  - 8×8 位图字体 (FONT8X8) 直接生成 grid，支持缩放/间距
  - TTF 模式：输入中文自动切换系统字体渲染，可调字高 (4-32)
  - 单方块选择、BlockCanvas 实时预览、导出同 BlockPaint
- **共享 UI**：`blueprintcommon/` 包 BlockCatalog/ExportPayloadCodec/BlockPreview/CollapsibleSection 等
- **共享导出**：`ExportBottomSheet` 组件（PNG + BlueprintPreviewContent），BlockPaint/TTB 共用
- **设置页 UI 重构**：分组标题（外观/功能/数据/关于）、统一卡片样式（40dp 图标方块 + titleSmall + 副标题省略）、头部弹性动画
- **缓存管理修复**：Room 数据库大小从 0B 修正（litematic.db → blockprintcat.db + WAL/SHM 文件汇总）；卡片化 UI
- **备份分享**：备份完成支持系统分享（ACTION_SEND）；加载/进度/完成三态卡片 + 百分比进度
- **语言/主题 Dialog 优化**：卡片式选择、选中高亮 + ✓ 图标、更紧凑的排版
- **AppBar 动画**：返回按钮 + customActions 均增加 expandHorizontally/shrinkHorizontally 动画，标题平滑过渡
- **工具页重新设计**：副标题精简、Hero 居中大卡片 + 下方工具并排布局

### 修复

- ITB `ImageToBlueprintState` companion 常量引用 `BlueprintUiDefaults`
- TTB 保存弹框取消卡死：dismiss 后 clearExport
- Pad 布局选择蓝图文件夹无响应：补传 `onRequestSafFolder`
- AppTopBar customActions 支持横滚，不挤压返回按钮
- 缓存 Room 大小始终显示 0B：数据库文件名错误

### 重构

- 删 ITB 旧 components（AdjustSlider/BlockGroupSection/BlockPreview/DitherDropdown/PreviewHero/ResultMaterialsPanel/WidthInput/ExportButton）
- 删 ITB ExportPayloadCodec/PreviewAnimations/PreviewDebounce → typealias 转接 blueprincommon
- ExportPayloadCodec v3：新增 `blockIds` 字段（逐格方块 ID，BlockPaint/TTB 直出）
- 语言/缓存/备份 Dialog 统一卡片设计语言
- 工具页副标题精简 + 卡片化布局

## [1.1.0] · 2026-07-07

### 新增

- **图片转蓝图画 UI 重做**：引入 M3 表达型设计，顶部 Hero 区支持原图/结果 Crossfade 切换，参数调整后 200ms 防抖自动重算，结果出现时边框高亮动画
  - 875 行单文件拆分为 8 个 `components/` + `flow/PreviewDebounce`
  - 新增底部弹出框导出：MC 命令（6 方向 + 复制/分享 .mcfunction）与 4 种蓝图格式（投影/创世神/NBT/建筑小帮手）+ 墙/平铺模式
  - 保存接入 `BlueprintManager.ingest`，保存后按钮变为「查看 + 跳转详情页」
  - 材料清单复用蓝图列表同源 i18n（`LangManager.displayName`）
- **文字转蓝图画**：新增工具入口，Canvas 渲染文字为像素风位图后传入 ITB 管线，支持 12-48sp 字号调整
- 工具页精简：移除 4 个占位工具，列表剩 3 个实际工具
- 方块缩略图模块级 cache + IO 线程预热

### 修复

- 参数 200ms 防抖 + 无图时不显示"更新中"
- 空方块组不再抛异常，重新选组可恢复
- WALL 模式蓝图上下颠倒修复
- 导出截图铺满容器
- 底部弹窗 Snackbar 占位问题修复
- 导航层 base64 双重解码崩溃修复
- `.gitattributes` 统一 LF 行尾

### 重构

- pixelart 包 `com.github.moxisuki` → `io.github.moxisuki`
- 移除方块筛选器模块
- 透明度预处理 `makeBackgroundTransparent`
- 统一进度条 `GenerationProgressBar`（400ms 缓冲）

## [1.0.1] · 2026-07-04

### 新增

- **关于页更新日志入口**：在「关于 → 更新」卡片下增加「更新日志」链接，直接打开本文件最新版本，无需联网检查更新即可看到当前迭代包含哪些改动。

### 移除

- **关于页测试崩溃上报入口**：移除「崩溃上报」测试按钮。该入口仅供开发者验证 Bugly SDK 上报链路，已被原生 Bugly 通道覆盖；保留仅会向最终用户暴露触发崩溃的操作面，故下线。
  - 影响范围：`AboutScreen.kt` 内「崩溃上报」卡片与确认弹窗
  - 不受影响：Bugly SDK 初始化与隐私条款中关于崩溃采集的说明保持不变，正常崩溃上报仍按既定逻辑执行

## [1.0.0] · 2026-07-02

首个稳定版本。所有蓝图、3D 预览、PC 端桥接、社区集成等核心功能均进入可用状态。

### 新增

- 桥接 v2 状态机：上传 / 下载分别引入 `UploadStateMachine`、`DownloadStateMachine` 纯状态机，规避回调竞态；`isTaskInFlight` 互斥锁防止并发传输
- 桥接守护线程：上传 IO 与 SHA 校验迁出主线程，丢弃不必要的 payload 驻留
- 蓝图格式目录（`FormatDisplay` / `FormatCatalog`）：作为格式短名、长名、过滤标签的单一真源
- 详情页文件名展示：在基本信息卡片中显示原始文件名
- 上传安全文件名：Sponge Schematic 上传时检测含中文 / 特殊字符的文件名，给出警告并自动剥离不安全字符
- 主页上传 / 下载按钮在桥接传输进行中自动禁用
- 渲染页 Modrinth 资源搜索与多版本安装

### 修复

- 修复桥接上传 / ready 消息与二进制块发送的时序竞态
- 修复 SAF 写入时已存在同名子文件导致 `createDocument` 失败的问题
- 修复转换格式时生成重复文件并误删原文件的问题
- 修复主页胶囊标签文本垂直居中偏差与切换抖动
- 修复状态栏图标颜色在深色 / 浅色切换瞬间的衍生色错误

### 重构

- **架构层**
  - `BlueprintDetailScreen` 由 1054 行拆分为多个聚焦文件
  - `HomeScreen` 由 955 行拆分为多个聚焦文件
  - `MainActivity` 拆分为 `AppNavGraph` 与辅助函数
  - `BridgeViewModel` 拆分为 Session 控制器与传输控制器
- **性能 / 稳定性**
  - `NavGraphFlags` 字段展平，恢复 Compose Stable 推断
  - 主页胶囊宽度通过 `BoxWithConstraints` 缓存
  - 跨分支派生状态从 `NavGraphFlags` 中上提
  - 启用 Compose Compiler Reports 与 metrics 输出
- **格式化**：`FormatChip` / 过滤行 / 转换弹窗统一从 `FormatCatalog` 读取
- **构建**：合并 `defaultConfig`、修正 ProGuard 包路径
- **依赖**：升级 `blockprint-core` 至 1.0.0（本地 Maven 仓库）

## [0.2.x] · 2026-06-24 前后

早期迭代：桥接协议与 SAF 基础功能逐步成型。

### 新增

- 桥接传输时附带 `source` 参数，便于服务端识别来源
- 桥接调试日志（下载二进制 + ready 流程）便于问题定位
- 主页 PC 端蓝图卡片显示格式 Chip
- 详情页新增格式 Chip（基于 `SchematicFormat.valueOf`）

### 修复

- 桥接 eager chunk 发送：`firstOrNull` 守护避免空指针
- 桥接 `isAnyTransferInFlight` 测试覆盖
- SAF 识别 `.json` 作为蓝图扩展名（Building Helper 格式）

### 重构

- 桥接事件 `UploadResult.errorCode` 重命名为 `error`，对齐服务端字段
- 桥接 `DownloadStateMachine` 暴露 `Failed` 终态与孤儿二进制路由
- 上传信号类型抽离为 `UploadSignal` 密封类型，删除死参数
- 格式 i18n：新增 `format_short_*` / `format_long_*` / `format_filter_*` 系列键

## [0.1.x] · 2026-06 之前

雏形阶段：本地蓝图导入、3D 预览、社区爬虫、Bugly 崩溃通道接入。

### 新增

- 本地蓝图目录选择（SAF）
- 3D 预览：基于 SceneView / Filament 渲染
- 社区蓝图：MCS / CMS 站点爬虫，QQ 登录
- 隐私条款：覆盖 Bugly SDK 设备级诊断信息的合规说明
- 主题与配色：动态 App 图标（v1–v4）
- 备份 / 缓存管理：本地蓝图打包为 ZIP、Room / GLB / 渲染资源清理

### 依赖

- `blockprint-core` 0.1.x：NBT / litematic / schematic 解析与 GLB 生成
