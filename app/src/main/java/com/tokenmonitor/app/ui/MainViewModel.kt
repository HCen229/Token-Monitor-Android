package com.tokenmonitor.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tokenmonitor.app.data.ConnectionConfig
import com.tokenmonitor.app.data.DiagnosticResult
import com.tokenmonitor.app.data.QuotaDisplayStyle
import com.tokenmonitor.app.data.TokenRepository
import com.tokenmonitor.app.data.TokenStats
import com.tokenmonitor.app.service.TokenNotificationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import com.tokenmonitor.app.data.RingCenterTextConfig
import com.tokenmonitor.app.data.RingOrderConfig
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    DASHBOARD("实时监控"),
    SETTINGS("设置")
}

sealed class UiState {
    data object Loading : UiState()
    data class Success(val stats: TokenStats) : UiState()
    data class Error(val message: String, val lastStats: TokenStats? = null) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TokenRepository(application)
    private var lastValidStats: TokenStats? = repository.getCachedStats()

    private val _uiState = MutableStateFlow<UiState>(
        lastValidStats?.let { UiState.Error(message = "正在连接电脑端...", lastStats = it) } ?: UiState.Loading
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _config = MutableStateFlow(repository.getConfig())
    val config: StateFlow<ConnectionConfig> = _config.asStateFlow()

    private val _islandConfig = MutableStateFlow(repository.getIslandConfig())
    val islandConfig: StateFlow<com.tokenmonitor.app.data.IslandConfig> = _islandConfig.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(com.tokenmonitor.app.data.PeriodTab.DAY)
    val selectedPeriod: StateFlow<com.tokenmonitor.app.data.PeriodTab> = _selectedPeriod.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _themeMode = MutableStateFlow(repository.getThemeMode())
    val themeMode: StateFlow<com.tokenmonitor.app.ui.theme.AppThemeMode> = _themeMode.asStateFlow()

    private val _providerOrder = MutableStateFlow(repository.getProviderOrder())
    val providerOrder: StateFlow<List<String>> = _providerOrder.asStateFlow()

    private val _quotaDisplayStyle = MutableStateFlow(repository.getQuotaDisplayStyle())
    val quotaDisplayStyle: StateFlow<QuotaDisplayStyle> = _quotaDisplayStyle.asStateFlow()

    private val _ringOrderConfig = MutableStateFlow(repository.getRingOrderConfig())
    val ringOrderConfig: StateFlow<RingOrderConfig> = _ringOrderConfig.asStateFlow()

    private val _ringCenterTextConfig = MutableStateFlow(repository.getRingCenterTextConfig())
    val ringCenterTextConfig: StateFlow<RingCenterTextConfig> = _ringCenterTextConfig.asStateFlow()

    private val _diagnosticState = MutableStateFlow(DiagnosticResult())
    val diagnosticState: StateFlow<DiagnosticResult> = _diagnosticState.asStateFlow()

    private var pollJob: Job? = null

    init {
        com.tokenmonitor.app.service.ScreenStateManager.init(application)
        viewModelScope.launch {
            com.tokenmonitor.app.service.ScreenStateManager.isScreenOn.collect { isScreenOn ->
                if (isScreenOn && com.tokenmonitor.app.TokenMonitorApp.isAppInForeground) {
                    fetchData(isSilent = _uiState.value is UiState.Success)
                    startPolling()
                } else if (!isScreenOn) {
                    stopPolling()
                }
            }
        }
        startPolling()
    }

    fun selectPeriod(period: com.tokenmonitor.app.data.PeriodTab) {
        _selectedPeriod.value = period
    }


    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                val isScreenOn = com.tokenmonitor.app.service.ScreenStateManager.isInteractive(getApplication())
                val isForeground = com.tokenmonitor.app.TokenMonitorApp.isAppInForeground
                if (isForeground && isScreenOn) {
                    fetchData(isSilent = _uiState.value is UiState.Success)
                    val interval = _config.value.refreshIntervalSec.coerceAtLeast(1)
                    delay(interval * 1000L)
                } else {
                    // App is in background or screen is OFF: stop ViewModel polling.
                    break
                }
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun manualRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchData(isSilent = false)
            _isRefreshing.value = false
        }
    }

    val currentStats: TokenStats?
        get() = when (val state = _uiState.value) {
            is UiState.Success -> state.stats
            is UiState.Error -> state.lastStats ?: lastValidStats ?: repository.getCachedStats()
            else -> lastValidStats ?: repository.getCachedStats()
        }

    val isConnected: Boolean
        get() = _uiState.value is UiState.Success

    private suspend fun fetchData(isSilent: Boolean) {
        if (!isSilent && _uiState.value !is UiState.Success && lastValidStats == null) {
            _uiState.value = UiState.Loading
        }

        val result = repository.fetchStats()
        result.onSuccess { stats ->
            lastValidStats = stats
            _uiState.value = UiState.Success(stats)
            TokenNotificationManager.updateStats(getApplication(), stats, isConnected = true)
        }.onFailure { err ->
            val statsToKeep = lastValidStats
                ?: (_uiState.value as? UiState.Success)?.stats
                ?: (_uiState.value as? UiState.Error)?.lastStats
                ?: repository.getCachedStats()
            if (statsToKeep != null) {
                lastValidStats = statsToKeep
            }
            _uiState.value = UiState.Error(
                message = err.message ?: "未连接电脑端 Token Monitor",
                lastStats = statsToKeep
            )
            TokenNotificationManager.updateStats(getApplication(), statsToKeep, isConnected = false)
        }
    }

    fun saveConfig(newConfig: ConnectionConfig) {
        repository.saveConfig(newConfig)
        _config.value = newConfig
        startPolling()
    }

    fun updateIslandConfig(newConfig: com.tokenmonitor.app.data.IslandConfig) {
        _islandConfig.value = newConfig
        repository.saveIslandConfig(newConfig)
        TokenNotificationManager.updateIslandConfig(getApplication(), newConfig)
    }

    fun updateThemeMode(mode: com.tokenmonitor.app.ui.theme.AppThemeMode) {
        _themeMode.value = mode
        repository.saveThemeMode(mode)
    }

    fun updateProviderOrder(newOrder: List<String>) {
        _providerOrder.value = newOrder
        repository.saveProviderOrder(newOrder)
    }

    fun moveProviderUp(providerName: String, allProviders: List<String>) {
        val current = _providerOrder.value.toMutableList()
        // Ensure all discovered providers exist in the list
        allProviders.forEach { p ->
            if (current.none { it.equals(p, ignoreCase = true) }) {
                current.add(p)
            }
        }
        val idx = current.indexOfFirst { it.equals(providerName, ignoreCase = true) }
        if (idx > 0) {
            val item = current.removeAt(idx)
            current.add(idx - 1, item)
            updateProviderOrder(current)
        }
    }

    fun moveProviderDown(providerName: String, allProviders: List<String>) {
        val current = _providerOrder.value.toMutableList()
        allProviders.forEach { p ->
            if (current.none { it.equals(p, ignoreCase = true) }) {
                current.add(p)
            }
        }
        val idx = current.indexOfFirst { it.equals(providerName, ignoreCase = true) }
        if (idx >= 0 && idx < current.size - 1) {
            val item = current.removeAt(idx)
            current.add(idx + 1, item)
            updateProviderOrder(current)
        }
    }

    fun resetProviderOrder() {
        updateProviderOrder(emptyList())
    }

    fun updateQuotaDisplayStyle(style: QuotaDisplayStyle) {
        _quotaDisplayStyle.value = style
        repository.saveQuotaDisplayStyle(style)
    }

    fun updateRingOrderConfig(config: RingOrderConfig) {
        _ringOrderConfig.value = config
        repository.saveRingOrderConfig(config)
    }

    fun updateRingCenterTextConfig(config: RingCenterTextConfig) {
        _ringCenterTextConfig.value = config
        repository.saveRingCenterTextConfig(config)
    }

    fun testConnection(host: String, port: Int, secret: String) {
        viewModelScope.launch {
            _diagnosticState.value = DiagnosticResult(isTesting = true, message = "正在进行 TCP 握手与 HTTP 鉴权测试...")
            val res = repository.runDiagnostics(host, port, secret)
            _diagnosticState.value = res
        }
    }

    // ----------------------------------------------------
    // GitHub OTA Updater Integration (Reference: Niskle-Link)
    // ----------------------------------------------------
    private val updateRepo = com.tokenmonitor.app.update.UpdateRepository(application)

    private val _updateState = MutableStateFlow<com.tokenmonitor.app.update.UpdateUiState>(com.tokenmonitor.app.update.UpdateUiState.Idle)
    val updateState: StateFlow<com.tokenmonitor.app.update.UpdateUiState> = _updateState.asStateFlow()

    private var downloadJob: Job? = null
    private var pendingInstallFile: java.io.File? = null
    private var pendingInstallInfo: com.tokenmonitor.app.update.UpdateInfo? = null

    fun checkForUpdate(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _updateState.value = com.tokenmonitor.app.update.UpdateUiState.Checking
            } else if (_updateState.value is com.tokenmonitor.app.update.UpdateUiState.Downloading) {
                return@launch
            }

            val result = updateRepo.fetchLatestRelease()
            result.fold(
                onSuccess = { info ->
                    if (info.isNewerThanInstalled()) {
                        val prev = _updateState.value as? com.tokenmonitor.app.update.UpdateUiState.Available
                        val showDialog = if (silent && prev != null && prev.info.versionName == info.versionName) {
                            prev.showDialog
                        } else {
                            true
                        }
                        _updateState.value = com.tokenmonitor.app.update.UpdateUiState.Available(info, showDialog = showDialog)
                    } else {
                        if (!silent) {
                            _updateState.value = com.tokenmonitor.app.update.UpdateUiState.UpToDate(com.tokenmonitor.app.BuildConfig.VERSION_NAME)
                        } else {
                            if (_updateState.value !is com.tokenmonitor.app.update.UpdateUiState.Available &&
                                _updateState.value !is com.tokenmonitor.app.update.UpdateUiState.Downloading &&
                                _updateState.value !is com.tokenmonitor.app.update.UpdateUiState.ReadyToInstall) {
                                _updateState.value = com.tokenmonitor.app.update.UpdateUiState.Idle
                            }
                        }
                    }
                },
                onFailure = { err ->
                    if (!silent) {
                        _updateState.value = com.tokenmonitor.app.update.UpdateUiState.Error(err.message ?: "检查更新失败")
                    }
                }
            )
        }
    }

    fun startDownload(info: com.tokenmonitor.app.update.UpdateInfo) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _updateState.value = com.tokenmonitor.app.update.UpdateUiState.Downloading(
                info,
                com.tokenmonitor.app.update.DownloadProgress(totalBytes = info.apkSize)
            )

            val result = updateRepo.downloadApk(info) { progress ->
                _updateState.value = com.tokenmonitor.app.update.UpdateUiState.Downloading(info, progress)
            }

            result.fold(
                onSuccess = { file ->
                    pendingInstallFile = file
                    pendingInstallInfo = info
                    _updateState.value = com.tokenmonitor.app.update.UpdateUiState.ReadyToInstall(info, file.absolutePath, showDialog = true)
                    tryInstallPending()
                },
                onFailure = { err ->
                    _updateState.value = com.tokenmonitor.app.update.UpdateUiState.Error(err.message ?: "下载更新失败")
                }
            )
        }
    }

    fun tryInstallPending(): Boolean {
        val file = pendingInstallFile ?: return false
        val app = getApplication<Application>()
        if (!com.tokenmonitor.app.update.ApkInstaller.canRequestPackageInstalls(app)) {
            return false
        }
        return com.tokenmonitor.app.update.ApkInstaller.install(app, file)
    }

    fun openInstallPermissionSettings() {
        val app = getApplication<Application>()
        app.startActivity(com.tokenmonitor.app.update.ApkInstaller.installPermissionSettingsIntent(app))
    }

    fun dismissUpdateDialog() {
        when (val state = _updateState.value) {
            is com.tokenmonitor.app.update.UpdateUiState.Available -> {
                _updateState.value = state.copy(showDialog = false)
            }
            is com.tokenmonitor.app.update.UpdateUiState.ReadyToInstall -> {
                _updateState.value = state.copy(showDialog = false)
            }
            is com.tokenmonitor.app.update.UpdateUiState.UpToDate,
            is com.tokenmonitor.app.update.UpdateUiState.Error -> {
                _updateState.value = com.tokenmonitor.app.update.UpdateUiState.Idle
            }
            else -> {}
        }
    }
}
