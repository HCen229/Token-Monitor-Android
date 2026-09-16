package com.tokenmonitor.app.data

enum class PeriodTab {
    DAY,
    MONTH,
    TOTAL
}

data class ModelStat(
    val client: String,
    val model: String,
    val tokens: Long,
    val cost: Double,
    val share: Float = 0f
)

data class ClientStat(
    val name: String,
    val tokens: Long,
    val cost: Double,
    val share: Float = 0f
)

data class PeriodData(
    val totalTokens: Long = 0L,
    val costUsd: Double = 0.0,
    val messageCount: Int = 0,
    val sessionCount: Int = 0,
    val models: List<ModelStat> = emptyList(),
    val clients: List<ClientStat> = emptyList()
)

data class WindowLimit(
    val kind: String,
    val label: String,
    val usedPercent: Double,
    val remainingPercent: Double,
    val resetsAt: String?,
    val resetDescription: String?,
    val showMeter: Boolean = true
)

data class ProviderLimit(
    val provider: String,
    val accountLabel: String,
    val windows: List<WindowLimit>,
    val balanceAmount: Double?,
    val balanceCurrency: String?,
    val resetCreditsCount: Int? = null,
    val resetCreditsExpiry: String? = null,
    val resetCreditsDescription: String? = null
)

data class DailyHistory(
    val date: String,
    val tokens: Long,
    val cost: Double
)

data class TokenStats(
    val today: PeriodData = PeriodData(),
    val month: PeriodData = PeriodData(),
    val allTime: PeriodData = PeriodData(),
    val daily: List<DailyHistory> = emptyList(),
    val peakDailyTokens: Long = 0L,
    val providers: List<ProviderLimit> = emptyList(),
    val activeDays: Int = 0,
    val currentStreak: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun forPeriod(period: PeriodTab): PeriodData = when (period) {
        PeriodTab.DAY -> today
        PeriodTab.MONTH -> month
        PeriodTab.TOTAL -> allTime
    }
}

data class DiagnosticResult(
    val isTesting: Boolean = false,
    val success: Boolean? = null,
    val message: String = "",
    val tcpLatencyMs: Long = 0,
    val httpStatus: Int = 0
)

data class ConnectionConfig(
    val host: String = "192.168.1.100",
    val port: Int = 17321,
    val secret: String = "",
    val refreshIntervalSec: Int = 3
)

enum class QuotaDisplayStyle(val id: String, val label: String) {
    DUAL_RINGS("dual_rings", "同心双圆环"),
    PROGRESS_BARS("progress_bars", "进度横条");

    companion object {
        fun fromId(id: String?): QuotaDisplayStyle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DUAL_RINGS
        }
    }
}

enum class RingOrderConfig(val id: String, val label: String) {
    OUTER_5H_INNER_WEEKLY("outer_5h", "外圈 5小时 · 内圈 周用量"),
    OUTER_WEEKLY_INNER_5H("outer_weekly", "外圈 周用量 · 内圈 5小时");

    companion object {
        fun fromId(id: String?): RingOrderConfig {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: OUTER_5H_INNER_WEEKLY
        }
    }
}

enum class RingCenterTextConfig(val id: String, val label: String) {
    SESSION_5H("session_5h", "5小时额度"),
    WEEKLY("weekly", "周用量额度");

    companion object {
        fun fromId(id: String?): RingCenterTextConfig {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SESSION_5H
        }
    }
}

enum class AppLanguage(val id: String, val displayNameZh: String, val displayNameEn: String) {
    SYSTEM("system", "跟随系统", "System Default"),
    ZH("zh", "简体中文", "Simplified Chinese"),
    EN("en", "English", "English");

    fun getDisplayName(isEnglish: Boolean): String = if (isEnglish) displayNameEn else displayNameZh

    companion object {
        fun fromId(id: String?): AppLanguage {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
        }
    }
}


