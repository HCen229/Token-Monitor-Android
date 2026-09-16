# 📝 更新日志 / CHANGELOG

本文件记录了 **Token Monitor Android** 的所有重要版本更新与改动。

All notable changes to **Token Monitor Android** are documented in this file.

---

## [1.1.0] - 2026-09-16

> **GitHub Release Tag**: `v1.1.0` · **VersionCode**: `2`

### 🇨🇳 简体中文

#### 🌟 核心更新亮点 (Highlights)

##### 1. 🏝️ 小米 HyperOS 超级岛展开态交互与快捷唤起
- **全新展开态定制卡片**：点击状态栏超级岛胶囊即可展开大尺寸卡片，内嵌今日 Token 完整用量、已连接 PC Hub 主机 IP、更新时间，以及前排核心供应商的同心双进度环。
- **高频秒级唤醒交互**：点击展开态卡片或状态栏胶囊时，自动触发 60 秒 1s 高频刷新；点击卡片任意区域均可一键回跳主应用界面。
- **Android 16 / AOSP 标准适配**：全面适配 Android 16 Live Updates 规范，在非小米机型上保持优雅的折叠/展开通知体验。

##### 2. ⚠️ 智能余量/余额不足超级岛系统级预警
- **两阶段防打扰生命周期状态机**：
  - **阈值预警**：当任一供应商配额低于设定阈值（默认 20%）或余额不足时，**仅弹出提醒一次**；
  - **耗尽预警**：当配额或余额降至 0 时，**仅弹出提醒一次**；
  - 缩回状态栏胶囊后持久化静默，**坚决杜绝反复自动弹窗轰炸**；仅在配额充值恢复后重置状态。
- **多窗口（Multi-Window）聚合判定**：同一供应商多窗口（如 5 小时会话与周额度）统一按最小剩余计算，彻底解决多窗口互相冲突的问题。
- **自由配置预警阈值**：在「设置 ➔ 超级岛」中可自由启闭预警，并独立调节剩余百分比与余额报警金额，提供一键测试预览按钮。

##### 3. 🌐 AI 供应商直连与按量计费余额追踪
- **脱离 PC Hub 独立测额**：新增供应商直连管理器（DirectProviderManager），支持在脱离电脑桌面端时，直接请求各大官方模型 API 接口获取额度。
- **供应商二级管理页**：精选 DeepSeek、Claude、OpenAI、Gemini、Codex、通义千问等各大 AI 品牌专属矢量 Logo 与色彩系统，支持多账号余额与配额追踪。
- **Codex OAuth 与重置积分支持**：新增 Codex 认证引导与额度窗口异常防护。

##### 4. 🫧 1:1 硬件级液态毛玻璃底部导航栏
- **AndroidLiquidGlass 动力学重构**：基于 AGSL 硬件着色器打造 1:1 液态玻璃悬浮底栏，支持 Squash & Stretch 物理拉伸阻尼交互、动态高光流转与光学透镜折射效果。

##### 5. 🌍 全量中英文国际化与开源致谢
- **完整双语支持**：内置 Simplified Chinese 与 English 语言包，可在设置中自由切换或跟随系统。
- **开源鸣谢名单**：在关于页中正式致谢收录 [MUKAPP/CustomLand](https://github.com/MUKAPP/CustomLand)（灵动岛与 Live Updates 参考项目，BSD-3-Clause），向开源社区先驱致敬。

---

### 🇺🇸 English

#### 🌟 Highlights

##### 1. 🏝️ Xiaomi HyperOS Super Island Expandable Layout & Quick Wakeup
- **Brand-New Expanded RemoteViews**: Tapping the status bar capsule now expands a full-sized interactive card displaying unshortened today tokens, connected PC Hub host IP, update timestamp, and dual concentric progress rings for top providers.
- **High-Frequency Refresh on Demand**: Interacting with the expanded island triggers 1-second fast polling for 60 seconds. Tapping anywhere on the expanded card smoothly returns you to the main app.
- **Android 16 / AOSP Live Updates Ready**: Strictly adheres to Android 16 Promoted Ongoing notification guidelines for consistent behavior across all OEM skins.

##### 2. ⚠️ Intelligent Quota & Low Balance System Alerts
- **Two-Stage Non-Intrusive Lifecycle State Machine**:
  - **Threshold Alert**: Triggers **only once** when a provider's remaining quota drops below the threshold (default 20%) or balance runs low.
  - **Depletion Alert**: Triggers **only once** when quota or balance reaches zero.
  - Remains silent in the status bar capsule after shrinking—**no annoying repeat popups**. Resets automatically only after quota is restored.
- **Multi-Window Aggregation**: Seamlessly aggregates multiple quota windows (e.g. 5-hour session + weekly limits) under the same provider to eliminate intra-provider window collisions.
- **Customizable Alert Thresholds**: Configure quota percentage and balance alert amounts freely in Settings with a real-time preview test button.

##### 3. 🌐 Direct AI Provider Connections & Pay-As-You-Go Balance
- **Standalone Remote Monitoring**: Query official provider APIs directly on mobile without requiring an active desktop PC Hub connection.
- **Dedicated Provider Hub**: Features official vector logos and authentic brand colors for DeepSeek, Claude, OpenAI, Gemini, Codex, Qwen, and more.
- **Codex OAuth & Reset Credits Support**: Includes Codex account authentication and quota reset tracking.

##### 4. 🫧 1:1 Hardware-Accelerated Liquid Glass Bottom Tabs
- **AndroidLiquidGlass Dynamic Overhaul**: Powered by AGSL shaders, delivering authentic Squash & Stretch drag physics, fluid highlights, and optical lens refraction.

##### 5. 🌍 Complete Internationalization & Open Source Acknowledgements
- **Full Bilingual Localization**: Effortlessly switch between Simplified Chinese and English, or follow system default language.
- **Community Acknowledgements**: Officially credited [MUKAPP/CustomLand](https://github.com/MUKAPP/CustomLand) (reference for Dynamic Island & Live Updates notifications, BSD-3-Clause).

---

## [1.0.0] - 2026-09-15

> **GitHub Release Tag**: `v1.0.0` · **VersionCode**: `1`

### 🇨🇳 简体中文
- **初始正式版发布**：
  - 支持局域网点对点连接与自动重连；
  - 核心用量看板：今日、本月、全部累计 Token 统计与折算费用；
  - 365 天 GitHub 风格活动热力矩阵与历史走势折线图；
  - 动态胶囊工坊（SuperIslandStudio）：自由拖拽编排左右显示元件；
  - 支持 Xiaomi HyperOS 超级岛胶囊与 AOSP 常驻状态栏通知；
  - 内置网络体检与诊断工具；
  - 针对大屏与折叠屏平板的响应式双栏自适应布局。

### 🇺🇸 English
- **Initial Official Release**:
  - LAN peer-to-peer sync with automatic reconnect;
  - Core metrics dashboard: Today, Month, All-Time Token usage & cost estimation;
  - 365-day GitHub-style activity contribution heatmap and trend curves;
  - Super Island Studio: drag & drop layout customizer for left/right capsule slots;
  - Xiaomi HyperOS Super Island capsule & AOSP ongoing status bar notification support;
  - Built-in connection diagnostic and port health check tool;
  - Responsive dual-column layout for tablets and foldable devices.
