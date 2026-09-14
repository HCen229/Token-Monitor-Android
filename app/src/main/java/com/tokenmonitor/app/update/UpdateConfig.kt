package com.tokenmonitor.app.update

object UpdateConfig {
    const val GITHUB_OWNER = "hcen229"
    const val GITHUB_REPO = "Token-Monitor-Android"

    /**
     * GitHub Releases API 官方最新正式版接口。
     * GitHub 规范：/releases/latest 仅返回最新正式发布的 Release，天然过滤 draft（草稿）与 prerelease（预发布）。
     */
    const val GITHUB_API_LATEST = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    /**
     * API 接口尝试列表（官方直连优先，备用镜像降级）。
     */
    val API_ENDPOINTS: List<String> = listOf(
        GITHUB_API_LATEST,
        "https://ghfast.top/$GITHUB_API_LATEST"
    )

    /**
     * 国内 Release APK 下载加速镜像前缀。
     * 直接下载遇到网络超时时，按顺序降级走镜像通道。
     */
    val DOWNLOAD_MIRRORS: List<String> = listOf(
        "", // 官方直连优先
        "https://ghproxy.net/",
        "https://mirror.ghproxy.com/"
    )

    const val CONNECT_TIMEOUT_MS = 15_000
    const val READ_TIMEOUT_MS = 60_000
    const val USER_AGENT = "Token-Monitor-Android-App"
}
