package com.tokenmonitor.app.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * OTA 更新仓储服务：负责与 GitHub Releases API 通信及断点/重定向文件流下载。
 */
class UpdateRepository(private val context: Context) {

    /**
     * 获取 GitHub 仓库最新正式版信息（过滤草稿与预发布）。
     */
    suspend fun fetchLatestRelease(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        var lastFailure: Exception? = null

        for (endpoint in UpdateConfig.API_ENDPOINTS) {
            try {
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    connectTimeout = UpdateConfig.CONNECT_TIMEOUT_MS
                    readTimeout = UpdateConfig.READ_TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", UpdateConfig.USER_AGENT)
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                    setRequestProperty("Cache-Control", "no-cache")
                    instanceFollowRedirects = true
                }

                try {
                    val code = conn.responseCode
                    if (code == 404) {
                        return@withContext Result.failure(
                            IllegalStateException("暂无正式版发布（或仓库尚未开放 Releases）")
                        )
                    }
                    if (code !in 200..299) {
                        lastFailure = IllegalStateException("HTTP $code (${conn.responseMessage})")
                        continue
                    }

                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val info = UpdateInfo.fromGitHubReleaseJson(body)

                    if (!info.isOfficialRelease()) {
                        lastFailure = IllegalStateException("最新版本非正式版构建")
                        continue
                    }

                    Log.i(TAG, "成功获取最新正式版: ${info.displayVersionLabel()} (${info.apkName})")
                    return@withContext Result.success(info)
                } finally {
                    conn.disconnect()
                }
            } catch (e: Exception) {
                lastFailure = e
                Log.w(TAG, "从 $endpoint 获取版本失败: ${e.message}")
            }
        }

        val err = lastFailure ?: IllegalStateException("无法连接 GitHub 更新服务器")
        Log.e(TAG, "所有接口尝试均失败: ${err.message}")
        Result.failure(err)
    }

    /**
     * 下载 APK 安装包至应用私有外部存储，并提供实时进度与网速回调。
     */
    suspend fun downloadApk(
        info: UpdateInfo,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var lastFailure: Throwable? = null

        // 构造候选下载地址列表（直连优先，镜像降级兜底）
        val candidates = UpdateConfig.DOWNLOAD_MIRRORS.map { prefix ->
            if (prefix.isEmpty()) info.apkUrl else prefix + info.apkUrl
        }

        for (url in candidates) {
            val result = downloadFrom(url, info, onProgress)
            if (result.isSuccess) {
                return@withContext result
            }
            lastFailure = result.exceptionOrNull()
            Log.w(TAG, "从 $url 下载失败，尝试下一镜像: ${lastFailure?.message}")
        }

        val e = lastFailure ?: IllegalStateException("所有下载通道均不可达")
        Log.e(TAG, "下载彻底失败: ${e.message}")
        Result.failure(e)
    }

    private suspend fun downloadFrom(
        initialUrl: String,
        info: UpdateInfo,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> {
        try {
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "updates").apply {
                if (!exists()) mkdirs()
            }
            val fileName = "TokenMonitor-${info.versionName}.apk"
            val outFile = File(dir, fileName)
            if (outFile.exists()) outFile.delete()

            // 处理 GitHub Release 的多重跨域重定向 (301/302/307/308 -> AWS S3 / CDN)
            var currentUrl = initialUrl
            var conn: HttpURLConnection? = null
            var redirects = 0
            val maxRedirects = 6

            while (redirects < maxRedirects) {
                conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = UpdateConfig.CONNECT_TIMEOUT_MS
                    readTimeout = UpdateConfig.READ_TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", UpdateConfig.USER_AGENT)
                    instanceFollowRedirects = false // 手动处理跨域重定向
                }

                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) {
                        return Result.failure(IllegalStateException("重定向未提供跳转地址 (HTTP $code)"))
                    }
                    currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                        location
                    } else {
                        URL(URL(currentUrl), location).toString()
                    }
                    redirects++
                } else if (code in 200..299) {
                    break
                } else {
                    conn.disconnect()
                    return Result.failure(IllegalStateException("下载请求错误 HTTP $code ($currentUrl)"))
                }
            }

            val finalConn = conn ?: return Result.failure(IllegalStateException("连接初始化失败"))

            try {
                val totalBytes = when {
                    finalConn.contentLengthLong > 0 -> finalConn.contentLengthLong
                    info.apkSize > 0 -> info.apkSize
                    else -> -1L
                }

                var readTotal = 0L
                var lastReportAt = System.currentTimeMillis()
                var lastReportBytes = 0L
                var speedBps = 0L
                val startAt = System.currentTimeMillis()

                fun emitProgress(force: Boolean = false) {
                    val now = System.currentTimeMillis()
                    val dt = now - lastReportAt
                    if (!force && dt < 200 && readTotal > 0) return
                    if (dt > 0) {
                        val db = readTotal - lastReportBytes
                        val instant = (db * 1000L) / dt
                        speedBps = if (speedBps <= 0L) instant else (speedBps * 3 + instant) / 4
                    }
                    lastReportAt = now
                    lastReportBytes = readTotal
                    val fraction = if (totalBytes > 0) (readTotal.toFloat() / totalBytes).coerceIn(0f, 1f) else -1f
                    onProgress(
                        DownloadProgress(
                            fraction = fraction,
                            downloadedBytes = readTotal,
                            totalBytes = totalBytes,
                            speedBytesPerSec = speedBps
                        )
                    )
                }

                emitProgress(force = true)

                BufferedInputStream(finalConn.inputStream).use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            readTotal += n
                            emitProgress()
                        }
                        output.flush()
                    }
                }

                val elapsed = (System.currentTimeMillis() - startAt).coerceAtLeast(1L)
                if (speedBps <= 0L && readTotal > 0) {
                    speedBps = (readTotal * 1000L) / elapsed
                }
                onProgress(
                    DownloadProgress(
                        fraction = 1f,
                        downloadedBytes = readTotal,
                        totalBytes = if (totalBytes > 0) totalBytes else readTotal,
                        speedBytesPerSec = speedBps
                    )
                )

                if (outFile.length() == 0L) {
                    outFile.delete()
                    return Result.failure(IllegalStateException("下载文件为空"))
                }

                Log.i(TAG, "下载成功完成: ${outFile.absolutePath} (${outFile.length()} 字节)")
                return Result.success(outFile)
            } finally {
                finalConn.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "从 $initialUrl 下载异常: ${e.message}")
            return Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "UpdateRepository"
    }
}
