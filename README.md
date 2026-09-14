# Token Monitor Android

<p align="center">
  <img src="public/icon-192.png" width="108" height="108" alt="Token Monitor Logo" />
</p>

<p align="center">
  <strong>现代、灵动、高性能的 AI Token 用量与配额监控 Android 原生客户端</strong>
</p>

<p align="center">
  <a href="https://github.com/hcen229/Token-Monitor-Android/releases"><img src="https://img.shields.io/github/v/release/hcen229/Token-Monitor-Android?style=flat-square&color=00D084&label=Release" alt="Release" /></a>
  <img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_1.7+-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="License" />
</p>

<p align="center">
  <a href="#-主要功能">主要功能</a> ·
  <a href="#-快速上手">快速上手</a> ·
  <a href="#-编译与构建">编译构建</a> ·
  <a href="#-常见问题-faq">常见问题</a>
</p>

---

## 📖 项目简介

**Token Monitor Android** 是专为配合 [Token Monitor](https://github.com/Javis603/token-monitor) 桌面端打造的移动端原生应用。

通过局域网点对点通信，它能把电脑上各 AI 编码工具（Cursor、Cline、Trae、Copilot 等）以及主流大模型（DeepSeek、Claude、OpenAI、Gemini、通义千问、Kimi、豆包等）的 **Token 消耗量、折算费用、请求统计、模型消耗占比与服务商剩余配额** 实时同步到手机，并以 **状态栏胶囊、实时通知与常驻状态栏** 的形式持续呈现。

> **一句话概括**：手机上不打断当前操作，抬眼即可看清电脑端 AI 的用量与工作进度。

---

## 💡 设计初衷：AI 跑任务，玩手机也能掌握进度

在 Cursor、Cline、Trae、Claude Code 等工具中下达复杂重构、批量代码生成或多步 Agent 任务后，往往需要等待 1～5 分钟。

这段等待时间里，许多用户习惯拿起手机刷刷资讯、回回消息，心里却始终挂念：

- *"AI 到底写完了没有？"*
- *"是网络卡住中断了，还是在等我点 Proceed 确认？"*

于是不得不频繁抬头、探身去看电脑屏幕，无法真正放松。

**Token Monitor Android 的解法**：把 AI 的实时工作脉搏直接带到手机状态栏——**无需切换正在浏览的手机 App，仅凭屏幕顶端的实时更新即可掌握进度**。

| 状态         | 触发条件             | 含义                             |
|:---------- |:---------------- |:------------------------------ |
| 🟢 **工作中** | 检测到 Token 数值持续增长 | 电脑端 AI 正在全力生成代码或调用工具，可以继续安心看手机 |
| ⚪ **空闲中**  | 数值停止变动满 2 分钟     | 任务已完成，或正在等待你的交互与下一步指令，该回电脑看结果了 |
| ⚠️ **离线**  | 桌面端退出或局域网中断      | 连接已断开，立即提示，避免空等                |

---

## 🌟 核心设计理念

- **纯原生打造**：采用 **Jetpack Compose + Material 3 + Kotlin 2.0** 构建，不引入 WebView 与跨平台框架。
- **广泛系统兼容**：面向通用 Android 8.0+ 开发，兼容各大品牌主流系统的状态栏胶囊、打孔屏胶囊扩展以及 Android 原生实时通知标准。
- **纯本地点对点通信**：数据仅在局域网内的手机与电脑之间直连传输，零云端依赖，无第三方商业广告与追踪 SDK。
- **智能节能设计**：内置前后台自适应刷新控制与唤醒锁守护，在保障数据实时性的同时大幅降低息屏与后台功耗。

---

## ✨ 主要功能

 

### 1. 监控看板与全景数据分析

- **多周期用量与费用统计**：支持自由切换「今日」「本月」「全部」周期，清晰呈现 Token 消耗、折算费用、请求次数与会话数。
- **365 天活动贡献热力图**：经典 GitHub 风格 7 行 × N 列贡献热力矩阵，按单日用量量化为 5 阶色级；长按任意方格可呼出浮层查看该日详细指标。
- **平滑贝塞尔走势折线图**：支持 24 小时与近 7 天走势曲线，采用平滑三次贝塞尔曲线配合渐变光带，自动寻找极值点并标注峰值徽标。
- **本地模型排行与客户端分布**：
  - 精确统计各模型的消耗量与百分比占比；
  - 直观统计 Cursor、Cline、Trae、Copilot 等不同客户端的贡献比例；
  - 数据超过 7 项时自动收纳至全览抽屉，保持主屏简洁。

### 2. 实时通知

- **多维度配额计算模式**：支持智能自动（自动捕获最紧张项或现金余额）、5 小时滚动限制、每周配额限制以及现金余额等多种匹配模式。
- **智能工作状态感知**：检测到 Token 变动自动标记为「工作中」；连续 2 分钟无变动自动转入「空闲中」，契合真实开发节奏。

### 3. 服务商配额与余量可视化

- **20+ 官方品牌矢量徽标**：内置 DeepSeek、Gemini、Claude、OpenAI/GPT（Codex）、Qwen（通义千问）、Kimi（月之暗面）、Doubao（豆包）、MiniMax、Grok、Llama / Mistral / Ollama、Antigravity、OpenCode、WorkBuddy、Cursor、Cline、Copilot、Trae、OpenRouter、阿里云、Droid Factory 等官方规范矢量图标与品牌专属色。
- **双风格可视化展示**：支持「同心双环」与「横向进度条」自由切换，支持自定义内外环绑定规则与环心文本。
- **自定义卡片排序**：支持长按拖拽调整各服务商卡片的展示顺序。

### 4. 界面美学与大屏自适应

- **悬浮液态玻璃底栏**：基于实时高斯模糊与物理弹簧吸附动效，触控交互自然流畅。
- **毛玻璃渐进式顶栏**：滑动浏览时，顶栏自然过渡至渐进式高斯模糊，与内容无缝融合。
- **响应式双栏排布**：针对平板设备与手机横屏专门适配（左侧核心概览，右侧配额与图表），充分利用宽屏显示空间。

### 5. 智能节能与长连接保活

- **前后台自适应降频**：应用在前台时极速刷新（默认 3 秒）；切入后台或息屏后自动降频至 45 秒，大幅减少后台唤醒与连接开销。
- **前台长连接守护**：采用前台服务（`dataSync` + `specialUse`）与 `PARTIAL_WAKE_LOCK` 唤醒锁守护机制，保障锁屏与后台状态下持续稳定同步。
- **状态自愈**：数据流异常或连接闪断后自动重连并恢复状态判定，无需手动干预。

---

## 📱 兼容性与生效范围

| 能力         | 支持范围                  | 说明                                             |
|:---------- |:--------------------- |:---------------------------------------------- |
| 应用运行       | Android 8.0+（API 26+） | `minSdk 26` / `targetSdk 34` / `compileSdk 36` |
| 常驻通知与数据同步  | Android 8.0+          | 前台服务 + 通知渠道                                    |
| 实时通知       | Android 16 及以上        | 走官方 Promoted Ongoing 通知路径                      |
| 超级岛 / 焦点通知 | 小米澎湃 OS（HyperOS 3-4）  | 走小米 Focus Notification V3 协议                   |
| 其他品牌系统     | Android 8.0+          | 自动回退为标准常驻通知展示                                  |
| 大屏与横屏      | 平板、折叠屏、手机横屏           | 响应式双栏布局                                        |
| 网络环境       | 局域网（同一 Wi-Fi / 网段）    | 目前不支持跨网段与公网直连                                  |

---

## 🚀 快速上手

### 前置条件

1. 电脑端已安装并运行 [Token Monitor 桌面端](https://github.com/Javis603/token-monitor)。
2. 电脑与手机连接至 **同一局域网**（例如连接同一个 Wi-Fi 路由器）。
3. 电脑端防火墙已放行对应端口（默认端口 **17321**）。
   *Windows 用户可右键以管理员身份运行项目自带的 `open-firewall.bat` 一键完成放行。*

### 使用步骤

1. 在手机上安装并打开 Token Monitor，滑动底栏进入 **「设置」** 页面。
2. 填入电脑的局域网 IP（例如 `192.168.1.100`）及桌面端设置的通信密钥（Secret）。
3. 点击 **「保存配置」**，随后点击 **「检查连接」** 验证连通性。
4. 体检通过后返回 **「实时监控」** 看板，即可查看实时同步的 Token 数据。
5. 在设置页中根据喜好配置胶囊槽位并开启通知，上滑返回桌面即可在状态栏持续查看用量。

---

## 🛠️ 编译与构建

### 环境要求

- JDK 17+
- Android SDK（`compileSdk 36`、`minSdk 26`、`build-tools 36.1.0`）

### 构建命令

```powershell
# Windows PowerShell：一键全自动编译、签名并校验 Release APK
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

```cmd
:: Windows CMD 命令行一键编译
.\build-apk.bat
```

```bash
# 使用 Gradle Wrapper 构建
./gradlew assembleDebug      # 构建 Debug 包
./gradlew assembleRelease    # 构建 Release 包
```

编译完成后，已签名的 Release APK 将输出至项目根目录：`TokenMonitor.apk`。

---

## 📁 项目结构

```text
Token-Monitor-Android/
├── app/src/main/
│   ├── AndroidManifest.xml                 # 权限、服务声明与应用元数据
│   └── java/com/tokenmonitor/app/
│       ├── MainActivity.kt                 # 应用主入口，沉浸式交互，权限调度
│       ├── TokenMonitorApp.kt              # Application 单例，初始化全局通知渠道
│       ├── data/
│       │   ├── Models.kt                   # 数据模型 (TokenStats, Quota, ClientStat...)
│       │   ├── IslandConfig.kt             # 状态栏胶囊与槽位配置模型
│       │   └── TokenRepository.kt          # 局域网通讯、持久化与双阶段网络体检引擎
│       ├── service/
│       │   ├── ScreenStateManager.kt       # 息屏 / 亮屏状态监听
│       │   ├── TokenMonitorService.kt      # 前台长连接服务（自适应节能降频）
│       │   └── TokenNotificationManager.kt # 状态栏通知与胶囊分发管理器
│       ├── ui/
│       │   ├── HomeScreen.kt               # 监控看板（Hero 卡片、数字滚轮、大屏适配）
│       │   ├── SettingsScreen.kt           # 设置页（参数配置、网络体检、开源鸣谢）
│       │   ├── MainViewModel.kt            # 核心 ViewModel
│       │   ├── ActivityTrendCharts.kt      # 活动热力图与走势图组件
│       │   ├── BrandIcon.kt                # 官方 AI 品牌矢量图标与色彩体系
│       │   ├── components/
│       │   │   └── SuperIslandStudio.kt    # 动态胶囊工坊（仿真舱、拖拽放置）
│       │   ├── glass/                      # 液态玻璃材质与滑动控件
│       │   └── theme/                      # 主题配色与排版
│       ├── update/                         # 应用内 OTA 在线升级模块
│       └── util/
│           └── CrashHandler.kt             # 全局异常捕获器
├── build-apk.ps1                           # 一键 Release 构建脚本 (PowerShell)
├── build-apk.bat                           # 一键构建批处理脚本 (CMD)
├── open-firewall.bat                       # 防火墙 17321 端口一键放行工具
├── PROJECT_MANUAL.md                       # 完整项目手册（架构与协议细节）
└── README.md                               # 项目说明文档
```

---

## 🔒 隐私与安全

- **纯局域网点对点通信**：所有数据仅在手机与电脑之间直接传输，不经过任何外部服务器。
- **无分析与追踪 SDK**：应用内不包含任何商业广告、数据统计或行为追踪组件。
- **密钥鉴权保护**：所有 API 请求均通过共享 Secret 鉴权，防止局域网内未授权访问。
- **按需最小权限**：仅申请通知、网络状态、前台服务与唤醒锁等必要权限，不读取通讯录、位置与相册。

---

## 🗺️ 更新计划与社区规划

- [ ] **Android 桌面小部件（Widget）**：支持添加桌面小组件，无需点开应用即可在手机主屏一眼掌握今日用量与配额。
- [ ] **配额不足与异常用量预警**：当服务商剩余配额过低（如低于 20%）或单日消耗激增时，主动发送通知预警。
- [ ] **独立获取供应商用量**：支持在脱离电脑端时，手机端直接请求部分服务商的剩余配额接口。

---

## ❓ 常见问题 (FAQ)

### Q1: 网络体检提示 TCP 端口连接超时？

1. 确认手机和电脑连接在同一个局域网 Wi-Fi 下，且路由器没有开启「AP 隔离」。
2. 确认填写的 IP 为电脑当前实际的局域网 IPv4 地址。
3. 确认电脑防火墙已放行 17321 端口（可运行项目目录下的 `open-firewall.bat`）。

### Q2: 为什么在前台打开应用时看不到状态栏胶囊？

主流系统为防止与前台界面的触控或标题栏冲突，通常会在应用处于前台活跃状态时主动压制本应用的胶囊展示。上滑退回桌面、锁屏或切换到其他应用时，胶囊便会自然展开。

### Q3: 息屏一段时间后状态栏不更新？

请在手机系统设置的「应用管理」中，将 Token Monitor 的电池优化调整为 **「无限制」**（或「不优化电池使用」），并允许后台自启动，防止系统过度杀死后台同步服务。

### Q4: 系统通知设置里找不到「通知类别」？

部分系统在应用从未真正发出过通知前，会隐藏通知类别列表。请先在应用设置页执行一次「触发通知 / 发送测试通知」，通知发出后类别即会出现在系统设置中。

---

## 🙏 致谢与开源参考

- **桌面端核心**：[Javis603/token-monitor](https://github.com/Javis603/token-monitor) — 优秀的桌面端 AI Token 监控工具及 Hub 协议 (MIT)
- **界面模糊组件**：[kyant0/backdrop](https://github.com/kyant0/backdrop) — 驱动硬件级 AGSL 液态毛玻璃模糊引擎 (Apache-2.0)
- **开源字体**：[JetBrains/JetBrainsMono](https://github.com/JetBrains/JetBrainsMono) — 现代等宽字体 (OFL 1.1)
- **上游数据原型**：[junhoyeo/tokscale](https://github.com/junhoyeo/tokscale) — CLI Token 追踪分析原型 (MIT)
- **架构参考**：
  - [jizizr/signaldock](https://github.com/jizizr/signaldock) — 打孔屏左右腔体分离设计参考 (MIT)
  - [FrancoGiudans/Capsulyric](https://github.com/FrancoGiudans/Capsulyric) — 状态栏通知胶囊互操作性技术参考 (GPL-3.0)

---

## 📄 开源协议

本项目基于 [MIT License](./LICENSE) 开源。

Copyright © 2026 Token Monitor Contributors
