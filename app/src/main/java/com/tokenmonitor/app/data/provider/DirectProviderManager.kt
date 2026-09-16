package com.tokenmonitor.app.data.provider

import android.content.Context
import android.util.Base64
import com.tokenmonitor.app.data.ProviderLimit
import com.tokenmonitor.app.data.WindowLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

data class DirectProviderConfig(
    val id: String,
    val name: String,
    val apiKey: String = "",
    val enabled: Boolean = false,
    val extra: Map<String, String> = emptyMap()
)

class DirectProviderManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("tm_direct_providers_prefs", Context.MODE_PRIVATE)

    companion object {
        const val PROVIDER_CODEX = "codex"
        const val PROVIDER_DEEPSEEK = "deepseek"
        const val PROVIDER_OPENROUTER = "openrouter"
        const val PROVIDER_MINIMAX = "minimax"
        const val PROVIDER_KIMI = "kimi"
        const val PROVIDER_ZAI = "zai"

        val DEFAULT_PROVIDERS = listOf(
            DirectProviderConfig(PROVIDER_CODEX, "Codex"),
            DirectProviderConfig(PROVIDER_DEEPSEEK, "DeepSeek"),
            DirectProviderConfig(PROVIDER_OPENROUTER, "OpenRouter"),
            DirectProviderConfig(PROVIDER_MINIMAX, "MiniMax"),
            DirectProviderConfig(PROVIDER_KIMI, "Kimi"),
            DirectProviderConfig(PROVIDER_ZAI, "GLM")
        )
    }

    object PkceHelper {
        const val CODEX_CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        const val CODEX_REDIRECT_URI = "http://localhost:1455/auth/callback"
        const val CODEX_AUTH_URL = "https://auth.openai.com/oauth/authorize"
        const val CODEX_TOKEN_URL = "https://auth.openai.com/oauth/token"

        fun generateCodeVerifier(): String {
            val secureRandom = SecureRandom()
            val codeVerifier = ByteArray(32)
            secureRandom.nextBytes(codeVerifier)
            return Base64.encodeToString(codeVerifier, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }

        fun generateCodeChallenge(codeVerifier: String): String {
            val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(bytes, 0, bytes.size)
            val digest = messageDigest.digest()
            return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }

        fun buildAuthorizeUrl(codeChallenge: String, state: String = "token_monitor"): String {
            val params = listOf(
                "client_id" to CODEX_CLIENT_ID,
                "response_type" to "code",
                "redirect_uri" to CODEX_REDIRECT_URI,
                "scope" to "openid profile email offline_access",
                "code_challenge" to codeChallenge,
                "code_challenge_method" to "S256",
                "state" to state
            )
            val query = params.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
            return "$CODEX_AUTH_URL?$query"
        }
    }

    fun getConfigs(): List<DirectProviderConfig> {
        val raw = prefs.getString("configs_json", null) ?: return DEFAULT_PROVIDERS
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<DirectProviderConfig>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                val name = obj.optString("name")
                val key = obj.optString("apiKey")
                val enabled = obj.optBoolean("enabled", false)
                val extraObj = obj.optJSONObject("extra")
                val extraMap = mutableMapOf<String, String>()
                if (extraObj != null) {
                    val keys = extraObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        extraMap[k] = extraObj.optString(k)
                    }
                }
                list.add(DirectProviderConfig(id = id, name = name, apiKey = key, enabled = enabled, extra = extraMap))
            }
            // Ensure all default providers exist
            val existingIds = list.map { it.id }.toSet()
            val merged = list.toMutableList()
            for (def in DEFAULT_PROVIDERS) {
                if (def.id !in existingIds) {
                    merged.add(def)
                }
            }
            merged
        } catch (_: Exception) {
            DEFAULT_PROVIDERS
        }
    }

    fun saveConfigs(configs: List<DirectProviderConfig>) {
        val arr = JSONArray()
        for (c in configs) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("apiKey", c.apiKey)
                put("enabled", c.enabled)
                if (c.extra.isNotEmpty()) {
                    val extraObj = JSONObject()
                    c.extra.forEach { (k, v) -> extraObj.put(k, v) }
                    put("extra", extraObj)
                }
            }
            arr.put(obj)
        }
        prefs.edit().putString("configs_json", arr.toString()).apply()
        invalidateCache()
    }

    fun updateConfig(updated: DirectProviderConfig) {
        val configs = getConfigs().map {
            if (it.id == updated.id) updated else it
        }
        saveConfigs(configs)
    }

    @Volatile
    private var cachedDirectProviders: List<ProviderLimit> = emptyList()
    @Volatile
    private var lastFetchTimeMs: Long = 0L

    fun invalidateCache() {
        cachedDirectProviders = emptyList()
        lastFetchTimeMs = 0L
    }

    fun hasEnabledProviders(): Boolean {
        return getConfigs().any { it.enabled && it.apiKey.isNotBlank() }
    }

    suspend fun fetchAllEnabled(
        configs: List<DirectProviderConfig>? = null,
        forceRefresh: Boolean = false
    ): List<ProviderLimit> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedDirectProviders.isNotEmpty() && (now - lastFetchTimeMs) < 60_000L) {
            return@withContext cachedDirectProviders
        }

        val activeConfigs = (configs ?: getConfigs()).filter { it.enabled && it.apiKey.isNotBlank() }
        if (activeConfigs.isEmpty()) {
            cachedDirectProviders = emptyList()
            return@withContext emptyList()
        }

        val results = activeConfigs.map { cfg ->
            async {
                fetchProvider(cfg).getOrNull()
            }
        }.awaitAll().filterNotNull()

        if (results.isNotEmpty()) {
            cachedDirectProviders = results
            lastFetchTimeMs = now
        }
        results
    }

    suspend fun fetchProvider(config: DirectProviderConfig): Result<ProviderLimit> = withContext(Dispatchers.IO) {
        val key = config.apiKey.trim()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key is empty"))
        }

        try {
            when (config.id.lowercase()) {
                PROVIDER_CODEX -> fetchCodex(config)
                PROVIDER_DEEPSEEK -> fetchDeepSeek(key)
                PROVIDER_OPENROUTER -> fetchOpenRouter(key)
                PROVIDER_MINIMAX -> fetchMiniMax(key)
                PROVIDER_KIMI -> fetchKimi(key)
                PROVIDER_ZAI -> fetchZai(key)
                else -> Result.failure(IllegalArgumentException("Unsupported provider: ${config.id}"))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun testProvider(id: String, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key 不能为空"))
        }
        val dummyConfig = DirectProviderConfig(id = id, name = id, apiKey = key, enabled = true)
        val fetchRes = fetchProvider(dummyConfig)
        if (fetchRes.isFailure) {
            val err = fetchRes.exceptionOrNull()
            return@withContext Result.failure(err ?: Exception("连接测试失败"))
        }

        val limit = fetchRes.getOrThrow()
        val summary = buildString {
            if (limit.balanceAmount != null) {
                val symbol = when (limit.balanceCurrency?.uppercase()) {
                    "CNY", "RMB" -> "¥"
                    "EUR" -> "€"
                    "GBP" -> "£"
                    else -> "$"
                }
                append(String.format(Locale.US, "余额 %s%.2f %s", symbol, limit.balanceAmount, limit.balanceCurrency.orEmpty()))
            }
            val nonBillingWindows = limit.windows.filter { !it.kind.equals("billing", ignoreCase = true) }
            if (nonBillingWindows.isNotEmpty()) {
                if (isNotEmpty()) append(" · ")
                append(nonBillingWindows.joinToString(" | ") { "${it.label}剩 ${it.remainingPercent.toInt()}%" })
            }
            if (!limit.resetCreditsDescription.isNullOrBlank()) {
                if (isNotEmpty()) append(" · ")
                append(limit.resetCreditsDescription)
            }
        }.ifEmpty { "连通成功 · 状态正常" }

        Result.success(summary)
    }

    // =========================================================================
    // 1. DeepSeek: GET https://api.deepseek.com/user/balance
    // =========================================================================
    private fun fetchDeepSeek(apiKey: String): Result<ProviderLimit> {
        val (code, body) = httpGet(
            urlStr = "https://api.deepseek.com/user/balance",
            headers = mapOf(
                "Authorization" to "Bearer $apiKey",
                "Accept" to "application/json"
            )
        )
        if (code !in 200..299) {
            return Result.failure(Exception("HTTP $code: $body"))
        }

        val json = JSONObject(body)
        val balanceInfos = json.optJSONArray("balance_infos") ?: return Result.failure(Exception("返回格式异常: 缺少 balance_infos"))

        var selectedAmount = 0.0
        var selectedCurrency = "CNY"
        var foundFunded = false

        for (i in 0 until balanceInfos.length()) {
            val row = balanceInfos.optJSONObject(i) ?: continue
            val curr = row.optString("currency", "CNY").uppercase()
            val total = row.optDouble("total_balance", 0.0)
            if (total > 0.0 && !foundFunded) {
                selectedAmount = total
                selectedCurrency = curr
                foundFunded = true
            } else if (!foundFunded && (curr == "CNY" || curr == "USD")) {
                selectedAmount = total
                selectedCurrency = curr
            }
        }

        val limit = ProviderLimit(
            provider = "deepseek",
            accountLabel = "Pay-as-you-go",
            windows = emptyList(),
            balanceAmount = selectedAmount,
            balanceCurrency = selectedCurrency
        )
        return Result.success(limit)
    }

    // =========================================================================
    // 2. OpenRouter: GET https://openrouter.ai/api/v1/credits & /key
    // =========================================================================
    private fun fetchOpenRouter(apiKey: String): Result<ProviderLimit> {
        val commonHeaders = mapOf(
            "Authorization" to "Bearer $apiKey",
            "Accept" to "application/json",
            "HTTP-Referer" to "https://github.com/hcen229/Token-Monitor-Android",
            "X-OpenRouter-Title" to "Token Monitor"
        )

        val (credCode, credBody) = httpGet("https://openrouter.ai/api/v1/credits", commonHeaders)
        if (credCode !in 200..299) {
            return Result.failure(Exception("HTTP $credCode: $credBody"))
        }

        val credJson = JSONObject(credBody)
        val dataObj = credJson.optJSONObject("data")
        val totalCredits = dataObj?.optDouble("total_credits", 0.0) ?: 0.0
        val totalUsage = dataObj?.optDouble("total_usage", 0.0) ?: 0.0
        val remainingCredits = max(0.0, totalCredits - totalUsage)

        val windows = mutableListOf<WindowLimit>()

        // Try reading key limits
        var accountLabel = "OpenRouter"
        try {
            val (keyCode, keyBody) = httpGet("https://openrouter.ai/api/v1/key", commonHeaders)
            if (keyCode in 200..299) {
                val keyJson = JSONObject(keyBody).optJSONObject("data")
                if (keyJson != null) {
                    val label = keyJson.optString("label", "")
                    if (label.isNotBlank()) accountLabel = label

                    val limit = keyJson.optDouble("limit", -1.0)
                    if (limit > 0.0) {
                        val usage = keyJson.optDouble("usage", 0.0)
                        val limitRemaining = keyJson.optDouble("limit_remaining", max(0.0, limit - usage))
                        val reset = keyJson.optString("limit_reset", "").lowercase()
                        val kind = when (reset) {
                            "daily" -> "session"
                            "weekly" -> "weekly"
                            else -> "billing"
                        }
                        val winLabel = when (reset) {
                            "daily" -> "Daily limit"
                            "weekly" -> "Weekly limit"
                            else -> "API key limit"
                        }
                        val remPct = max(0.0, min(100.0, (limitRemaining / limit) * 100.0))
                        windows.add(
                            WindowLimit(
                                kind = kind,
                                label = winLabel,
                                usedPercent = max(0.0, 100.0 - remPct),
                                remainingPercent = remPct,
                                resetsAt = null,
                                resetDescription = null,
                                showMeter = true
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        val limit = ProviderLimit(
            provider = "openrouter",
            accountLabel = accountLabel,
            windows = windows,
            balanceAmount = remainingCredits,
            balanceCurrency = "USD"
        )
        return Result.success(limit)
    }

    // =========================================================================
    // 3. MiniMax: GET https://api.minimaxi.com/v1/api/openplatform/coding_plan/remains
    // =========================================================================
    private fun fetchMiniMax(apiKey: String): Result<ProviderLimit> {
        val headers = mapOf(
            "Authorization" to "Bearer $apiKey",
            "Accept" to "application/json"
        )

        var codeAndBody = httpGet("https://api.minimaxi.com/v1/api/openplatform/coding_plan/remains", headers)
        if (codeAndBody.first !in 200..299) {
            // fallback to global endpoint
            codeAndBody = httpGet("https://api.minimax.io/v1/api/openplatform/coding_plan/remains", headers)
        }

        val (code, body) = codeAndBody
        if (code !in 200..299) {
            return Result.failure(Exception("HTTP $code: $body"))
        }

        val json = JSONObject(body)
        val dataObj = json.optJSONObject("data")
        val modelRemains = dataObj?.optJSONArray("model_remains") ?: json.optJSONArray("model_remains")
            ?: return Result.failure(Exception("返回格式异常: 未找到 model_remains"))

        var generalRow: JSONObject? = null
        for (i in 0 until modelRemains.length()) {
            val row = modelRemains.optJSONObject(i) ?: continue
            if (row.optString("model_name") == "general") {
                generalRow = row
                break
            }
        }
        if (generalRow == null && modelRemains.length() > 0) {
            generalRow = modelRemains.optJSONObject(0)
        }
        if (generalRow == null) {
            return Result.failure(Exception("未找到生效的 MiniMax Coding Plan 配额数据"))
        }

        val windows = mutableListOf<WindowLimit>()

        // 5h session window
        val intervalRemainStr = generalRow.optString("current_interval_remaining_percent", "")
        val intervalRemain = intervalRemainStr.toDoubleOrNull()
        val intervalEndTime = generalRow.optLong("end_time", 0L)
        if (intervalRemain != null) {
            windows.add(
                WindowLimit(
                    kind = "session",
                    label = "5h",
                    usedPercent = max(0.0, 100.0 - intervalRemain),
                    remainingPercent = max(0.0, min(100.0, intervalRemain)),
                    resetsAt = formatEpochMillis(intervalEndTime),
                    resetDescription = null,
                    showMeter = true
                )
            )
        }

        // Weekly window
        val weeklyRemainStr = generalRow.optString("current_weekly_remaining_percent", "")
        val weeklyRemain = weeklyRemainStr.toDoubleOrNull()
        val weeklyEndTime = generalRow.optLong("weekly_end_time", 0L)
        if (weeklyRemain != null) {
            windows.add(
                WindowLimit(
                    kind = "weekly",
                    label = "Weekly",
                    usedPercent = max(0.0, 100.0 - weeklyRemain),
                    remainingPercent = max(0.0, min(100.0, weeklyRemain)),
                    resetsAt = formatEpochMillis(weeklyEndTime),
                    resetDescription = null,
                    showMeter = true
                )
            )
        }

        val limit = ProviderLimit(
            provider = "minimax",
            accountLabel = "Coding Plan",
            windows = windows,
            balanceAmount = null,
            balanceCurrency = null
        )
        return Result.success(limit)
    }

    // =========================================================================
    // 4. Kimi: GET https://api.kimi.com/coding/v1/usages
    // =========================================================================
    private fun fetchKimi(apiKey: String): Result<ProviderLimit> {
        val headers = mapOf(
            "Authorization" to "Bearer $apiKey",
            "Accept" to "application/json"
        )
        val (code, body) = httpGet("https://api.kimi.com/coding/v1/usages", headers)
        if (code !in 200..299) {
            return Result.failure(Exception("HTTP $code: $body"))
        }

        val json = JSONObject(body)
        val dataObj = if (json.has("data") && json.optJSONObject("data") != null) json.optJSONObject("data")!! else json
        val windows = mutableListOf<WindowLimit>()

        // Weekly quota in top-level `usage`
        val usageObj = dataObj.optJSONObject("usage")
        if (usageObj != null) {
            val used = usageObj.optDouble("used", -1.0)
            val limitVal = usageObj.optDouble("limit", -1.0)
            val rem = usageObj.optDouble("remaining", -1.0)
            val resetAt = usageObj.optString("reset_at", usageObj.optString("resetAt", ""))

            var remPct: Double? = null
            if (limitVal > 0 && rem >= 0) {
                remPct = (rem / limitVal) * 100.0
            } else if (limitVal > 0 && used >= 0) {
                remPct = ((limitVal - used) / limitVal) * 100.0
            }

            if (remPct != null) {
                windows.add(
                    WindowLimit(
                        kind = "weekly",
                        label = "Weekly",
                        usedPercent = max(0.0, 100.0 - remPct),
                        remainingPercent = max(0.0, min(100.0, remPct)),
                        resetsAt = resetAt.ifBlank { null },
                        resetDescription = null,
                        showMeter = true
                    )
                )
            }
        }

        // 5h Session quota in `limits`
        val limitsArr = dataObj.optJSONArray("limits")
        if (limitsArr != null) {
            for (i in 0 until limitsArr.length()) {
                val item = limitsArr.optJSONObject(i) ?: continue
                val detail = item.optJSONObject("detail") ?: item
                val win = item.optJSONObject("window")

                val duration = win?.optInt("duration", 0) ?: 0
                val used = detail.optDouble("used", -1.0)
                val limitVal = detail.optDouble("limit", -1.0)
                val rem = detail.optDouble("remaining", -1.0)
                val resetAt = detail.optString("reset_at", detail.optString("resetAt", ""))

                var remPct: Double? = null
                if (limitVal > 0 && rem >= 0) {
                    remPct = (rem / limitVal) * 100.0
                } else if (limitVal > 0 && used >= 0) {
                    remPct = ((limitVal - used) / limitVal) * 100.0
                }

                if (remPct != null) {
                    val kind = if (duration in 1..360) "session" else "weekly"
                    val label = if (kind == "session") "5h" else "Weekly"
                    windows.add(
                        WindowLimit(
                            kind = kind,
                            label = label,
                            usedPercent = max(0.0, 100.0 - remPct),
                            remainingPercent = max(0.0, min(100.0, remPct)),
                            resetsAt = resetAt.ifBlank { null },
                            resetDescription = null,
                            showMeter = true
                        )
                    )
                }
            }
        }

        val limit = ProviderLimit(
            provider = "kimi",
            accountLabel = "Kimi Code",
            windows = windows,
            balanceAmount = null,
            balanceCurrency = null
        )
        return Result.success(limit)
    }

    // =========================================================================
    // 5. 智谱 AI / ZAI: GET https://open.bigmodel.cn/api/monitor/usage/quota/limit
    // =========================================================================
    private fun fetchZai(apiKey: String): Result<ProviderLimit> {
        val headers = mapOf(
            "Authorization" to "Bearer $apiKey",
            "Accept" to "application/json"
        )
        var codeAndBody = httpGet("https://open.bigmodel.cn/api/monitor/usage/quota/limit", headers)
        if (codeAndBody.first !in 200..299) {
            codeAndBody = httpGet("https://api.z.ai/api/monitor/usage/quota/limit", headers)
        }

        val (code, body) = codeAndBody
        if (code !in 200..299) {
            return Result.failure(Exception("HTTP $code: $body"))
        }

        val json = JSONObject(body)
        val dataObj = json.optJSONObject("data")
        val limitsArr = dataObj?.optJSONArray("limits") ?: return Result.failure(Exception("返回格式异常: 缺少 limits"))

        val windows = mutableListOf<WindowLimit>()
        for (i in 0 until limitsArr.length()) {
            val item = limitsArr.optJSONObject(i) ?: continue
            val unit = item.optInt("unit", 0) // 5=minutes, 3=hours, 1=days, 6=weeks
            val number = item.optInt("number", 0)
            val usage = item.optDouble("usage", 0.0)
            val remaining = item.optDouble("remaining", -1.0)
            val percentage = item.optDouble("percentage", -1.0)

            val remPct = if (usage > 0 && remaining >= 0) {
                (remaining / usage) * 100.0
            } else if (percentage in 0.0..100.0) {
                100.0 - percentage
            } else {
                continue
            }

            val kind = when (unit) {
                5, 3 -> "session"
                6 -> "weekly"
                else -> "daily"
            }
            val label = when (unit) {
                5, 3 -> "${number}h"
                6 -> "Weekly"
                else -> "Daily"
            }

            windows.add(
                WindowLimit(
                    kind = kind,
                    label = label,
                    usedPercent = max(0.0, 100.0 - remPct),
                    remainingPercent = max(0.0, min(100.0, remPct)),
                    resetsAt = null,
                    resetDescription = null,
                    showMeter = true
                )
            )
        }

        val limit = ProviderLimit(
            provider = "zai",
            accountLabel = "GLM / BigModel",
            windows = windows,
            balanceAmount = null,
            balanceCurrency = null
        )
        return Result.success(limit)
    }

    private fun fetchCodex(config: DirectProviderConfig): Result<ProviderLimit> {
        val rawToken = config.apiKey.trim()
        if (rawToken.isBlank()) {
            return Result.failure(IllegalArgumentException("ChatGPT Access Token 不能为空"))
        }

        // Support pasting auth.json directly
        val token = if (rawToken.startsWith("{") && rawToken.endsWith("}")) {
            try {
                val obj = JSONObject(rawToken)
                val acc = obj.optString("access_token", rawToken)
                val ref = obj.optString("refresh_token")
                if (ref.isNotBlank()) {
                    updateConfig(config.copy(apiKey = acc, extra = config.extra + ("refreshToken" to ref)))
                }
                acc
            } catch (_: Exception) {
                rawToken
            }
        } else {
            rawToken
        }

        var (limitResult, activeToken) = queryCodexUsage(token) to token
        if (limitResult.isFailure) {
            val refreshToken = config.extra["refreshToken"]
            if (!refreshToken.isNullOrBlank()) {
                val refreshResult = refreshCodexTokenSync(refreshToken)
                if (refreshResult.isSuccess) {
                    val (newAccess, newRefresh) = refreshResult.getOrThrow()
                    val updated = config.copy(
                        apiKey = newAccess,
                        extra = config.extra + ("refreshToken" to newRefresh)
                    )
                    updateConfig(updated)
                    limitResult = queryCodexUsage(newAccess)
                    activeToken = newAccess
                }
            }
        }

        if (limitResult.isFailure) {
            return limitResult
        }

        val baseLimit = limitResult.getOrThrow()
        // Query reset credits (best effort)
        val resetInfo = queryCodexResetCredits(activeToken)
        val finalLimit = if (resetInfo != null) {
            baseLimit.copy(
                resetCreditsCount = resetInfo.first,
                resetCreditsExpiry = resetInfo.second,
                resetCreditsDescription = resetInfo.third
            )
        } else {
            baseLimit
        }
        return Result.success(finalLimit)
    }

    private fun parseExpiryEpochMs(raw: Any?): Long? {
        if (raw == null) return null
        return when (raw) {
            is Number -> {
                val n = raw.toLong()
                if (n <= 0L) null
                else if (n < 100000000000L) n * 1000L
                else n
            }
            is String -> {
                val s = raw.trim()
                s.toLongOrNull()?.let { n ->
                    if (n <= 0L) null
                    else if (n < 100000000000L) n * 1000L
                    else n
                } ?: try {
                    val formats = listOf(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        "yyyy-MM-dd'T'HH:mm:ssXXX",
                        "yyyy-MM-dd"
                    )
                    var parsed: Long? = null
                    for (fmt in formats) {
                        try {
                            val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            parsed = sdf.parse(s)?.time
                            if (parsed != null) break
                        } catch (_: Exception) {}
                    }
                    parsed
                } catch (_: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    private fun formatExpiryDate(epochMs: Long): String {
        return try {
            val sdf = SimpleDateFormat("MM月dd日", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            sdf.format(Date(epochMs))
        } catch (_: Exception) {
            ""
        }
    }

    private fun queryCodexResetCredits(token: String): Triple<Int, String?, String?>? {
        return try {
            val headers = mapOf(
                "Authorization" to "Bearer $token",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
                "Accept" to "application/json",
                "originator" to "Codex Desktop",
                "OAI-Product-Sku" to "CODEX"
            )
            val (code, body) = httpGet("https://chatgpt.com/backend-api/wham/rate-limit-reset-credits", headers, timeoutMs = 8000)
            if (code !in 200..299) return null

            val json = JSONObject(body)
            val availableCountRaw = json.optInt("available_count", -1)
            val creditsArr = json.optJSONArray("credits")
            val availableCredits = mutableListOf<JSONObject>()
            if (creditsArr != null) {
                for (i in 0 until creditsArr.length()) {
                    val item = creditsArr.optJSONObject(i) ?: continue
                    val status = item.optString("status", "")
                    if (status.isBlank() || status.equals("available", ignoreCase = true)) {
                        availableCredits.add(item)
                    }
                }
            }
            val count = if (availableCountRaw >= 0) availableCountRaw else availableCredits.size

            var earliestExpiryEpochMs: Long? = null
            for (item in availableCredits) {
                val epochMs = parseExpiryEpochMs(item.opt("expires_at"))
                if (epochMs != null && epochMs > 0L) {
                    if (earliestExpiryEpochMs == null || epochMs < earliestExpiryEpochMs) {
                        earliestExpiryEpochMs = epochMs
                    }
                }
            }

            val expiryText = if (earliestExpiryEpochMs != null) {
                val now = System.currentTimeMillis()
                val diffMs = earliestExpiryEpochMs - now
                val daysLeft = diffMs / (1000L * 60 * 60 * 24)
                val dateStr = formatExpiryDate(earliestExpiryEpochMs)
                if (daysLeft > 0) {
                    "$dateStr 到期 (剩 $daysLeft 天)"
                } else if (daysLeft == 0L && diffMs > 0) {
                    "$dateStr 到期 (今天到期)"
                } else {
                    "$dateStr (已到期)"
                }
            } else null

            val desc = if (count > 0) {
                if (expiryText != null) "$count 张重置卡 · $expiryText" else "$count 张重置卡可用"
            } else {
                "暂无可用重置卡"
            }

            Triple(count, expiryText, desc)
        } catch (_: Exception) {
            null
        }
    }

    private fun queryCodexUsage(token: String): Result<ProviderLimit> {
        val headers = mapOf(
            "Authorization" to "Bearer $token",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
            "Accept" to "application/json"
        )
        val (code, body) = httpGet("https://chatgpt.com/backend-api/wham/usage", headers)
        if (code == 401 || code == 403) {
            return Result.failure(Exception("HTTP $code: 登录状态失效 ($body)"))
        }
        if (code !in 200..299) {
            return Result.failure(Exception("HTTP $code: $body"))
        }

        return try {
            val json = JSONObject(body)
            val planType = json.optString("plan_type", "Codex")
            val rateLimit = json.optJSONObject("rate_limit") ?: json.optJSONObject("rate_limits")
            val windows = mutableListOf<WindowLimit>()

            fun parseWindow(obj: JSONObject?, defaultKind: String, defaultLabel: String): WindowLimit? {
                if (obj == null) return null
                val rawUsed = obj.optDouble("used_percent", -1.0)
                val usedPct = if (rawUsed in 0.0..1.0 && rawUsed > 0.0) rawUsed * 100.0 else rawUsed
                val remPct = if (usedPct >= 0.0) max(0.0, min(100.0, 100.0 - usedPct)) else 100.0

                val windowSec = obj.optLong("limit_window_seconds", 0L)
                val isFree = planType.equals("free", ignoreCase = true)

                val (resolvedKind, resolvedLabel) = when {
                    windowSec in 14400..28800 -> "5-hour" to "5小时"
                    windowSec in 72000..108000 -> "daily" to "日限额"
                    windowSec in 500000..700000 -> "weekly" to "周用量"
                    windowSec >= 1200000 -> "monthly" to "月限额"
                    isFree -> "monthly" to "月限额"
                    else -> defaultKind to defaultLabel
                }

                val resetAtSec = obj.optLong("reset_at", 0L)
                val resetAfterSec = obj.optLong("reset_after_seconds", 0L)
                val resetsAt = when {
                    resetAtSec > 0L -> formatEpochMillis(if (resetAtSec < 100000000000L) resetAtSec * 1000L else resetAtSec)
                    resetAfterSec > 0L -> formatEpochMillis(System.currentTimeMillis() + resetAfterSec * 1000L)
                    else -> null
                }
                val resetDesc = if (resetAfterSec > 0L) {
                    val days = resetAfterSec / 86400
                    val hours = (resetAfterSec % 86400) / 3600
                    val mins = (resetAfterSec % 3600) / 60
                    when {
                        days > 0 -> "${days}天${hours}小时"
                        hours > 0 -> "${hours}小时${mins}分"
                        else -> "${mins}分钟"
                    }
                } else null

                return WindowLimit(
                    kind = resolvedKind,
                    label = resolvedLabel,
                    usedPercent = max(0.0, 100.0 - remPct),
                    remainingPercent = remPct,
                    resetsAt = resetsAt,
                    resetDescription = resetDesc,
                    showMeter = true
                )
            }

            if (rateLimit != null) {
                parseWindow(rateLimit.optJSONObject("primary_window"), "5-hour", "5小时")?.let { windows.add(it) }
                parseWindow(rateLimit.optJSONObject("secondary_window"), "weekly", "周用量")?.let { windows.add(it) }
            }

            if (windows.isEmpty() && rateLimit != null) {
                val keys = rateLimit.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val childObj = rateLimit.optJSONObject(k) ?: continue
                    parseWindow(childObj, k, k)?.let { windows.add(it) }
                }
            }

            if (windows.isEmpty()) {
                return Result.failure(Exception("未能从 ChatGPT 响应中解析出配额数据"))
            }

            val label = if (planType.isNotBlank()) "ChatGPT ${planType.replaceFirstChar { it.uppercase() }}" else "Codex"
            Result.success(
                ProviderLimit(
                    provider = "codex",
                    accountLabel = label,
                    windows = windows,
                    balanceAmount = null,
                    balanceCurrency = null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exchangeCodexOAuthCode(code: String, codeVerifier: String): Result<DirectProviderConfig> {
        val formBody = listOf(
            "grant_type" to "authorization_code",
            "client_id" to PkceHelper.CODEX_CLIENT_ID,
            "code" to code,
            "redirect_uri" to PkceHelper.CODEX_REDIRECT_URI,
            "code_verifier" to codeVerifier
        ).joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

        val headers = mapOf(
            "Content-Type" to "application/x-www-form-urlencoded",
            "Accept" to "application/json"
        )

        val (respCode, respBody) = httpPost(PkceHelper.CODEX_TOKEN_URL, headers, formBody)
        if (respCode !in 200..299) {
            return Result.failure(Exception("OAuth exchange failed ($respCode): $respBody"))
        }

        return try {
            val json = JSONObject(respBody)
            val accessToken = json.optString("access_token")
            val refreshToken = json.optString("refresh_token")
            if (accessToken.isBlank()) {
                return Result.failure(Exception("未获取到 access_token"))
            }
            val existing = getConfigs().find { it.id == PROVIDER_CODEX }
            val updated = DirectProviderConfig(
                id = PROVIDER_CODEX,
                name = "Codex",
                apiKey = accessToken,
                enabled = true,
                extra = buildMap {
                    if (existing != null) putAll(existing.extra)
                    if (refreshToken.isNotBlank()) put("refreshToken", refreshToken)
                }
            )
            updateConfig(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun refreshCodexTokenSync(refreshToken: String): Result<Pair<String, String>> {
        val formBody = listOf(
            "grant_type" to "refresh_token",
            "client_id" to PkceHelper.CODEX_CLIENT_ID,
            "refresh_token" to refreshToken
        ).joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

        val headers = mapOf(
            "Content-Type" to "application/x-www-form-urlencoded",
            "Accept" to "application/json"
        )

        val (respCode, respBody) = httpPost(PkceHelper.CODEX_TOKEN_URL, headers, formBody)
        if (respCode !in 200..299) {
            return Result.failure(Exception("Token refresh failed ($respCode): $respBody"))
        }

        return try {
            val json = JSONObject(respBody)
            val newAccess = json.optString("access_token")
            val newRefresh = json.optString("refresh_token", refreshToken)
            if (newAccess.isBlank()) {
                Result.failure(Exception("未获取到新 access_token"))
            } else {
                Result.success(newAccess to newRefresh)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun httpPost(urlStr: String, headers: Map<String, String>, body: String, timeoutMs: Int = 15000): Pair<Int, String> {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            instanceFollowRedirects = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            conn.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            code to text
        } finally {
            conn.disconnect()
        }
    }

    private fun httpGet(urlStr: String, headers: Map<String, String>, timeoutMs: Int = 10000): Pair<Int, String> {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            instanceFollowRedirects = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            code to text
        } finally {
            conn.disconnect()
        }
    }

    private fun formatEpochMillis(epochMs: Long): String? {
        if (epochMs <= 0L) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.format(Date(epochMs))
        } catch (_: Exception) {
            null
        }
    }
}
