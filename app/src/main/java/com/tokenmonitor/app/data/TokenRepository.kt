package com.tokenmonitor.app.data

import android.content.Context
import android.content.SharedPreferences
import com.tokenmonitor.app.data.provider.DirectProviderConfig
import com.tokenmonitor.app.data.provider.DirectProviderManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

class TokenRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("TokenMonitorPrefs", Context.MODE_PRIVATE)

    val directProviderManager = DirectProviderManager(context)

    fun getDirectProviderConfigs(): List<DirectProviderConfig> = directProviderManager.getConfigs()

    fun saveDirectProviderConfigs(configs: List<DirectProviderConfig>) = directProviderManager.saveConfigs(configs)

    suspend fun testDirectProvider(id: String, apiKey: String): Result<String> = directProviderManager.testProvider(id, apiKey)

    suspend fun exchangeCodexOAuth(code: String, codeVerifier: String): Result<DirectProviderConfig> =
        withContext(Dispatchers.IO) {
            directProviderManager.exchangeCodexOAuthCode(code, codeVerifier)
        }

    suspend fun fetchDirectProviders(forceRefresh: Boolean = false): List<ProviderLimit> = directProviderManager.fetchAllEnabled(forceRefresh = forceRefresh)

    fun getConfig(): ConnectionConfig {
        return ConnectionConfig(
            host = prefs.getString("host", "192.168.1.100") ?: "192.168.1.100",
            port = prefs.getInt("port", 17321),
            secret = prefs.getString("secret", "") ?: "",
            refreshIntervalSec = prefs.getInt("interval", 3)
        )
    }

    fun saveConfig(config: ConnectionConfig) {
        prefs.edit()
            .putString("host", config.host.trim())
            .putInt("port", config.port)
            .putString("secret", config.secret.trim())
            .putInt("interval", config.refreshIntervalSec)
            .apply()
    }

    fun getIslandConfig(): IslandConfig {
        val raw = prefs.getString("island_config", null)
        return IslandConfig.fromJson(raw)
    }

    fun saveIslandConfig(config: IslandConfig) {
        prefs.edit()
            .putString("island_config", config.toJson())
            .apply()
    }

    fun getThemeMode(): com.tokenmonitor.app.ui.theme.AppThemeMode {
        return com.tokenmonitor.app.ui.theme.AppThemeMode.DARK
    }

    fun saveThemeMode(mode: com.tokenmonitor.app.ui.theme.AppThemeMode) {
        prefs.edit()
            .putString("app_theme_mode", mode.id)
            .apply()
    }

    fun getLanguage(): AppLanguage {
        val raw = prefs.getString("app_language", AppLanguage.SYSTEM.id)
        return AppLanguage.fromId(raw)
    }

    fun saveLanguage(language: AppLanguage) {
        prefs.edit()
            .putString("app_language", language.id)
            .apply()
    }

    fun isEnglish(): Boolean {
        val lang = getLanguage()
        return when (lang) {
            AppLanguage.EN -> true
            AppLanguage.ZH -> false
            AppLanguage.SYSTEM -> {
                val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    context.resources.configuration.locales.get(0) ?: java.util.Locale.getDefault()
                } else {
                    @Suppress("DEPRECATION")
                    context.resources.configuration.locale ?: java.util.Locale.getDefault()
                }
                !locale.language.startsWith("zh", ignoreCase = true)
            }
        }
    }

    fun getProviderOrder(): List<String> {
        val raw = prefs.getString("provider_order", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i)
                if (item.isNotBlank()) list.add(item)
            }
            list
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun saveProviderOrder(order: List<String>) {
        val arr = org.json.JSONArray()
        order.forEach { arr.put(it) }
        prefs.edit()
            .putString("provider_order", arr.toString())
            .apply()
    }

    fun getQuotaDisplayStyle(): QuotaDisplayStyle {
        val raw = prefs.getString("quota_display_style", QuotaDisplayStyle.DUAL_RINGS.id)
        return QuotaDisplayStyle.fromId(raw)
    }

    fun saveQuotaDisplayStyle(style: QuotaDisplayStyle) {
        prefs.edit()
            .putString("quota_display_style", style.id)
            .apply()
    }

    fun getRingOrderConfig(): RingOrderConfig {
        val raw = prefs.getString("ring_order_config", RingOrderConfig.OUTER_5H_INNER_WEEKLY.id)
        return RingOrderConfig.fromId(raw)
    }

    fun saveRingOrderConfig(config: RingOrderConfig) {
        prefs.edit()
            .putString("ring_order_config", config.id)
            .apply()
    }

    fun getRingCenterTextConfig(): RingCenterTextConfig {
        val raw = prefs.getString("ring_center_text_config", RingCenterTextConfig.SESSION_5H.id)
        return RingCenterTextConfig.fromId(raw)
    }

    fun saveRingCenterTextConfig(config: RingCenterTextConfig) {
        prefs.edit()
            .putString("ring_center_text_config", config.id)
            .apply()
    }

    fun getCachedStats(): TokenStats? {
        val raw = prefs.getString("cached_stats_json", null) ?: return null
        return try {
            val json = JSONObject(raw)
            parseStatsJson(json)
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun fetchStats(forceDirectRefresh: Boolean = false): Result<TokenStats> = withContext(Dispatchers.IO) {
        val config = getConfig()
        val url = "http://${config.host}:${config.port}/api/stats"

        val directEnabled = directProviderManager.hasEnabledProviders()
        val directDeferred: Deferred<List<ProviderLimit>>? = if (directEnabled) {
            async {
                try {
                    directProviderManager.fetchAllEnabled(forceRefresh = forceDirectRefresh)
                } catch (_: Throwable) {
                    emptyList()
                }
            }
        } else null

        var conn: HttpURLConnection? = null
        var shouldDisconnect = false
        var hubStats: TokenStats? = null
        var hubError: Exception? = null
        var rawBodyStr: String? = null

        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 4000
                setRequestProperty("Authorization", "Bearer ${config.secret}")
                setRequestProperty("x-token-monitor-secret", config.secret)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Connection", "Keep-Alive")
                instanceFollowRedirects = true
            }

            val code = conn.responseCode
            val en = isEnglish()
            if (code == 200) {
                rawBodyStr = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val json = JSONObject(rawBodyStr)
                hubStats = parseStatsJson(json)
            } else if (code == 401 || code == 403) {
                shouldDisconnect = true
                val err = if (en) "Authentication failed (HTTP $code): Secret key is invalid." else "身份验证失败 (HTTP $code)：共享密钥不正确，请核对。"
                hubError = Exception(err)
            } else {
                shouldDisconnect = true
                val err = if (en) "Server returned error status code HTTP $code" else "服务端返回错误状态码 HTTP $code"
                hubError = Exception(err)
            }
        } catch (e: Exception) {
            shouldDisconnect = true
            val err = e.message ?: e.toString()
            val en = isEnglish()
            val msg = if (en) "Cannot connect to http://${config.host}:${config.port}\nDetails: $err" else "无法连接至 http://${config.host}:${config.port}\n错误详情: $err"
            hubError = Exception(msg)
        } finally {
            if (shouldDisconnect) {
                try {
                    conn?.disconnect()
                } catch (_: Throwable) {
                }
            }
        }

        val directProviders = directDeferred?.await().orEmpty()

        if (hubStats != null) {
            val finalProviders = mergeProviders(hubStats.providers, directProviders)
            val finalStats = hubStats.copy(providers = finalProviders)
            saveStatsToCache(finalStats, rawBodyStr)
            Result.success(finalStats)
        } else {
            val cached = getCachedStats()
            if (directProviders.isNotEmpty()) {
                val base = cached ?: TokenStats()
                val finalProviders = mergeProviders(base.providers, directProviders)
                val finalStats = base.copy(providers = finalProviders)
                saveStatsToCache(finalStats, null)
            }
            Result.failure(hubError ?: Exception("Unknown error"))
        }
    }

    private fun mergeProviders(
        base: List<ProviderLimit>,
        direct: List<ProviderLimit>
    ): List<ProviderLimit> {
        if (direct.isEmpty()) return base
        val result = base.toMutableList()
        for (dp in direct) {
            val idx = result.indexOfFirst { it.provider.equals(dp.provider, ignoreCase = true) }
            if (idx >= 0) {
                val existing = result[idx]
                if (existing.windows.isEmpty() || dp.windows.isNotEmpty()) {
                    result[idx] = dp
                }
            } else {
                result.add(dp)
            }
        }
        return result
    }

    fun saveStatsToCache(stats: TokenStats, originalJsonStr: String? = null) {
        try {
            val json = if (originalJsonStr != null) {
                JSONObject(originalJsonStr)
            } else {
                val raw = prefs.getString("cached_stats_json", null)
                if (raw != null) JSONObject(raw) else JSONObject()
            }
            val limitsObj = json.optJSONObject("limits") ?: JSONObject()
            val provArr = JSONArray()
            for (p in stats.providers) {
                val pObj = JSONObject().apply {
                    put("provider", p.provider)
                    put("accountLabel", p.accountLabel)
                    if (p.balanceAmount != null) {
                        put("balance", JSONObject().apply {
                            put("amount", p.balanceAmount)
                            put("currency", p.balanceCurrency ?: "CNY")
                        })
                    }
                    if (p.resetCreditsCount != null) put("resetCreditsCount", p.resetCreditsCount)
                    if (p.resetCreditsExpiry != null) put("resetCreditsExpiry", p.resetCreditsExpiry)
                    if (p.resetCreditsDescription != null) put("resetCreditsDescription", p.resetCreditsDescription)
                    val winArr = JSONArray()
                    for (w in p.windows) {
                        winArr.put(JSONObject().apply {
                            put("kind", w.kind)
                            put("label", w.label)
                            put("usedPercent", w.usedPercent)
                            put("remainingPercent", w.remainingPercent)
                            if (w.resetsAt != null) put("resetsAt", w.resetsAt)
                            if (w.resetDescription != null) put("resetDescription", w.resetDescription)
                            put("showMeter", w.showMeter)
                        })
                    }
                    put("windows", winArr)
                }
                provArr.put(pObj)
            }
            limitsObj.put("providers", provArr)
            json.put("limits", limitsObj)
            prefs.edit().putString("cached_stats_json", json.toString()).apply()
        } catch (_: Throwable) {
        }
    }

    suspend fun runDiagnostics(host: String, port: Int, secret: String): DiagnosticResult = withContext(Dispatchers.IO) {
        val en = isEnglish()
        val resultSb = StringBuilder()
        var tcpLatency = 0L
        var httpCode = 0

        // 1. TCP Port test
        val t0 = System.currentTimeMillis()
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 2500)
            socket.close()
            tcpLatency = System.currentTimeMillis() - t0
            if (en) {
                resultSb.append("[1/2] Port connected successfully (TCP handshake: ${tcpLatency}ms)\n")
            } else {
                resultSb.append("[1/2] 端口连通正常 (TCP 握手耗时: ${tcpLatency}ms)\n")
            }
        } catch (e: Exception) {
            val errMsg = if (en) {
                "[1/2] Cannot connect to TCP port (${host}:${port})\nReason: ${e.message ?: e.toString()}\n\n" +
                        "Troubleshooting:\n" +
                        "1. Ensure phone and computer are connected to the same Wi-Fi router.\n" +
                        "2. Ensure Token Monitor is running on the computer.\n" +
                        "3. Check computer firewall for port 17321."
            } else {
                "[1/2] TCP 端口无法连接 (${host}:${port})\n原因: ${e.message ?: e.toString()}\n\n" +
                        "排查建议：\n" +
                        "1. 确认手机 Wi-Fi 与电脑连接在同一局域网路由器下。\n" +
                        "2. 确认电脑端已启动 Token Monitor。\n" +
                        "3. 电脑防火墙是否放行 17321 端口 (运行桌面 open-firewall.bat)。"
            }
            return@withContext DiagnosticResult(
                isTesting = false,
                success = false,
                message = errMsg
            )
        }

        // 2. HTTP Request test
        var conn: HttpURLConnection? = null
        try {
            val url = "http://${host}:${port}/api/stats"
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
                setRequestProperty("Authorization", "Bearer $secret")
                setRequestProperty("x-token-monitor-secret", secret)
                setRequestProperty("Accept", "application/json")
            }

            httpCode = conn.responseCode
            if (httpCode == 200) {
                if (en) {
                    resultSb.append("[2/2] HTTP 200 OK, live data fetched successfully!\nLAN communication is completely healthy.")
                } else {
                    resultSb.append("[2/2] HTTP 200 验证通过，已成功拉取实时数据！\n局域网通讯完全健康。")
                }
                DiagnosticResult(
                    isTesting = false,
                    success = true,
                    message = resultSb.toString(),
                    tcpLatencyMs = tcpLatency,
                    httpStatus = httpCode
                )
            } else if (httpCode == 401 || httpCode == 403) {
                if (en) {
                    resultSb.append("[2/2] Port connected, but secret verification failed (HTTP $httpCode)\nPlease verify the shared secret key.")
                } else {
                    resultSb.append("[2/2] 端口已通，但密钥校验失败 (HTTP $httpCode)\n请核对电脑 Token Monitor 设置里的共享密钥。")
                }
                DiagnosticResult(
                    isTesting = false,
                    success = false,
                    message = resultSb.toString(),
                    tcpLatencyMs = tcpLatency,
                    httpStatus = httpCode
                )
            } else {
                if (en) {
                    resultSb.append("[2/2] Server returned HTTP $httpCode")
                } else {
                    resultSb.append("[2/2] 服务端返回状态码 HTTP $httpCode")
                }
                DiagnosticResult(
                    isTesting = false,
                    success = false,
                    message = resultSb.toString(),
                    tcpLatencyMs = tcpLatency,
                    httpStatus = httpCode
                )
            }
        } catch (e: Exception) {
            val failMsg = if (en) "[2/2] HTTP request failed: ${e.message ?: e.toString()}" else "[2/2] HTTP 请求失败: ${e.message ?: e.toString()}"
            resultSb.append(failMsg)
            DiagnosticResult(
                isTesting = false,
                success = false,
                message = resultSb.toString(),
                tcpLatencyMs = tcpLatency
            )
        } finally {
            conn?.disconnect()
        }
    }

    private fun parsePeriodData(periodObj: JSONObject?): PeriodData {
        if (periodObj == null) return PeriodData()
        val totalTokens = periodObj.optLong("totalTokens", 0L)
        val costUsd = periodObj.optDouble("costUsd", 0.0)

        var messageCount = 0
        var sessionCount = 0
        val sessions = periodObj.optJSONObject("sessions")
        if (sessions != null) {
            val it = sessions.keys()
            while (it.hasNext()) {
                sessionCount++
                val sess = sessions.optJSONObject(it.next())
                if (sess != null) {
                    messageCount += sess.optInt("messageCount", 0)
                }
            }
        }

        // Parse models
        val modelsList = mutableListOf<ModelStat>()
        val clientModels = periodObj.optJSONObject("clientModels")
        val clientCosts = periodObj.optJSONObject("clientModelCosts")
        if (clientModels != null) {
            val cKeys = clientModels.keys()
            while (cKeys.hasNext()) {
                val client = cKeys.next()
                val mObj = clientModels.optJSONObject(client)
                val cObj = clientCosts?.optJSONObject(client)
                if (mObj != null) {
                    val mKeys = mObj.keys()
                    while (mKeys.hasNext()) {
                        val model = mKeys.next()
                        val t = mObj.optLong(model, 0L)
                        val c = cObj?.optDouble(model, 0.0) ?: 0.0
                        val share = if (totalTokens > 0) (t.toFloat() / totalTokens).coerceIn(0f, 1f) else 0f
                        modelsList.add(ModelStat(client, model, t, c, share))
                    }
                }
            }
        } else {
            val models = periodObj.optJSONObject("models")
            val costs = periodObj.optJSONObject("modelCosts")
            if (models != null) {
                val it = models.keys()
                while (it.hasNext()) {
                    val model = it.next()
                    val t = models.optLong(model, 0L)
                    val c = costs?.optDouble(model, 0.0) ?: 0.0
                    val share = if (totalTokens > 0) (t.toFloat() / totalTokens).coerceIn(0f, 1f) else 0f
                    modelsList.add(ModelStat("all", model, t, c, share))
                }
            }
        }
        modelsList.sortByDescending { it.tokens }

        // Parse clients / tools
        val clientsList = mutableListOf<ClientStat>()
        val clients = periodObj.optJSONObject("clients")
        val clientCostsMap = periodObj.optJSONObject("clientCosts")
        if (clients != null) {
            val it = clients.keys()
            while (it.hasNext()) {
                val client = it.next()
                val t = clients.optLong(client, 0L)
                val c = clientCostsMap?.optDouble(client, 0.0) ?: 0.0
                val share = if (totalTokens > 0) (t.toFloat() / totalTokens).coerceIn(0f, 1f) else 0f
                clientsList.add(ClientStat(client, t, c, share))
            }
        }
        clientsList.sortByDescending { it.tokens }

        return PeriodData(
            totalTokens = totalTokens,
            costUsd = costUsd,
            messageCount = messageCount,
            sessionCount = sessionCount,
            models = modelsList,
            clients = clientsList
        )
    }

    private fun parseStatsJson(json: JSONObject): TokenStats {
        val periods = json.optJSONObject("periods")
        val todayData = parsePeriodData(periods?.optJSONObject("today"))
        val monthData = parsePeriodData(periods?.optJSONObject("month"))
        val allTimeData = parsePeriodData(periods?.optJSONObject("allTime"))

        // History summary
        var activeDays = 0
        var currentStreak = 0
        val dailyList = mutableListOf<DailyHistory>()

        val histPreview = json.optJSONObject("historyPreview")
        var peakDailyTokens = 0L
        if (histPreview != null) {
            val summary = histPreview.optJSONObject("summary")
            if (summary != null) {
                activeDays = summary.optInt("activeDays", 0)
                currentStreak = summary.optInt("currentStreak", 0)
                peakDailyTokens = summary.optLong("peakDayTokens", 0L)
            }

            val daily = histPreview.optJSONArray("daily")
            if (daily != null) {
                for (i in 0 until daily.length()) {
                    val d = daily.optJSONObject(i) ?: continue
                    dailyList.add(
                        DailyHistory(
                            date = d.optString("date", ""),
                            tokens = d.optLong("tokens", 0L),
                            cost = d.optDouble("cost", 0.0)
                        )
                    )
                }
            }
        }
        if (peakDailyTokens <= 0L) {
            peakDailyTokens = dailyList.maxOfOrNull { it.tokens } ?: 0L
        }

        // Provider Limits & Quotas
        val providerList = mutableListOf<ProviderLimit>()
        val limits = json.optJSONObject("limits")
        if (limits != null) {
            val providers = limits.optJSONArray("providers")
            if (providers != null) {
                for (i in 0 until providers.length()) {
                    val p = providers.optJSONObject(i) ?: continue
                    val pName = p.optString("provider", "Tool")
                    val label = p.optString("accountLabel", "")
                    val windows = p.optJSONArray("windows")
                    val winList = mutableListOf<WindowLimit>()

                    if (windows != null) {
                        for (w in 0 until windows.length()) {
                            val win = windows.optJSONObject(w) ?: continue
                            val kind = win.optString("kind", "quota")
                            val isBilling = kind.equals("billing", ignoreCase = true) || win.optString("label").contains("balance", ignoreCase = true)
                            val remPercent = if (win.has("remainingPercent") && !win.isNull("remainingPercent")) {
                                win.optDouble("remainingPercent", 100.0)
                            } else if (isBilling) {
                                -1.0
                            } else {
                                100.0
                            }
                            winList.add(
                                WindowLimit(
                                    kind = kind,
                                    label = win.optString("label", win.optString("kind", "Quota")),
                                    usedPercent = win.optDouble("usedPercent", 0.0),
                                    remainingPercent = remPercent,
                                    resetsAt = if (win.has("resetsAt") && !win.isNull("resetsAt")) win.optString("resetsAt") else null,
                                    resetDescription = if (win.has("resetDescription") && !win.isNull("resetDescription")) win.optString("resetDescription") else null,
                                    showMeter = if (isBilling) false else win.optBoolean("showMeter", true)
                                )
                            )
                        }
                    }

                    var balAmt: Double? = null
                    var balCurr: String? = null
                    val bal = p.optJSONObject("balance")
                    if (bal != null) {
                        if (bal.has("amount") && !bal.isNull("amount")) {
                            balAmt = bal.optDouble("amount")
                        }
                        if (bal.has("currency") && !bal.isNull("currency")) {
                            balCurr = bal.optString("currency")
                        }
                    }
                    if (balAmt == null && p.has("balanceUsd") && !p.isNull("balanceUsd")) {
                        balAmt = p.optDouble("balanceUsd")
                        balCurr = "USD"
                    }
                    if (balAmt == null && windows != null) {
                        for (w in 0 until windows.length()) {
                            val win = windows.optJSONObject(w) ?: continue
                            val k = win.optString("kind")
                            val l = win.optString("label")
                            if (k.equals("billing", true) || l.contains("balance", true)) {
                                if (win.has("remaining") && !win.isNull("remaining")) {
                                    balAmt = win.optDouble("remaining")
                                    balCurr = win.optString("currency", "CNY")
                                    break
                                }
                            }
                        }
                    }

                    val resetCount = if (p.has("resetCreditsCount")) p.optInt("resetCreditsCount") else null
                    val resetExpiry = if (p.has("resetCreditsExpiry") && !p.isNull("resetCreditsExpiry")) p.optString("resetCreditsExpiry") else null
                    val resetDesc = if (p.has("resetCreditsDescription") && !p.isNull("resetCreditsDescription")) p.optString("resetCreditsDescription") else null

                    providerList.add(
                        ProviderLimit(
                            provider = pName,
                            accountLabel = label,
                            windows = winList,
                            balanceAmount = balAmt,
                            balanceCurrency = balCurr,
                            resetCreditsCount = resetCount,
                            resetCreditsExpiry = resetExpiry,
                            resetCreditsDescription = resetDesc
                        )
                    )
                }
            }
        }

        return TokenStats(
            today = todayData,
            month = monthData,
            allTime = allTimeData,
            daily = dailyList,
            peakDailyTokens = peakDailyTokens,
            providers = providerList,
            activeDays = activeDays,
            currentStreak = currentStreak
        )
    }
}

