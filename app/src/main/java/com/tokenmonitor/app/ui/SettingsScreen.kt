package com.tokenmonitor.app.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tokenmonitor.app.ui.glass.LocalLiquidGlassBackdrop
import com.tokenmonitor.app.ui.glass.ProgressiveBlurHeader
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.app.BuildConfig
import com.tokenmonitor.app.data.ConnectionConfig
import com.tokenmonitor.app.ui.components.SuperIslandStudio
import com.tokenmonitor.app.ui.glass.LiquidActionButton
import com.tokenmonitor.app.ui.glass.LiquidButtonTone
import com.tokenmonitor.app.ui.theme.AppThemeMode
import com.tokenmonitor.app.ui.theme.LocalThemeMode
import com.tokenmonitor.app.ui.theme.TmAccent
import com.tokenmonitor.app.ui.theme.TmPrimary
import com.tokenmonitor.app.ui.theme.TmTextMuted
import com.tokenmonitor.app.ui.theme.TmTextPrimary
import com.tokenmonitor.app.ui.theme.TmTextSecondary
import com.tokenmonitor.app.update.DownloadProgress
import com.tokenmonitor.app.update.UpdateInfo
import com.tokenmonitor.app.update.UpdateUiState

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsState()
    val islandConfig by viewModel.islandConfig.collectAsState()
    val diagnostic by viewModel.diagnosticState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val currentStats = (uiState as? UiState.Success)?.stats
        ?: (uiState as? UiState.Error)?.lastStats
    val isConnected = uiState is UiState.Success
    val context = LocalContext.current

    var host by remember(config.host) { mutableStateOf(config.host) }
    var portText by remember(config.port) { mutableStateOf(config.port.toString()) }
    var secret by remember(config.secret) { mutableStateOf(config.secret) }
    var interval by remember(config.refreshIntervalSec) { mutableStateOf(config.refreshIntervalSec.toString()) }
    var secretVisible by remember { mutableStateOf(false) }
    var showCreditsPage by remember { mutableStateOf(false) }

    if (showCreditsPage) {
        AcknowledgementsScreen(
            onBack = { showCreditsPage = false },
            modifier = modifier
        )
        return
    }

    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = isLandscape || configuration.screenWidthDp >= 600

    val supportsOpticalGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val ambientBackdrop = LocalLiquidGlassBackdrop.current
    val ambientState: Backdrop = ambientBackdrop ?: emptyBackdrop()
    val headerBackdrop = if (supportsOpticalGlass) rememberLayerBackdrop() else null
    val combinedHeaderBackdrop = if (headerBackdrop != null && ambientBackdrop != null) {
        rememberCombinedBackdrop(ambientBackdrop, headerBackdrop)
    } else {
        ambientState
    }
    val headerCaptureModifier = headerBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier

    val density = LocalDensity.current
    val headerScrollThresholdPx = with(density) { 56.dp.toPx() }.coerceAtLeast(1f)
    val headerBlurProgress = (scrollState.value / headerScrollThresholdPx).coerceIn(0f, 1f)

    val topContentPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val settingsHeaderHeight = topContentPadding + 58.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Scrollable Content Layer (captured by headerBackdrop)
        Column(
            modifier = Modifier
                .then(if (isWideScreen) Modifier.widthIn(max = 760.dp) else Modifier.fillMaxWidth())
                .padding(horizontal = 14.dp)
                .then(headerCaptureModifier)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(settingsHeaderHeight + 8.dp))

        val isThemeLight = LocalThemeMode.current == AppThemeMode.LIGHT
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TmPrimary,
            unfocusedBorderColor = if (isThemeLight) Color(0xFFCBD5E1) else Color(0x33FFFFFF),
            focusedTextColor = TmTextPrimary,
            unfocusedTextColor = TmTextPrimary,
            focusedLabelColor = TmPrimary,
            unfocusedLabelColor = TmTextMuted,
            focusedPlaceholderColor = TmTextMuted,
            unfocusedPlaceholderColor = TmTextMuted
        )

        // 1. Connection Parameters Card
        TmCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "连接参数",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextMuted,
                    letterSpacing = 1.2.sp
                )

                // Host
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("IP 地址") },
                    placeholder = { Text("例如 192.168.1.100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                // Port
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("端口 (17321)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                // Secret
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("密钥 (Secret)") },
                    visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (secretVisible) "隐藏" else "显示",
                            modifier = Modifier
                                .clickable { secretVisible = !secretVisible }
                                .padding(8.dp),
                            color = TmAccent,
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                // Interval
                OutlinedTextField(
                    value = interval,
                    onValueChange = { interval = it },
                    label = { Text("轮询间隔 (秒)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Save Button
                LiquidActionButton(
                    onClick = {
                        val p = portText.toIntOrNull() ?: 17321
                        val iv = interval.toIntOrNull() ?: 3
                        val newCfg = ConnectionConfig(
                            host = host.trim(),
                            port = p,
                            secret = secret.trim(),
                            refreshIntervalSec = iv
                        )
                        viewModel.saveConfig(newCfg)
                        Toast.makeText(context, "配置已保存", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    tone = LiquidButtonTone.PRIMARY,
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "保存配置",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // 2. Diagnostics Card
        TmCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "网络体检",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextMuted,
                    letterSpacing = 1.2.sp
                )

                LiquidActionButton(
                    onClick = {
                        val p = portText.toIntOrNull() ?: 17321
                        viewModel.testConnection(host.trim(), p, secret.trim())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    tone = LiquidButtonTone.SECONDARY,
                    loading = diagnostic.isTesting,
                    contentPadding = PaddingValues(vertical = 9.dp)
                ) {
                    Text(
                        text = if (diagnostic.isTesting) "正在检测..." else "检查连接",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                // Diagnostic Result Box
                if (diagnostic.message.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (diagnostic.success == true) Color(0x1F30D158) else Color(0x1FFF453A)
                            )
                            .border(
                                1.dp,
                                if (diagnostic.success == true) Color(0x4D30D158) else Color(0x4DFF453A),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        val diagTextColor = when {
                            diagnostic.success == true -> if (isThemeLight) Color(0xFF065F46) else Color(0xFFD1FAE5)
                            else -> if (isThemeLight) Color(0xFF991B1B) else Color(0xFFFFD8D8)
                        }
                        Text(
                            text = diagnostic.message,
                            fontSize = 12.sp,
                            color = diagTextColor,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // 3. Live Notification Card
        TmCard(modifier = Modifier.fillMaxWidth()) {
            val isPermGranted = com.tokenmonitor.app.service.TokenNotificationManager.isPermissionGranted(context)
            val areNotifsEnabled = com.tokenmonitor.app.service.TokenNotificationManager.areNotificationsEnabled(context)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "实时通知",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextMuted,
                    letterSpacing = 1.2.sp
                )

                // 1. Notification Permission Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "通知权限", fontSize = 13.sp, color = TmTextSecondary)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (areNotifsEnabled && isPermGranted) Color(0x1F30D158) else Color(0x1FFF453A))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (areNotifsEnabled && isPermGranted) "已开启" else "未开启",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (areNotifsEnabled && isPermGranted) Color(0xFF30D158) else Color(0xFFFF453A)
                        )
                    }
                }

                // 2. Foreground Service Component Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "后台常驻服务", fontSize = 13.sp, color = TmTextSecondary)
                    Text(
                        text = "运行中",
                        fontSize = 12.sp,
                        color = TmAccent,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Super Island Drag-and-Drop Studio
                SuperIslandStudio(
                    config = islandConfig,
                    stats = currentStats,
                    isConnected = isConnected,
                    onConfigChange = { newConfig ->
                        viewModel.updateIslandConfig(newConfig)
                    }
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidActionButton(
                        onClick = {
                            com.tokenmonitor.app.service.TokenNotificationManager.postTestNotification(context)
                            val activity = when (context) {
                                is android.app.Activity -> context
                                is android.content.ContextWrapper -> context.baseContext as? android.app.Activity
                                else -> null
                            }
                            activity?.moveTaskToBack(true)
                        },
                        tone = LiquidButtonTone.PRIMARY,
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text(
                            text = "测试实时通知",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    LiquidActionButton(
                        onClick = { com.tokenmonitor.app.service.TokenNotificationManager.openNotificationSettings(context) },
                        tone = LiquidButtonTone.SECONDARY,
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text(
                            text = "系统通知设置",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                // Foreground toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isThemeLight) Color(0xFFF8FAFC) else Color(0x18FFFFFF))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "实时通知开关",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TmTextPrimary
                        )
                        Text(
                            text = "在状态栏与锁屏持续展示实时用量",
                            fontSize = 10.sp,
                            color = TmTextMuted
                        )
                    }
                    var monitorActive by remember {
                        mutableStateOf(com.tokenmonitor.app.service.TokenNotificationManager.isMonitoringActive)
                    }
                    androidx.compose.material3.Switch(
                        checked = monitorActive,
                        onCheckedChange = {
                            monitorActive = it
                            if (it) {
                                com.tokenmonitor.app.service.TokenNotificationManager.startMonitoring(context)
                            } else {
                                com.tokenmonitor.app.service.TokenNotificationManager.stopMonitoring(context)
                            }
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TmAccent
                        )
                    )
                }
            }
        }

        // 4. Software Update (GitHub OTA) Card
        TmCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "软件更新",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TmTextMuted,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "GitHub 正式版",
                        fontSize = 11.sp,
                        color = TmAccent,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "当前版本", fontSize = 13.sp, color = TmTextSecondary)
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        fontSize = 12.sp,
                        color = TmTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Dynamic Status & Action based on UpdateUiState
                when (val state = updateState) {
                    is UpdateUiState.Idle -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "检查更新", fontSize = 13.sp, color = TmTextSecondary)
                            Text(
                                text = "检查更新",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TmAccent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.checkForUpdate(silent = false) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    is UpdateUiState.Checking -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "检查更新", fontSize = 13.sp, color = TmTextSecondary)
                            Text(
                                text = "检查中…",
                                fontSize = 12.sp,
                                color = TmTextMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    is UpdateUiState.UpToDate -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "检查更新", fontSize = 13.sp, color = TmTextSecondary)
                            Text(
                                text = "已是最新",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF30D158),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.checkForUpdate(silent = false) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    is UpdateUiState.Available -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "发现新版本 ${state.info.displayVersionLabel()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30D158)
                                )
                                if (state.info.apkSize > 0) {
                                    Text(
                                        text = "大小: ${DownloadProgress.formatBytes(state.info.apkSize)}",
                                        fontSize = 11.sp,
                                        color = TmTextMuted
                                    )
                                }
                            }
                            Text(
                                text = "立即下载",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF30D158),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.startDownload(state.info) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    is UpdateUiState.Downloading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x180A84FF))
                                .border(0.75.dp, Color(0x440A84FF), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "正在下载 ${state.info.displayVersionLabel()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64D2FF)
                                )
                                Text(
                                    text = state.progress.percentText(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64D2FF)
                                )
                            }
                            LinearProgressIndicator(
                                progress = { state.progress.fraction.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = TmAccent,
                                trackColor = Color(0x33FFFFFF)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = state.progress.sizeText(), fontSize = 10.sp, color = TmTextMuted)
                                Text(text = state.progress.speedText(), fontSize = 10.sp, color = TmTextMuted)
                            }
                        }
                    }
                    is UpdateUiState.ReadyToInstall -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${state.info.displayVersionLabel()} 安装包已就绪",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30D158)
                                )
                                Text(text = "下载完成，点击立即开始安装", fontSize = 11.sp, color = TmTextMuted)
                            }
                            Text(
                                text = "立即安装",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF30D158),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (!viewModel.tryInstallPending()) {
                                            viewModel.openInstallPermissionSettings()
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    is UpdateUiState.Error -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "更新检查提示",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF453A)
                                )
                                Text(
                                    text = state.message,
                                    fontSize = 11.sp,
                                    color = TmTextMuted,
                                    lineHeight = 16.sp
                                )
                            }
                            Text(
                                text = "重试",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TmAccent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.checkForUpdate(silent = false) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. About Card
        TmCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "关于",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextMuted,
                    letterSpacing = 1.2.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "版本", fontSize = 12.sp, color = TmTextSecondary)
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        fontSize = 12.sp,
                        color = TmTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "开源仓库", fontSize = 12.sp, color = TmTextSecondary)
                    Text(text = "hcen229/Token-Monitor-Android", fontSize = 12.sp, color = TmAccent, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "渲染引擎", fontSize = 12.sp, color = TmTextSecondary)
                    Text(text = "Backdrop (Hardware AGSL)", fontSize = 12.sp, color = TmAccent, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "同步协议", fontSize = 12.sp, color = TmTextSecondary)
                    Text(text = "Hub Sync v1", fontSize = 12.sp, color = TmTextPrimary)
                }

                Spacer(Modifier.height(4.dp))
                val isThemeLight = LocalThemeMode.current == AppThemeMode.LIGHT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isThemeLight) Color(0xFFF8FAFC) else Color(0x14000000))
                        .border(0.5.dp, if (isThemeLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF), RoundedCornerShape(8.dp))
                        .clickable { showCreditsPage = true }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "开源鸣谢名单",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TmTextPrimary
                        )
                        Text(
                            text = "向灵感来源与开源先驱致敬",
                            fontSize = 10.sp,
                            color = TmTextMuted
                        )
                    }
                    Text(
                        text = "查看 ↗",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TmAccent
                    )
                }
            }
        }

        // Bottom spacer for the floating dock (increased to prevent any occlusion)
        Spacer(modifier = Modifier.height(135.dp))
    }

    // 2. ProgressiveBlurHeader pinned at top
    ProgressiveBlurHeader(
        backdrop = combinedHeaderBackdrop,
        progress = headerBlurProgress,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(settingsHeaderHeight),
        shape = RoundedCornerShape(0.dp),
        uniformOverlay = true
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .then(if (isWideScreen) Modifier.widthIn(max = 760.dp) else Modifier.fillMaxWidth())
                    .fillMaxSize()
            ) {
                Spacer(Modifier.height(topContentPadding + 4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "设置",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TmTextPrimary
                    )
                    Text(
                        text = "连接参数与实时通知配置",
                        fontSize = 12.sp,
                        color = TmTextMuted
                    )
                }
            }
        }
    }
}

    // OTA Update Dialog Modal
    when (val state = updateState) {
        is UpdateUiState.Available -> {
            if (state.showDialog) {
                val info = state.info
                AlertDialog(
                    onDismissRequest = { viewModel.dismissUpdateDialog() },
                    title = {
                        Text(
                            text = "发现新版本 ${info.displayVersionLabel()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TmTextPrimary
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("当前版本: v${BuildConfig.VERSION_NAME}", fontSize = 12.sp, color = TmTextMuted)
                                Text("最新版本: ${info.displayVersionLabel()}", fontSize = 12.sp, color = TmAccent, fontWeight = FontWeight.Bold)
                            }
                            if (info.apkSize > 0) {
                                Text(
                                    text = "安装包大小: ${DownloadProgress.formatBytes(info.apkSize)}",
                                    fontSize = 11.sp,
                                    color = TmTextMuted
                                )
                            }
                            if (info.releaseNotes.isNotBlank()) {
                                Text(
                                    text = "更新内容:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TmTextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x14FFFFFF))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = info.releaseNotes,
                                        fontSize = 12.sp,
                                        color = TmTextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.startDownload(info) }) {
                            Text("下载并安装", color = TmAccent, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                            Text("稍后", color = TmTextMuted)
                        }
                    },
                    containerColor = Color(0xFF1E2128),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        is UpdateUiState.ReadyToInstall -> {
            if (state.showDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissUpdateDialog() },
                    title = {
                        Text(
                            text = "安装包准备就绪",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TmTextPrimary
                        )
                    },
                    text = {
                        Text(
                            text = "新版本 ${state.info.displayVersionLabel()} 安装包已下载完成。点击立即安装，若系统提示阻止，请允许开启「安装未知应用」权限。",
                            fontSize = 13.sp,
                            color = TmTextSecondary,
                            lineHeight = 19.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (!viewModel.tryInstallPending()) {
                                viewModel.openInstallPermissionSettings()
                            }
                        }) {
                            Text("立即安装", color = TmAccent, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                            Text("稍后", color = TmTextMuted)
                        }
                    },
                    containerColor = Color(0xFF1E2128),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        else -> {}
    }
}

private data class CreditProject(
    val name: String,
    val repo: String,
    val url: String,
    val description: String,
    val tags: List<String>
)

@Composable
fun AcknowledgementsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    val projects = remember {
        listOf(
            CreditProject(
                name = "Token Monitor Android",
                repo = "hcen229/Token-Monitor-Android",
                url = "https://github.com/hcen229/Token-Monitor-Android",
                description = "Token Monitor 原生 Android 客户端应用，支持与桌面端无缝同步、实时状态栏通知、灵动岛监控与多服务商限额可视化。",
                tags = listOf("本项目", "原生应用", "MIT")
            ),
            CreditProject(
                name = "Token Monitor 桌面端",
                repo = "Javis603/token-monitor",
                url = "https://github.com/Javis603/token-monitor",
                description = "原版桌面端 Token 统计监控工具，提供核心数据同步协议规范、多服务商额度聚合与经典桌面端监控逻辑。",
                tags = listOf("原版核心", "数据协议", "MIT")
            ),
            CreditProject(
                name = "Backdrop",
                repo = "kyant0/backdrop",
                url = "https://github.com/kyant0/backdrop",
                description = "通过 Gradle 依赖直接引入的外部开源库（io.github.kyant0:backdrop），用于驱动 Android 13+ 实时硬件级液态毛玻璃与光学高斯模糊。",
                tags = listOf("第三方依赖", "AGSL引擎", "Apache-2.0")
            ),
            CreditProject(
                name = "JetBrains Mono",
                repo = "JetBrains/JetBrainsMono",
                url = "https://github.com/JetBrains/JetBrainsMono",
                description = "直接内置于 APK 资源目录（res/font/）的开源等宽字体资产，为全应用提供高清晰度的数字对齐与代码阅读质感。",
                tags = listOf("内置字体", "OFL 1.1")
            ),
            CreditProject(
                name = "Tokscale",
                repo = "junhoyeo/tokscale",
                url = "https://github.com/junhoyeo/tokscale",
                description = "极简高效的命令行 Token 追踪分析工具，为桌面端与跨端数据结构提供了上游模型设计与解析原型。",
                tags = listOf("上游原型", "数据解析", "MIT")
            ),
            CreditProject(
                name = "SignalDock",
                repo = "jizizr/signaldock",
                url = "https://github.com/jizizr/signaldock",
                description = "针对 Android 物理打孔屏左右腔体分离、悬浮避让与灵动岛通知交互架构提供设计参考。",
                tags = listOf("技术参考", "MIT")
            ),
            CreditProject(
                name = "Capsulyric",
                repo = "FrancoGiudans/Capsulyric",
                url = "https://github.com/FrancoGiudans/Capsulyric",
                description = "针对国产系统（ColorOS 流体云、HyperOS 焦点通知等）私有状态栏通知 Extras 键名与协议参数提供互操作性技术参考。",
                tags = listOf("技术参考", "GPL-3.0")
            )
        )
    }

    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = isLandscape || configuration.screenWidthDp >= 600

    val supportsOpticalGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val ambientBackdrop = LocalLiquidGlassBackdrop.current
    val ambientState: Backdrop = ambientBackdrop ?: emptyBackdrop()
    val headerBackdrop = if (supportsOpticalGlass) rememberLayerBackdrop() else null
    val combinedHeaderBackdrop = if (headerBackdrop != null && ambientBackdrop != null) {
        rememberCombinedBackdrop(ambientBackdrop, headerBackdrop)
    } else {
        ambientState
    }
    val headerCaptureModifier = headerBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier

    val density = LocalDensity.current
    val headerScrollThresholdPx = with(density) { 56.dp.toPx() }.coerceAtLeast(1f)
    val headerBlurProgress = (scrollState.value / headerScrollThresholdPx).coerceIn(0f, 1f)

    val topContentPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navHeaderHeight = topContentPadding + 48.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. Scrollable Content Layer (captured by headerBackdrop)
        Column(
            modifier = Modifier
                .then(if (isWideScreen) Modifier.widthIn(max = 760.dp) else Modifier.fillMaxWidth())
                .padding(horizontal = 14.dp)
                .then(headerCaptureModifier)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(navHeaderHeight + 8.dp))

            // Title Block
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "开源鸣谢",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TmTextPrimary
                )
                Text(
                    text = "Token Monitor Android 的诞生离不开开源社区的智慧与贡献。衷心感谢以下所有开源项目、工具与灵感来源：",
                    fontSize = 12.sp,
                    color = TmTextSecondary,
                    lineHeight = 18.sp
                )
            }

            // Projects Cards List
            projects.forEach { item ->
                TmCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header: Project Name & Tags
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TmTextPrimary
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item.tags.forEach { tag ->
                                    val isOurs = tag == "本项目"
                                    val isDep = tag == "第三方依赖"
                                    val isFont = tag == "内置字体"
                                    val isUpstream = tag == "上游原型"
                                    val isRef = tag == "技术参考"
                                    val isLicense = tag.contains("MIT") || tag.contains("OFL") || tag.contains("Apache") || tag.contains("GPL")

                                    val bg = when {
                                        isOurs -> TmAccent.copy(alpha = 0.16f)
                                        isDep -> if (isLight) Color(0xFFF3E8FF) else Color(0x28A855F7)
                                        isFont -> if (isLight) Color(0xFFFEF3C7) else Color(0x28F59E0B)
                                        isUpstream -> if (isLight) Color(0xFFE0E7FF) else Color(0x286366F1)
                                        isRef -> if (isLight) Color(0xFFEFF6FF) else Color(0x280A84FF)
                                        else -> if (isLight) Color(0xFFF1F5F9) else Color(0x18FFFFFF)
                                    }

                                    val borderColor = when {
                                        isOurs -> TmAccent.copy(alpha = 0.45f)
                                        isDep -> if (isLight) Color(0xFFC084FC) else Color(0x60A855F7)
                                        isFont -> if (isLight) Color(0xFFFBBF24) else Color(0x60F59E0B)
                                        isUpstream -> if (isLight) Color(0xFF818CF8) else Color(0x606366F1)
                                        isRef -> if (isLight) Color(0x600A84FF) else Color(0x4064D2FF)
                                        else -> Color.Transparent
                                    }

                                    val textColor = when {
                                        isOurs -> TmAccent
                                        isDep -> if (isLight) Color(0xFF7E22CE) else Color(0xFFC084FC)
                                        isFont -> if (isLight) Color(0xFFB45309) else Color(0xFFFBBF24)
                                        isUpstream -> if (isLight) Color(0xFF4338CA) else Color(0xFF818CF8)
                                        isRef -> if (isLight) Color(0xFF0284C7) else Color(0xFF64D2FF)
                                        isLicense -> TmAccent
                                        else -> TmTextMuted
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(bg)
                                            .border(width = 0.5.dp, color = borderColor, shape = RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 9.5.sp,
                                            fontWeight = if (isOurs || isDep || isFont || isUpstream || isRef) FontWeight.SemiBold else FontWeight.Medium,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }

                        // Repository Link Row (Clickable to open in browser)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isLight) Color(0xFFF8FAFC) else Color(0x14000000))
                                .border(0.5.dp, if (isLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF), RoundedCornerShape(6.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Throwable) {
                                        Toast.makeText(context, "无法打开浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.repo,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TmPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "访问仓库 ↗",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TmPrimary
                            )
                        }

                        // Description
                        Text(
                            text = item.description,
                            fontSize = 11.5.sp,
                            color = TmTextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(135.dp))
        }

        // 2. ProgressiveBlurHeader pinned at top
        ProgressiveBlurHeader(
            backdrop = combinedHeaderBackdrop,
            progress = headerBlurProgress,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(navHeaderHeight),
            shape = RoundedCornerShape(0.dp),
            uniformOverlay = true
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .then(if (isWideScreen) Modifier.widthIn(max = 760.dp) else Modifier.fillMaxWidth())
                        .fillMaxSize()
                ) {
                    Spacer(Modifier.height(topContentPadding))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onBack() }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "‹",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TmPrimary
                            )
                            Text(
                                text = "设置",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TmPrimary
                            )
                        }

                        Text(
                            text = "共 ${projects.size} 个开源项目",
                            fontSize = 11.sp,
                            color = TmTextMuted
                        )
                    }
                }
            }
        }
    }
}
