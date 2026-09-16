package com.tokenmonitor.app.ui

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.tokenmonitor.app.ui.components.CodexOAuthDialog
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
import androidx.compose.foundation.shape.CircleShape
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
import com.tokenmonitor.app.data.provider.DirectProviderConfig
import com.tokenmonitor.app.ui.components.SuperIslandStudio
import com.tokenmonitor.app.ui.glass.LiquidActionButton
import com.tokenmonitor.app.ui.glass.LiquidButtonTone
import com.tokenmonitor.app.ui.i18n.LocalAppStrings
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
    val currentLanguage by viewModel.language.collectAsState()
    val directProviders by viewModel.directProviders.collectAsState()
    val providerTestStates by viewModel.providerTestStates.collectAsState()
    val strings = com.tokenmonitor.app.ui.i18n.LocalAppStrings.current
    val currentStats = (uiState as? UiState.Success)?.stats
        ?: (uiState as? UiState.Error)?.lastStats
    val isConnected = uiState is UiState.Success
    val context = LocalContext.current

    var host by remember(config.host) { mutableStateOf(config.host) }
    var portText by remember(config.port) { mutableStateOf(config.port.toString()) }
    var secret by remember(config.secret) { mutableStateOf(config.secret) }
    var interval by remember(config.refreshIntervalSec) { mutableStateOf(config.refreshIntervalSec.toString()) }
    var secretVisible by remember { mutableStateOf(false) }
    var visibleKeyIds by remember { mutableStateOf(setOf<String>()) }
    var expandedIds by remember { mutableStateOf(setOf<String>()) }
    var showCreditsPage by remember { mutableStateOf(false) }
    var showProvidersList by remember { mutableStateOf(false) }
    var editingProviderId by remember { mutableStateOf<String?>(null) }

    if (showCreditsPage) {
        AcknowledgementsScreen(
            onBack = { showCreditsPage = false },
            modifier = modifier
        )
        return
    }

    val editingProvider = directProviders.firstOrNull { it.id == editingProviderId }
    if (editingProvider != null) {
        ConnectProviderDetailScreen(
            provider = editingProvider,
            testState = providerTestStates[editingProvider.id] ?: ProviderTestState(),
            onBack = { editingProviderId = null },
            onUpdate = { updated -> viewModel.updateDirectProvider(updated) },
            onSave = { updated ->
                viewModel.updateDirectProvider(updated)
                viewModel.saveDirectProviders(
                    directProviders.map { if (it.id == updated.id) updated else it }
                )
            },
            onTest = { id, key -> viewModel.testDirectProvider(id, key) },
            onOAuthExchange = { code, verifier, callback ->
                viewModel.exchangeCodexOAuth(code, verifier, callback)
            },
            modifier = modifier
        )
        return
    }

    if (showProvidersList) {
        ConnectProvidersScreen(
            directProviders = directProviders,
            onBack = { showProvidersList = false },
            onSelectProvider = { editingProviderId = it },
            onToggleEnable = { cfg, enabled ->
                val updated = cfg.copy(enabled = enabled)
                viewModel.updateDirectProvider(updated)
                viewModel.saveDirectProviders(
                    directProviders.map { if (it.id == updated.id) updated else it }
                )
            },
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

        // 0. Language Settings Card
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
                        text = strings.languageSectionTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TmTextMuted,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = currentLanguage.getDisplayName(strings.isEnglish),
                        fontSize = 11.sp,
                        color = TmAccent,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isThemeLight) Color(0xFFF1F5F9) else Color(0x18FFFFFF))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val langOptions = listOf(
                        com.tokenmonitor.app.data.AppLanguage.SYSTEM to strings.languageFollowSystem,
                        com.tokenmonitor.app.data.AppLanguage.ZH to strings.languageZh,
                        com.tokenmonitor.app.data.AppLanguage.EN to strings.languageEn
                    )

                    langOptions.forEach { (lang, label) ->
                        val isSelected = currentLanguage == lang
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) TmAccent else Color.Transparent
                                )
                                .clickable {
                                    viewModel.updateLanguage(lang)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TmTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 1. Connection Parameters Card
        TmCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.connectionParams,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextMuted,
                    letterSpacing = 1.2.sp
                )

                // Host
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(strings.ipAddress) },
                    placeholder = { Text(strings.ipAddressPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                // Port
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text(strings.portLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                // Secret
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text(strings.secretKey) },
                    visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (secretVisible) strings.hideText else strings.showText,
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
                    label = { Text(strings.pollingInterval) },
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
                        Toast.makeText(context, strings.configSaved, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    tone = LiquidButtonTone.PRIMARY,
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = strings.saveConfig,
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
                    text = strings.networkDiagnostics,
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
                        text = if (diagnostic.isTesting) strings.testing else strings.testConnection,
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

        // 2.5 Connect Providers Entrance Card
        TmCard(modifier = Modifier.fillMaxWidth()) {
            val enabledCount = directProviders.count { it.enabled }
            val totalCount = directProviders.size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showProvidersList = true }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.directProvidersTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TmTextPrimary
                        )
                        if (enabledCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x1F30D158))
                                    .padding(horizontal = 7.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = "$enabledCount/$totalCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30D158)
                                )
                            }
                        }
                    }
                    Text(
                        text = strings.directProvidersSummary(enabledCount, totalCount),
                        fontSize = 12.sp,
                        color = TmTextSecondary
                    )
                }

                Text(
                    text = "›",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Light,
                    color = TmTextMuted
                )
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
                    text = strings.liveNotification,
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
                    Text(text = strings.notificationPermission, fontSize = 13.sp, color = TmTextSecondary)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (areNotifsEnabled && isPermGranted) Color(0x1F30D158) else Color(0x1FFF453A))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (areNotifsEnabled && isPermGranted) strings.permissionGranted else strings.permissionDenied,
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
                    Text(text = strings.backgroundService, fontSize = 13.sp, color = TmTextSecondary)
                    Text(
                        text = strings.running,
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

                // Super Island Quota Alert Studio Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isThemeLight) Color(0xFFF8FAFC) else Color(0x18FFFFFF))
                        .border(1.dp, if (isThemeLight) Color(0xFFE2E8F0) else Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Title & Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = strings.islandQuotaAlertCardTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TmTextPrimary
                            )
                            Text(
                                text = strings.islandQuotaAlertCardDesc,
                                fontSize = 11.sp,
                                color = TmTextMuted,
                                lineHeight = 15.sp
                            )
                        }
                        androidx.compose.material3.Switch(
                            checked = islandConfig.quotaAlertEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateIslandConfig(islandConfig.copy(quotaAlertEnabled = enabled))
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TmAccent
                            )
                        )
                    }

                    if (islandConfig.quotaAlertEnabled) {
                        // Usage Threshold Selection (10%, 15%, 20%, 25%, 30%)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = strings.islandQuotaAlertUsageThreshold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64D2FF)
                            )
                            val usageOptions = listOf(10, 15, 20, 25, 30)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isThemeLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                usageOptions.forEach { pct ->
                                    val isSelected = islandConfig.quotaAlertThresholdPercent == pct
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) TmAccent else Color.Transparent
                                            )
                                            .clickable {
                                                viewModel.updateIslandConfig(islandConfig.copy(quotaAlertThresholdPercent = pct))
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$pct%",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF0F1115) else TmTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Balance Threshold Selection (¥1, ¥2, ¥5, ¥10)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = strings.islandQuotaAlertBalanceThreshold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF34D399)
                            )
                            val balanceOptions = listOf(1.0, 2.0, 5.0, 10.0)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isThemeLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                balanceOptions.forEach { bal ->
                                    val isSelected = kotlin.math.abs(islandConfig.balanceAlertThresholdCny - bal) < 0.01
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) TmAccent else Color.Transparent
                                            )
                                            .clickable {
                                                viewModel.updateIslandConfig(islandConfig.copy(balanceAlertThresholdCny = bal))
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "¥${bal.toInt()}",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF0F1115) else TmTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Test Quota Alert Button
                        LiquidActionButton(
                            onClick = {
                                com.tokenmonitor.app.service.TokenNotificationManager.postTestQuotaAlert(context)
                                val activity = when (context) {
                                    is android.app.Activity -> context
                                    is android.content.ContextWrapper -> context.baseContext as? android.app.Activity
                                    else -> null
                                }
                                activity?.moveTaskToBack(true)
                            },
                            tone = LiquidButtonTone.SECONDARY,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text(
                                text = strings.testIslandQuotaAlert,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text(
                            text = strings.testLiveNotification,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                    LiquidActionButton(
                        onClick = { com.tokenmonitor.app.service.TokenNotificationManager.openNotificationSettings(context) },
                        tone = LiquidButtonTone.SECONDARY,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Text(
                            text = strings.systemNotificationSettings,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
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
                            text = strings.liveNotificationSwitch,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TmTextPrimary
                        )
                        Text(
                            text = strings.liveNotificationSwitchDesc,
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
                        text = strings.softwareUpdate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TmTextMuted,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = strings.githubOfficial,
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
                    Text(text = strings.currentVersion, fontSize = 13.sp, color = TmTextSecondary)
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
                            Text(text = strings.checkUpdate, fontSize = 13.sp, color = TmTextSecondary)
                            Text(
                                text = strings.checkUpdate,
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
                            Text(text = strings.checkUpdate, fontSize = 13.sp, color = TmTextSecondary)
                            Text(
                                text = strings.checkingUpdate,
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
                            Text(text = strings.checkUpdate, fontSize = 13.sp, color = TmTextSecondary)
                            Text(
                                text = strings.upToDate,
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
                                    text = "${strings.newVersionFound} ${state.info.displayVersionLabel()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30D158)
                                )
                                if (state.info.apkSize > 0) {
                                    Text(
                                        text = "${strings.packageSize}: ${DownloadProgress.formatBytes(state.info.apkSize)}",
                                        fontSize = 11.sp,
                                        color = TmTextMuted
                                    )
                                }
                            }
                            Text(
                                text = strings.downloadNow,
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
                                    text = "${strings.downloading} ${state.info.displayVersionLabel()}",
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
                                    text = "${state.info.displayVersionLabel()} ${strings.readyToInstall}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30D158)
                                )
                                Text(text = strings.readyToInstallDesc, fontSize = 11.sp, color = TmTextMuted)
                            }
                            Text(
                                text = strings.installNow,
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
                                    text = strings.updateNotice,
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
                                text = strings.retry,
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
                    text = strings.about,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextMuted,
                    letterSpacing = 1.2.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = strings.version, fontSize = 12.sp, color = TmTextSecondary)
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
                    Text(text = strings.openSourceRepo, fontSize = 12.sp, color = TmTextSecondary)
                    Text(text = "hcen229/Token-Monitor-Android", fontSize = 12.sp, color = TmAccent, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = strings.renderEngine, fontSize = 12.sp, color = TmTextSecondary)
                    Text(text = "Backdrop (Hardware AGSL)", fontSize = 12.sp, color = TmAccent, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = strings.syncProtocol, fontSize = 12.sp, color = TmTextSecondary)
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = strings.openSourceCredits,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TmTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.openSourceCreditsSubtitle,
                            fontSize = 10.sp,
                            color = TmTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = strings.viewCredits,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TmAccent,
                        maxLines = 1,
                        softWrap = false
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
                        text = strings.settingsTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TmTextPrimary
                    )
                    Text(
                        text = strings.settingsSubtitle,
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
                            text = strings.updateDialogTitle(info.displayVersionLabel()),
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
                                Text(strings.updateDialogCurrent("v${BuildConfig.VERSION_NAME}"), fontSize = 12.sp, color = TmTextMuted)
                                Text(strings.updateDialogLatest(info.displayVersionLabel()), fontSize = 12.sp, color = TmAccent, fontWeight = FontWeight.Bold)
                            }
                            if (info.apkSize > 0) {
                                Text(
                                    text = strings.updateDialogSize(DownloadProgress.formatBytes(info.apkSize)),
                                    fontSize = 11.sp,
                                    color = TmTextMuted
                                )
                            }
                            if (info.releaseNotes.isNotBlank()) {
                                Text(
                                    text = strings.updateDialogReleaseNotes,
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
                            Text(strings.updateDialogDownload, color = TmAccent, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                            Text(strings.updateDialogLater, color = TmTextMuted)
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
                            text = strings.updateDialogReadyTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TmTextPrimary
                        )
                    },
                    text = {
                        Text(
                            text = strings.updateDialogReadyMessage(state.info.displayVersionLabel()),
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
                            Text(strings.installNow, color = TmAccent, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                            Text(strings.updateDialogLater, color = TmTextMuted)
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
    BackHandler { onBack() }
    val context = LocalContext.current
    val strings = com.tokenmonitor.app.ui.i18n.LocalAppStrings.current
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    val projects = remember(strings) {
        strings.getCreditProjects()
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
                    text = strings.creditsTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TmTextPrimary
                )
                Text(
                    text = strings.creditsIntro,
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
                                    val isOurs = tag == "本项目" || tag == "This Project"
                                    val isDep = tag == "第三方依赖" || tag == "Dependency"
                                    val isFont = tag == "内置字体" || tag == "Bundled Font"
                                    val isUpstream = tag == "上游原型" || tag == "Upstream Model"
                                    val isRef = tag == "技术参考" || tag == "Reference"
                                    val isLicense = tag.contains("MIT") || tag.contains("OFL") || tag.contains("Apache") || tag.contains("GPL") || tag.contains("BSD")

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
                                    Toast.makeText(context, "${strings.toastCannotOpenSettings("")}: ${e.message}", Toast.LENGTH_SHORT).show()
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
                                text = strings.creditsOpenRepo,
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
                                text = strings.settingsTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TmPrimary
                            )
                        }

                        Text(
                            text = strings.creditsTotalProjects(projects.size),
                            fontSize = 11.sp,
                            color = TmTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectProvidersScreen(
    directProviders: List<DirectProviderConfig>,
    onBack: () -> Unit,
    onSelectProvider: (String) -> Unit,
    onToggleEnable: (DirectProviderConfig, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val strings = LocalAppStrings.current
    val isThemeLight = LocalThemeMode.current == AppThemeMode.LIGHT

    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = isLandscape || configuration.screenWidthDp >= 600

    val supportsOpticalGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val ambientBackdrop = LocalLiquidGlassBackdrop.current
    val ambientState: Backdrop = ambientBackdrop ?: emptyBackdrop()
    val headerBackdrop = if (supportsOpticalGlass) rememberLayerBackdrop() else null
    val combinedHeaderBackdrop = if (supportsOpticalGlass && headerBackdrop != null) {
        rememberCombinedBackdrop(ambientState, headerBackdrop)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = strings.directProvidersTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TmTextPrimary
                )
                Text(
                    text = strings.directProvidersSummary(directProviders.count { it.enabled }, directProviders.size),
                    fontSize = 12.sp,
                    color = TmTextSecondary,
                    lineHeight = 18.sp
                )
            }

            // Providers list card
            TmCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    directProviders.forEach { cfg ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isThemeLight) Color(0xFFF8FAFC) else Color(0x12FFFFFF))
                                .border(
                                    0.5.dp,
                                    if (cfg.enabled) TmAccent.copy(alpha = 0.35f)
                                    else if (isThemeLight) Color(0xFFE2E8F0)
                                    else Color(0x1FFFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelectProvider(cfg.id)
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Official brand logo
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (isThemeLight) Color(0xFFEFF6FF) else Color(0x18FFFFFF))
                                        .border(
                                            0.5.dp,
                                            if (isThemeLight) Color(0xFFDBEAFE) else Color(0x22FFFFFF),
                                            RoundedCornerShape(9.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BrandIcon(name = cfg.id, size = 20.dp)
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = cfg.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TmTextPrimary
                                    )
                                    val isCodex = cfg.id == "codex"
                                    val statusText = when {
                                        cfg.enabled && cfg.apiKey.isNotBlank() -> if (isCodex) strings.codexLoggedIn else strings.directProviderEnabled
                                        cfg.apiKey.isNotBlank() -> strings.directProviderStatusConfigured
                                        else -> strings.directProviderStatusNotConfigured
                                    }
                                    val statusColor = when {
                                        cfg.enabled && cfg.apiKey.isNotBlank() -> Color(0xFF30D158)
                                        cfg.apiKey.isNotBlank() -> TmTextSecondary
                                        else -> TmTextMuted
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (cfg.enabled && cfg.apiKey.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF30D158))
                                            )
                                        }
                                        Text(
                                            text = statusText,
                                            fontSize = 11.sp,
                                            color = statusColor
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                androidx.compose.material3.Switch(
                                    checked = cfg.enabled,
                                    onCheckedChange = { checked ->
                                        onToggleEnable(cfg, checked)
                                    },
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = TmAccent
                                    )
                                )
                                Text(
                                    text = "›",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Light,
                                    color = TmTextMuted
                                )
                            }
                        }
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
                                text = strings.settingsTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TmPrimary
                            )
                        }

                        Text(
                            text = strings.directProvidersTitle,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TmTextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectProviderDetailScreen(
    provider: DirectProviderConfig,
    testState: ProviderTestState,
    onBack: () -> Unit,
    onUpdate: (DirectProviderConfig) -> Unit,
    onSave: (DirectProviderConfig) -> Unit,
    onTest: (String, String) -> Unit,
    onOAuthExchange: ((String, String, (Boolean, String?) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val isThemeLight = LocalThemeMode.current == AppThemeMode.LIGHT

    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = isLandscape || configuration.screenWidthDp >= 600

    val supportsOpticalGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val ambientBackdrop = LocalLiquidGlassBackdrop.current
    val ambientState: Backdrop = ambientBackdrop ?: emptyBackdrop()
    val headerBackdrop = if (supportsOpticalGlass) rememberLayerBackdrop() else null
    val combinedHeaderBackdrop = if (supportsOpticalGlass && headerBackdrop != null) {
        rememberCombinedBackdrop(ambientState, headerBackdrop)
    } else {
        ambientState
    }
    val headerCaptureModifier = headerBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier

    val density = LocalDensity.current
    val headerScrollThresholdPx = with(density) { 56.dp.toPx() }.coerceAtLeast(1f)
    val headerBlurProgress = (scrollState.value / headerScrollThresholdPx).coerceIn(0f, 1f)

    val topContentPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navHeaderHeight = topContentPadding + 48.dp

    var apiKey by remember(provider.apiKey) { mutableStateOf(provider.apiKey) }
    var enabled by remember(provider.enabled) { mutableStateOf(provider.enabled) }
    var keyVisible by remember { mutableStateOf(false) }

    var showOAuthDialog by remember { mutableStateOf(false) }
    var isExchangingOAuth by remember { mutableStateOf(false) }

    if (showOAuthDialog) {
        CodexOAuthDialog(
            isExchanging = isExchangingOAuth,
            onDismiss = { showOAuthDialog = false },
            onSuccess = { code, verifier ->
                isExchangingOAuth = true
                onOAuthExchange?.invoke(code, verifier) { success, err ->
                    isExchangingOAuth = false
                    if (success) {
                        showOAuthDialog = false
                        Toast.makeText(context, strings.codexLoginSuccess, Toast.LENGTH_SHORT).show()
                    } else {
                        val msg = err ?: strings.codexLoginFailed(err.orEmpty())
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            },
            onError = { err ->
                isExchangingOAuth = false
                Toast.makeText(context, strings.codexLoginFailed(err), Toast.LENGTH_LONG).show()
            }
        )
    }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TmPrimary,
        unfocusedBorderColor = if (isThemeLight) Color(0xFFCBD5E1) else Color(0x33FFFFFF),
        focusedTextColor = TmTextPrimary,
        unfocusedTextColor = TmTextPrimary,
        focusedLabelColor = TmPrimary,
        unfocusedLabelColor = TmTextSecondary,
        cursorColor = TmPrimary
    )

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

            // Hero Brand Identity Card
            TmCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isThemeLight) Color(0xFFEFF6FF) else Color(0x18FFFFFF))
                                .border(
                                    0.5.dp,
                                    if (isThemeLight) Color(0xFFDBEAFE) else Color(0x22FFFFFF),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            BrandIcon(name = provider.id, size = 28.dp)
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = provider.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TmTextPrimary
                            )
                            Text(
                                text = strings.directProviderHint(provider.id),
                                fontSize = 12.sp,
                                color = TmTextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Enable Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isThemeLight) Color(0xFFF8FAFC) else Color(0x14000000))
                            .border(
                                0.5.dp,
                                if (isThemeLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = strings.directProviderEnableSwitch,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TmTextPrimary
                            )
                            Text(
                                text = strings.directProviderEnableSwitchDesc,
                                fontSize = 11.sp,
                                color = TmTextMuted,
                                lineHeight = 15.sp
                            )
                        }

                        androidx.compose.material3.Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                onUpdate(provider.copy(enabled = it, apiKey = apiKey.trim()))
                            },
                            colors = androidx.compose.material3.SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TmAccent
                            )
                        )
                    }
                }
            }

            // If Codex, show ChatGPT OAuth Login Card
            if (provider.id == "codex") {
                val hasToken = apiKey.isNotBlank()
                TmCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.codexLoginWithChatGPT,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TmTextMuted,
                                letterSpacing = 1.2.sp
                            )
                            if (hasToken) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF30D158))
                                    )
                                    Text(
                                        text = strings.codexLoggedIn,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF30D158)
                                    )
                                }
                            }
                        }

                        Text(
                            text = strings.codexLoginPrompt,
                            fontSize = 12.sp,
                            color = TmTextSecondary,
                            lineHeight = 16.sp
                        )

                        LiquidActionButton(
                            onClick = { showOAuthDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            tone = if (hasToken) LiquidButtonTone.SECONDARY else LiquidButtonTone.PRIMARY,
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (hasToken) strings.codexReLogin else strings.codexLoginWithChatGPT,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // API Key & Testing Card
            val isCodex = provider.id == "codex"
            TmCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isCodex) strings.codexManualTokenHint else strings.directProviderApiKey,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TmTextMuted,
                        letterSpacing = 1.2.sp
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            val trimmed = it.trim()
                            if (isCodex && trimmed.startsWith("{") && trimmed.endsWith("}")) {
                                try {
                                    val obj = org.json.JSONObject(trimmed)
                                    val acc = obj.optString("access_token", trimmed)
                                    val ref = obj.optString("refresh_token")
                                    val updatedExtra = if (ref.isNotBlank()) provider.extra + ("refreshToken" to ref) else provider.extra
                                    onUpdate(provider.copy(apiKey = acc, extra = updatedExtra, enabled = enabled))
                                } catch (_: Exception) {
                                    onUpdate(provider.copy(apiKey = trimmed, enabled = enabled))
                                }
                            } else {
                                onUpdate(provider.copy(apiKey = trimmed, enabled = enabled))
                            }
                        },
                        label = { Text(if (isCodex) "Access Token" else strings.directProviderApiKey) },
                        placeholder = { Text(if (isCodex) "Bearer Token / auth.json" else strings.directProviderApiKeyPlaceholder) },
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (keyVisible) strings.hideText else strings.showText,
                                modifier = Modifier
                                    .clickable { keyVisible = !keyVisible }
                                    .padding(8.dp),
                                color = TmAccent,
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors
                    )

                    // Test Connection Button
                    LiquidActionButton(
                        onClick = {
                            onTest(provider.id, apiKey.trim())
                        },
                        modifier = Modifier.fillMaxWidth(),
                        tone = LiquidButtonTone.SECONDARY,
                        loading = testState.isTesting,
                        contentPadding = PaddingValues(vertical = 9.dp)
                    ) {
                        Text(
                            text = if (testState.isTesting) strings.directProviderTesting else strings.directProviderTest,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }

                    // Test Result Banner
                    if (testState.success != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (testState.success == true) Color(0x1F30D158) else Color(0x1FFF453A)
                                )
                                .border(
                                    1.dp,
                                    if (testState.success == true) Color(0x4D30D158) else Color(0x4DFF453A),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            val msgTextColor = when {
                                testState.success == true -> if (isThemeLight) Color(0xFF065F46) else Color(0xFFD1FAE5)
                                else -> if (isThemeLight) Color(0xFF991B1B) else Color(0xFFFFD8D8)
                            }
                            Text(
                                text = if (testState.success == true) {
                                    strings.directProviderTestSuccess(testState.message)
                                } else {
                                    strings.directProviderTestFailed(testState.message)
                                },
                                fontSize = 12.sp,
                                color = msgTextColor,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Save Button
                    LiquidActionButton(
                        onClick = {
                            val updated = provider.copy(apiKey = apiKey.trim(), enabled = enabled)
                            onSave(updated)
                            Toast.makeText(context, strings.directProviderSaved, Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        tone = LiquidButtonTone.PRIMARY,
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = strings.directProviderSave,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
                                text = strings.directProviderBack,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TmPrimary
                            )
                        }

                        Text(
                            text = provider.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TmTextPrimary
                        )
                    }
                }
            }
        }
    }
}

