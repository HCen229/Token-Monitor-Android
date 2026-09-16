package com.tokenmonitor.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tokenmonitor.app.MainActivity
import com.tokenmonitor.app.R
import com.tokenmonitor.app.data.IslandConfig
import com.tokenmonitor.app.data.IslandItemType
import com.tokenmonitor.app.data.TokenRepository
import com.tokenmonitor.app.data.TokenStats
import com.tokenmonitor.app.ui.i18n.getAppStrings
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

object TokenNotificationManager {

    // Stable channel IDs
    const val CHANNEL_ID_FOCUS = "token_monitor_live_channel"
    const val CHANNEL_ID_SERVICE = "token_monitor_service_channel"
    const val CHANNEL_ID_QUOTA_ALERT = "token_monitor_quota_alert"
    const val NOTIFICATION_ID = 10086
    private const val ALERT_NOTIFICATION_BASE_ID = 20000

    private val alertedQuotaConditions = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val QUOTA_ALERT_COOLDOWN_MS = 2 * 3600 * 1000L // 2 hours cooldown

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

    fun updateStats(context: Context, stats: TokenStats?, isConnected: Boolean) {
        try {
            if (stats != null) {
                cachedStats = stats
            } else if (cachedStats == null) {
                cachedStats = TokenRepository(context).getCachedStats()
            }
            cachedConnected = isConnected

            if (stats != null) {
                val currentTokens = stats.allTime.totalTokens
                if (lastTotalTokens > 0L && currentTokens > lastTotalTokens) {
                    lastTokenIncreaseTime = System.currentTimeMillis()
                }
                lastTotalTokens = currentTokens

                // Check and post system quota & low-balance alerts
                checkAndPostQuotaAlerts(context, stats)
            }

            if (isMonitoringActive) {
                postNotification(context)
            }
        } catch (e: Throwable) {
            android.util.Log.e("TokenNotification", "updateStats failed", e)
        }
    }

    fun checkAndPostQuotaAlerts(context: Context, stats: TokenStats) {
        if (!isPermissionGranted(context) || !areNotificationsEnabled(context)) return
        ensureChannels(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val strings = getAppStrings(context)
        val now = System.currentTimeMillis()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        for (provider in stats.providers) {
            val providerName = provider.provider.replaceFirstChar { it.uppercase() }
            val providerKey = provider.provider.lowercase().trim()

            // 1. Check window quota limits (remaining <= 20%)
            for (win in provider.windows) {
                if (!win.showMeter || win.kind.equals("billing", ignoreCase = true)) continue
                val percent = win.remainingPercent
                val alertKey = "${providerKey}_window_${win.kind.lowercase()}"
                if (percent in 0.0..20.0) {
                    val lastAlerted = alertedQuotaConditions[alertKey] ?: 0L
                    if (now - lastAlerted > QUOTA_ALERT_COOLDOWN_MS) {
                        alertedQuotaConditions[alertKey] = now
                        val winLabel = win.label.ifBlank { win.kind }
                        val title = strings.quotaAlertTitle(providerName)
                        val body = strings.quotaAlertWindowBody(providerName, winLabel, percent.toInt())
                        postSystemAlert(context, nm, alertKey.hashCode(), title, body, pendingIntent)
                    }
                } else if (percent > 25.0) {
                    alertedQuotaConditions.remove(alertKey)
                }
            }

            // 2. Check pay-as-you-go balance (balance <= 2.00 CNY, or <= $0.30 USD)
            if (provider.balanceAmount != null) {
                val amount = provider.balanceAmount
                val curr = provider.balanceCurrency.orEmpty().uppercase()
                val isLowBalance = when (curr) {
                    "USD" -> amount <= 0.30
                    else -> amount <= 2.00
                }

                val alertKey = "${providerKey}_balance"
                if (isLowBalance) {
                    val lastAlerted = alertedQuotaConditions[alertKey] ?: 0L
                    if (now - lastAlerted > QUOTA_ALERT_COOLDOWN_MS) {
                        alertedQuotaConditions[alertKey] = now
                        val formattedAmount = String.format(Locale.US, "%.2f", amount)
                        val title = strings.quotaAlertTitle(providerName)
                        val body = strings.quotaAlertBalanceBody(providerName, formattedAmount, provider.balanceCurrency ?: "CNY")
                        postSystemAlert(context, nm, alertKey.hashCode(), title, body, pendingIntent)
                    }
                } else {
                    val isRecovered = when (curr) {
                        "USD" -> amount > 0.40
                        else -> amount > 2.50
                    }
                    if (isRecovered) {
                        alertedQuotaConditions.remove(alertKey)
                    }
                }
            }
        }
    }

    private fun postSystemAlert(
        context: Context,
        nm: NotificationManager,
        hashKey: Int,
        title: String,
        body: String,
        pendingIntent: PendingIntent
    ) {
        val notifId = ALERT_NOTIFICATION_BASE_ID + Math.abs(hashKey % 5000)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID_QUOTA_ALERT)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        builder
            .setSmallIcon(R.drawable.ic_stat_token_monitor)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setCategory(Notification.CATEGORY_ALARM)
            .setColor(0xFFFF9F0A.toInt())
            .setStyle(Notification.BigTextStyle().bigText(body))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC)
        }

        nm.notify(notifId, builder.build())
    }

    fun postNotification(context: Context, force: Boolean = false) {
        try {
            ensureChannels(context)
            val now = System.currentTimeMillis()
            val isWorking = (lastTokenIncreaseTime > 0L && (now - lastTokenIncreaseTime) < WORKING_THRESHOLD_MS)
            val stats = cachedStats
            val config = getIslandConfig(context)
            val quotaVal = resolveAiQuotaValue(stats, config.selectedProvider, config.quotaMode)
            val fingerprint = "${cachedConnected}|${isWorking}|${stats?.today?.totalTokens}|${stats?.today?.costUsd}|" +
                    "${stats?.month?.totalTokens}|${config.leftItem}|${config.rightItem}|${config.showIcon}|" +
                    "${config.selectedProvider}|${config.quotaMode}|$quotaVal"

            if (!force && fingerprint == lastPostedFingerprint && (now - lastPostedTime) < 30_000L) {
                return // Throttled: data is identical and last posted within 30s
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
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
        // 2. Xiaomi HyperOS: uses custom dynamic capsule icon with composite drawing.
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
                leftCapsuleIcon
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

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID_FOCUS)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        builder
            .setSmallIcon(smallIcon)
            .setSubText(if (leftTitle.isNotBlank()) leftTitle else statusWord)
            .setContentTitle(statusTitle)
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setAutoCancel(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .setColor(accentColor)
            .setColorized(false)
            .setStyle(Notification.BigTextStyle().bigText(expandedBigText))

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

        // 2. Xiaomi HyperOS Super Island Focus Notification V3 JSON Payload (miui.focus.param)
        val focusIslandParam = try {
            JSONObject().apply {
                val paramIsland = JSONObject().apply {
                    put("islandProperty", 1) // Persistent island result
                    put("islandPriority", 2)
                    put("islandTimeout", 86400)
                    put("dismissIsland", false)
                    put("expandedTime", 0)
                    put("maxSize", false)
                    put("needCloseAnimation", true)

                    // Small Island (Compressed state on status bar)
                    put("smallIslandArea", JSONObject().apply {
                        put("picInfo", JSONObject().apply {
                            put("type", 1)
                            put("pic", "miui.focus.pic_small")
                        })
                    })

                    // Big Island: Left slot (Icon + Dot + Text composite) | Punch hole | Right slot (Right text)
                    put("bigIslandArea", JSONObject().apply {
                        put("imageTextInfoLeft", JSONObject().apply {
                            put("type", 1)
                            if (config.showIcon) {
                                put("picInfo", JSONObject().apply {
                                    put("type", 1)
                                    put("pic", "miui.focus.pic_icon")
                                })
                            }
                            if (leftTitle.isNotBlank()) {
                                put("textInfo", JSONObject().apply {
                                    put("title", leftTitle)
                                    put("colorTitle", "#FFFFFF")
                                    put("turnAnim", false)
                                    put("narrowFont", true)
                                    put("showHighlightColor", false)
                                })
                            }
                        })
                        put("imageTextInfoRight", JSONObject().apply {
                            put("type", 2)
                            put("textInfo", JSONObject().apply {
                                put("title", if (rightTitle.isNotBlank()) rightTitle else " ")
                                put("colorTitle", "#FFFFFF")
                                put("turnAnim", false)
                                put("narrowFont", true)
                                put("showHighlightColor", false)
                            })
                        })
                    })
                }

                val paramV2 = JSONObject().apply {
                    put("protocol", 1)
                    put("business", "tools")
                    put("enableFloat", true)
                    put("updatable", true)
                    put("islandFirstFloat", false)
                    put("isShowNotification", true)
                    put("ticker", if (combinedText.isNotBlank()) "$statusDot $combinedText" else statusTitle)
                    put("tickerPic", "miui.focus.pic_ticker")
                    put("timeout", 86400)

                    // BaseInfo for expanded card
                    put("baseInfo", JSONObject().apply {
                        put("type", 2)
                        put("title", statusTitle)
                        put("content", statusText)
                        put("colorTitle", "#000000")
                        put("colorTitleDark", "#FFFFFF")
                        put("colorContent", "#666666")
                        put("colorContentDark", "#B8B8B8")
                        put("showDivider", false)
                        put("showContentDivider", false)
                    })

                    // PicInfo for expanded card
                    put("picInfo", JSONObject().apply {
                        put("type", 1)
                        put("pic", "miui.focus.pic_icon")
                        put("picDark", "miui.focus.pic_icon")
                    })

                    // HintInfo for expanded card
                    put("hintInfo", JSONObject().apply {
                        put("type", 1)
                        put("title", if (leftTitle.isNotBlank()) leftTitle else "$statusDot $statusWord")
                        put("colorTitle", "#000000")
                        put("colorTitleDark", "#FFFFFF")
                        put("content", if (rightTitle.isNotBlank()) rightTitle else compactTokens)
                        put("colorContent", if (isWorking) "#30D158" else "#0A84FF")
                        put("colorContentDark", if (isWorking) "#30D158" else "#0A84FF")
                    })

                    put("param_island", paramIsland)
                }

                put("param_v2", paramV2)
            }.toString()
        } catch (_: Throwable) {
            null
        }

        // Bundle Xiaomi HyperOS SystemUI parcelable icons
        val picsBundle = Bundle().apply {
            putParcelable("miui.focus.pic_ticker", baseAppIcon)
            putParcelable("miui.focus.pic_small", leftCapsuleIcon)
            putParcelable("miui.focus.pic_icon", leftCapsuleIcon)
        }

        // Extras injection for HyperOS, ColorOS Fluid Cloud, and Android 16 / AOSP Live Updates
        val extras = Bundle().apply {
            if (isXiaomi) {
                if (focusIslandParam != null) {
                    putString("miui.focus.param", focusIslandParam)
                    putString("miui.focus.rv", focusIslandParam)
                }
                putBundle("miui.focus.pics", picsBundle)
                putBoolean("miui.enableFloat", true)
                putBoolean("miui.showFloat", true)
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
}
