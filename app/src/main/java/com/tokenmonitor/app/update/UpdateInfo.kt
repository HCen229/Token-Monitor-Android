package com.tokenmonitor.app.update

import com.tokenmonitor.app.BuildConfig
import org.json.JSONObject
import java.util.Locale

/**
 * GitHub Release 正式版元数据。
 */
data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkName: String,
    val apkSize: Long,
    val publishedAt: String,
    val isDraft: Boolean = false,
    val isPrerelease: Boolean = false
) {
    /** 严格校验仅正式版 */
    fun isOfficialRelease(): Boolean = !isDraft && !isPrerelease

    /** 是否高于当前已安装版本（语义化版本比较） */
    fun isNewerThanInstalled(): Boolean {
        if (!isOfficialRelease()) return false
        val installedName = BuildConfig.VERSION_NAME
        return compareSemVer(versionName, installedName) > 0
    }

    fun isSameAsInstalled(): Boolean {
        if (!isOfficialRelease()) return false
        val installedName = BuildConfig.VERSION_NAME
        return compareSemVer(versionName, installedName) == 0
    }

    fun displayVersionLabel(): String {
        val clean = versionName.trim().trimStart('v', 'V')
        return "v$clean"
    }

    companion object {
        /**
         * 语义化版本比对（例如 1.0.1 比 1.0.0 新；2.0 比 1.9.9 新）。
         * @return >0 表示 v1 比 v2 新；<0 表示 v1 比 v2 旧；=0 表示相同
         */
        fun compareSemVer(v1: String, v2: String): Int {
            val parts1 = v1.trim().trimStart('v', 'V')
                .split('.')
                .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
            val parts2 = v2.trim().trimStart('v', 'V')
                .split('.')
                .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(parts1.size, parts2.size)
            for (i in 0 until maxLen) {
                val num1 = parts1.getOrElse(i) { 0 }
                val num2 = parts2.getOrElse(i) { 0 }
                if (num1 != num2) return num1.compareTo(num2)
            }
            return 0
        }

        /**
         * 解析 GitHub Releases API 返回的单条 Release JSON。
         */
        fun fromGitHubReleaseJson(raw: String): UpdateInfo {
            val o = JSONObject(raw)
            val tagName = o.getString("tag_name")
            val isDraft = o.optBoolean("draft", false)
            val isPrerelease = o.optBoolean("prerelease", false)
            val title = o.optString("name", tagName).ifBlank { tagName }
            val body = o.optString("body", "")
            val publishedAt = o.optString("published_at", "")

            // 提取版本号（去除前导 v/V）
            val versionName = tagName.trim().trimStart('v', 'V')

            // 遍历 assets 查找 .apk 资产文件
            val assetsArr = o.optJSONArray("assets")
            var apkUrl = ""
            var apkName = ""
            var apkSize = 0L

            if (assetsArr != null && assetsArr.length() > 0) {
                // 优先选取名为 TokenMonitor*.apk 的安装包，否则取第一个 .apk
                var candidateIndex = -1
                for (i in 0 until assetsArr.length()) {
                    val assetObj = assetsArr.getJSONObject(i)
                    val name = assetObj.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        if (name.startsWith("TokenMonitor", ignoreCase = true) || candidateIndex == -1) {
                            candidateIndex = i
                            if (name.startsWith("TokenMonitor", ignoreCase = true)) break
                        }
                    }
                }

                if (candidateIndex != -1) {
                    val matchedAsset = assetsArr.getJSONObject(candidateIndex)
                    apkName = matchedAsset.getString("name")
                    apkUrl = matchedAsset.getString("browser_download_url")
                    apkSize = matchedAsset.optLong("size", 0L)
                }
            }

            if (apkUrl.isBlank()) {
                throw IllegalStateException("GitHub 发行版 [$tagName] 中未发现 APK 资产文件")
            }

            return UpdateInfo(
                tagName = tagName,
                versionName = versionName,
                title = title,
                releaseNotes = body,
                apkUrl = apkUrl,
                apkName = apkName,
                apkSize = apkSize,
                publishedAt = publishedAt,
                isDraft = isDraft,
                isPrerelease = isPrerelease
            )
        }
    }
}

/**
 * 下载进度度量模型（带 EMA 平滑瞬时网速计算）
 */
data class DownloadProgress(
    val fraction: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val speedBytesPerSec: Long = 0L
) {
    fun percentText(): String =
        if (fraction >= 0f) "${(fraction * 100).toInt()}%" else "…"

    fun sizeText(): String {
        val done = formatBytes(downloadedBytes)
        return if (totalBytes > 0) {
            "$done / ${formatBytes(totalBytes)}"
        } else {
            "$done / ?"
        }
    }

    fun speedText(): String {
        if (speedBytesPerSec <= 0L) return "计算中…"
        return "${formatBytes(speedBytesPerSec)}/s"
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes < 0) return "?"
            val b = bytes.toDouble()
            return when {
                b < 1024 -> "${bytes} B"
                b < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", b / 1024.0)
                b < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.2f MB", b / (1024.0 * 1024.0))
                else -> String.format(Locale.US, "%.2f GB", b / (1024.0 * 1024.0 * 1024.0))
            }
        }
    }
}

/**
 * OTA 更新 UI 状态
 */
sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data class UpToDate(val checkedVersion: String) : UpdateUiState()

    data class Available(
        val info: UpdateInfo,
        val showDialog: Boolean = true
    ) : UpdateUiState()

    data class Downloading(
        val info: UpdateInfo,
        val progress: DownloadProgress = DownloadProgress()
    ) : UpdateUiState()

    data class ReadyToInstall(
        val info: UpdateInfo,
        val apkPath: String,
        val showDialog: Boolean = true
    ) : UpdateUiState()

    data class Error(
        val message: String
    ) : UpdateUiState()
}
