package com.tokenmonitor.app.ui.i18n

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import com.tokenmonitor.app.data.AppLanguage
import com.tokenmonitor.app.data.IslandItemType
import com.tokenmonitor.app.data.PeriodTab
import com.tokenmonitor.app.data.QuotaDisplayStyle
import com.tokenmonitor.app.data.RingCenterTextConfig
import com.tokenmonitor.app.data.RingOrderConfig
import com.tokenmonitor.app.data.TokenRepository
import java.text.SimpleDateFormat
import java.util.Locale

interface AppStrings {
    val isEnglish: Boolean

    // Navigation & Bar
    val navHome: String
    val navSettings: String

    // Status Header
    fun statusConnected(host: String): String
    val statusRemote: String
    val statusDisconnectedCached: String
    val statusDisconnected: String
    val statusConnecting: String
    fun lastUpdated(time: String): String

    // Status State
    val statusActive: String
    val statusIdle: String
    val statusOffline: String

    // Period Tabs & Metrics
    val periodDay: String
    val periodMonth: String
    val periodTotal: String
    val todayTokens: String
    val monthTokens: String
    val totalTokens: String
    fun messageCount(count: Int): String
    fun sessionCount(count: Int): String
    val estimatedCost: String

    // Quotas & Providers
    val remainingQuota: String
    fun providerCount(count: Int): String
    fun percentRemaining(percent: Int): String
    fun resetsIn(time: String): String
    fun resetPrefix(time: String): String
    fun outerRing(label: String): String
    fun innerRing(label: String): String
    val fiveHours: String
    val weekly: String
    val session: String
    val monthly: String
    val monthlyLimit: String
    fun resetCreditsAvailable(count: Int): String
    val resetCreditsNone: String
    fun formatResetCreditsExpiry(rawExpiry: String): String

    // Breakdown Cards
    val modelBreakdown: String
    fun modelCount(count: Int): String
    val clientBreakdown: String
    fun clientCount(count: Int): String
    val viewAll: String
    fun viewAllModels(count: Int): String
    fun viewAllClients(count: Int): String
    val allModelsDetail: String
    val allClientsDetail: String
    val inputTokens: String
    val outputTokens: String
    val cacheReadTokens: String
    val cacheCreationTokens: String
    val close: String
    val noData: String

    // Activity & Trends
    val activity: String
    val activityHint: String
    fun activeDays(days: Int): String
    val noUsage: String
    val trend: String
    fun peak(tokens: String): String
    fun formatDateForActivity(dateStr: String): String
    fun formatMonthForActivity(dateStr: String): String

    // Error Card
    val disconnectedCachedError: String
    val networkAnomalyError: String
    val retry: String

    // Settings Screen
    val settingsTitle: String
    val settingsSubtitle: String
    val languageSectionTitle: String
    val languageFollowSystem: String
    val languageZh: String
    val languageEn: String

    val connectionParams: String
    val ipAddress: String
    val ipAddressPlaceholder: String
    val portLabel: String
    val secretKey: String
    val showText: String
    val hideText: String
    val pollingInterval: String
    val saveConfig: String
    val configSaved: String

    val networkDiagnostics: String
    val testConnection: String
    val testing: String

    // Direct AI Providers
    val directProvidersTitle: String
    val directProvidersSubtitle: String
    val directProviderApiKey: String
    val directProviderApiKeyPlaceholder: String
    val directProviderTest: String
    val directProviderTesting: String
    val directProviderSave: String
    val directProviderSaved: String
    fun directProviderTestSuccess(msg: String): String
    fun directProviderTestFailed(err: String): String
    val directProviderEnabled: String
    val directProviderStatusConfigured: String
    val directProviderStatusNotConfigured: String
    val directProviderEnableSwitch: String
    val directProviderEnableSwitchDesc: String
    val directProviderBack: String
    fun directProviderHint(id: String): String
    fun directProvidersSummary(enabled: Int, total: Int): String
    fun quotaAlertTitle(provider: String): String
    fun quotaAlertWindowBody(provider: String, window: String, percent: Int): String
    fun quotaAlertBalanceBody(provider: String, amount: String, currency: String): String
    val codexLoginWithChatGPT: String
    val codexLoginPrompt: String
    val codexLoggedIn: String
    val codexReLogin: String
    val codexManualTokenHint: String
    val codexLoggingIn: String
    val codexLoginSuccess: String
    fun codexLoginFailed(err: String): String

    val liveNotification: String
    val notificationPermission: String
    val permissionGranted: String
    val permissionDenied: String
    val backgroundService: String
    val running: String
    val testLiveNotification: String
    val systemNotificationSettings: String
    val liveNotificationSwitch: String
    val liveNotificationSwitchDesc: String
    val islandQuotaAlertCardTitle: String
    val islandQuotaAlertCardDesc: String
    val islandQuotaAlertUsageThreshold: String
    val islandQuotaAlertBalanceThreshold: String
    val testIslandQuotaAlert: String

    val softwareUpdate: String
    val githubOfficial: String
    val currentVersion: String
    val checkUpdate: String
    val checkingUpdate: String
    val upToDate: String
    val newVersionFound: String
    val packageSize: String
    val downloadNow: String
    val downloading: String
    val readyToInstall: String
    val readyToInstallDesc: String
    val installNow: String
    val updateNotice: String

    val about: String
    val version: String
    val openSourceRepo: String
    val renderEngine: String
    val syncProtocol: String
    val openSourceCredits: String
    val openSourceCreditsSubtitle: String
    val viewCredits: String

    // Super Island Studio
    val islandStudioTitle: String
    val islandStudioDesc: String
    val restoreDefaults: String
    val leftSlotBadge: String
    val leftSlotName: String
    val rightSlotBadge: String
    val elementRepository: String
    val elementRepositoryHint: String
    val badgeLeftFixed: String
    val badgeRight: String
    val badgeBoth: String
    val tapToPick: String
    val releaseToPlace: String
    val replaceWithCurrent: String
    val pickRightSlotTitle: String
    val pickRightSlotSubtitle: String
    val currentBadge: String
    val cancel: String
    val aiProviderSelectTitle: String
    val aiQuotaModeTitle: String
    val autoSelectRecommended: String
    val modeAuto: String
    val mode5h: String
    val modeWeekly: String
    val modeBalance: String
    val desc5h: String
    val descWeekly: String
    val descBalance: String
    val descAuto: String

    // OTA Update Dialog
    fun updateDialogTitle(version: String): String
    fun updateDialogCurrent(version: String): String
    fun updateDialogLatest(version: String): String
    fun updateDialogSize(size: String): String
    val updateDialogReleaseNotes: String
    val updateDialogDownload: String
    val updateDialogLater: String
    val updateDialogReadyTitle: String
    fun updateDialogReadyMessage(version: String): String

    // Acknowledgements
    val creditsTitle: String
    val creditsSubtitle: String
    val creditsBack: String
    val creditsIntro: String
    val creditsOpenRepo: String
    fun creditsTotalProjects(count: Int): String

    // Notifications & Toasts
    val toastLiveNotificationSent: String
    fun toastSendFailed(reason: String): String
    fun toastCannotOpenSettings(reason: String): String
    val notifWaitingHub: String
    val notifToday: String
    val notifMonthTotal: String
    val notifCost: String

    // Config labels
    fun labelFor(style: QuotaDisplayStyle): String
    fun labelFor(order: RingOrderConfig): String
    fun labelFor(center: RingCenterTextConfig): String
    fun titleFor(item: IslandItemType): String
    fun sampleFor(item: IslandItemType): String
    fun cleanWindowLabel(original: String, groupTitle: String, fallback: String): String

    // Acknowledgement Items
    fun getCreditProjects(): List<CreditItem>
}

data class CreditItem(
    val name: String,
    val repo: String,
    val url: String,
    val description: String,
    val tags: List<String>
)

object ZhStrings : AppStrings {
    override val isEnglish: Boolean = false

    override val navHome: String = "主页"
    override val navSettings: String = "设置"

    override fun statusConnected(host: String): String = if (host.isNotBlank()) "已连接至 $host" else "已连接至电脑端"
    override val statusRemote: String = "已连接至电脑端"
    override val statusDisconnectedCached: String = "连接已断开 · 已保留离线数据"
    override val statusDisconnected: String = "连接已断开"
    override val statusConnecting: String = "正在连接电脑端 Hub..."
    override fun lastUpdated(time: String): String = "更新于 $time"

    override val statusActive: String = "工作中"
    override val statusIdle: String = "空闲中"
    override val statusOffline: String = "离线"

    override val periodDay: String = "今日"
    override val periodMonth: String = "本月"
    override val periodTotal: String = "全部"
    override val todayTokens: String = "今日 Token"
    override val monthTokens: String = "本月 Token"
    override val totalTokens: String = "全部 Token"
    override fun messageCount(count: Int): String = "$count 请求次数"
    override fun sessionCount(count: Int): String = "$count 会话"
    override val estimatedCost: String = "预估消耗"

    override val remainingQuota: String = "剩余用量"
    override fun providerCount(count: Int): String = "$count 个服务商"
    override fun percentRemaining(percent: Int): String = "$percent% 剩余"
    override fun resetsIn(time: String): String = "重置 $time"
    override fun resetPrefix(time: String): String = "重置: $time"
    override fun outerRing(label: String): String = "外环 · $label"
    override fun innerRing(label: String): String = "内环 · $label"
    override val fiveHours: String = "5小时"
    override val weekly: String = "周用量"
    override val session: String = "会话"
    override val monthly: String = "月"
    override val monthlyLimit: String = "月限额"
    override fun resetCreditsAvailable(count: Int): String = "$count 张用量重置卡可用"
    override val resetCreditsNone: String = "暂无可用重置卡"
    override fun formatResetCreditsExpiry(rawExpiry: String): String = rawExpiry

    override val modelBreakdown: String = "模型排行"
    override fun modelCount(count: Int): String = "共 $count 个模型"
    override val clientBreakdown: String = "客户端用量"
    override fun clientCount(count: Int): String = "共 $count 个客户端"
    override val viewAll: String = "查看全部 ›"
    override fun viewAllModels(count: Int): String = "查看全部 $count 个模型 ›"
    override fun viewAllClients(count: Int): String = "查看全部 $count 个客户端 ›"
    override val allModelsDetail: String = "全部模型用量名单"
    override val allClientsDetail: String = "全部客户端用量名单"
    override val inputTokens: String = "输入 Token"
    override val outputTokens: String = "输出 Token"
    override val cacheReadTokens: String = "缓存读取"
    override val cacheCreationTokens: String = "缓存写入"
    override val close: String = "关闭"
    override val noData: String = "暂无数据"

    override val activity: String = "活动"
    override val activityHint: String = "长按查看当日用量"
    override fun activeDays(days: Int): String = "活跃 $days 天"
    override val noUsage: String = "无消耗"
    override val trend: String = "趋势"
    override fun peak(tokens: String): String = "峰值 $tokens"

    override fun formatDateForActivity(dateStr: String): String {
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfIn.parse(dateStr.take(10)) ?: return dateStr
            val sdfOut = SimpleDateFormat("yyyy年M月d日 E", Locale.CHINESE)
            sdfOut.format(date)
        } catch (_: Exception) {
            dateStr
        }
    }

    override fun formatMonthForActivity(dateStr: String): String {
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfIn.parse(dateStr.take(10)) ?: return ""
            val monthFmt = SimpleDateFormat("M月", Locale.CHINESE)
            monthFmt.format(date)
        } catch (_: Exception) {
            ""
        }
    }

    override val disconnectedCachedError: String = "连接已断开 (已保留离线数据)"
    override val networkAnomalyError: String = "网络通讯异常"
    override val retry: String = "重试"

    override val settingsTitle: String = "设置"
    override val settingsSubtitle: String = "连接参数与实时通知配置"
    override val languageSectionTitle: String = "界面语言"
    override val languageFollowSystem: String = "跟随系统"
    override val languageZh: String = "简体中文"
    override val languageEn: String = "English"

    override val connectionParams: String = "连接参数"
    override val ipAddress: String = "IP 地址"
    override val ipAddressPlaceholder: String = "例如 192.168.1.100"
    override val portLabel: String = "端口 (17321)"
    override val secretKey: String = "密钥 (Secret)"
    override val showText: String = "显示"
    override val hideText: String = "隐藏"
    override val pollingInterval: String = "轮询间隔 (秒)"
    override val saveConfig: String = "保存配置"
    override val configSaved: String = "配置已保存"

    override val networkDiagnostics: String = "网络体检"
    override val testConnection: String = "检查连接"
    override val testing: String = "正在检测..."

    // Direct AI Providers
    override val directProvidersTitle: String = "连接供应商"
    override val directProvidersSubtitle: String = ""
    override val directProviderApiKey: String = "API Key"
    override val directProviderApiKeyPlaceholder: String = "输入平台 API 密钥..."
    override val directProviderTest: String = "测试连接"
    override val directProviderTesting: String = "正在测试连接..."
    override val directProviderSave: String = "保存配置"
    override val directProviderSaved: String = "供应商配置已保存"
    override fun directProviderTestSuccess(msg: String): String = "连通成功: $msg"
    override fun directProviderTestFailed(err: String): String = "连接失败: $err"
    override val directProviderEnabled: String = "已启用"
    override val directProviderStatusConfigured: String = "已配置密钥"
    override val directProviderStatusNotConfigured: String = "未配置密钥"
    override val directProviderEnableSwitch: String = "启用此供应商"
    override val directProviderEnableSwitchDesc: String = "在无电脑端 Hub 时，手机直接向该平台查询实时用量与额度"
    override val directProviderBack: String = "返回"
    override fun directProviderHint(id: String): String = when (id.lowercase()) {
        "deepseek" -> "查询 balance_infos 账户余额 (CNY/USD)"
        "openrouter" -> "查询 credits 余额与当前 Key 限制"
        "minimax" -> "查询 Coding Plan 5小时与周度用量"
        "kimi" -> "查询 Kimi Coding 5小时与周度用量"
        "zai" -> "查询智谱开放平台 5小时与周度用量"
        "codex" -> "查询 ChatGPT 订阅 (Plus/Pro) 5小时与周度用量"
        else -> "查询用量与配额"
    }
    override fun directProvidersSummary(enabled: Int, total: Int): String = "已启用 $enabled / 共 $total 个服务商"
    override fun quotaAlertTitle(provider: String): String = "$provider 用量预警"
    override fun quotaAlertWindowBody(provider: String, window: String, percent: Int): String = "$provider $window 剩余额度仅剩 $percent%，请注意规划使用"
    override fun quotaAlertBalanceBody(provider: String, amount: String, currency: String): String = "$provider 账户余额仅剩 $amount $currency，已达预警阈值，请及时充值"
    override val codexLoginWithChatGPT: String = "一键登录 ChatGPT 账号"
    override val codexLoginPrompt: String = "登录官方 ChatGPT 账号以授权直接查询 Codex 5小时与周度用量"
    override val codexLoggedIn: String = "已授权 ChatGPT 账号"
    override val codexReLogin: String = "重新授权登录"
    override val codexManualTokenHint: String = "高级方式：支持直接粘贴 access_token 或 auth.json"
    override val codexLoggingIn: String = "正在完成授权交换..."
    override val codexLoginSuccess: String = "ChatGPT 账号授权成功！"
    override fun codexLoginFailed(err: String): String = "授权失败: $err"

    override val liveNotification: String = "实时通知"
    override val notificationPermission: String = "通知权限"
    override val permissionGranted: String = "已开启"
    override val permissionDenied: String = "未开启"
    override val backgroundService: String = "后台常驻服务"
    override val running: String = "运行中"
    override val testLiveNotification: String = "测试实时通知"
    override val systemNotificationSettings: String = "系统通知设置"
    override val liveNotificationSwitch: String = "实时通知开关"
    override val liveNotificationSwitchDesc: String = "在状态栏与锁屏持续展示实时用量"
    override val islandQuotaAlertCardTitle: String = "超级岛余量预警"
    override val islandQuotaAlertCardDesc: String = "模型供应商额度或余额不足时以超级岛弹窗提醒"
    override val islandQuotaAlertUsageThreshold: String = "剩余用量预警阈值 (低于)"
    override val islandQuotaAlertBalanceThreshold: String = "账户余额预警阈值 (低于)"
    override val testIslandQuotaAlert: String = "测试超级岛余量提醒"

    override val softwareUpdate: String = "软件更新"
    override val githubOfficial: String = "GitHub 正式版"
    override val currentVersion: String = "当前版本"
    override val checkUpdate: String = "检查更新"
    override val checkingUpdate: String = "检查中…"
    override val upToDate: String = "已是最新"
    override val newVersionFound: String = "发现新版本"
    override val packageSize: String = "大小"
    override val downloadNow: String = "立即下载"
    override val downloading: String = "正在下载"
    override val readyToInstall: String = "安装包已就绪"
    override val readyToInstallDesc: String = "下载完成，点击立即开始安装"
    override val installNow: String = "立即安装"
    override val updateNotice: String = "更新检查提示"

    override val about: String = "关于"
    override val version: String = "版本"
    override val openSourceRepo: String = "开源仓库"
    override val renderEngine: String = "渲染引擎"
    override val syncProtocol: String = "同步协议"
    override val openSourceCredits: String = "开源鸣谢名单"
    override val openSourceCreditsSubtitle: String = "向灵感来源与开源先驱致敬"
    override val viewCredits: String = "查看 ↗"

    override val islandStudioTitle: String = "自定义"
    override val islandStudioDesc: String = "自由编排摄像头两侧的展示内容，支持拖拽放置或点击挑选"
    override val restoreDefaults: String = "恢复默认"
    override val leftSlotBadge: String = "左侧 (固定)"
    override val leftSlotName: String = "运行状态"
    override val rightSlotBadge: String = "右侧 (自定义)"
    override val elementRepository: String = "可用元件仓库"
    override val elementRepositoryHint: String = "按住 ⠿ 拖拽放入，或轻点快速放置"
    override val badgeLeftFixed: String = "左侧 (固定)"
    override val badgeRight: String = "右侧"
    override val badgeBoth: String = "左右"
    override val tapToPick: String = "+ 拖入或点击挑选"
    override val releaseToPlace: String = "松手放入此槽位"
    override val replaceWithCurrent: String = "松手替换为当前"
    override val pickRightSlotTitle: String = "选择放入【右侧区域】的内容"
    override val pickRightSlotSubtitle: String = "右侧在挖孔右侧独立呈现，可自由配置"
    override val currentBadge: String = "当前"
    override val cancel: String = "取消"
    override val aiProviderSelectTitle: String = "AI 厂商选择："
    override val aiQuotaModeTitle: String = "AI 配额计算模式："
    override val autoSelectRecommended: String = "自动选择 (最紧张)"
    override val modeAuto: String = "智能自动"
    override val mode5h: String = "5小时限制"
    override val modeWeekly: String = "周限制"
    override val modeBalance: String = "余额模式"
    override val desc5h: String = "优先匹配 5 小时滚动窗口剩余百分比（适合 Claude/Codex 快速窗口）"
    override val descWeekly: String = "优先匹配每周额度限制剩余百分比（适合周周期重置模型）"
    override val descBalance: String = "优先匹配现金余额（如 $12.50 或 ¥8.37）"
    override val descAuto: String = "智能自动匹配最紧俏的周期额度或现金余额（如 DeepSeek 自动显示 ¥8.37）"

    override fun updateDialogTitle(version: String): String = "发现新版本 $version"
    override fun updateDialogCurrent(version: String): String = "当前版本: $version"
    override fun updateDialogLatest(version: String): String = "最新版本: $version"
    override fun updateDialogSize(size: String): String = "安装包大小: $size"
    override val updateDialogReleaseNotes: String = "更新内容:"
    override val updateDialogDownload: String = "下载并安装"
    override val updateDialogLater: String = "稍后"
    override val updateDialogReadyTitle: String = "安装包准备就绪"
    override fun updateDialogReadyMessage(version: String): String =
        "新版本 $version 安装包已下载完成。点击立即安装，若系统提示阻止，请允许开启「安装未知应用」权限。"

    override val creditsTitle: String = "开源鸣谢"
    override val creditsSubtitle: String = "向灵感来源与开源先驱致敬"
    override val creditsBack: String = "返回"
    override val creditsIntro: String = "Token Monitor Android 的诞生离不开开源社区的智慧与贡献。衷心感谢以下所有开源项目、工具与灵感来源："
    override val creditsOpenRepo: String = "访问仓库 ↗"
    override fun creditsTotalProjects(count: Int): String = "共 $count 个开源项目"

    override val toastLiveNotificationSent: String = "已激活实时监控与通知！请上划回到桌面或查看状态栏"
    override fun toastSendFailed(reason: String): String = "启动失败: $reason"
    override fun toastCannotOpenSettings(reason: String): String = "无法打开系统设置: $reason"
    override val notifWaitingHub: String = "正在等待与电脑端 Hub 建立连接..."
    override val notifToday: String = "今日用量"
    override val notifMonthTotal: String = "本月累计"
    override val notifCost: String = "费用"

    override fun labelFor(style: QuotaDisplayStyle): String = when (style) {
        QuotaDisplayStyle.DUAL_RINGS -> "同心双圆环"
        QuotaDisplayStyle.PROGRESS_BARS -> "进度横条"
    }

    override fun labelFor(order: RingOrderConfig): String = when (order) {
        RingOrderConfig.OUTER_5H_INNER_WEEKLY -> "外圈 5小时 · 内圈 周用量"
        RingOrderConfig.OUTER_WEEKLY_INNER_5H -> "外圈 周用量 · 内圈 5小时"
    }

    override fun labelFor(center: RingCenterTextConfig): String = when (center) {
        RingCenterTextConfig.SESSION_5H -> "5小时用量"
        RingCenterTextConfig.WEEKLY -> "周用量"
    }

    override fun titleFor(item: IslandItemType): String = when (item) {
        IslandItemType.STATUS -> "运行状态"
        IslandItemType.TODAY_TOKENS -> "今日Token"
        IslandItemType.AI_QUOTA -> "AI剩余用量"
        IslandItemType.TODAY_COST -> "今日花费"
        IslandItemType.MONTH_TOKENS -> "本月Token"
        IslandItemType.NONE -> "不显示"
    }

    override fun sampleFor(item: IslandItemType): String = when (item) {
        IslandItemType.STATUS -> "工作中"
        IslandItemType.TODAY_TOKENS -> "70M"
        IslandItemType.AI_QUOTA -> "Claude 82%"
        IslandItemType.TODAY_COST -> "$0.15"
        IslandItemType.MONTH_TOKENS -> "18.5M"
        IslandItemType.NONE -> ""
    }

    override fun cleanWindowLabel(original: String, groupTitle: String, fallback: String): String {
        val clean = original
            .replace(groupTitle, "", ignoreCase = true)
            .replace("Claude", "", ignoreCase = true)
            .replace("Codex", "", ignoreCase = true)
            .trim()
            .trimStart('-', '—', '·', ':')
            .trim()
        if (clean.isEmpty()) return fallback
        val lower = clean.lowercase()
        return when {
            lower.contains("5-hour") || lower.contains("5 hour") || lower.contains("5h") -> if (clean.contains("限额") || lower.contains("limit")) "5小时限额" else "5小时用量"
            lower.contains("session") -> if (clean.contains("限额") || lower.contains("limit")) "会话限额" else "会话用量"
            lower.contains("weekly") || lower.contains("week") -> if (clean.contains("限额") || lower.contains("limit")) "周限额" else "周用量"
            lower.contains("monthly") || lower.contains("month") -> if (clean.contains("限额") || lower.contains("limit")) "月限额" else "月用量"
            else -> clean
        }
    }

    override fun getCreditProjects(): List<CreditItem> = listOf(
        CreditItem(
            name = "Token Monitor Android",
            repo = "hcen229/Token-Monitor-Android",
            url = "https://github.com/hcen229/Token-Monitor-Android",
            description = "Token Monitor 原生 Android 客户端应用，支持与桌面端无缝同步、实时状态栏通知、灵动岛监控与多服务商限额可视化。",
            tags = listOf("本项目", "原生应用", "MIT")
        ),
        CreditItem(
            name = "Token Monitor 桌面端",
            repo = "Javis603/token-monitor",
            url = "https://github.com/Javis603/token-monitor",
            description = "原版桌面端 Token 统计监控工具，提供核心数据同步协议规范、多服务商额度聚合与经典桌面端监控逻辑。",
            tags = listOf("原版核心", "数据协议", "MIT")
        ),
        CreditItem(
            name = "AndroidLiquidGlass",
            repo = "Kyant0/AndroidLiquidGlass",
            url = "https://github.com/Kyant0/AndroidLiquidGlass",
            description = "通过 Gradle 依赖引入的开源项目（Kyant0/AndroidLiquidGlass 及底层 io.github.kyant0:backdrop 引擎），用于驱动 Android 13+ 实时硬件级液态毛玻璃与光学折射悬浮底栏动效。",
            tags = listOf("开源项目", "AGSL引擎", "Apache-2.0")
        ),
        CreditItem(
            name = "JetBrains Mono",
            repo = "JetBrains/JetBrainsMono",
            url = "https://github.com/JetBrains/JetBrainsMono",
            description = "直接内置于 APK 资源目录（res/font/）的开源等宽字体资产，为全应用提供高清晰度的数字对齐与代码阅读质感。",
            tags = listOf("内置字体", "OFL 1.1")
        ),
        CreditItem(
            name = "Tokscale",
            repo = "junhoyeo/tokscale",
            url = "https://github.com/junhoyeo/tokscale",
            description = "极简高效的命令行 Token 追踪分析工具，为桌面端与跨端数据结构提供了上游模型设计与解析原型。",
            tags = listOf("上游原型", "数据解析", "MIT")
        ),
        CreditItem(
            name = "SignalDock",
            repo = "jizizr/signaldock",
            url = "https://github.com/jizizr/signaldock",
            description = "针对 Android 物理打孔屏左右腔体分离、悬浮避让与灵动岛通知交互架构提供设计参考。",
            tags = listOf("技术参考", "MIT")
        ),
        CreditItem(
            name = "Capsulyric",
            repo = "FrancoGiudans/Capsulyric",
            url = "https://github.com/FrancoGiudans/Capsulyric",
            description = "针对国产系统（ColorOS 流体云、HyperOS 焦点通知等）私有状态栏通知 Extras 键名与协议参数提供互操作性技术参考。",
            tags = listOf("技术参考", "GPL-3.0")
        ),
        CreditItem(
            name = "CustomLand",
            repo = "MUKAPP/CustomLand",
            url = "https://github.com/MUKAPP/CustomLand",
            description = "基于 AI 的 Android 截图信息识别与提取工具，支持视觉模型、OCR + 文本模型、灵动岛与 Live Updates 实时通知。",
            tags = listOf("技术参考", "BSD-3-Clause")
        )
    )
}

object EnStrings : AppStrings {
    override val isEnglish: Boolean = true

    override val navHome: String = "Home"
    override val navSettings: String = "Settings"

    override fun statusConnected(host: String): String = "Connected: $host"
    override val statusRemote: String = "Remote"
    override val statusDisconnectedCached: String = "Disconnected · Cached"
    override val statusDisconnected: String = "Disconnected"
    override val statusConnecting: String = "Connecting to Hub..."
    override fun lastUpdated(time: String): String = "at $time"

    override val statusActive: String = "Active"
    override val statusIdle: String = "Idle"
    override val statusOffline: String = "Offline"

    override val periodDay: String = "Day"
    override val periodMonth: String = "Month"
    override val periodTotal: String = "Total"
    override val todayTokens: String = "Today's Tokens"
    override val monthTokens: String = "Month's Tokens"
    override val totalTokens: String = "Total Tokens"
    override fun messageCount(count: Int): String = "$count msgs"
    override fun sessionCount(count: Int): String = "$count sessions"
    override val estimatedCost: String = "Est. Cost"

    override val remainingQuota: String = "Remaining Quota"
    override fun providerCount(count: Int): String = "$count providers"
    override fun percentRemaining(percent: Int): String = "$percent% left"
    override fun resetsIn(time: String): String = "in $time"
    override fun resetPrefix(time: String): String = "Reset: $time"
    override fun outerRing(label: String): String = "Outer · $label"
    override fun innerRing(label: String): String = "Inner · $label"
    override val fiveHours: String = "5-Hour"
    override val weekly: String = "Weekly"
    override val session: String = "Session"
    override val monthly: String = "Monthly"
    override val monthlyLimit: String = "Monthly Limit"
    override fun resetCreditsAvailable(count: Int): String =
        if (count == 1) "1 Reset Credit available" else "$count Reset Credits available"
    override val resetCreditsNone: String = "No Reset Credits available"
    override fun formatResetCreditsExpiry(rawExpiry: String): String {
        val monthMap = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        return rawExpiry
            .replace(Regex("(\\d+)月(\\d+)日\\s*到期\\s*\\(剩\\s*(\\d+)\\s*天\\)")) {
                val m = it.groupValues[1].toIntOrNull() ?: 1
                val d = it.groupValues[2]
                val days = it.groupValues[3]
                val mon = monthMap.getOrElse(m - 1) { "$m" }
                val dayStr = if (days == "1") "1 day left" else "$days days left"
                "Expires $mon $d ($dayStr)"
            }
            .replace(Regex("(\\d+)月(\\d+)日\\s*到期\\s*\\(今天到期\\)")) {
                val m = it.groupValues[1].toIntOrNull() ?: 1
                val d = it.groupValues[2]
                val mon = monthMap.getOrElse(m - 1) { "$m" }
                "Expires $mon $d (today)"
            }
            .replace(Regex("(\\d+)月(\\d+)日\\s*\\(已到期\\)")) {
                val m = it.groupValues[1].toIntOrNull() ?: 1
                val d = it.groupValues[2]
                val mon = monthMap.getOrElse(m - 1) { "$m" }
                "Expired $mon $d"
            }
    }

    override val modelBreakdown: String = "Models"
    override fun modelCount(count: Int): String = "$count models"
    override val clientBreakdown: String = "Clients"
    override fun clientCount(count: Int): String = "$count clients"
    override val viewAll: String = "View All ›"
    override fun viewAllModels(count: Int): String = "View all $count models ›"
    override fun viewAllClients(count: Int): String = "View all $count clients ›"
    override val allModelsDetail: String = "All Models Usage Breakdown"
    override val allClientsDetail: String = "All Clients Usage Breakdown"
    override val inputTokens: String = "Input Tokens"
    override val outputTokens: String = "Output Tokens"
    override val cacheReadTokens: String = "Cache Read"
    override val cacheCreationTokens: String = "Cache Creation"
    override val close: String = "Close"
    override val noData: String = "No Data"

    override val activity: String = "Activity"
    override val activityHint: String = "Hold to view"
    override fun activeDays(days: Int): String = "Active: ${days}d"
    override val noUsage: String = "No usage"
    override val trend: String = "Trend"
    override fun peak(tokens: String): String = "Peak: $tokens"

    override fun formatDateForActivity(dateStr: String): String {
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfIn.parse(dateStr.take(10)) ?: return dateStr
            val sdfOut = SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH)
            sdfOut.format(date)
        } catch (_: Exception) {
            dateStr
        }
    }

    override fun formatMonthForActivity(dateStr: String): String {
        return try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfIn.parse(dateStr.take(10)) ?: return ""
            val monthFmt = SimpleDateFormat("MMM", Locale.ENGLISH)
            monthFmt.format(date)
        } catch (_: Exception) {
            ""
        }
    }

    override val disconnectedCachedError: String = "Disconnected (Cached Data)"
    override val networkAnomalyError: String = "Network Connection Error"
    override val retry: String = "Retry"

    override val settingsTitle: String = "Settings"
    override val settingsSubtitle: String = "Connection parameters & live notifications"
    override val languageSectionTitle: String = "Display Language"
    override val languageFollowSystem: String = "System Default"
    override val languageZh: String = "简体中文"
    override val languageEn: String = "English"

    override val connectionParams: String = "Connection Parameters"
    override val ipAddress: String = "IP Address"
    override val ipAddressPlaceholder: String = "e.g. 192.168.1.100"
    override val portLabel: String = "Port (17321)"
    override val secretKey: String = "Secret Key"
    override val showText: String = "Show"
    override val hideText: String = "Hide"
    override val pollingInterval: String = "Polling Interval (s)"
    override val saveConfig: String = "Save Configuration"
    override val configSaved: String = "Configuration saved"

    override val networkDiagnostics: String = "Network Diagnostics"
    override val testConnection: String = "Test Connection"
    override val testing: String = "Testing..."

    // Direct AI Providers
    override val directProvidersTitle: String = "Connect Providers"
    override val directProvidersSubtitle: String = ""
    override val directProviderApiKey: String = "API Key"
    override val directProviderApiKeyPlaceholder: String = "Enter platform API Key..."
    override val directProviderTest: String = "Test Connection"
    override val directProviderTesting: String = "Testing..."
    override val directProviderSave: String = "Save Configuration"
    override val directProviderSaved: String = "Provider configuration saved"
    override fun directProviderTestSuccess(msg: String): String {
        val monthMap = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val translated = msg
            .replace("连通成功 · 状态正常", "Connected · Status OK")
            .replace("余额", "Balance")
            .replace("月限额剩", "Monthly ")
            .replace("月用量剩", "Monthly ")
            .replace("周用量剩", "Weekly ")
            .replace("周限额剩", "Weekly ")
            .replace("5小时剩", "5-Hour ")
            .replace("5小时限额剩", "5-Hour ")
            .replace("5小时用量剩", "5-Hour ")
            .replace("会话剩", "Session ")
            .replace("剩 ", " ")
            .replace("暂无可用重置卡", "No reset credits")
            .replace(Regex("(\\d+)\\s*张重置卡可用")) { "${it.groupValues[1]} Reset Credits available" }
            .replace(Regex("(\\d+)\\s*张重置卡")) { "${it.groupValues[1]} Reset Credits" }
            .replace(Regex("(\\d+)月(\\d+)日\\s*到期\\s*\\(剩\\s*(\\d+)\\s*天\\)")) {
                val m = it.groupValues[1].toIntOrNull() ?: 1
                val d = it.groupValues[2]
                val days = it.groupValues[3]
                val mon = monthMap.getOrElse(m - 1) { "$m" }
                val dayStr = if (days == "1") "1 day left" else "$days days left"
                "Expires $mon $d ($dayStr)"
            }
            .replace(Regex("(\\d+)月(\\d+)日\\s*到期\\s*\\(今天到期\\)")) {
                val m = it.groupValues[1].toIntOrNull() ?: 1
                val d = it.groupValues[2]
                val mon = monthMap.getOrElse(m - 1) { "$m" }
                "Expires $mon $d (today)"
            }
            .replace(Regex("(\\d+)月(\\d+)日\\s*\\(已到期\\)")) {
                val m = it.groupValues[1].toIntOrNull() ?: 1
                val d = it.groupValues[2]
                val mon = monthMap.getOrElse(m - 1) { "$m" }
                "Expired $mon $d"
            }
        return "Connected: $translated"
    }
    override fun directProviderTestFailed(err: String): String = "Failed: $err"
    override val directProviderEnabled: String = "Enabled"
    override val directProviderStatusConfigured: String = "Key Configured"
    override val directProviderStatusNotConfigured: String = "Not Configured"
    override val directProviderEnableSwitch: String = "Enable this provider"
    override val directProviderEnableSwitchDesc: String = "Directly query this provider's quota & balance on phone"
    override val directProviderBack: String = "Back"
    override fun directProviderHint(id: String): String = when (id.lowercase()) {
        "deepseek" -> "Queries balance_infos (CNY/USD)"
        "openrouter" -> "Queries credits & current Key limits"
        "minimax" -> "Queries Coding Plan 5h & weekly usage"
        "kimi" -> "Queries Kimi Coding 5h & weekly usage"
        "zai" -> "Queries GLM/BigModel 5h & weekly usage"
        "codex" -> "Queries ChatGPT subscription, quota & reset credits"
        else -> "Queries usage & quota"
    }
    override fun directProvidersSummary(enabled: Int, total: Int): String = "$enabled of $total enabled"
    override fun quotaAlertTitle(provider: String): String = "$provider Quota Alert"
    override fun quotaAlertWindowBody(provider: String, window: String, percent: Int): String {
        val win = cleanWindowLabel(window, "", window)
        return "$provider $win remaining quota is down to $percent%. Please plan your usage."
    }
    override fun quotaAlertBalanceBody(provider: String, amount: String, currency: String): String = "$provider balance is down to $amount $currency. Please recharge."
    override val codexLoginWithChatGPT: String = "Log in with ChatGPT"
    override val codexLoginPrompt: String = "Log in with ChatGPT account to query Codex quota & reset credits"
    override val codexLoggedIn: String = "ChatGPT Account Authorized"
    override val codexReLogin: String = "Re-authorize"
    override val codexManualTokenHint: String = "Advanced: Or paste access_token / auth.json directly"
    override val codexLoggingIn: String = "Completing authorization..."
    override val codexLoginSuccess: String = "ChatGPT account authorized successfully!"
    override fun codexLoginFailed(err: String): String = "Authorization failed: $err"

    override val liveNotification: String = "Live Notification"
    override val notificationPermission: String = "Notification Permission"
    override val permissionGranted: String = "Granted"
    override val permissionDenied: String = "Disabled"
    override val backgroundService: String = "Background Service"
    override val running: String = "Running"
    override val testLiveNotification: String = "Test Alert"
    override val systemNotificationSettings: String = "System Settings"
    override val liveNotificationSwitch: String = "Live Notification Switch"
    override val liveNotificationSwitchDesc: String = "Continuously display usage in status bar & lock screen"
    override val islandQuotaAlertCardTitle: String = "Super Island Quota Alert"
    override val islandQuotaAlertCardDesc: String = "Pop up Super Island banner when provider quota or balance is low"
    override val islandQuotaAlertUsageThreshold: String = "Quota Alert Threshold (Below)"
    override val islandQuotaAlertBalanceThreshold: String = "Balance Alert Threshold (Below)"
    override val testIslandQuotaAlert: String = "Test Island Quota Alert"

    override val softwareUpdate: String = "Software Update"
    override val githubOfficial: String = "GitHub Official"
    override val currentVersion: String = "Current Version"
    override val checkUpdate: String = "Check for Updates"
    override val checkingUpdate: String = "Checking…"
    override val upToDate: String = "Up to date"
    override val newVersionFound: String = "New Version Available"
    override val packageSize: String = "Size"
    override val downloadNow: String = "Download Now"
    override val downloading: String = "Downloading"
    override val readyToInstall: String = "Ready to Install"
    override val readyToInstallDesc: String = "Download complete. Tap to install"
    override val installNow: String = "Install Now"
    override val updateNotice: String = "Update Notice"

    override val about: String = "About"
    override val version: String = "Version"
    override val openSourceRepo: String = "Repository"
    override val renderEngine: String = "Render Engine"
    override val syncProtocol: String = "Sync Protocol"
    override val openSourceCredits: String = "Acknowledgements"
    override val openSourceCreditsSubtitle: String = "Honoring inspirations & open source"
    override val viewCredits: String = "View ↗"

    override val islandStudioTitle: String = "Super Island Studio"
    override val islandStudioDesc: String = "Customize punch-hole camera slot display with drag-and-drop or tap"
    override val restoreDefaults: String = "Restore Defaults"
    override val leftSlotBadge: String = "Left (Fixed)"
    override val leftSlotName: String = "Live Status"
    override val rightSlotBadge: String = "Right (Custom)"
    override val elementRepository: String = "Element Repository"
    override val elementRepositoryHint: String = "Hold ⠿ to drag into slot, or tap to place"
    override val badgeLeftFixed: String = "Left (Fixed)"
    override val badgeRight: String = "Right"
    override val badgeBoth: String = "Both"
    override val tapToPick: String = "+ Pick"
    override val releaseToPlace: String = "Release to place here"
    override val replaceWithCurrent: String = "Replace with current"
    override val pickRightSlotTitle: String = "Select Right Slot Element"
    override val pickRightSlotSubtitle: String = "Tap an item below to display on the right slot:"
    override val currentBadge: String = "Selected"
    override val cancel: String = "Cancel"
    override val aiProviderSelectTitle: String = "AI Provider Selection:"
    override val aiQuotaModeTitle: String = "AI Quota Mode:"
    override val autoSelectRecommended: String = "Auto Select (Recommended)"
    override val modeAuto: String = "Auto Mode"
    override val mode5h: String = "5-Hour Cycle"
    override val modeWeekly: String = "Weekly Cycle"
    override val modeBalance: String = "Balance Mode"
    override val desc5h: String = "Matches 5-hour reset remaining percentage (ideal for Claude/Codex)"
    override val descWeekly: String = "Matches weekly reset remaining percentage (ideal for most models)"
    override val descBalance: String = "Matches account available balance (e.g. $12.50 or ¥8.37)"
    override val descAuto: String = "Automatically matches provider reset cycle or account balance"

    override fun updateDialogTitle(version: String): String = "New Version Available: $version"
    override fun updateDialogCurrent(version: String): String = "Current: $version"
    override fun updateDialogLatest(version: String): String = "Latest: $version"
    override fun updateDialogSize(size: String): String = "Package size: $size"
    override val updateDialogReleaseNotes: String = "Release Notes:"
    override val updateDialogDownload: String = "Download & Install"
    override val updateDialogLater: String = "Later"
    override val updateDialogReadyTitle: String = "Package Ready to Install"
    override fun updateDialogReadyMessage(version: String): String =
        "New version $version has finished downloading. Tap Install Now. If prompted, please allow 'Install unknown apps' permission."

    override val creditsTitle: String = "Acknowledgements"
    override val creditsSubtitle: String = "Honoring open-source projects and pioneers"
    override val creditsBack: String = "Back"
    override val creditsIntro: String = "Token Monitor Android could not exist without the open-source community. Sincere thanks to the following projects:"
    override val creditsOpenRepo: String = "View Repo ↗"
    override fun creditsTotalProjects(count: Int): String = "Total: $count open-source projects"

    override val toastLiveNotificationSent: String = "Live notification posted. Swipe up to home screen to view status bar."
    override fun toastSendFailed(reason: String): String = "Failed to send: $reason"
    override fun toastCannotOpenSettings(reason: String): String = "Cannot open system settings: $reason"
    override val notifWaitingHub: String = "Waiting for remote Hub data sync..."
    override val notifToday: String = "Today"
    override val notifMonthTotal: String = "Month Total"
    override val notifCost: String = "Cost"

    override fun labelFor(style: QuotaDisplayStyle): String = when (style) {
        QuotaDisplayStyle.DUAL_RINGS -> "Concentric Dual Rings"
        QuotaDisplayStyle.PROGRESS_BARS -> "Progress Bars"
    }

    override fun labelFor(order: RingOrderConfig): String = when (order) {
        RingOrderConfig.OUTER_5H_INNER_WEEKLY -> "Outer: 5h · Inner: Weekly"
        RingOrderConfig.OUTER_WEEKLY_INNER_5H -> "Outer: Weekly · Inner: 5h"
    }

    override fun labelFor(center: RingCenterTextConfig): String = when (center) {
        RingCenterTextConfig.SESSION_5H -> "5-Hour Quota"
        RingCenterTextConfig.WEEKLY -> "Weekly Quota"
    }

    override fun titleFor(item: IslandItemType): String = when (item) {
        IslandItemType.STATUS -> "Live Status"
        IslandItemType.TODAY_TOKENS -> "Today's Tokens"
        IslandItemType.AI_QUOTA -> "AI Quotas"
        IslandItemType.TODAY_COST -> "Today's Cost"
        IslandItemType.MONTH_TOKENS -> "Month's Tokens"
        IslandItemType.NONE -> "Hidden"
    }

    override fun sampleFor(item: IslandItemType): String = when (item) {
        IslandItemType.STATUS -> "Active"
        IslandItemType.TODAY_TOKENS -> "70M"
        IslandItemType.AI_QUOTA -> "Claude 82%"
        IslandItemType.TODAY_COST -> "$0.15"
        IslandItemType.MONTH_TOKENS -> "18.5M"
        IslandItemType.NONE -> ""
    }

    override fun cleanWindowLabel(original: String, groupTitle: String, fallback: String): String {
        val clean = original
            .replace(groupTitle, "", ignoreCase = true)
            .replace("Claude", "", ignoreCase = true)
            .replace("Codex", "", ignoreCase = true)
            .trim()
            .trimStart('-', '—', '·', ':')
            .trim()
        if (clean.isEmpty()) return fallback
        val lower = clean.lowercase()
        return when {
            lower.contains("5-hour") || lower.contains("5 hour") || lower.contains("5h") || lower.contains("5小时") ->
                if (clean.contains("限额") || lower.contains("limit")) "5-Hour Limit" else "5-Hour"
            lower.contains("session") || lower.contains("会话") ->
                if (clean.contains("限额") || lower.contains("limit")) "Session Limit" else "Session"
            lower.contains("weekly") || lower.contains("week") || lower.contains("周") ->
                if (clean.contains("限额") || lower.contains("limit")) "Weekly Limit" else "Weekly"
            lower.contains("monthly") || lower.contains("month") || lower.contains("月") ->
                if (clean.contains("限额") || lower.contains("limit")) "Monthly Limit" else "Monthly"
            else -> clean
        }
    }

    override fun getCreditProjects(): List<CreditItem> = listOf(
        CreditItem(
            name = "Token Monitor Android",
            repo = "hcen229/Token-Monitor-Android",
            url = "https://github.com/hcen229/Token-Monitor-Android",
            description = "Native Android client for Token Monitor, supporting real-time desktop sync, status bar notifications, Dynamic Island, and multi-provider quota tracking.",
            tags = listOf("This Project", "Native App", "MIT")
        ),
        CreditItem(
            name = "Token Monitor Desktop",
            repo = "Javis603/token-monitor",
            url = "https://github.com/Javis603/token-monitor",
            description = "The original desktop Token Monitor tool, providing core sync protocol specs, multi-provider quota aggregation, and classic desktop monitoring.",
            tags = listOf("Original Core", "Data Protocol", "MIT")
        ),
        CreditItem(
            name = "AndroidLiquidGlass",
            repo = "Kyant0/AndroidLiquidGlass",
            url = "https://github.com/Kyant0/AndroidLiquidGlass",
            description = "Open-source project (Kyant0/AndroidLiquidGlass and io.github.kyant0:backdrop) powering Android 13+ real-time hardware liquid glass and optical refraction floating bottom tab bar.",
            tags = listOf("Open Source", "AGSL Engine", "Apache-2.0")
        ),
        CreditItem(
            name = "JetBrains Mono",
            repo = "JetBrains/JetBrainsMono",
            url = "https://github.com/JetBrains/JetBrainsMono",
            description = "Open-source monospace font bundled in res/font/, providing clean tabular figures and code readability throughout the app.",
            tags = listOf("Bundled Font", "OFL 1.1")
        ),
        CreditItem(
            name = "Tokscale",
            repo = "junhoyeo/tokscale",
            url = "https://github.com/junhoyeo/tokscale",
            description = "Minimalist CLI token tracking tool providing upstream data model design and parsing prototypes.",
            tags = listOf("Upstream Model", "Data Parsing", "MIT")
        ),
        CreditItem(
            name = "SignalDock",
            repo = "jizizr/signaldock",
            url = "https://github.com/jizizr/signaldock",
            description = "Design inspiration for Android punch-hole camera separation, floating overlay, and island notification interactions.",
            tags = listOf("Reference", "MIT")
        ),
        CreditItem(
            name = "Capsulyric",
            repo = "FrancoGiudans/Capsulyric",
            url = "https://github.com/FrancoGiudans/Capsulyric",
            description = "Technical reference for OEM status bar notifications (ColorOS Fluid Cloud, HyperOS Focus Notification) Extras parameters and interoperability.",
            tags = listOf("Reference", "GPL-3.0")
        ),
        CreditItem(
            name = "CustomLand",
            repo = "MUKAPP/CustomLand",
            url = "https://github.com/MUKAPP/CustomLand",
            description = "AI-powered Android screenshot information extraction tool supporting vision models, OCR + text models, Dynamic Island, and Live Updates.",
            tags = listOf("Reference", "BSD-3-Clause")
        )
    )
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { ZhStrings }

@Composable
fun rememberAppStrings(language: AppLanguage): AppStrings {
    val configuration = LocalConfiguration.current
    return remember(language, configuration) {
        val isZh = when (language) {
            AppLanguage.ZH -> true
            AppLanguage.EN -> false
            AppLanguage.SYSTEM -> {
                val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    configuration.locales.get(0) ?: Locale.getDefault()
                } else {
                    @Suppress("DEPRECATION")
                    configuration.locale ?: Locale.getDefault()
                }
                locale.language.startsWith("zh", ignoreCase = true)
            }
        }
        if (isZh) ZhStrings else EnStrings
    }
}

fun getAppStrings(context: Context): AppStrings {
    val repo = TokenRepository(context)
    val lang = repo.getLanguage()
    val isZh = when (lang) {
        AppLanguage.ZH -> true
        AppLanguage.EN -> false
        AppLanguage.SYSTEM -> {
            val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales.get(0) ?: Locale.getDefault()
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale ?: Locale.getDefault()
            }
            locale.language.startsWith("zh", ignoreCase = true)
        }
    }
    return if (isZh) ZhStrings else EnStrings
}
