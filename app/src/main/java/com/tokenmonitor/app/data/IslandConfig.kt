package com.tokenmonitor.app.data

import org.json.JSONObject

enum class IslandItemType(
    val id: String,
    val title: String,
    val emoji: String,
    val defaultSample: String
) {
    STATUS("status", "运行状态", "", "工作中"),
    TODAY_TOKENS("today_tokens", "今日Token", "", "70M"),
    AI_QUOTA("ai_quota", "AI剩余用量", "", "Claude 82%"),
    TODAY_COST("today_cost", "今日花费", "", "$0.15"),
    MONTH_TOKENS("month_tokens", "本月Token", "", "18.5M"),
    NONE("none", "不显示", "", "");

    companion object {
        fun fromId(id: String): IslandItemType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: STATUS
        }
    }
}

/**
 * Super Island Configuration matching Xiaomi HyperOS physical layout:
 * [App/Brand Icon] [Left Text (Locked to STATUS)] 📷 Camera [Right Text (Customizable)]
 * Fixed: [Icon] [工作中] 📷 [Customizable Slot]
 */
data class IslandConfig(
    val leftItem: IslandItemType = IslandItemType.STATUS,
    val rightItem: IslandItemType = IslandItemType.TODAY_TOKENS,
    val showIcon: Boolean = true,
    val selectedProvider: String = "auto",
    val quotaMode: String = "auto",
    val quotaAlertEnabled: Boolean = true,
    val quotaAlertThresholdPercent: Int = 20,
    val balanceAlertThresholdCny: Double = 2.0
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("leftItem", IslandItemType.STATUS.id)
        obj.put("rightItem", rightItem.id)
        obj.put("showIcon", showIcon)
        obj.put("provider", selectedProvider)
        obj.put("quotaMode", quotaMode)
        obj.put("quotaAlertEnabled", quotaAlertEnabled)
        obj.put("quotaAlertThresholdPercent", quotaAlertThresholdPercent)
        obj.put("balanceAlertThresholdCny", balanceAlertThresholdCny)
        return obj.toString()
    }

    companion object {
        val DEFAULT = IslandConfig(
            leftItem = IslandItemType.STATUS,
            rightItem = IslandItemType.TODAY_TOKENS,
            showIcon = true,
            selectedProvider = "auto",
            quotaMode = "auto",
            quotaAlertEnabled = true,
            quotaAlertThresholdPercent = 20,
            balanceAlertThresholdCny = 2.0
        )

        fun fromJson(jsonStr: String?): IslandConfig {
            if (jsonStr.isNullOrBlank()) return DEFAULT
            return try {
                val obj = JSONObject(jsonStr)
                val right = IslandItemType.fromId(obj.optString("rightItem", "today_tokens"))
                val showIcon = obj.optBoolean("showIcon", true)
                val provider = obj.optString("provider", "auto")
                val quotaMode = obj.optString("quotaMode", "auto")
                val alertEnabled = obj.optBoolean("quotaAlertEnabled", true)
                val quotaThreshold = obj.optInt("quotaAlertThresholdPercent", 20)
                val balanceThreshold = obj.optDouble("balanceAlertThresholdCny", 2.0)

                IslandConfig(
                    leftItem = IslandItemType.STATUS, // Locked to working status
                    rightItem = right,
                    showIcon = showIcon,
                    selectedProvider = provider,
                    quotaMode = quotaMode,
                    quotaAlertEnabled = alertEnabled,
                    quotaAlertThresholdPercent = quotaThreshold,
                    balanceAlertThresholdCny = balanceThreshold
                )
            } catch (_: Throwable) {
                DEFAULT
            }
        }
    }
}
