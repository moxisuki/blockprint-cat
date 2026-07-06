# 更新日志 (CHANGELOG)

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范。

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
