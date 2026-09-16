package com.tokenmonitor.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.tokenmonitor.app.MainActivity
import com.tokenmonitor.app.R
import com.tokenmonitor.app.data.IslandConfig
import com.tokenmonitor.app.data.IslandItemType
import com.tokenmonitor.app.data.ProviderLimit
import com.tokenmonitor.app.data.TokenRepository
import com.tokenmonitor.app.data.TokenStats
import com.tokenmonitor.app.ui.i18n.getAppStrings
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TokenNotificationManager {

    private val alertScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var alertRevertJob: Job? = null

    // Stable channel IDs
    const val CHANNEL_ID_FOCUS = "token_monitor_live_channel"
    const val CHANNEL_ID_SERVICE = "token_monitor_service_channel"
    const val CHANNEL_ID_QUOTA_ALERT = "token_monitor_quota_alert"
    const val NOTIFICATION_ID = 10086
    private const val ALERT_NOTIFICATION_BASE_ID = 20000

    private const val ALERT_PREFS_NAME = "token_monitor_quota_alerts"
    private const val STAGE_THRESHOLD = "threshold"
    private const val STAGE_ZERO = "zero"

    private fun getAlertStage(context: Context, key: String): String? {
        return context.getSharedPreferences(ALERT_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, null)
    }

    private fun setAlertStage(context: Context, key: String, stage: String) {
        context.getSharedPreferences(ALERT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, stage)
            .apply()
    }

    private fun clearAlertStage(context: Context, key: String) {
        context.getSharedPreferences(ALERT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(key)
            .apply()
    }

    fun resetAllAlertStages(context: Context) {
        context.getSharedPreferences(ALERT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private const val WORKING_THRESHOLD_MS = 120_000L // 2 minutes (120 seconds)

    private var lastTotalTokens: Long = 0L
    private var lastTokenIncreaseTime: Long = 0L
    var isMonitoringActive: Boolean = false
    @Volatile
    var cachedStats: TokenStats? = null
        private set
    @Volatile
    var cachedConnected: Boolean = false
        private set
    private var cachedIslandConfig: IslandConfig? = null
    private var legacyChannelsCleaned: Boolean = false

    @Volatile
    private var lastPostedFingerprint: String? = null
    @Volatile
    private var lastPostedTime: Long = 0L

    @Volatile
    private var lastCapsuleKey: String? = null
    @Volatile
    private var cachedCapsuleIcon: Icon? = null
    private val ringBitmapCache = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()

    fun isXiaomiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase(Locale.ROOT)
        val brand = Build.BRAND.orEmpty().lowercase(Locale.ROOT)
        return manufacturer.contains("xiaomi") || manufacturer.contains("redmi") ||
               brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")
    }

    fun isColorOsDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase(Locale.ROOT)
        val brand = Build.BRAND.orEmpty().lowercase(Locale.ROOT)
        return manufacturer.contains("oppo") || manufacturer.contains("oneplus") || manufacturer.contains("realme") ||
               brand.contains("oppo") || brand.contains("oneplus") || brand.contains("realme")
    }

    fun init(context: Context) {
        ensureChannels(context)
        getIslandConfig(context)
        if (cachedStats == null) {
            cachedStats = TokenRepository(context).getCachedStats()
        }
    }

    fun getIslandConfig(context: Context): IslandConfig {
        if (cachedIslandConfig == null) {
            cachedIslandConfig = TokenRepository(context).getIslandConfig()
        }
        return cachedIslandConfig ?: IslandConfig.DEFAULT
    }

    fun updateIslandConfig(context: Context, config: IslandConfig) {
        val oldConfig = cachedIslandConfig
        if (oldConfig != null && (
            oldConfig.quotaAlertThresholdPercent != config.quotaAlertThresholdPercent ||
            oldConfig.balanceAlertThresholdCny != config.balanceAlertThresholdCny ||
            oldConfig.quotaAlertEnabled != config.quotaAlertEnabled
        )) {
            resetAllAlertStages(context)
        }
        cachedIslandConfig = config
        TokenRepository(context).saveIslandConfig(config)
        if (isMonitoringActive) {
            postNotification(context, force = true)
        }
    }

    fun ensureChannels(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

                // One-time cleanup of obsolete legacy channels
                if (!legacyChannelsCleaned) {
                    legacyChannelsCleaned = true
                    val legacyChannels = listOf(
                        "token_focus_capsule_v1",
                        "token_sync_service_v1",
                        "token_monitor_focus_island",
                        "token_monitor_service",
                        "token_monitor_live_service",
                        "token_monitor_live_island_v3"
                    )
                    for (c in legacyChannels) {
                        try { nm.deleteNotificationChannel(c) } catch (_: Throwable) {}
                    }
                    try { nm.deleteNotificationChannelGroup("token_monitor_group") } catch (_: Throwable) {}
                }

                val strings = getAppStrings(context)
                // 1. Primary Live Notification Channel (AOSP standard + Xiaomi Island compatible)
                val focusChannel = NotificationChannel(
                    CHANNEL_ID_FOCUS,
                    if (strings.isEnglish) "Token Live Monitor (Status Bar & Notification)" else "Token 实时监控 (状态栏与实时通知)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = if (strings.isEnglish) "Real-time Token usage and status in status bar, notification center, and Dynamic Island" else "实时在手机状态栏、通知中心与灵动岛/胶囊展示 Token 用量与工作状态"
                    setShowBadge(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    enableVibration(false)
                    enableLights(false)
                    setSound(null, null)
                }
                nm.createNotificationChannel(focusChannel)

                // 2. Secondary Sync Service Channel (IMPORTANCE_LOW)
                val existingService = nm.getNotificationChannel(CHANNEL_ID_SERVICE)
                if (existingService == null) {
                    val serviceChannel = NotificationChannel(
                        CHANNEL_ID_SERVICE,
                        if (strings.isEnglish) "Token Background Sync Service" else "Token 后台同步服务",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = if (strings.isEnglish) "Keeps background real-time data sync and stable Hub connection" else "保持后台实时数据同步与 Hub 稳定连接"
                        setShowBadge(false)
                        lockscreenVisibility = Notification.VISIBILITY_SECRET
                        enableVibration(false)
                        enableLights(false)
                        setSound(null, null)
                    }
                    nm.createNotificationChannel(serviceChannel)
                }

                // 3. Quota & Low Balance Alert Channel (IMPORTANCE_HIGH)
                val existingAlert = nm.getNotificationChannel(CHANNEL_ID_QUOTA_ALERT)
                if (existingAlert == null) {
                    val alertChannel = NotificationChannel(
                        CHANNEL_ID_QUOTA_ALERT,
                        if (strings.isEnglish) "Quota & Balance Alerts" else "配额与余额预警",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = if (strings.isEnglish) "Alerts when quota is below 20% or balance is below 2.00 CNY" else "当供应商剩余配额不足 20% 或余额不足 2.00 CNY 时发出系统预警通知"
                        setShowBadge(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        enableVibration(true)
                        enableLights(true)
                    }
                    nm.createNotificationChannel(alertChannel)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("TokenNotification", "ensureChannels failed", e)
        }
    }

    fun isChannelRegistered(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
            nm.getNotificationChannel(CHANNEL_ID_FOCUS) != null
        } else {
            true
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Starts live monitoring: registers channels, posts notification immediately,
     * and activates TokenMonitorService as a foreground service.
     */
    fun startMonitoring(context: Context) {
        try {
            ensureChannels(context)
            isMonitoringActive = true
            TokenMonitorService.start(context)
            postNotification(context)
        } catch (e: Throwable) {
            android.util.Log.e("TokenNotification", "startMonitoring failed", e)
        }
    }

    fun stopMonitoring(context: Context) {
        try {
            isMonitoringActive = false
            TokenMonitorService.stop(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID)
        } catch (e: Throwable) {
            android.util.Log.e("TokenNotification", "stopMonitoring failed", e)
        }
    }

    fun updateStats(context: Context, stats: TokenStats?, isConnected: Boolean, force: Boolean = false) {
        try {
            if (stats != null) {
                cachedStats = stats
            } else if (cachedStats == null) {
                cachedStats = TokenRepository(context).getCachedStats()
            }
            cachedConnected = isConnected

            if (stats != null) {
                val currentTokens = stats.today.totalTokens
                if (lastTotalTokens > 0L && currentTokens > lastTotalTokens) {
                    lastTokenIncreaseTime = System.currentTimeMillis()
                }
                lastTotalTokens = currentTokens

                // Check and post system quota & low-balance alerts
                checkAndPostQuotaAlerts(context, stats)
            }

            if (isMonitoringActive) {
                // While a 15-second alert is active, don't let routine 1s background polling
                // re-post notifications which would disrupt the island expand animation or countdown
                if (activeQuotaAlert != null) {
                    return
                }
                postNotification(context, force = force)
            }
        } catch (e: Throwable) {
            android.util.Log.e("TokenNotification", "updateStats failed", e)
        }
    }

    data class QuotaAlertState(
        val providerName: String,
        val providerIconRes: Int,
        val message: String,
        val triggerTime: Long = System.currentTimeMillis(),
        val durationMs: Long = 15_000L
    )

    @Volatile
    private var activeQuotaAlert: QuotaAlertState? = null

    fun getActiveQuotaAlert(): QuotaAlertState? {
        val alert = activeQuotaAlert ?: return null
        return if (System.currentTimeMillis() - alert.triggerTime < alert.durationMs) alert else null
    }

    fun triggerQuotaAlert(
        context: Context,
        providerName: String,
        providerIconRes: Int,
        alertMessage: String,
        durationMs: Long = 15_000L
    ) {
        val triggerTime = System.currentTimeMillis()
        activeQuotaAlert = QuotaAlertState(
            providerName = providerName,
            providerIconRes = providerIconRes,
            message = alertMessage,
            triggerTime = triggerTime,
            durationMs = durationMs
        )
        if (!isMonitoringActive) {
            startMonitoring(context)
        }
        postNotification(context, force = true)

        alertRevertJob?.cancel()
        alertRevertJob = alertScope.launch {
            delay(durationMs)
            if (activeQuotaAlert?.triggerTime == triggerTime) {
                activeQuotaAlert = null
                postNotification(context, force = true)
            }
        }
    }

    fun clearQuotaAlert(context: Context) {
        alertRevertJob?.cancel()
        alertRevertJob = null
        if (activeQuotaAlert != null) {
            activeQuotaAlert = null
            if (isMonitoringActive) {
                postNotification(context, force = true)
            }
        }
    }

    fun checkAndPostQuotaAlerts(context: Context, stats: TokenStats) {
        if (!isPermissionGranted(context) || !areNotificationsEnabled(context)) return
        val islandConfig = getIslandConfig(context)
        if (!islandConfig.quotaAlertEnabled) return
        if (activeQuotaAlert != null) return

        ensureChannels(context)
        val strings = getAppStrings(context)

        val quotaThreshold = islandConfig.quotaAlertThresholdPercent.toDouble()
        val balanceThreshold = islandConfig.balanceAlertThresholdCny

        for (provider in stats.providers) {
            val providerName = provider.provider.replaceFirstChar { it.uppercase() }
            val providerKey = provider.provider.lowercase().trim()
            val iconRes = resolveProviderIconRes(provider.provider)

            // 1. Check window quota limits (Aggregate per provider to avoid multi-window collision)
            val meterWindows = provider.windows.filter { it.showMeter && !it.kind.equals("billing", ignoreCase = true) }
            if (meterWindows.isNotEmpty()) {
                val minPercent = meterWindows.minOf { it.remainingPercent }
                val alertKey = "${providerKey}_quota"
                val currentStage = getAlertStage(context, alertKey)

                if (minPercent <= 0.0) {
                    // Stage 2: 归零 -> 仅提醒一次
                    if (currentStage != STAGE_ZERO) {
                        setAlertStage(context, alertKey, STAGE_ZERO)
                        val alertMsg = if (strings.isEnglish) {
                            "$providerName quota is depleted (0%), please plan usage or recharge."
                        } else {
                            "$providerName 剩余用量已耗尽 (0%)，请合理规划使用或充值。"
                        }
                        triggerQuotaAlert(context, providerName, iconRes, alertMsg)
                        return
                    }
                } else if (minPercent <= quotaThreshold) {
                    // Stage 1: 到阈值 -> 仅提醒一次
                    if (currentStage == null) {
                        setAlertStage(context, alertKey, STAGE_THRESHOLD)
                        val alertMsg = if (strings.isEnglish) {
                            "$providerName quota is low (${minPercent.toInt()}%), please plan usage or recharge."
                        } else {
                            "$providerName 剩余用量不足 (${minPercent.toInt()}%)，请合理规划使用或充值。"
                        }
                        triggerQuotaAlert(context, providerName, iconRes, alertMsg)
                        return
                    }
                } else if (minPercent > (quotaThreshold + 5.0)) {
                    // 只有当该供应商的所有计量窗口最低剩余都恢复到阈值以上时，才重置状态
                    if (currentStage != null) {
                        clearAlertStage(context, alertKey)
                    }
                }
            }

            // 2. Check pay-as-you-go balance
            if (provider.balanceAmount != null) {
                val amount = provider.balanceAmount
                val curr = provider.balanceCurrency.orEmpty().uppercase()
                val isZeroBalance = amount <= 0.0
                val isLowBalance = when (curr) {
                    "USD" -> amount <= (balanceThreshold * 0.15)
                    else -> amount <= balanceThreshold
                }

                val alertKey = "${providerKey}_balance"
                val currentStage = getAlertStage(context, alertKey)
                val currSymbol = if (curr == "USD") "$" else "¥"
                val formattedAmount = String.format(Locale.US, "%.2f", amount)

                if (isZeroBalance) {
                    // Stage 2: 归零 -> 仅提醒一次
                    if (currentStage != STAGE_ZERO) {
                        setAlertStage(context, alertKey, STAGE_ZERO)
                        val alertMsg = if (strings.isEnglish) {
                            "$providerName balance is depleted ($currSymbol$formattedAmount), please recharge to continue."
                        } else {
                            "$providerName 余额已耗尽 ($currSymbol$formattedAmount)，请充值后继续使用。"
                        }
                        triggerQuotaAlert(context, providerName, iconRes, alertMsg)
                        return
                    }
                } else if (isLowBalance) {
                    // Stage 1: 到阈值 -> 仅提醒一次
                    if (currentStage == null) {
                        setAlertStage(context, alertKey, STAGE_THRESHOLD)
                        val alertMsg = if (strings.isEnglish) {
                            "$providerName balance is low ($currSymbol$formattedAmount), please plan usage or recharge."
                        } else {
                            "$providerName 余额不足 ($currSymbol$formattedAmount)，请合理规划使用或充值。"
                        }
                        triggerQuotaAlert(context, providerName, iconRes, alertMsg)
                        return
                    }
                } else {
                    val isRecovered = when (curr) {
                        "USD" -> amount > (balanceThreshold * 0.15 + 0.20)
                        else -> amount > (balanceThreshold + 1.0)
                    }
                    if (isRecovered && currentStage != null) {
                        clearAlertStage(context, alertKey)
                    }
                }
            }
        }
    }

    fun postTestQuotaAlert(context: Context) {
        ensureChannels(context)
        val stats = cachedStats ?: TokenRepository(context).getCachedStats().also { cachedStats = it }
        val firstProvider = stats?.providers?.firstOrNull()
        val providerName = firstProvider?.provider?.replaceFirstChar { it.uppercase() } ?: "DeepSeek"
        val iconRes = resolveProviderIconRes(firstProvider?.provider ?: "deepseek")
        val alertMsg = "$providerName 余额不足 (¥1.50)，请合理规划使用或充值。"
        triggerQuotaAlert(
            context = context,
            providerName = providerName,
            providerIconRes = iconRes,
            alertMessage = alertMsg,
            durationMs = 15_000L
        )
    }

    fun postNotification(context: Context, force: Boolean = false) {
        try {
            ensureChannels(context)
            val currentAlert = getActiveQuotaAlert()
            val isAlertActive = (currentAlert != null)

            // When a 15-second alert is active, don't allow routine background polling to re-post
            // every 1s, which would reset HyperOS floating heads-up and countdown timers
            if (!force && isAlertActive) {
                return
            }

            val now = System.currentTimeMillis()
            val isWorking = (lastTokenIncreaseTime > 0L && (now - lastTokenIncreaseTime) < WORKING_THRESHOLD_MS)
            val stats = cachedStats
            val config = getIslandConfig(context)
            val quotaVal = resolveAiQuotaValue(stats, config.selectedProvider, config.quotaMode)
            val fingerprint = "${cachedConnected}|${isWorking}|${stats?.today?.totalTokens}|${stats?.today?.costUsd}|" +
                    "${stats?.month?.totalTokens}|${config.leftItem}|${config.rightItem}|${config.showIcon}|" +
                    "${config.selectedProvider}|${config.quotaMode}|$quotaVal|$isAlertActive|${currentAlert?.triggerTime}"

            val isFast = force || isAlertActive || TokenMonitorService.isFastRefreshActive()

            if (!isFast && fingerprint == lastPostedFingerprint && (now - lastPostedTime) < 2_000L) {
                return // Throttled: data is identical and last posted within 2s
            }

            val notification = buildCurrentNotification(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIFICATION_ID, notification)
            lastPostedFingerprint = fingerprint
            lastPostedTime = now
        } catch (e: Throwable) {
            android.util.Log.e("TokenNotification", "postNotification failed", e)
        }
    }

    fun postTestNotification(context: Context) {
        ensureChannels(context)
        val strings = getAppStrings(context)
        lastTokenIncreaseTime = System.currentTimeMillis() // simulate active working
        try {
            startMonitoring(context)
            postNotification(context, force = true)
            Toast.makeText(context, strings.toastLiveNotificationSent, Toast.LENGTH_LONG).show()
        } catch (e: Throwable) {
            Toast.makeText(context, strings.toastSendFailed(e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    fun openNotificationSettings(context: Context) {
        ensureChannels(context)
        val strings = getAppStrings(context)
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Throwable) {
            try {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (e: Throwable) {
                Toast.makeText(context, strings.toastCannotOpenSettings(e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun isWorkingState(): Boolean {
        val now = System.currentTimeMillis()
        return (lastTokenIncreaseTime > 0L && (now - lastTokenIncreaseTime) < WORKING_THRESHOLD_MS)
    }

    fun buildCurrentNotification(context: Context): Notification {
        ensureChannels(context)

        val stats = cachedStats
        val isConnected = cachedConnected
        val isWorking = isWorkingState()
        val config = getIslandConfig(context)

        val todayTokens = stats?.today?.totalTokens ?: 0L
        val todayCost = stats?.today?.costUsd ?: 0.0
        val compactTokens = formatCompactTokens(todayTokens)
        val fullTokensFormatted = NumberFormat.getNumberInstance(Locale.US).format(todayTokens)
        val costFormatted = String.format(Locale.US, "$%.2f", todayCost)

        val strings = getAppStrings(context)
        val statusWord = when {
            !isConnected -> strings.statusOffline
            isWorking -> strings.statusActive
            else -> strings.statusIdle
        }

        val statusDot = ""

        val statusTitle = "Token Monitor · $statusWord"
        val statusText = if (isConnected) {
            "${strings.notifToday}: $compactTokens ($fullTokensFormatted) · $costFormatted"
        } else {
            strings.notifWaitingHub
        }

        // Left slot is locked to working status (IslandItemType.STATUS)
        val leftTitle = resolveItemText(
            IslandItemType.STATUS,
            stats,
            isConnected,
            isWorking,
            compactTokens,
            costFormatted,
            statusDot,
            statusWord,
            config.selectedProvider,
            config.quotaMode
        )

        val rightTitle = resolveItemText(
            config.rightItem,
            stats,
            isConnected,
            isWorking,
            compactTokens,
            costFormatted,
            statusDot,
            statusWord,
            config.selectedProvider,
            config.quotaMode
        )

        val combinedText = listOfNotNull(
            leftTitle.ifBlank { null },
            rightTitle.ifBlank { null }
        ).joinToString(" · ")

        // Select suitable brand icon (if AI Quota is placed on left or right)
        val hasAiQuota = (config.leftItem == IslandItemType.AI_QUOTA || config.rightItem == IslandItemType.AI_QUOTA)
        val targetProvider = resolveTargetProvider(stats, config.selectedProvider)
        val selectedAiProvider = targetProvider?.provider
            ?: if (config.selectedProvider.isNotBlank() && config.selectedProvider != "auto") {
                config.selectedProvider
            } else {
                stats?.providers?.firstOrNull()?.provider ?: "deepseek"
            }

        val iconRes = if (hasAiQuota) {
            resolveProviderIconRes(selectedAiProvider)
        } else {
            R.drawable.ic_brand_token_monitor
        }
        val baseAppIcon = Icon.createWithResource(context, iconRes)

        val leftCapsuleIcon = generateLeftCapsuleIcon(
            context = context,
            baseIconRes = iconRes,
            showIcon = config.showIcon,
            itemType = IslandItemType.STATUS,
            text = leftTitle,
            isConnected = isConnected,
            isWorking = isWorking
        )

        // Physical camera punch-hole standard separation:
        // Left side of punch-hole: Left chamber (iconRes + leftTitle in imageTextInfoLeft)
        // Right side of punch-hole: Right chamber (rightTitle in imageTextInfoRight & shortCriticalText)
        // Strictly separated by the camera punch-hole, NEVER concatenated into one side!
        val shortCriticalText = when {
            rightTitle.isNotBlank() -> rightTitle
            leftTitle.isNotBlank() -> leftTitle
            else -> compactTokens
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isXiaomi = isXiaomiDevice()
        val isColorOs = isColorOsDevice()

        // Choose Small Icon according to AOSP standard vs Xiaomi HyperOS vs ColorOS Fluid Cloud:
        // 1. ColorOS (OPPO/OnePlus/Realme): Fluid Cloud capsule left side cannot render images ("没办法显示图，应该放字才对"),
        //    uses pure text alpha mask bitmap to display crisp status text glyphs.
        // 2. Xiaomi HyperOS: uses clean baseAppIcon without embedded text to prevent duplicate text display alongside imageTextInfoLeft.
        // 3. All other AOSP devices: monochrome alpha vector icon to prevent solid white square glitch.
        val smallIcon = when {
            isColorOs -> {
                generateColorOsTextCapsuleIcon(
                    context = context,
                    text = leftTitle,
                    isConnected = isConnected,
                    isWorking = isWorking
                )
            }
            isXiaomi -> {
                baseAppIcon
            }
            else -> {
                Icon.createWithResource(context, R.drawable.ic_stat_token_monitor)
            }
        }

        // Color indicator: Green when working, Sky Blue when idle, Red/Orange when disconnected
        val accentColor = when {
            !isConnected -> 0xFFFF453A.toInt() // System Red
            isWorking -> 0xFF10B981.toInt()    // Emerald Green
            else -> 0xFF0A84FF.toInt()         // Sky Blue
        }

        val statusIndicator = when {
            !isConnected -> "⚠️ ${strings.statusOffline}"
            isWorking -> "🟢 ${strings.statusActive}"
            else -> "💤 ${strings.statusIdle}"
        }

        val quotaVal = resolveAiQuotaValue(stats, config.selectedProvider, config.quotaMode)

        // Build concise, structured BigText for AOSP notification card
        val expandedBigText = buildString {
            append("• ${strings.leftSlotName}: $statusIndicator\n")
            append("• ${strings.notifToday}: $compactTokens ($fullTokensFormatted) · ${strings.notifCost}: $costFormatted")
            val monthTokens = stats?.month?.totalTokens ?: 0L
            val monthCost = stats?.month?.costUsd ?: 0.0
            if (monthTokens > 0L || monthCost > 0.0) {
                val compactMonth = formatCompactTokens(monthTokens)
                val monthCostFormatted = String.format(Locale.US, "$%.2f", monthCost)
                append("\n• ${strings.notifMonthTotal}: $compactMonth · ${strings.notifCost}: $monthCostFormatted")
            }
        }

        val isSystemDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // PendingIntent for Island expand/click actions (triggers 1-second fast refresh)
        val islandIntent = Intent(context, IslandActionReceiver::class.java).apply {
            action = IslandActionReceiver.ACTION_ISLAND_EXPAND
            `package` = context.packageName
        }
        val islandPendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            islandIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pre-build RemoteViews matching the user's sketch
        val rvLight = bindExpandedRemoteViews(
            context = context,
            layoutId = R.layout.layout_focus_custom,
            stats = stats,
            isConnected = isConnected,
            isWorking = isWorking,
            statusWord = statusWord,
            isDarkMode = false,
            contentPendingIntent = pendingIntent,
            actionPendingIntent = islandPendingIntent
        )
        val rvDark = bindExpandedRemoteViews(
            context = context,
            layoutId = R.layout.layout_focus_custom_night,
            stats = stats,
            isConnected = isConnected,
            isWorking = isWorking,
            statusWord = statusWord,
            isDarkMode = true,
            contentPendingIntent = pendingIntent,
            actionPendingIntent = islandPendingIntent
        )
        val rvIslandExpand = bindExpandedRemoteViews(
            context = context,
            layoutId = R.layout.layout_island_notification_expanded,
            stats = stats,
            isConnected = isConnected,
            isWorking = isWorking,
            statusWord = statusWord,
            isDarkMode = true,
            contentPendingIntent = pendingIntent,
            actionPendingIntent = islandPendingIntent
        )

        val alert = getActiveQuotaAlert()
        val isAlertActive = (alert != null)

        val hostIp = try {
            TokenRepository(context).getConfig().host.ifBlank { "127.0.0.1" }
        } catch (_: Throwable) {
            "127.0.0.1"
        }
        val alertTime = alert?.triggerTime ?: stats?.lastUpdated ?: System.currentTimeMillis()
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(alertTime))
        val boldAlertText: CharSequence = if (alert != null) {
            HtmlCompat.fromHtml("<b>${alert.message}</b>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        } else ""

        val alertClickIds = listOf(
            R.id.island_alert_root,
            R.id.iv_alert_logo,
            R.id.tv_alert_title,
            R.id.tv_alert_host_ip,
            R.id.tv_alert_time,
            R.id.iv_alert_provider_icon,
            R.id.tv_alert_message
        )

        val alertRvLight = if (alert != null) {
            RemoteViews(context.packageName, R.layout.layout_island_alert_light).apply {
                setImageViewResource(R.id.iv_alert_logo, R.drawable.ic_brand_token_monitor)
                setTextViewText(R.id.tv_alert_title, "Token Monitor Σ · 预警")
                setTextViewText(R.id.tv_alert_host_ip, "已连接至 $hostIp")
                setTextViewText(R.id.tv_alert_time, "更新于 $timeStr")
                setImageViewResource(R.id.iv_alert_provider_icon, alert.providerIconRes)
                setTextViewText(R.id.tv_alert_message, boldAlertText)
                for (id in alertClickIds) {
                    try { setOnClickPendingIntent(id, pendingIntent) } catch (_: Throwable) {}
                }
            }
        } else null

        val alertRvNight = if (alert != null) {
            RemoteViews(context.packageName, R.layout.layout_island_alert_night).apply {
                setImageViewResource(R.id.iv_alert_logo, R.drawable.ic_brand_token_monitor)
                setTextViewText(R.id.tv_alert_title, "Token Monitor Σ · 预警")
                setTextViewText(R.id.tv_alert_host_ip, "已连接至 $hostIp")
                setTextViewText(R.id.tv_alert_time, "更新于 $timeStr")
                setImageViewResource(R.id.iv_alert_provider_icon, alert.providerIconRes)
                setTextViewText(R.id.tv_alert_message, boldAlertText)
                for (id in alertClickIds) {
                    try { setOnClickPendingIntent(id, pendingIntent) } catch (_: Throwable) {}
                }
            }
        } else null

        val effectiveRvLight = if (isAlertActive) alertRvLight!! else rvLight
        val effectiveRvDark = if (isAlertActive) alertRvNight!! else rvDark
        val effectiveRvIslandExpand = if (isAlertActive) {
            alertRvNight!!
        } else {
            rvIslandExpand
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID_FOCUS)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        builder
            .setSmallIcon(smallIcon)
            .setSubText(if (alert != null) "⚠️ 预警" else (if (leftTitle.isNotBlank()) leftTitle else statusWord))
            .setContentTitle(if (alert != null) "Token Monitor Σ · 预警" else statusTitle)
            .setContentText(if (alert != null) alert.message else statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(alert == null)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setCategory(if (alert != null) Notification.CATEGORY_ALARM else Notification.CATEGORY_STATUS)
            .setColor(if (alert != null) 0xFFFF453A.toInt() else accentColor)
            .setColorized(false)

        if (alert != null) {
            builder.setVibrate(longArrayOf(0, 180, 80, 180))
            builder.setDefaults(Notification.DEFAULT_VIBRATE)
        }

        if (isXiaomi) {
            val customRv = if (isSystemDarkMode) effectiveRvDark else effectiveRvLight
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    builder.setCustomContentView(customRv)
                    builder.setCustomBigContentView(customRv)
                    if (alert != null) {
                        builder.setCustomHeadsUpContentView(customRv)
                    }
                } catch (t: Throwable) {
                    android.util.Log.e("TokenNotification", "Failed to set custom content views", t)
                    builder.setStyle(Notification.BigTextStyle().bigText(expandedBigText))
                }
            }
        } else {
            if (alert != null) {
                val customRv = if (isSystemDarkMode) effectiveRvDark else effectiveRvLight
                builder.setCustomContentView(customRv)
                builder.setCustomBigContentView(customRv)
                builder.setCustomHeadsUpContentView(customRv)
            } else {
                builder.setStyle(Notification.BigTextStyle().bigText(expandedBigText))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        builder.setLargeIcon(baseAppIcon)

        // 1. Android 16 / HyperOS 4 Promoted Ongoing (Live Updates) native APIs
        try {
            val method = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
            method.invoke(builder, true)
        } catch (_: Throwable) {}

        try {
            val method = builder.javaClass.getMethod("setShortCriticalText", CharSequence::class.java)
            method.invoke(builder, shortCriticalText)
        } catch (_: Throwable) {}

        // 2. Xiaomi HyperOS Custom RemoteViews Payload (miui.focus.param.custom)
        val customParamJson = try {
            JSONObject().apply {
                put("protocol", 1)
                put("enableFloat", alert != null) // When alert, pop down floating heads-up!
                put("updatable", true)
                put("isShowNotification", true)
                put("reopen", "reopen")
                put("ticker", if (alert != null) "Token Monitor Σ · 预警: ${alert.message}" else (if (combinedText.isNotBlank()) "$statusDot $combinedText" else statusTitle))
                put("aodTitle", if (alert != null) "Token Monitor 预警" else statusTitle)
                put("aodPic", "miui.focus.pic_icon_light")
                put("timeout", if (alert != null) 15 else 86400)

                val paramIsland = JSONObject().apply {
                    put("islandProperty", 1)
                    put("islandTimeout", if (alert != null) 15 else 86400)
                    put("dismissIsland", false)
                    put("expandedTime", if (alert != null) 15000 else 300000)
                    put("needCloseAnimation", true)

                    // Small Island (Status bar pill/capsule)
                    put("smallIslandArea", JSONObject().apply {
                        put("picInfo", JSONObject().apply {
                            put("type", 1)
                            put("pic", "miui.focus.pic_small")
                            put("action", "action_expand")
                        })
                    })

                    // Big Island (Split pill)
                    put("bigIslandArea", JSONObject().apply {
                        put("imageTextInfoLeft", JSONObject().apply {
                            put("type", 1)
                            put("action", "action_expand")
                            if (config.showIcon || alert != null) {
                                put("picInfo", JSONObject().apply {
                                    put("type", 1)
                                    put("pic", "miui.focus.pic_icon_light")
                                })
                            }
                            put("textInfo", JSONObject().apply {
                                put("title", if (alert != null) "⚠️ 预警" else leftTitle)
                            })
                        })
                        put("imageTextInfoRight", JSONObject().apply {
                            put("type", 2)
                            put("action", "action_expand")
                            put("textInfo", JSONObject().apply {
                                put("title", if (alert != null) alert.providerName else (if (rightTitle.isNotBlank()) rightTitle else " "))
                                if (alert == null) {
                                    put("turnAnim", true) // Xiaomi native number roll animation
                                }
                            })
                        })
                    })

                    put("shareData", JSONObject().apply {
                        put("title", if (alert != null) "Token Monitor 预警" else statusTitle)
                        put("pic", "miui.focus.pic_icon_light")
                        put("shareContent", if (alert != null) alert.message else (if (combinedText.isNotBlank()) combinedText else statusTitle))
                    })
                }

                put("param_island", paramIsland)
            }.toString()
        } catch (_: Throwable) {
            null
        }

        // Actions bundle for Xiaomi HyperOS focus notifications
        val actionsBundle = Bundle().apply {
            val expandAction = Notification.Action.Builder(
                baseAppIcon,
                "Expand",
                islandPendingIntent
            ).build()
            putParcelable("action_expand", expandAction)

            val openAction = Notification.Action.Builder(
                baseAppIcon,
                "Open",
                pendingIntent
            ).build()
            putParcelable("action_open", openAction)
        }

        // Bundle Xiaomi HyperOS SystemUI parcelable icons
        val alertIcon = if (alert != null) {
            Icon.createWithResource(context, alert.providerIconRes)
        } else {
            baseAppIcon
        }

        val picsBundle = Bundle().apply {
            putParcelable("miui.focus.pic_ticker", alertIcon)
            putParcelable("miui.focus.pic_small", alertIcon)
            putParcelable("miui.focus.pic_icon", alertIcon)
            putParcelable("miui.focus.pic_icon_light", alertIcon)
            putParcelable("miui.focus.pic_icon_dark", alertIcon)
            putParcelable("miui.focus.pic_icon_light_secondary", alertIcon)
        }

        // Extras injection for HyperOS, ColorOS Fluid Cloud, and Android 16 / AOSP Live Updates
        val extras = Bundle().apply {
            if (isXiaomi) {
                // When using custom RemoteViews, ONLY inject miui.focus.param.custom (do NOT inject miui.focus.param)
                if (customParamJson != null) {
                    putString("miui.focus.param.custom", customParamJson)
                }
                putString("miui.focus.ticker", if (alert != null) "Token Monitor Σ · 预警: ${alert.message}" else (if (combinedText.isNotBlank()) "$statusDot $combinedText" else statusTitle))

                putParcelable("miui.focus.rv", effectiveRvLight)
                putParcelable("miui.focus.rvNight", effectiveRvDark)
                putParcelable("miui.focus.rv.fullAod", effectiveRvDark)
                putParcelable("miui.focus.rv.island.expand", effectiveRvIslandExpand)

                putBundle("miui.focus.pics", picsBundle)
                putBundle("miui.focus.actions", actionsBundle)
                putParcelable("miui.pending.intent", pendingIntent)

                // When alert is active, enable float so HyperOS pops it down!
                putBoolean("miui.enableFloat", alert != null)
                putBoolean("miui.showFloat", alert != null)
                putBoolean("miui.enableKeyguard", true)
                putBoolean("miui.focus.isFocus", true)
            }

            if (isColorOs) {
                val cleanLeft = leftTitle.ifBlank { statusWord }
                val cleanRight = rightTitle.ifBlank { compactTokens }
                putString("oplus.capsule.left_text", cleanLeft)
                putString("oplus.capsule.right_text", cleanRight)
                putString("oplus.capsule.title", statusTitle)
                putString("oplus.capsule.content", statusText)
                putString("oppo.capsule.left_text", cleanLeft)
                putString("oppo.capsule.right_text", cleanRight)
            }

            // Android 16 / AOSP Live Updates & Promoted Ongoing standard extras (safe for all brands)
            putBoolean("android.requestPromotedOngoing", true)
            putBoolean("android.promotedOngoing", true)
            putCharSequence("android.shortCriticalText", shortCriticalText)
        }

        builder.addExtras(extras)

        val notification = builder.build()
        @Suppress("DEPRECATION")
        if (notification.icon == 0) {
            notification.icon = if (isXiaomi) iconRes else R.drawable.ic_stat_token_monitor
        }

        return notification
    }

    private fun resolveItemText(
        item: IslandItemType,
        stats: TokenStats?,
        isConnected: Boolean,
        isWorking: Boolean,
        compactTokens: String,
        costFormatted: String,
        statusDot: String,
        statusWord: String,
        selectedProvider: String,
        quotaMode: String = "auto"
    ): String {
        return when (item) {
            IslandItemType.STATUS -> statusWord
            IslandItemType.TODAY_TOKENS -> compactTokens
            IslandItemType.AI_QUOTA -> resolveAiQuotaValue(stats, selectedProvider, quotaMode)
            IslandItemType.TODAY_COST -> costFormatted
            IslandItemType.MONTH_TOKENS -> formatCompactTokens(stats?.month?.totalTokens ?: 0L)
            IslandItemType.NONE -> ""
        }
    }

    fun resolveTargetProvider(stats: TokenStats?, selectedProvider: String): com.tokenmonitor.app.data.ProviderLimit? {
        val providers = stats?.providers.orEmpty()
        if (providers.isEmpty()) return null
        return if (selectedProvider.isNotBlank() && selectedProvider != "auto") {
            providers.firstOrNull { it.provider.equals(selectedProvider, ignoreCase = true) }
                ?: providers.firstOrNull { it.provider.contains(selectedProvider, ignoreCase = true) }
        } else {
            // In auto mode, find provider with lowest quota percentage, or first provider with balance
            val providersWithQuota = providers.filter { p ->
                p.windows.any { !it.kind.equals("billing", true) && it.remainingPercent >= 0.0 }
            }
            if (providersWithQuota.isNotEmpty()) {
                providersWithQuota.minByOrNull { p ->
                    p.windows.filter { !it.kind.equals("billing", true) && it.remainingPercent >= 0.0 }
                        .minOfOrNull { it.remainingPercent } ?: 100.0
                } ?: providers.first()
            } else {
                providers.first()
            }
        }
    }

    /**
     * Resolves the AI quota display value without verbose English brand names.
     * Supports: 5-hour limit ("5h"), weekly limit ("weekly"), balance mode ("balance"), and "auto".
     */
    fun resolveAiQuotaValue(
        stats: TokenStats?,
        selectedProvider: String,
        quotaMode: String = "auto"
    ): String {
        val targetProvider = resolveTargetProvider(stats, selectedProvider) ?: return "100%"
        val windows = targetProvider.windows
        val quotaWindows = windows.filter { w ->
            !w.kind.equals("billing", ignoreCase = true) &&
            !w.label.contains("balance", ignoreCase = true) &&
            w.remainingPercent >= 0.0
        }

        val hasBalance = targetProvider.balanceAmount != null
        val isDeepSeek = targetProvider.provider.contains("deepseek", ignoreCase = true)

        fun formatBalance(): String {
            val balAmt = targetProvider.balanceAmount ?: return "100%"
            val balCurr = targetProvider.balanceCurrency ?: "USD"
            val symbol = when {
                balCurr.contains("CNY", ignoreCase = true) || balCurr.contains("RMB", ignoreCase = true) -> "¥"
                balCurr.contains("EUR", ignoreCase = true) -> "€"
                balCurr.contains("GBP", ignoreCase = true) -> "£"
                else -> "$"
            }
            return String.format(Locale.US, "%s%.2f", symbol, balAmt)
        }

        return when (quotaMode.lowercase()) {
            "5h", "five_hour" -> {
                val win = quotaWindows.firstOrNull { w ->
                    val k = w.kind.lowercase()
                    val l = w.label.lowercase()
                    k.contains("five") || k.contains("5h") || l.contains("5h") || l.contains("5小时")
                } ?: quotaWindows.firstOrNull()
                if (win != null) {
                    val rem = win.remainingPercent.toInt()
                    "$rem%"
                } else if (hasBalance) {
                    formatBalance()
                } else {
                    "100%"
                }
            }
            "weekly", "week" -> {
                val win = quotaWindows.firstOrNull { w ->
                    val k = w.kind.lowercase()
                    val l = w.label.lowercase()
                    k.contains("week") || l.contains("周")
                } ?: quotaWindows.firstOrNull()
                if (win != null) {
                    val rem = win.remainingPercent.toInt()
                    "$rem%"
                } else if (hasBalance) {
                    formatBalance()
                } else {
                    "100%"
                }
            }
            "balance" -> {
                if (hasBalance) {
                    formatBalance()
                } else if (quotaWindows.isNotEmpty()) {
                    val lowest = quotaWindows.minByOrNull { it.remainingPercent }
                    val rem = lowest?.remainingPercent?.toInt() ?: 100
                    "$rem%"
                } else {
                    "100%"
                }
            }
            else -> {
                // Auto mode:
                // If DeepSeek or provider has balance and no quota windows, display cash balance!
                if ((isDeepSeek && hasBalance) || (hasBalance && quotaWindows.isEmpty())) {
                    formatBalance()
                } else if (quotaWindows.isNotEmpty()) {
                    val lowest = quotaWindows.minByOrNull { it.remainingPercent }
                    val rem = lowest?.remainingPercent?.toInt() ?: 100
                    "$rem%"
                } else if (hasBalance) {
                    formatBalance()
                } else {
                    "100%"
                }
            }
        }
    }

    fun resolveProviderIconRes(providerName: String): Int {
        val lower = providerName.lowercase()
        return when {
            lower.contains("claude") || lower.contains("anthropic") -> R.drawable.ic_brand_claude
            lower.contains("deepseek") -> R.drawable.ic_brand_deepseek
            lower.contains("gemini") -> R.drawable.ic_brand_gemini
            lower.contains("antigravity") -> R.drawable.ic_brand_antigravity
            lower.contains("openai") || lower.contains("codex") -> R.drawable.ic_brand_codex
            lower.contains("qwen") || lower.contains("alibaba") -> R.drawable.ic_brand_qwen
            lower.contains("kimi") || lower.contains("moonshot") -> R.drawable.ic_brand_kimi
            lower.contains("copilot") || lower.contains("github") -> R.drawable.ic_brand_copilot
            lower.contains("cursor") -> R.drawable.ic_brand_cursor
            lower.contains("cline") -> R.drawable.ic_brand_cline
            lower.contains("doubao") || lower.contains("volcengine") || lower.contains("arkcli") -> R.drawable.ic_brand_doubao
            lower.contains("droid") || lower.contains("factory") -> R.drawable.ic_brand_droid
            lower.contains("grok") || lower.contains("xai") -> R.drawable.ic_brand_grok
            lower.contains("minimax") -> R.drawable.ic_brand_minimax
            lower.contains("ollama") -> R.drawable.ic_brand_ollama
            lower.contains("openrouter") -> R.drawable.ic_brand_openrouter
            lower.contains("trae") -> R.drawable.ic_brand_trae
            lower.contains("opencode") -> R.drawable.ic_brand_opencode
            lower.contains("workbuddy") -> R.drawable.ic_brand_workbuddy
            lower.contains("glm") || lower.contains("zhipu") || lower.contains("zai") || lower.contains("bigmodel") -> R.drawable.ic_brand_glm
            else -> R.drawable.ic_brand_token_monitor
        }
    }

    fun formatCompactTokens(num: Long): String {
        return when {
            num >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", num / 1_000_000_000.0)
            num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
            num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
            else -> num.toString()
        }
    }

    /**
     * Generates a pure text alpha mask Icon for ColorOS (OPPO/OnePlus/Realme) status bar capsule.
     * ColorOS status bar does not support rendering multi-color image bitmaps in the capsule left area
     * ("coloros系统流体云左侧没办法显示图，应该放字才对").
     * By rendering pure text glyphs with Color.WHITE on an entirely transparent canvas (alpha = 0),
     * ColorOS status bar alpha mask renders crisp, legible text characters (e.g. "工作中" / "离线" / "空闲中").
     */
    private fun generateColorOsTextCapsuleIcon(
        context: Context,
        text: String?,
        isConnected: Boolean,
        isWorking: Boolean
    ): Icon {
        val strings = getAppStrings(context)
        val cleanText = text?.replace("🟢", "")?.replace("💤", "")?.replace("⚠️", "")?.trim().orEmpty()
            .ifEmpty { if (!isConnected) strings.statusOffline else if (isWorking) strings.statusActive else strings.statusIdle }

        val cacheKey = "coloros|$cleanText|$isConnected|$isWorking"
        val cached = cachedCapsuleIcon
        if (cached != null && cacheKey == lastCapsuleKey) {
            return cached
        }

        try {
            val density = context.resources.displayMetrics.density
            val scale = 2.0f
            val heightPx = (24 * density * scale).toInt().coerceAtLeast(96)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                color = Color.WHITE
                textSize = 14f * density * scale
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                hinting = Paint.HINTING_ON
                textAlign = Paint.Align.CENTER
            }

            val fontMetrics = textPaint.fontMetrics
            val textWidth = textPaint.measureText(cleanText)
            val paddingPx = (6 * density * scale).toInt()
            val widthPx = (textWidth + paddingPx * 2).toInt().coerceAtLeast(heightPx)

            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            bitmap.density = (context.resources.displayMetrics.densityDpi * scale).toInt()
            val canvas = Canvas(bitmap)

            val baseline = (heightPx / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(cleanText, widthPx / 2f, baseline, textPaint)

            val icon = Icon.createWithBitmap(bitmap)
            lastCapsuleKey = cacheKey
            cachedCapsuleIcon = icon
            return icon
        } catch (e: Throwable) {
            android.util.Log.e("TokenNotification", "generateColorOsTextCapsuleIcon failed, fallback", e)
            return Icon.createWithResource(context, R.drawable.ic_stat_token_monitor)
        }
    }

    /**
     * Generates a dynamic composite Icon for the left side of the physical punch-hole camera.
     * Combines [baseIconRes] (if [showIcon] is true) + [text] (if non-empty) into a single
     * clean, crisp ARGB_8888 bitmap icon for the status bar.
     * When [itemType] is AI_QUOTA, automatically ensures the AI logo is drawn with the compact value.
     */
    private fun generateLeftCapsuleIcon(
        context: Context,
        baseIconRes: Int,
        showIcon: Boolean,
        itemType: IslandItemType,
        text: String?,
        isConnected: Boolean,
        isWorking: Boolean
    ): Icon {
        val cleanText = text?.trim().orEmpty()
        val effectiveShowIcon = showIcon || (itemType == IslandItemType.AI_QUOTA)

        // If no text needs to be drawn and icon is shown, just use the resource icon directly
        if (cleanText.isEmpty() && effectiveShowIcon) {
            return Icon.createWithResource(context, baseIconRes)
        }

        // If both text and icon are absent, fallback to base resource icon
        if (cleanText.isEmpty() && !effectiveShowIcon) {
            return Icon.createWithResource(context, baseIconRes)
        }

        val cacheKey = "$baseIconRes|$effectiveShowIcon|${itemType.name}|$cleanText|$isConnected|$isWorking"
        val cached = cachedCapsuleIcon
        if (cached != null && cacheKey == lastCapsuleKey) {
            return cached
        }

        try {
            val density = context.resources.displayMetrics.density
            val scale = 2.0f
            val heightPx = (28 * density * scale).toInt().coerceAtLeast(112)
            val iconSizePx = (16 * density * scale).toInt()
            val gapPx = (4 * density * scale).toInt()
            val sidePaddingPx = (4 * density * scale).toInt()

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                color = Color.WHITE
                textSize = 13.5f * density * scale
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                hinting = Paint.HINTING_ON
            }

            // Check if we should render a native status dot (for STATUS item)
            val isStatusItem = itemType == IslandItemType.STATUS
            val statusDotRadius = 3f * density * scale
            val statusDotSize = (statusDotRadius * 2).toInt()
            val statusDotColor = when {
                !isConnected -> 0xFFFF453A.toInt() // Red
                isWorking -> 0xFF30D158.toInt()    // Green
                else -> 0xFF8E8E93.toInt()         // Muted gray
            }

            // If text contains status emoji like "🟢 " or "💤 " or "⚠️ ", strip it because we draw a crisp dot
            val displayText = if (isStatusItem) {
                cleanText.replace("🟢", "").replace("💤", "").replace("⚠️", "").trim()
            } else {
                cleanText
            }

            val textWidth = if (displayText.isNotEmpty()) textPaint.measureText(displayText) else 0f

            var totalWidth = sidePaddingPx
            if (effectiveShowIcon) {
                totalWidth += iconSizePx
                if (isStatusItem || displayText.isNotEmpty()) {
                    totalWidth += gapPx
                }
            }
            if (isStatusItem) {
                totalWidth += statusDotSize + (3 * density * scale).toInt()
            }
            if (displayText.isNotEmpty()) {
                totalWidth += textWidth.toInt()
            }
            totalWidth += sidePaddingPx

            val widthPx = totalWidth.coerceAtLeast(heightPx)
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            bitmap.density = (context.resources.displayMetrics.densityDpi * scale).toInt()
            val canvas = Canvas(bitmap)

            var currentX = if (widthPx > totalWidth) {
                ((widthPx - totalWidth) / 2f).coerceAtLeast(0f) + sidePaddingPx.toFloat()
            } else {
                sidePaddingPx.toFloat()
            }

            // 1. Draw base icon if effectiveShowIcon is true
            if (effectiveShowIcon) {
                var drawable: Drawable? = null
                try {
                    drawable = ContextCompat.getDrawable(context, baseIconRes)?.mutate()
                } catch (e: Throwable) {
                    android.util.Log.w("TokenNotification", "Failed to load baseIconRes $baseIconRes", e)
                }
                if (drawable == null && baseIconRes != R.drawable.ic_brand_token_monitor) {
                    try {
                        drawable = ContextCompat.getDrawable(context, R.drawable.ic_brand_token_monitor)?.mutate()
                    } catch (_: Throwable) {}
                }
                if (drawable != null) {
                    val iconTop = (heightPx - iconSizePx) / 2
                    drawable.setBounds(currentX.toInt(), iconTop, (currentX + iconSizePx).toInt(), iconTop + iconSizePx)
                    drawable.draw(canvas)
                }
                currentX += iconSizePx
                if (isStatusItem || displayText.isNotEmpty()) {
                    currentX += gapPx
                }
            }

            // 2. Draw status dot if STATUS item
            if (isStatusItem) {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = statusDotColor
                    style = Paint.Style.FILL
                }
                val dotCenterY = heightPx / 2f
                canvas.drawCircle(currentX + statusDotRadius, dotCenterY, statusDotRadius, dotPaint)
                currentX += statusDotSize + (3 * density * scale).toInt()
            }

            // 3. Draw text with exact vertical centering
            if (displayText.isNotEmpty()) {
                val fontMetrics = textPaint.fontMetrics
                val baseline = (heightPx / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(displayText, currentX, baseline, textPaint)
            }

            val generatedIcon = Icon.createWithBitmap(bitmap)
            lastCapsuleKey = cacheKey
            cachedCapsuleIcon = generatedIcon
            return generatedIcon
        } catch (e: Throwable) {
            android.util.Log.e("TokenNotification", "generateLeftCapsuleIcon failed, fallback", e)
            return Icon.createWithResource(context, baseIconRes)
        }
    }

    data class ProviderDisplayData(
        val name: String,
        val iconRes: Int,
        val remainingPercent: Double,
        val quotaOrBalanceText: String,
        val usageText: CharSequence,
        val brandColor: Int
    )

    /**
     * DeepSeek 梁文峰时段/梁文谷时段判定 (北京时间 UTC+8):
     * 周一到周五: 上午 08:00 - 12:00, 下午 14:00 - 18:00 为梁文峰时段 (高峰繁忙期)
     * 其余时段 (午休 12:00-14:00、晚间 18:00-08:00、周末全天) 为梁文谷时段 (低峰优惠期)
     */
    private fun isDeepSeekPeakPeriod(): Boolean {
        return try {
            val beijingZone = java.time.ZoneId.of("Asia/Shanghai")
            val now = java.time.ZonedDateTime.now(beijingZone)
            val dayOfWeek = now.dayOfWeek
            val isWeekday = dayOfWeek != java.time.DayOfWeek.SATURDAY && dayOfWeek != java.time.DayOfWeek.SUNDAY
            if (!isWeekday) {
                return false
            }

            val minuteOfDay = now.hour * 60 + now.minute
            // 上午 8点到 12点: 08:00 - 12:00 (480..719 分钟)
            val isMorningPeak = minuteOfDay in 480 until 720
            // 下午 2点到 6点: 14:00 - 18:00 (840..1079 分钟)
            val isAfternoonPeak = minuteOfDay in 840 until 1080

            isMorningPeak || isAfternoonPeak
        } catch (_: Throwable) {
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+8"))
            val day = cal.get(java.util.Calendar.DAY_OF_WEEK)
            val isWeekday = day != java.util.Calendar.SATURDAY && day != java.util.Calendar.SUNDAY
            if (!isWeekday) return false
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            (hour in 8..11) || (hour in 14..17)
        }
    }

    private fun getDeepSeekPeriodName(): String {
        return if (isDeepSeekPeakPeriod()) "梁文峰时段" else "梁文谷时段"
    }

    private fun parseEpochMs(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val num = trimmed.toLongOrNull()
        if (num != null) {
            return if (num > 100_000_000_000L) num else num * 1000L
        }
        return try {
            java.time.Instant.parse(trimmed).toEpochMilli()
        } catch (_: Throwable) {
            try {
                java.time.OffsetDateTime.parse(trimmed).toInstant().toEpochMilli()
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun generateProviderRingBitmap(
        context: Context,
        providerIconRes: Int,
        remainingPercent: Double,
        brandColorInt: Int,
        isDarkMode: Boolean
    ): Bitmap {
        val cacheKey = "${providerIconRes}_${(remainingPercent * 10).toInt()}_${brandColorInt}_${isDarkMode}"
        ringBitmapCache[cacheKey]?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        val density = context.resources.displayMetrics.density
        val sizePx = (42 * density).toInt().coerceAtLeast(1)
        val strokePx = 3f * density
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = sizePx / 2f
        val radius = (center - strokePx).coerceAtLeast(1f)

        // 1. Draw track (background circle)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            color = if (isDarkMode) Color.argb(45, 255, 255, 255) else Color.argb(35, 0, 0, 0)
        }
        canvas.drawCircle(center, center, radius, trackPaint)

        // 2. Draw progress arc (representing remaining quota percent)
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
            strokeCap = Paint.Cap.ROUND
            color = brandColorInt
        }
        val oval = RectF(center - radius, center - radius, center + radius, center + radius)
        val sweepAngle = (Math.max(0.0, Math.min(100.0, remainingPercent)) * 3.6).toFloat()
        canvas.drawArc(oval, -90f, sweepAngle, false, progressPaint)

        // 3. Draw center provider icon
        try {
            val drawable = ContextCompat.getDrawable(context, providerIconRes)?.mutate()
            if (drawable != null) {
                val innerIconSize = (20 * density).toInt()
                val left = ((sizePx - innerIconSize) / 2)
                val top = ((sizePx - innerIconSize) / 2)
                drawable.setBounds(left, top, left + innerIconSize, top + innerIconSize)
                drawable.draw(canvas)
            }
        } catch (_: Throwable) {}

        ringBitmapCache[cacheKey] = bitmap
        return bitmap
    }

    private fun resolveTopProviders(context: Context, stats: TokenStats?): List<ProviderDisplayData> {
        val providers = stats?.providers.orEmpty()
        val todayModels = stats?.today?.models.orEmpty()

        if (providers.isNotEmpty()) {
            val providerOrder = try {
                TokenRepository(context).getProviderOrder()
            } catch (_: Throwable) {
                emptyList()
            }
            val sortedProviders = if (providerOrder.isEmpty()) {
                providers
            } else {
                providers.sortedBy { p ->
                    val idx = providerOrder.indexOfFirst { it.equals(p.provider, ignoreCase = true) }
                    if (idx >= 0) idx else Int.MAX_VALUE
                }
            }

            return sortedProviders.take(2).map { p ->
                val pName = p.accountLabel.ifBlank {
                    p.provider.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                }
                val icon = resolveProviderIconRes(p.provider)

                // Brand color
                val lower = p.provider.lowercase()
                val color = when {
                    lower.contains("deepseek") -> if (isDeepSeekPeakPeriod()) 0xFF0284C7.toInt() else 0xFF10B981.toInt()
                    lower.contains("antigravity") -> 0xFF06B6D4.toInt()
                    lower.contains("claude") || lower.contains("anthropic") -> 0xFFD97706.toInt()
                    lower.contains("openai") || lower.contains("codex") -> 0xFF10B981.toInt()
                    lower.contains("gemini") -> 0xFF3B82F6.toInt()
                    lower.contains("qwen") -> 0xFF8B5CF6.toInt()
                    else -> 0xFF38BDF8.toInt()
                }

                // Remaining percent and quota / balance text
                val qWindow = p.windows.firstOrNull { !it.kind.equals("billing", true) && it.remainingPercent >= 0.0 }
                val remPct = qWindow?.remainingPercent ?: 100.0

                val quotaText = if (qWindow != null) {
                    val epochMs = parseEpochMs(qWindow.resetsAt)
                    if (epochMs != null && epochMs > System.currentTimeMillis()) {
                        val diffSec = (epochMs - System.currentTimeMillis()) / 1000
                        val hours = diffSec / 3600
                        val minutes = (diffSec % 3600) / 60
                        val seconds = diffSec % 60
                        val cd = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                        "${remPct.toInt()}% | $cd 后重置"
                    } else if (!qWindow.resetDescription.isNullOrBlank()) {
                        "${remPct.toInt()}% | ${qWindow.resetDescription}"
                    } else {
                        "${remPct.toInt()}% 剩余"
                    }
                } else if (p.balanceAmount != null) {
                    val amt = String.format(Locale.US, "%.2f", p.balanceAmount)
                    if (lower.contains("deepseek")) {
                        "$amt ¥ | ${getDeepSeekPeriodName()}"
                    } else {
                        "$$amt | 正常"
                    }
                } else {
                    if (lower.contains("deepseek")) {
                        "100% 充足 | ${getDeepSeekPeriodName()}"
                    } else {
                        "100% 充足"
                    }
                }

                // Calculate usage for this specific provider (not overall total)
                val pTokens = calculateProviderTodayTokens(p, stats)
                val compact = formatCompactTokens(pTokens)
                val usageFormatted: CharSequence = HtmlCompat.fromHtml(
                    "已使用 <b>$compact</b> Token",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )

                ProviderDisplayData(
                    name = pName,
                    iconRes = icon,
                    remainingPercent = remPct,
                    quotaOrBalanceText = quotaText,
                    usageText = usageFormatted,
                    brandColor = color
                )
            }
        }

        // Fallback if no direct providers: check today's models
        if (todayModels.isNotEmpty()) {
            return todayModels.take(2).map { m ->
                val icon = resolveProviderIconRes(m.model)
                val compact = formatCompactTokens(m.tokens)
                val usageFormatted: CharSequence = HtmlCompat.fromHtml(
                    "已使用 <b>$compact</b> Token",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
                ProviderDisplayData(
                    name = m.model,
                    iconRes = icon,
                    remainingPercent = 100.0,
                    quotaOrBalanceText = "实时监控",
                    usageText = usageFormatted,
                    brandColor = 0xFF0284C7.toInt()
                )
            }
        }

        return emptyList()
    }

    private fun calculateProviderTodayTokens(provider: ProviderLimit, stats: TokenStats?): Long {
        if (stats == null) return 0L
        val today = stats.today
        val pRaw = provider.provider.lowercase().trim()
        val pNorm = pRaw.replace(Regex("[^a-z0-9]"), "")
        if (pNorm.isBlank()) return 0L

        // Strategy 1: Match by client name in today.clients
        val matchedClients = today.clients.filter { c ->
            val cNorm = c.name.lowercase().replace(Regex("[^a-z0-9]"), "")
            isClientMatchingProvider(pNorm, cNorm)
        }
        val clientTokens = matchedClients.sumOf { it.tokens }
        if (clientTokens > 0L) {
            return clientTokens
        }

        // Strategy 2: Match models where m.client matches the provider
        val clientModelTokens = today.models.filter { m ->
            val cNorm = m.client.lowercase().replace(Regex("[^a-z0-9]"), "")
            cNorm.isNotBlank() && cNorm != "all" && isClientMatchingProvider(pNorm, cNorm)
        }.sumOf { it.tokens }
        if (clientModelTokens > 0L) {
            return clientModelTokens
        }

        // Strategy 3: Match models by model name (for direct API providers or flat model list)
        val modelTokens = today.models.filter { m ->
            val mLower = m.model.lowercase().replace(Regex("[^a-z0-9]"), "")
            isModelMatchingProvider(pNorm, mLower)
        }.sumOf { it.tokens }
        if (modelTokens > 0L) {
            return modelTokens
        }

        return 0L
    }

    private fun isClientMatchingProvider(pNorm: String, cNorm: String): Boolean {
        if (pNorm == cNorm || pNorm.contains(cNorm) || cNorm.contains(pNorm)) return true
        if (pNorm.contains("antigravity") && cNorm.contains("antigravity")) return true
        if ((pNorm.contains("claude") || pNorm.contains("anthropic")) && (cNorm.contains("claude") || cNorm.contains("anthropic"))) return true
        if ((pNorm.contains("openai") || pNorm.contains("codex") || pNorm.contains("chatgpt")) &&
            (cNorm.contains("openai") || cNorm.contains("codex") || cNorm.contains("chatgpt"))) return true
        if (pNorm.contains("copilot") && cNorm.contains("copilot")) return true
        if (pNorm.contains("cursor") && cNorm.contains("cursor")) return true
        if (pNorm.contains("deepseek") && cNorm.contains("deepseek")) return true
        if ((pNorm.contains("gemini") || pNorm.contains("google")) && (cNorm.contains("gemini") || cNorm.contains("google"))) return true
        if ((pNorm.contains("qwen") || pNorm.contains("tongyi")) && (cNorm.contains("qwen") || cNorm.contains("tongyi"))) return true
        if (pNorm.contains("opencode") && cNorm.contains("opencode")) return true
        if (pNorm.contains("windsurf") && cNorm.contains("windsurf")) return true
        if (pNorm.contains("trae") && cNorm.contains("trae")) return true
        return false
    }

    private fun isModelMatchingProvider(pNorm: String, mLower: String): Boolean {
        if (pNorm.contains("deepseek") && mLower.contains("deepseek")) return true
        if ((pNorm.contains("openai") || pNorm.contains("codex") || pNorm.contains("chatgpt")) &&
            (mLower.contains("gpt") || mLower.contains("o1") || mLower.contains("o3") || mLower.contains("openai"))) return true
        if ((pNorm.contains("claude") || pNorm.contains("anthropic")) && (mLower.contains("claude") || mLower.contains("anthropic"))) return true
        if ((pNorm.contains("gemini") || pNorm.contains("google")) && (mLower.contains("gemini") || mLower.contains("google"))) return true
        if ((pNorm.contains("qwen") || pNorm.contains("tongyi")) && (mLower.contains("qwen") || mLower.contains("tongyi"))) return true
        return mLower.contains(pNorm)
    }

    private fun bindExpandedRemoteViews(
        context: Context,
        layoutId: Int,
        stats: TokenStats?,
        isConnected: Boolean,
        isWorking: Boolean,
        statusWord: String,
        isDarkMode: Boolean,
        contentPendingIntent: PendingIntent,
        actionPendingIntent: PendingIntent
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, layoutId)

        // 1. Header: Logo + Title/Status + Host IP + Update Time
        rv.setImageViewResource(R.id.iv_exp_app_logo, R.drawable.ic_brand_token_monitor)
        rv.setTextViewText(R.id.tv_exp_title_status, "Token Monitor Σ · $statusWord")

        val hostIp = try {
            TokenRepository(context).getConfig().host
        } catch (_: Throwable) {
            "127.0.0.1"
        }
        val hostText = if (isConnected) "已连接至 $hostIp" else "未连接至 PC Hub"
        rv.setTextViewText(R.id.tv_exp_host_ip, hostText)

        val timeStr = try {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            "更新于 " + sdf.format(Date(stats?.lastUpdated ?: System.currentTimeMillis()))
        } catch (_: Throwable) {
            "更新于 刚刚"
        }
        rv.setTextViewText(R.id.tv_exp_update_time, timeStr)

        // 2. Today tokens: Compact unit badge (e.g. 300M) + Unshortened with commas (e.g. 300,000,000)
        val todayTotal = stats?.today?.totalTokens ?: 0L
        val compactToday = formatCompactTokens(todayTotal)
        rv.setTextViewText(R.id.tv_exp_today_compact, compactToday)

        val exactTodayTokens = NumberFormat.getNumberInstance(Locale.US).format(todayTotal)
        val boldTodayTokens: CharSequence = HtmlCompat.fromHtml(
            "<b>$exactTodayTokens</b>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        rv.setTextViewText(R.id.tv_exp_today_tokens, boldTodayTokens)

        // 3. Resolve Providers (Single vs Two)
        val providersList = resolveTopProviders(context, stats)

        if (providersList.size >= 2) {
            rv.setViewVisibility(R.id.layout_two_providers, View.VISIBLE)
            rv.setViewVisibility(R.id.layout_single_provider, View.GONE)

            val p1 = providersList[0]
            val p2 = providersList[1]

            val ring1 = generateProviderRingBitmap(context, p1.iconRes, p1.remainingPercent, p1.brandColor, isDarkMode)
            rv.setImageViewBitmap(R.id.iv_provider_1_ring, ring1)
            rv.setTextViewText(R.id.tv_provider_1_name, p1.name)
            rv.setTextViewText(R.id.tv_provider_1_quota, p1.quotaOrBalanceText)
            rv.setTextColor(R.id.tv_provider_1_quota, p1.brandColor)
            rv.setTextViewText(R.id.tv_provider_1_usage, p1.usageText)

            val ring2 = generateProviderRingBitmap(context, p2.iconRes, p2.remainingPercent, p2.brandColor, isDarkMode)
            rv.setImageViewBitmap(R.id.iv_provider_2_ring, ring2)
            rv.setTextViewText(R.id.tv_provider_2_name, p2.name)
            rv.setTextViewText(R.id.tv_provider_2_quota, p2.quotaOrBalanceText)
            rv.setTextColor(R.id.tv_provider_2_quota, p2.brandColor)
            rv.setTextViewText(R.id.tv_provider_2_usage, p2.usageText)
        } else if (providersList.size == 1) {
            rv.setViewVisibility(R.id.layout_two_providers, View.GONE)
            rv.setViewVisibility(R.id.layout_single_provider, View.VISIBLE)

            val p = providersList[0]
            val ring = generateProviderRingBitmap(context, p.iconRes, p.remainingPercent, p.brandColor, isDarkMode)
            rv.setImageViewBitmap(R.id.iv_single_provider_ring, ring)
            rv.setTextViewText(R.id.tv_single_provider_name, p.name)
            rv.setTextViewText(R.id.tv_single_provider_quota, p.quotaOrBalanceText)
            rv.setTextColor(R.id.tv_single_provider_quota, p.brandColor)
            rv.setTextViewText(R.id.tv_single_provider_usage, p.usageText)
        } else {
            // Fallback if completely empty
            rv.setViewVisibility(R.id.layout_two_providers, View.GONE)
            rv.setViewVisibility(R.id.layout_single_provider, View.VISIBLE)
            val ring = generateProviderRingBitmap(context, R.drawable.ic_brand_token_monitor, 100.0, 0xFF0284C7.toInt(), isDarkMode)
            rv.setImageViewBitmap(R.id.iv_single_provider_ring, ring)
            rv.setTextViewText(R.id.tv_single_provider_name, "PC Hub 实时监控")
            rv.setTextViewText(R.id.tv_single_provider_quota, "配额正常")
            rv.setTextColor(R.id.tv_single_provider_quota, 0xFF0284C7.toInt())
            val fallbackUsage: CharSequence = HtmlCompat.fromHtml(
                "已使用 <b>$exactTodayTokens</b> Token",
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            rv.setTextViewText(R.id.tv_single_provider_usage, fallbackUsage)
        }

        // Click behaviors:
        // All elements in the expanded view open MainActivity
        val clickIds = listOf(
            R.id.island_expanded_root,
            R.id.focus_card_root,
            R.id.iv_exp_app_logo,
            R.id.tv_exp_title_status,
            R.id.tv_exp_host_ip,
            R.id.tv_exp_update_time,
            R.id.tv_exp_today_label,
            R.id.tv_exp_today_compact,
            R.id.tv_exp_today_tokens,
            R.id.layout_two_providers,
            R.id.layout_single_provider,
            R.id.iv_provider_1_ring,
            R.id.iv_provider_2_ring,
            R.id.iv_single_provider_ring,
            R.id.tv_provider_1_name,
            R.id.tv_provider_2_name,
            R.id.tv_single_provider_name,
            R.id.tv_provider_1_quota,
            R.id.tv_provider_2_quota,
            R.id.tv_single_provider_quota,
            R.id.tv_provider_1_usage,
            R.id.tv_provider_2_usage,
            R.id.tv_single_provider_usage
        )
        for (id in clickIds) {
            try {
                rv.setOnClickPendingIntent(id, contentPendingIntent)
            } catch (_: Throwable) {}
        }

        return rv
    }
}
