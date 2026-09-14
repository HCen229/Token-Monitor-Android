package com.tokenmonitor.app.ui

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.app.WallpaperManager
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import android.content.res.Configuration
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.graphics.drawable.toBitmap
import com.tokenmonitor.app.ui.theme.AppThemeMode
import com.tokenmonitor.app.ui.theme.LocalThemeMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.tokenmonitor.app.data.ClientStat
import com.tokenmonitor.app.data.ConnectionConfig
import com.tokenmonitor.app.data.DailyHistory
import com.tokenmonitor.app.data.ModelStat
import com.tokenmonitor.app.data.PeriodData
import com.tokenmonitor.app.data.PeriodTab
import com.tokenmonitor.app.data.ProviderLimit
import com.tokenmonitor.app.data.QuotaDisplayStyle
import com.tokenmonitor.app.data.RingCenterTextConfig
import com.tokenmonitor.app.data.RingOrderConfig
import com.tokenmonitor.app.data.TokenStats
import com.tokenmonitor.app.data.WindowLimit
import com.tokenmonitor.app.ui.glass.HomeNavTab
import com.tokenmonitor.app.ui.glass.LiquidActionButton
import com.tokenmonitor.app.ui.glass.LiquidBottomBar
import com.tokenmonitor.app.ui.glass.LiquidButtonTone
import com.tokenmonitor.app.ui.glass.LiquidGlassSurface
import com.tokenmonitor.app.ui.glass.LiquidMaterial
import com.tokenmonitor.app.ui.glass.LiquidSegmentedTabs
import com.tokenmonitor.app.ui.glass.LocalLiquidGlassBackdrop
import com.tokenmonitor.app.ui.glass.ProgressiveBlurHeader
import com.tokenmonitor.app.ui.theme.TmAccent
import com.tokenmonitor.app.ui.theme.TmAccentSoft
import com.tokenmonitor.app.ui.theme.TmError
import com.tokenmonitor.app.ui.theme.TmLive
import com.tokenmonitor.app.ui.theme.TmPrimary
import com.tokenmonitor.app.ui.theme.TmTextMuted
import com.tokenmonitor.app.ui.theme.TmTextPrimary
import com.tokenmonitor.app.ui.theme.TmTextSecondary
import com.tokenmonitor.app.ui.theme.TmWarning
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val diagnosticState by viewModel.diagnosticState.collectAsStateWithLifecycle()
    val providerOrder by viewModel.providerOrder.collectAsStateWithLifecycle()
    val quotaDisplayStyle by viewModel.quotaDisplayStyle.collectAsStateWithLifecycle()
    val ringOrderConfig by viewModel.ringOrderConfig.collectAsStateWithLifecycle()
    val ringCenterTextConfig by viewModel.ringCenterTextConfig.collectAsStateWithLifecycle()

    var showAllModelsSheet by remember { mutableStateOf(false) }
    var showAllClientsSheet by remember { mutableStateOf(false) }

    var selectedNavTab by remember { mutableStateOf(HomeNavTab.HOME) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Niskle-Link optical backdrop setup (API 33+ hardware-accelerated lens & blur)
    val supportsOpticalGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val ambientBackdrop = if (supportsOpticalGlass) rememberLayerBackdrop() else null
    val ambientState: Backdrop = ambientBackdrop ?: emptyBackdrop()
    val pageBackdrop = if (supportsOpticalGlass) rememberLayerBackdrop() else null
    val pageBackdropState: Backdrop = if (ambientBackdrop != null && pageBackdrop != null) {
        rememberCombinedBackdrop(ambientBackdrop, pageBackdrop)
    } else {
        ambientState
    }
    val ambientCaptureModifier = ambientBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier
    val pageCaptureModifier = pageBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier

    val currentStats: TokenStats? = when (val state = uiState) {
        is UiState.Success -> state.stats
        is UiState.Error -> state.lastStats ?: viewModel.currentStats
        else -> viewModel.currentStats
    }

    val isConnected = uiState is UiState.Success
    val isError = uiState is UiState.Error

    CompositionLocalProvider(LocalLiquidGlassBackdrop provides ambientBackdrop) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Ambient Backdrop capture layer with luminous atmospheric lighting (extends edge-to-edge under status bar)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(ambientCaptureModifier)
            ) {
                AmbientBackdrop()
            }

            // Page Content Layer - Captured into pageBackdrop so bottom bar blurs page content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(pageCaptureModifier)
            ) {
                if (selectedNavTab == HomeNavTab.HOME) {
                    val themeMode = LocalThemeMode.current
                    val isLight = themeMode == AppThemeMode.LIGHT
                    val isWallpaper = themeMode == AppThemeMode.WALLPAPER
                    val configuration = LocalConfiguration.current
                    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val isWideScreen = isLandscape || configuration.screenWidthDp >= 600

                    val horizontalPadding = if (isWideScreen) 20.dp else 14.dp
                    val density = LocalDensity.current
                    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val topContentPadding = statusBarTop + if (isLandscape) 4.dp else 6.dp
                    val headerContentHeight = 52.dp
                    val headerTotalHeight = topContentPadding + headerContentHeight

                    val scrollState = rememberScrollState()
                    val blurThresholdPx = with(density) { 72.dp.toPx() }.coerceAtLeast(1f)
                    val headerBlurProgress = (scrollState.value / blurThresholdPx).coerceIn(0f, 1f)

                    val headerBackdrop = if (supportsOpticalGlass) rememberLayerBackdrop() else null
                    val combinedHeaderBackdrop = if (headerBackdrop != null) {
                        rememberCombinedBackdrop(ambientState, headerBackdrop)
                    } else {
                        ambientState
                    }
                    val headerCaptureModifier = headerBackdrop?.let { Modifier.layerBackdrop(it) } ?: Modifier

                    Box(modifier = Modifier.fillMaxSize()) {
                        // 1. Scrollable Content Layer - Captured into headerBackdrop for progressive top bar blur
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(headerCaptureModifier)
                        ) {

                            val periodData = currentStats?.forPeriod(selectedPeriod) ?: PeriodData()
                            val rawProviders = currentStats?.providers.orEmpty()
                            val sortedProviders = remember(rawProviders, providerOrder) {
                                if (providerOrder.isEmpty()) {
                                    rawProviders
                                } else {
                                    rawProviders.sortedWith(compareBy { p ->
                                        val idx = providerOrder.indexOfFirst { it.equals(p.provider, ignoreCase = true) }
                                        if (idx >= 0) idx else Int.MAX_VALUE
                                    })
                                }
                            }
                            val dailyHistory = currentStats?.daily.orEmpty()
                            val models = periodData.models
                            val clients = periodData.clients

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = horizontalPadding),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Spacer(modifier = Modifier.height(headerTotalHeight + 8.dp))

                                if (isWideScreen) {
                                    // Tablet / Landscape 2-Column Dashboard Layout
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Left Column: Total Token -> Error -> Activity & Trend -> Client Distribution
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            // 1. Total Panel / Hero Card (enlarged for landscape/tablet)
                                            HeroTotalCard(
                                                period = selectedPeriod,
                                                periodData = periodData,
                                                lastUpdated = currentStats?.lastUpdated ?: System.currentTimeMillis(),
                                                isWideScreen = true
                                            )

                                            // Error banner if any
                                            if (uiState is UiState.Error) {
                                                val errMsg = (uiState as UiState.Error).message
                                                ErrorBanner(
                                                    message = errMsg,
                                                    hasCachedData = currentStats != null,
                                                    onRetry = { viewModel.manualRefresh() }
                                                )
                                            }

                                            // 2. Activity Heatmap Matrix & Trend Spline Chart (placed under Total Token card)
                                            if (dailyHistory.isNotEmpty()) {
                                                ActivityHeatmapCard(
                                                    daily = dailyHistory,
                                                    activeDays = currentStats?.activeDays ?: 0
                                                )
                                                TrendSplineChartCard(
                                                    daily = dailyHistory,
                                                    peakTokens = currentStats?.peakDailyTokens ?: 0L
                                                )
                                            }

                                            // 3. Tools / Clients Distribution (客户端用量放左侧)
                                            if (clients.isNotEmpty()) {
                                                ToolDistributionCard(
                                                    clients = clients,
                                                    onViewAll = { showAllClientsSheet = true }
                                                )
                                            }
                                        }

                                        // Right Column: AI Tool Limits & Quotas ("剩余用量") + Model Usage Breakdown ("模型排行")
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            // 1. AI Tool Limits & Quotas ("剩余用量")
                                            if (sortedProviders.isNotEmpty()) {
                                                AiToolLimitsCard(
                                                    providers = sortedProviders,
                                                    quotaDisplayStyle = quotaDisplayStyle,
                                                    ringOrderConfig = ringOrderConfig,
                                                    ringCenterTextConfig = ringCenterTextConfig
                                                )
                                            }

                                            // 2. Model Usage Breakdown (模型排行放右侧)
                                            if (models.isNotEmpty()) {
                                                ModelBreakdownCard(
                                                    models = models,
                                                    period = selectedPeriod,
                                                    onViewAll = { showAllModelsSheet = true }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Phone Portrait 1-Column Layout
                                    // 2. Total Panel / Hero Card
                                    HeroTotalCard(
                                        period = selectedPeriod,
                                        periodData = periodData,
                                        lastUpdated = currentStats?.lastUpdated ?: System.currentTimeMillis(),
                                        isWideScreen = false
                                    )

                                // Error banner if any
                                if (uiState is UiState.Error) {
                                    val errMsg = (uiState as UiState.Error).message
                                    ErrorBanner(
                                        message = errMsg,
                                        hasCachedData = currentStats != null,
                                        onRetry = { viewModel.manualRefresh() }
                                    )
                                }

                                // 3. AI Tool Limits & Quotas ("剩余用量")
                                if (sortedProviders.isNotEmpty()) {
                                    AiToolLimitsCard(
                                        providers = sortedProviders,
                                        quotaDisplayStyle = quotaDisplayStyle,
                                        ringOrderConfig = ringOrderConfig,
                                        ringCenterTextConfig = ringCenterTextConfig
                                    )
                                }

                                // 4. Activity Heatmap Matrix
                                if (dailyHistory.isNotEmpty()) {
                                    ActivityHeatmapCard(
                                        daily = dailyHistory,
                                        activeDays = currentStats?.activeDays ?: 0
                                    )
                                    TrendSplineChartCard(
                                        daily = dailyHistory,
                                        peakTokens = currentStats?.peakDailyTokens ?: 0L
                                    )
                                }

                                // 5. Model Usage Breakdown
                                if (models.isNotEmpty()) {
                                    ModelBreakdownCard(
                                        models = models,
                                        period = selectedPeriod,
                                        onViewAll = { showAllModelsSheet = true }
                                    )
                                }

                                // 6. Tools / Clients Distribution
                                if (clients.isNotEmpty()) {
                                    ToolDistributionCard(
                                        clients = clients,
                                        onViewAll = { showAllClientsSheet = true }
                                    )
                                }
                            }

                            // Generous bottom spacer so content is never obscured by the floating dock
                            Spacer(modifier = Modifier.height(115.dp))
                        }
                    }

                    // 2. ProgressiveBlurHeader directly ported from Niskle-Link
                    ProgressiveBlurHeader(
                        backdrop = combinedHeaderBackdrop,
                        progress = headerBlurProgress,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(headerTotalHeight),
                        shape = RoundedCornerShape(0.dp),
                        uniformOverlay = true
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = horizontalPadding)
                        ) {
                            Spacer(Modifier.height(topContentPadding))
                            DesktopTitleBar(
                                isConnected = isConnected,
                                isError = isError,
                                hasCachedData = currentStats != null,
                                host = config.host,
                                selectedPeriod = selectedPeriod,
                                onPeriodSelected = { viewModel.selectPeriod(it) },
                                hazeState = combinedHeaderBackdrop
                            )
                        }
                    }
                }
            } else {
                // Settings Page
                SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 7. iOS-style Narrow Floating Liquid Dock (Home vs Settings with sliding lens)
        LiquidBottomBar(
            selected = selectedNavTab,
            onSelect = { selectedNavTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            hazeState = pageBackdropState
        )

            // Full list sheets for Models and Clients
            if (showAllModelsSheet) {
                val periodData = currentStats?.forPeriod(selectedPeriod) ?: PeriodData()
                AllModelsSheet(
                    models = periodData.models,
                    period = selectedPeriod,
                    onDismiss = { showAllModelsSheet = false }
                )
            }

            if (showAllClientsSheet) {
                val periodData = currentStats?.forPeriod(selectedPeriod) ?: PeriodData()
                AllClientsSheet(
                    clients = periodData.clients,
                    onDismiss = { showAllClientsSheet = false }
                )
            }
        }
    }
}

/**
 * 1:1 Desktop Titlebar:
 * Left: "Token Monitor Σ" + Pulsing Live Dot + status
 * Right: LiquidSegmentedTabs with "DAY", "MONTH", "TOTAL"
 */
@Composable
private fun DesktopTitleBar(
    isConnected: Boolean,
    isError: Boolean,
    hasCachedData: Boolean = false,
    host: String = "",
    selectedPeriod: PeriodTab,
    onPeriodSelected: (PeriodTab) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: Backdrop? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title + Live Dot + Status
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Token Monitor",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TmTextPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Σ",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TmAccent
                )

                val liveColor = TmLive
                val errorColor = TmError

                // Pulsing live dot matching desktop `.live-dot`
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(if (isConnected) (0.9f + pulseAlpha * 0.2f) else 1f)
                        .clip(CircleShape)
                        .background(
                            when {
                                isConnected -> liveColor.copy(alpha = pulseAlpha)
                                isError && hasCachedData -> Color(0xFFFF9F0A)
                                isError -> errorColor
                                else -> Color(0xFF5B6471)
                            }
                        )
                        .drawBehind {
                            if (isConnected) {
                                drawCircle(
                                    color = liveColor.copy(alpha = 0.35f * pulseAlpha),
                                    radius = size.minDimension * 1.5f
                                )
                            }
                        }
                )
            }

            Text(
                text = when {
                    isConnected -> if (host.isNotBlank()) "已连接至 $host" else "已连接至电脑端"
                    isError && hasCachedData -> "连接已断开 · 已保留离线数据"
                    isError -> "连接已断开"
                    else -> "正在连接电脑端 Hub..."
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                color = when {
                    isConnected -> if (isLight) Color(0xFF64748B) else Color(0xFF8E8E93)
                    isError && hasCachedData -> Color(0xFFFF9F0A)
                    isError -> TmError.copy(alpha = 0.9f)
                    else -> TmWarning
                }
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right Segmented Tabs: 今日 / 本月 / 全部 (Niskle-Link style optical liquid glass control)
        HeaderLiquidSegmentedTabs(
            tabs = listOf(PeriodTab.DAY, PeriodTab.MONTH, PeriodTab.TOTAL),
            selectedTab = selectedPeriod,
            onTabSelected = onPeriodSelected,
            modifier = Modifier.width(176.dp),
            hazeState = hazeState
        )
    }
}

/**
 * Optical Liquid Glass Segmented Tab Control directly matching Niskle-Link's design.
 * Features pill shape (RoundedCornerShape(999.dp)), translucent grey mask carrier,
 * sliding optical lens pill with soft drop shadow and crisp border, spring settling physics,
 * and tactile haptic feedback.
 */
@Composable
private fun HeaderLiquidSegmentedTabs(
    tabs: List<PeriodTab>,
    selectedTab: PeriodTab,
    onTabSelected: (PeriodTab) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: Backdrop? = null
) {
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val haptics = LocalHapticFeedback.current
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    val carrierBorderBrush = if (isLight) {
        Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = 0.18f),
                Color.Black.copy(alpha = 0.08f),
                Color.Black.copy(alpha = 0.02f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.30f),
                Color.White.copy(alpha = 0.14f),
                Color.White.copy(alpha = 0.04f)
            )
        )
    }

    val maskColor = if (isLight) {
        Color(0xFF64748B).copy(alpha = 0.10f)
    } else {
        Color(0xFF64748B).copy(alpha = 0.20f)
    }

    LiquidGlassSurface(
        hazeState = hazeState,
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp)),
        shape = RoundedCornerShape(999.dp),
        variant = LiquidMaterial.CLEAR,
        surfaceAlpha = 0.001f,
        enableBlur = true,
        enableRefraction = true,
        refractionScale = 0.85f,
        borderBrush = carrierBorderBrush,
        borderWidth = 0.85.dp,
        contentPadding = PaddingValues(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(999.dp))
                .background(maskColor)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val tabCount = tabs.size.coerceAtLeast(1)
                val tabWidth = maxWidth / tabCount
                val tabWidthPx = with(density) { tabWidth.toPx() }

                val indicatorOffset by animateFloatAsState(
                    targetValue = selectedIndex.toFloat(),
                    animationSpec = spring(dampingRatio = 0.84f, stiffness = 380f),
                    label = "headerPillIndicator"
                )

                // Sliding optical liquid pill
                Box(
                    modifier = Modifier
                        .width(tabWidth)
                        .fillMaxHeight()
                        .offset { IntOffset((indicatorOffset * tabWidthPx).roundToInt(), 0) }
                        .shadow(
                            elevation = 2.dp,
                            shape = RoundedCornerShape(999.dp),
                            ambientColor = Color.Black.copy(alpha = 0.25f),
                            spotColor = Color.Black.copy(alpha = 0.35f)
                        )
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isLight) Color.White.copy(alpha = 0.85f)
                            else Color.White.copy(alpha = 0.25f)
                        )
                        .border(
                            width = 0.75.dp,
                            color = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.38f),
                            shape = RoundedCornerShape(999.dp)
                        )
                )

                // Tab Labels
                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = index == selectedIndex
                        val textColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> if (isLight) Color(0xFF0F172A) else TmAccent
                                else -> if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
                            },
                            animationSpec = tween(160),
                            label = "headerTabText"
                        )

                        val label = when (tab) {
                            PeriodTab.DAY -> "今日"
                            PeriodTab.MONTH -> "本月"
                            PeriodTab.TOTAL -> "全部"
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTabSelected(tab)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1:1 Desktop Hero Card:
 * Header Label ("TODAY TOKENS", "THIS MONTH TOKENS", "ALL-TIME TOKENS")
 * Big Tabular Token Number e.g. "7,841,063" + Compact Badge "≈ 7.8M"
 * Cost in USD & CNY e.g. "$1.18 ≈ ¥8.50" + Message & Session count
 */
@Composable
private fun HeroTotalCard(
    period: PeriodTab,
    periodData: PeriodData,
    lastUpdated: Long,
    isWideScreen: Boolean = false
) {
    val periodTitle = when (period) {
        PeriodTab.DAY -> "今日 Token"
        PeriodTab.MONTH -> "本月 Token"
        PeriodTab.TOTAL -> "全部 Token"
    }

    // Token & Cost rolling jumping animation matching desktop animateNumber(from, to, duration = 850)
    val animatedTokens = remember { Animatable(periodData.totalTokens.toFloat()) }
    val animatedCost = remember { Animatable(periodData.costUsd.toFloat()) }

    LaunchedEffect(periodData.totalTokens) {
        animatedTokens.animateTo(
            targetValue = periodData.totalTokens.toFloat(),
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(periodData.costUsd) {
        animatedCost.animateTo(
            targetValue = periodData.costUsd.toFloat(),
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    val currentTokens = animatedTokens.value.toLong().coerceAtLeast(0L)
    val currentCost = animatedCost.value.toDouble().coerceAtLeast(0.0)

    val compactTokenStr = formatCompactTokens(currentTokens)
    val numberFormatted = formatNumber(currentTokens)
    val costUsdFormatted = String.format(Locale.US, "$%.2f", currentCost)
    val costCnyFormatted = String.format(Locale.US, "≈ ¥%.2f", currentCost * 7.23)

    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    TmCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isWideScreen) 6.dp else 0.dp)
        ) {
            // Label Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL TOKENS",
                    fontSize = if (isWideScreen) 12.sp else 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextMuted,
                    letterSpacing = if (isWideScreen) 1.5.sp else 1.2.sp
                )

                Text(
                    text = "更新于 ${formatTime(lastUpdated)}",
                    fontSize = if (isWideScreen) 11.sp else 10.sp,
                    color = TmTextMuted
                )
            }

            Spacer(modifier = Modifier.height(if (isWideScreen) 14.dp else 10.dp))

            // Hero Number + Compact Unit Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isWideScreen) 14.dp else 10.dp)
            ) {
                Text(
                    text = numberFormatted,
                    fontSize = if (isWideScreen) {
                        if (numberFormatted.length > 11) 36.sp else 46.sp
                    } else {
                        if (numberFormatted.length > 11) 28.sp else 36.sp
                    },
                    fontWeight = FontWeight.Bold,
                    color = TmTextPrimary,
                    lineHeight = if (isWideScreen) 50.sp else 40.sp
                )

                // Compact Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TmAccentSoft)
                        .border(
                            0.75.dp,
                            TmAccent.copy(alpha = 0.35f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(
                            horizontal = if (isWideScreen) 10.dp else 7.dp,
                            vertical = if (isWideScreen) 4.dp else 2.dp
                        )
                ) {
                    Text(
                        text = compactTokenStr,
                        fontSize = if (isWideScreen) 14.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TmAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isWideScreen) 18.dp else 14.dp))

            // Bottom Metrics Bar: Cost & Message/Session Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLight) Color(0xFFF8FAFC) else Color(0x24000000))
                    .border(
                        0.5.dp,
                        if (isLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(
                        horizontal = if (isWideScreen) 16.dp else 12.dp,
                        vertical = if (isWideScreen) 13.dp else 10.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cost in USD + CNY
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = costUsdFormatted,
                        fontSize = if (isWideScreen) 18.sp else 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TmTextPrimary
                    )
                    Text(
                        text = costCnyFormatted,
                        fontSize = if (isWideScreen) 13.5.sp else 12.sp,
                        color = TmTextMuted
                    )
                }

                // Messages & Sessions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (periodData.messageCount > 0) {
                        Text(
                            text = "${periodData.messageCount} 请求次数",
                            fontSize = if (isWideScreen) 12.5.sp else 11.sp,
                            color = TmTextSecondary
                        )
                    }
                    if (periodData.sessionCount > 0) {
                        Text(
                            text = "${periodData.sessionCount} 会话",
                            fontSize = if (isWideScreen) 12.5.sp else 11.sp,
                            color = TmTextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. AI Tool Limits & Quotas Card
 * Supports both:
 * - Concentric Dual Rings (side-by-side concentric dual rings with text below for multi-model providers like Antigravity)
 * - Progress Bars (sleek grouped horizontal progress bars with complete details)
 * Includes an option/toggle for users to switch between both presentation modes.
 */
@Composable
private fun AiToolLimitsCard(
    providers: List<ProviderLimit>,
    quotaDisplayStyle: QuotaDisplayStyle = QuotaDisplayStyle.DUAL_RINGS,
    ringOrderConfig: RingOrderConfig = RingOrderConfig.OUTER_5H_INNER_WEEKLY,
    ringCenterTextConfig: RingCenterTextConfig = RingCenterTextConfig.SESSION_5H
) {
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    TmCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "剩余用量",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TmTextMuted,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "${providers.size} 个服务商",
                        fontSize = 10.sp,
                        color = Color(0xFF636366)
                    )
                }
            }

            // Providers
            providers.forEach { provider ->
                ProviderQuotaItem(
                    provider = provider,
                    quotaDisplayStyle = quotaDisplayStyle,
                    ringOrderConfig = ringOrderConfig,
                    ringCenterTextConfig = ringCenterTextConfig
                )
            }
        }
    }
}

@Composable
private fun ProviderQuotaItem(
    provider: ProviderLimit,
    quotaDisplayStyle: QuotaDisplayStyle = QuotaDisplayStyle.DUAL_RINGS,
    ringOrderConfig: RingOrderConfig = RingOrderConfig.OUTER_5H_INNER_WEEKLY,
    ringCenterTextConfig: RingCenterTextConfig = RingCenterTextConfig.SESSION_5H
) {
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    val quotaGroups = remember(provider.windows) {
        groupProviderWindows(provider.windows)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isLight) Color(0xFFF8FAFC) else Color(0x1A000000))
            .border(
                0.5.dp,
                if (isLight) Color(0xFFE2E8F0) else Color(0x1FFFFFFF),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Provider Name, Account Badge & Balance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Provider Brand Icon
                BrandIcon(
                    name = provider.provider,
                    size = 18.dp
                )

                Text(
                    text = provider.provider.replaceFirstChar { it.uppercase() },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextPrimary
                )

                if (provider.accountLabel.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isLight) Color(0xFFE2E8F0) else Color(0x22FFFFFF))
                            .padding(horizontal = 6.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = provider.accountLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            // Balance if present
            if (provider.balanceAmount != null) {
                Text(
                    text = String.format(Locale.US, "%.2f %s", provider.balanceAmount, provider.balanceCurrency ?: "").trim(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmAccent
                )
            }
        }

        // Quota Window Groups
        if (quotaGroups.isNotEmpty()) {
            if (quotaDisplayStyle == QuotaDisplayStyle.PROGRESS_BARS) {
                // Mode 1: Horizontal Progress Bars View
                ProviderQuotaBarsView(
                    groups = quotaGroups,
                    providerName = provider.provider
                )
            } else {
                // Mode 2: Concentric Dual Rings View
                // When 2 models exist (e.g. Antigravity with Gemini + Claude/GPT), render side-by-side in one row, with text below
                if (quotaGroups.size == 2) {
                    QuotaDualRingsSideBySideRow(
                        groups = quotaGroups,
                        providerName = provider.provider,
                        ringOrderConfig = ringOrderConfig,
                        ringCenterTextConfig = ringCenterTextConfig
                    )
                } else if (quotaGroups.size > 2) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        quotaGroups.chunked(2).forEach { pair ->
                            QuotaDualRingsSideBySideRow(
                                groups = pair,
                                providerName = provider.provider,
                                ringOrderConfig = ringOrderConfig,
                                ringCenterTextConfig = ringCenterTextConfig
                            )
                        }
                    }
                } else {
                    // Single group (e.g. Opencode): concentric ring on left, text on right
                    QuotaGroupRow(
                        group = quotaGroups[0],
                        providerName = provider.provider,
                        ringOrderConfig = ringOrderConfig,
                        ringCenterTextConfig = ringCenterTextConfig
                    )
                }
            }
        }
    }
}

/**
 * Antigravity (and 2-model providers): One row with two concentric dual rings side-by-side,
 * with text descriptions placed cleanly underneath each ring.
 */
@Composable
private fun QuotaDualRingsSideBySideRow(
    groups: List<QuotaGroup>,
    providerName: String,
    ringOrderConfig: RingOrderConfig = RingOrderConfig.OUTER_5H_INNER_WEEKLY,
    ringCenterTextConfig: RingCenterTextConfig = RingCenterTextConfig.SESSION_5H
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        groups.forEach { group ->
            Box(modifier = Modifier.weight(1f)) {
                QuotaDualRingColumnCard(
                    group = group,
                    providerName = providerName,
                    ringOrderConfig = ringOrderConfig,
                    ringCenterTextConfig = ringCenterTextConfig
                )
            }
        }
    }
}

@Composable
private fun QuotaDualRingColumnCard(
    group: QuotaGroup,
    providerName: String,
    ringOrderConfig: RingOrderConfig = RingOrderConfig.OUTER_5H_INNER_WEEKLY,
    ringCenterTextConfig: RingCenterTextConfig = RingCenterTextConfig.SESSION_5H
) {
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    val sessionWin = group.sessionWindow
    val weeklyWin = group.weeklyWindow

    val is5hOuter = ringOrderConfig == RingOrderConfig.OUTER_5H_INNER_WEEKLY

    val outerWin = if (is5hOuter) sessionWin else weeklyWin
    val innerWin = if (is5hOuter) weeklyWin else sessionWin

    val sessionRemaining = sessionWin?.remainingPercent?.toFloat()?.coerceIn(0f, 100f) ?: 100f
    val weeklyRemaining = weeklyWin?.remainingPercent?.toFloat()?.coerceIn(0f, 100f) ?: 100f

    val sessionColor = when {
        sessionRemaining < 20f -> TmError
        sessionRemaining < 50f -> TmWarning
        else -> Color(0xFF10B981) // Emerald Green for 5H
    }

    val weeklyColor = when {
        weeklyRemaining < 20f -> TmError
        weeklyRemaining < 50f -> TmWarning
        else -> Color(0xFF0A84FF) // Sky Blue for Weekly
    }

    val outerColor = if (is5hOuter) sessionColor else weeklyColor
    val innerColor = if (is5hOuter) weeklyColor else sessionColor

    val outerRemaining = if (is5hOuter) sessionRemaining else weeklyRemaining
    val innerRemaining = if (is5hOuter) weeklyRemaining else sessionRemaining

    val centerLabel = when (ringCenterTextConfig) {
        RingCenterTextConfig.SESSION_5H -> {
            when {
                sessionWin != null -> "${sessionRemaining.toInt()}%"
                weeklyWin != null -> "${weeklyRemaining.toInt()}%"
                else -> ""
            }
        }
        RingCenterTextConfig.WEEKLY -> {
            when {
                weeklyWin != null -> "${weeklyRemaining.toInt()}%"
                sessionWin != null -> "${sessionRemaining.toInt()}%"
                else -> ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isLight) Color(0x66FFFFFF) else Color(0x0CFFFFFF))
            .border(
                0.5.dp,
                if (isLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dual Ring Gauge centered
        ConcentricDualRingGauge(
            outerPercent = if (outerWin != null) outerRemaining else 0f,
            outerColor = outerColor,
            innerPercent = if (innerWin != null) innerRemaining else 0f,
            innerColor = innerColor,
            centerText = centerLabel,
            hasOuter = outerWin != null,
            hasInner = innerWin != null,
            modifier = Modifier.size(68.dp)
        )

        // Model Title below the rings
        if (group.title.isNotBlank()) {
            Text(
                text = group.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TmTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Text explanations below the rings (Outer first, Inner second)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // First item: Outer Ring
            if (outerWin != null) {
                val rawReset = outerWin.resetsAt ?: outerWin.resetDescription
                val resetText = formatResetTime(rawReset)
                val defaultLbl = if (is5hOuter) "5小时" else "周用量"
                val cleanLabel = cleanWindowLabel(outerWin.label, group.title, defaultLbl)
                val compactReset = resetText?.let { formatCompactResetTime(it) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(outerColor)
                        )
                        Text(
                            text = "外环 · $cleanLabel",
                            fontSize = 10.5.sp,
                            color = TmTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "${outerRemaining.toInt()}%",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = outerColor
                    )
                }

                if (!compactReset.isNullOrBlank()) {
                    Text(
                        text = "重置 $compactReset",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(start = 9.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Second item: Inner Ring
            if (innerWin != null) {
                val rawReset = innerWin.resetsAt ?: innerWin.resetDescription
                val resetText = formatResetTime(rawReset)
                val defaultLbl = if (is5hOuter) "周用量" else "5小时"
                val cleanLabel = cleanWindowLabel(innerWin.label, group.title, defaultLbl)
                val compactReset = resetText?.let { formatCompactResetTime(it) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(innerColor)
                        )
                        Text(
                            text = "内环 · $cleanLabel",
                            fontSize = 10.5.sp,
                            color = TmTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "${innerRemaining.toInt()}%",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = innerColor
                    )
                }

                if (!compactReset.isNullOrBlank()) {
                    Text(
                        text = "重置 $compactReset",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(start = 9.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Extra windows if any
            group.extraWindows.forEach { extra ->
                val rem = extra.remainingPercent.toFloat().coerceIn(0f, 100f)
                val color = when {
                    rem < 20f -> TmError
                    rem < 50f -> TmWarning
                    else -> getProviderColor(providerName)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = extra.label.ifBlank { extra.kind },
                        fontSize = 10.sp,
                        color = TmTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${rem.toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = color
                    )
                }
            }
        }
    }
}

/**
 * Modern grouped progress bars mode for providers.
 */
@Composable
private fun ProviderQuotaBarsView(
    groups: List<QuotaGroup>,
    providerName: String
) {
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        groups.forEach { group ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isLight) Color(0x66FFFFFF) else Color(0x0CFFFFFF))
                    .border(
                        0.5.dp,
                        if (isLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group Title (e.g. Gemini, Claude/GPT) if present
                if (group.title.isNotBlank()) {
                    Text(
                        text = group.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TmTextPrimary
                    )
                }

                // 5-Hour / Session Bar
                group.sessionWindow?.let { sessionWin ->
                    val rem = sessionWin.remainingPercent.toFloat().coerceIn(0f, 100f)
                    val color = when {
                        rem < 20f -> TmError
                        rem < 50f -> TmWarning
                        else -> Color(0xFF30D158) // Emerald Green
                    }
                    val label = cleanWindowLabel(sessionWin.label, group.title, "5小时用量")
                    val rawReset = sessionWin.resetsAt ?: sessionWin.resetDescription
                    val resetText = formatResetTime(rawReset)

                    QuotaProgressBarRow(
                        label = label,
                        percent = rem,
                        color = color,
                        resetText = resetText,
                        isLight = isLight
                    )
                }

                // Weekly Bar
                group.weeklyWindow?.let { weeklyWin ->
                    val rem = weeklyWin.remainingPercent.toFloat().coerceIn(0f, 100f)
                    val color = when {
                        rem < 20f -> TmError
                        rem < 50f -> TmWarning
                        else -> Color(0xFF0A84FF) // Sky Blue
                    }
                    val label = cleanWindowLabel(weeklyWin.label, group.title, "周用量")
                    val rawReset = weeklyWin.resetsAt ?: weeklyWin.resetDescription
                    val resetText = formatResetTime(rawReset)

                    QuotaProgressBarRow(
                        label = label,
                        percent = rem,
                        color = color,
                        resetText = resetText,
                        isLight = isLight
                    )
                }

                // Extra Windows
                group.extraWindows.forEach { extra ->
                    val rem = extra.remainingPercent.toFloat().coerceIn(0f, 100f)
                    val color = when {
                        rem < 20f -> TmError
                        rem < 50f -> TmWarning
                        else -> getProviderColor(providerName)
                    }
                    val rawReset = extra.resetsAt ?: extra.resetDescription
                    val resetText = formatResetTime(rawReset)

                    QuotaProgressBarRow(
                        label = extra.label.ifBlank { extra.kind },
                        percent = rem,
                        color = color,
                        resetText = resetText,
                        isLight = isLight
                    )
                }
            }
        }
    }
}

@Composable
private fun QuotaProgressBarRow(
    label: String,
    percent: Float,
    color: Color,
    resetText: String?,
    isLight: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = TmTextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${percent.toInt()}% 剩余",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
                if (!resetText.isNullOrBlank()) {
                    Text(
                        text = "· $resetText",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.5.dp)
                .clip(RoundedCornerShape(3.25.dp)),
            color = color,
            trackColor = if (isLight) Color(0xFFE2E8F0) else Color(0x33000000),
            strokeCap = StrokeCap.Round
        )
    }
}

private data class QuotaGroup(
    val title: String,
    val sessionWindow: WindowLimit?, // 5-Hour / Session quota (Outer ring)
    val weeklyWindow: WindowLimit?,  // Weekly quota (Inner ring)
    val extraWindows: List<WindowLimit> = emptyList()
)

private fun groupProviderWindows(windows: List<WindowLimit>): List<QuotaGroup> {
    val meterWindows = windows.filter { it.showMeter && it.remainingPercent >= 0.0 }
    if (meterWindows.isEmpty()) return emptyList()

    fun extractGroupKey(label: String): String {
        return label
            .replace(Regex("(?i)\\b(5[- ]?hour|5h|session|weekly|week|monthly|month|quota|limit)\\b"), "")
            .trim()
            .trim('-', '_', '·', ':')
            .trim()
    }

    val grouped = meterWindows.groupBy { extractGroupKey(it.label.ifBlank { it.kind }) }

    return grouped.map { (key, winList) ->
        val session = winList.firstOrNull { w ->
            val t = "${w.kind} ${w.label} ${w.resetDescription.orEmpty()}".lowercase()
            t.contains("5-hour") || t.contains("5 hour") || t.contains("5h") ||
            t.contains("session") || t.contains("short") || t.contains("hourly")
        } ?: winList.firstOrNull()

        val remainingList = winList.filter { it != session }
        val weekly = remainingList.firstOrNull { w ->
            val t = "${w.kind} ${w.label} ${w.resetDescription.orEmpty()}".lowercase()
            t.contains("week") || t.contains("7-day") || t.contains("7d") ||
            t.contains("month") || t.contains("long")
        } ?: remainingList.firstOrNull()

        val extras = remainingList.filter { it != weekly }

        QuotaGroup(
            title = key,
            sessionWindow = session,
            weeklyWindow = weekly,
            extraWindows = extras
        )
    }
}

private fun cleanWindowLabel(fullLabel: String, groupTitle: String, defaultLabel: String): String {
    var s = fullLabel
    if (groupTitle.isNotBlank()) {
        s = s.replace(groupTitle, "", ignoreCase = true).trim('-', '_', '·', ' ')
    }
    val lower = s.lowercase()
    return when {
        lower.contains("5-hour") || lower.contains("5 hour") || lower.contains("5h") -> "5小时"
        lower.contains("session") -> "会话"
        lower.contains("weekly") || lower.contains("week") -> "周用量"
        lower.contains("monthly") || lower.contains("month") -> "月用量"
        s.isNotBlank() -> s
        else -> defaultLabel
    }
}

private fun formatCompactResetTime(formatted: String): String {
    val trimmed = formatted.trim()
    val parts = trimmed.split(" ")
    return if (parts.size >= 2) {
        val date = parts[0].substringAfter("-") // "09-13"
        val time = parts[1] // "00:17"
        "$date $time"
    } else {
        trimmed
    }
}

@Composable
private fun QuotaGroupRow(
    group: QuotaGroup,
    providerName: String,
    ringOrderConfig: RingOrderConfig = RingOrderConfig.OUTER_5H_INNER_WEEKLY,
    ringCenterTextConfig: RingCenterTextConfig = RingCenterTextConfig.SESSION_5H
) {
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    val sessionWin = group.sessionWindow
    val weeklyWin = group.weeklyWindow

    val is5hOuter = ringOrderConfig == RingOrderConfig.OUTER_5H_INNER_WEEKLY

    val outerWin = if (is5hOuter) sessionWin else weeklyWin
    val innerWin = if (is5hOuter) weeklyWin else sessionWin

    val sessionRemaining = sessionWin?.remainingPercent?.toFloat()?.coerceIn(0f, 100f) ?: 100f
    val weeklyRemaining = weeklyWin?.remainingPercent?.toFloat()?.coerceIn(0f, 100f) ?: 100f

    val sessionColor = when {
        sessionRemaining < 20f -> TmError
        sessionRemaining < 50f -> TmWarning
        else -> Color(0xFF10B981) // Emerald Green for 5H
    }

    val weeklyColor = when {
        weeklyRemaining < 20f -> TmError
        weeklyRemaining < 50f -> TmWarning
        else -> Color(0xFF0A84FF) // Sky Blue for Weekly
    }

    val outerColor = if (is5hOuter) sessionColor else weeklyColor
    val innerColor = if (is5hOuter) weeklyColor else sessionColor

    val outerRemaining = if (is5hOuter) sessionRemaining else weeklyRemaining
    val innerRemaining = if (is5hOuter) weeklyRemaining else sessionRemaining

    val centerLabel = when (ringCenterTextConfig) {
        RingCenterTextConfig.SESSION_5H -> {
            when {
                sessionWin != null -> "${sessionRemaining.toInt()}%"
                weeklyWin != null -> "${weeklyRemaining.toInt()}%"
                else -> ""
            }
        }
        RingCenterTextConfig.WEEKLY -> {
            when {
                weeklyWin != null -> "${weeklyRemaining.toInt()}%"
                sessionWin != null -> "${sessionRemaining.toInt()}%"
                else -> ""
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isLight) Color(0x66FFFFFF) else Color(0x0CFFFFFF))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Concentric Dual Ring Gauge
        ConcentricDualRingGauge(
            outerPercent = if (outerWin != null) outerRemaining else 0f,
            outerColor = outerColor,
            innerPercent = if (innerWin != null) innerRemaining else 0f,
            innerColor = innerColor,
            centerText = centerLabel,
            hasOuter = outerWin != null,
            hasInner = innerWin != null,
            modifier = Modifier.size(64.dp)
        )

        // Textual Explanations (Outer first, Inner second)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (group.title.isNotBlank()) {
                Text(
                    text = group.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TmTextPrimary
                )
            }

            // First item: Outer Quota Text
            if (outerWin != null) {
                val rawReset = outerWin.resetsAt ?: outerWin.resetDescription
                val resetText = formatResetTime(rawReset)
                val defaultLbl = if (is5hOuter) "5小时" else "周用量"
                val cleanLabel = cleanWindowLabel(outerWin.label, group.title, defaultLbl)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(outerColor)
                        )
                        Text(
                            text = "外环 · $cleanLabel",
                            fontSize = 11.sp,
                            color = TmTextSecondary
                        )
                    }

                    Text(
                        text = "${outerRemaining.toInt()}% 剩余",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = outerColor
                    )
                }

                if (!resetText.isNullOrBlank()) {
                    Text(
                        text = "重置: $resetText",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(start = 11.dp)
                    )
                }
            }

            // Second item: Inner Quota Text
            if (innerWin != null) {
                val rawReset = innerWin.resetsAt ?: innerWin.resetDescription
                val resetText = formatResetTime(rawReset)
                val defaultLbl = if (is5hOuter) "周用量" else "5小时"
                val cleanLabel = cleanWindowLabel(innerWin.label, group.title, defaultLbl)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(innerColor)
                        )
                        Text(
                            text = "内环 · $cleanLabel",
                            fontSize = 11.sp,
                            color = TmTextSecondary
                        )
                    }

                    Text(
                        text = "${innerRemaining.toInt()}% 剩余",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = innerColor
                    )
                }

                if (!resetText.isNullOrBlank()) {
                    Text(
                        text = "重置: $resetText",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(start = 11.dp)
                    )
                }
            }

            // Extra windows if any
            group.extraWindows.forEach { extra ->
                val rem = extra.remainingPercent.toFloat().coerceIn(0f, 100f)
                val color = when {
                    rem < 20f -> TmError
                    rem < 50f -> TmWarning
                    else -> getProviderColor(providerName)
                }
                val rawReset = extra.resetsAt ?: extra.resetDescription
                val resetText = formatResetTime(rawReset)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = extra.label.ifBlank { extra.kind },
                        fontSize = 10.sp,
                        color = TmTextMuted
                    )
                    Text(
                        text = "${rem.toInt()}% 剩余",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = color
                    )
                }
                if (!resetText.isNullOrBlank()) {
                    Text(
                        text = "重置: $resetText",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConcentricDualRingGauge(
    innerPercent: Float,
    outerPercent: Float,
    innerColor: Color,
    outerColor: Color,
    centerText: String,
    hasInner: Boolean,
    hasOuter: Boolean,
    modifier: Modifier = Modifier
) {
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT
    val trackColor = if (isLight) Color(0xFFE2E8F0) else Color(0x22FFFFFF)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeOuter = 5.dp.toPx()
            val strokeInner = 4.5.dp.toPx()
            val gap = 2.5.dp.toPx()

            val canvasSize = size.minDimension
            val outerRadius = (canvasSize - strokeOuter) / 2f
            val innerRadius = outerRadius - (strokeOuter / 2f) - gap - (strokeInner / 2f)
            val center = Offset(size.width / 2f, size.height / 2f)

            // 1. Outer Ring (5-Hour / Session)
            if (hasOuter) {
                // Background Track
                drawCircle(
                    color = trackColor,
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = strokeOuter)
                )

                // Progress Arc
                val clampedOuter = outerPercent.coerceIn(0f, 100f)
                if (clampedOuter >= 99.9f) {
                    drawCircle(
                        color = outerColor,
                        radius = outerRadius,
                        center = center,
                        style = Stroke(width = strokeOuter)
                    )
                } else if (clampedOuter > 0f) {
                    val sweep = (clampedOuter / 100f) * 360f
                    drawArc(
                        color = outerColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                        size = Size(outerRadius * 2f, outerRadius * 2f),
                        style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                    )
                }
            }

            // 2. Inner Ring (Weekly)
            if (hasInner) {
                // Background Track
                drawCircle(
                    color = trackColor,
                    radius = innerRadius,
                    center = center,
                    style = Stroke(width = strokeInner)
                )

                // Progress Arc
                val clampedInner = innerPercent.coerceIn(0f, 100f)
                if (clampedInner >= 99.9f) {
                    drawCircle(
                        color = innerColor,
                        radius = innerRadius,
                        center = center,
                        style = Stroke(width = strokeInner)
                    )
                } else if (clampedInner > 0f) {
                    val sweep = (clampedInner / 100f) * 360f
                    drawArc(
                        color = innerColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                        size = Size(innerRadius * 2f, innerRadius * 2f),
                        style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Center Value
        if (centerText.isNotBlank()) {
            Text(
                text = centerText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TmTextPrimary,
                maxLines = 1
            )
        }
    }
}

/**
 * 4. Model Usage Breakdown Card
 */
@Composable
private fun ModelBreakdownCard(
    models: List<ModelStat>,
    period: PeriodTab,
    onViewAll: () -> Unit = {}
) {
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    TmCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "模型排行",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextMuted,
                    letterSpacing = 1.2.sp
                )

                if (models.size > 7) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onViewAll() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "共 ${models.size} 个模型",
                            fontSize = 10.5.sp,
                            color = TmTextMuted
                        )
                        Text(
                            text = "查看全部 ›",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TmAccent
                        )
                    }
                } else {
                    Text(
                        text = "共 ${models.size} 个模型",
                        fontSize = 10.5.sp,
                        color = TmTextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            models.take(7).forEach { modelStat ->
                ModelRowItem(model = modelStat)
            }

            if (models.size > 7) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLight) Color(0xFFF1F5F9) else Color(0x18FFFFFF))
                        .clickable { onViewAll() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "查看全部 ${models.size} 个模型 ›",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TmPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelRowItem(model: ModelStat) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val brandName = BrandHelper.resolveModelBrandName(model.model, model.client)
                BrandIcon(
                    name = brandName,
                    size = 15.dp
                )

                Text(
                    text = model.model,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TmTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (model.client.isNotBlank() && model.client != "all") {
                    BrandBadge(
                        name = model.client,
                        label = model.client
                    )
                }
            }

            // Tokens & Share %
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatCompactTokens(model.tokens),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextPrimary
                )

                Text(
                    text = String.format(Locale.US, "%.1f%%", model.share * 100f),
                    fontSize = 11.sp,
                    color = TmTextMuted,
                    modifier = Modifier.width(42.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        val isLight = LocalThemeMode.current == AppThemeMode.LIGHT
        // Sub bar
        LinearProgressIndicator(
            progress = { model.share.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp)),
            color = if (isLight) TmPrimary else Color(0xFF64D2FF),
            trackColor = if (isLight) Color(0xFFE2E8F0) else Color(0x14FFFFFF),
            strokeCap = StrokeCap.Round
        )
    }
}

/**
 * 5. Tools / Clients Distribution Card
 */
@Composable
private fun ToolDistributionCard(
    clients: List<ClientStat>,
    onViewAll: () -> Unit = {}
) {
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

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
                    text = "客户端用量",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextMuted,
                    letterSpacing = 1.2.sp
                )

                if (clients.size > 7) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onViewAll() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "共 ${clients.size} 个客户端",
                            fontSize = 10.5.sp,
                            color = TmTextMuted
                        )
                        Text(
                            text = "查看全部 ›",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TmAccent
                        )
                    }
                } else {
                    Text(
                        text = "共 ${clients.size} 个客户端",
                        fontSize = 10.5.sp,
                        color = TmTextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            clients.take(7).forEach { client ->
                ClientRowItem(client = client)
            }

            if (clients.size > 7) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLight) Color(0xFFF1F5F9) else Color(0x18FFFFFF))
                        .clickable { onViewAll() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "查看全部 ${clients.size} 个客户端 ›",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TmPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ClientRowItem(client: ClientStat) {
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrandIcon(
                    name = client.name,
                    size = 16.dp
                )
                Text(
                    text = client.name.replaceFirstChar { it.uppercase() },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TmTextPrimary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCompactTokens(client.tokens),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextPrimary
                )
                Text(
                    text = String.format(Locale.US, "%.1f%%", client.share * 100f),
                    fontSize = 11.sp,
                    color = TmTextMuted,
                    modifier = Modifier.width(42.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        LinearProgressIndicator(
            progress = { client.share.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp)),
            color = if (isLight) TmPrimary else Color(0xFF64D2FF),
            trackColor = if (isLight) Color(0xFFE2E8F0) else Color(0x14FFFFFF),
            strokeCap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllModelsSheet(
    models: List<ModelStat>,
    period: PeriodTab,
    onDismiss: () -> Unit
) {
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT
    val periodLabel = when (period) {
        PeriodTab.DAY -> "今日"
        PeriodTab.MONTH -> "本月"
        PeriodTab.TOTAL -> "全部"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF1E2128),
        scrimColor = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "全部模型用量名单",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TmTextPrimary
                    )
                    Text(
                        text = "$periodLabel · 共 ${models.size} 个模型",
                        fontSize = 12.sp,
                        color = TmTextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isLight) Color(0xFFF1F5F9) else Color(0x22FFFFFF))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        fontSize = 13.sp,
                        color = if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Lazy list of all models
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(models, key = { index, modelStat -> "${modelStat.model}_$index" }) { index, modelStat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLight) Color(0xFFF8FAFC) else Color(0x14000000))
                            .border(
                                0.5.dp,
                                if (isLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Rank
                        Text(
                            text = "#${index + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (index < 3) TmAccent else TmTextMuted,
                            modifier = Modifier.width(26.dp)
                        )

                        // Model info
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val brandName = BrandHelper.resolveModelBrandName(modelStat.model, modelStat.client)
                                BrandIcon(name = brandName, size = 15.dp)

                                Text(
                                    text = modelStat.model,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TmTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (modelStat.client.isNotBlank() && modelStat.client != "all") {
                                    BrandBadge(name = modelStat.client, label = modelStat.client)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { modelStat.share.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = TmAccent,
                                    trackColor = if (isLight) Color(0xFFE2E8F0) else Color(0x1AFFFFFF)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", modelStat.share * 100f),
                                    fontSize = 10.sp,
                                    color = TmTextMuted
                                )
                            }
                        }

                        // Tokens & Cost
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = formatCompactTokens(modelStat.tokens),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TmTextPrimary
                            )
                            if (modelStat.cost > 0.0) {
                                Text(
                                    text = String.format(Locale.US, "$%.2f", modelStat.cost),
                                    fontSize = 11.sp,
                                    color = TmAccentSoft
                                )
                            } else {
                                Text(
                                    text = "${formatNumber(modelStat.tokens)} t",
                                    fontSize = 10.sp,
                                    color = TmTextMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllClientsSheet(
    clients: List<ClientStat>,
    onDismiss: () -> Unit
) {
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isLight) Color(0xFFFFFFFF) else Color(0xFF1E2128),
        scrimColor = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "全部客户端用量名单",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TmTextPrimary
                    )
                    Text(
                        text = "共 ${clients.size} 个客户端",
                        fontSize = 12.sp,
                        color = TmTextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isLight) Color(0xFFF1F5F9) else Color(0x22FFFFFF))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        fontSize = 13.sp,
                        color = if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Lazy list of all clients
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(clients, key = { index, client -> "${client.name}_$index" }) { index, client ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLight) Color(0xFFF8FAFC) else Color(0x14000000))
                            .border(
                                0.5.dp,
                                if (isLight) Color(0xFFE2E8F0) else Color(0x18FFFFFF),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Rank
                        Text(
                            text = "#${index + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (index < 3) TmAccent else TmTextMuted,
                            modifier = Modifier.width(26.dp)
                        )

                        // Client info
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BrandIcon(name = client.name, size = 16.dp)
                                Text(
                                    text = client.name.replaceFirstChar { it.uppercase() },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TmTextPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { client.share.coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = if (isLight) TmPrimary else Color(0xFF64D2FF),
                                    trackColor = if (isLight) Color(0xFFE2E8F0) else Color(0x1AFFFFFF)
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", client.share * 100f),
                                    fontSize = 10.sp,
                                    color = TmTextMuted
                                )
                            }
                        }

                        // Tokens & Cost
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = formatCompactTokens(client.tokens),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TmTextPrimary
                            )
                            if (client.cost > 0.0) {
                                Text(
                                    text = String.format(Locale.US, "$%.2f", client.cost),
                                    fontSize = 11.sp,
                                    color = TmAccentSoft
                                )
                            } else {
                                Text(
                                    text = "${formatNumber(client.tokens)} t",
                                    fontSize = 10.sp,
                                    color = TmTextMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

/**
 * 6. 7-Day Usage Trend Card
 */
@Composable
private fun SevenDayTrendCard(
    daily: List<DailyHistory>,
    peakDailyTokens: Long
) {
    val visibleDays = daily.takeLast(7)
    val maxTokens = (visibleDays.maxOfOrNull { it.tokens } ?: 1L).coerceAtLeast(1L)

    TmCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "7-DAY TREND · 用量趋势",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TmTextMuted,
                    letterSpacing = 1.2.sp
                )

                Text(
                    text = "峰值 ${formatCompactTokens(peakDailyTokens)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TmAccent
                )
            }

            // 7 Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                visibleDays.forEach { day ->
                    val ratio = (day.tokens.toFloat() / maxTokens.toFloat()).coerceIn(0.04f, 1f)
                    val isPeak = day.tokens == maxTokens

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = formatShortTokens(day.tokens),
                            fontSize = 9.sp,
                            color = if (isPeak) TmAccent else Color(0xFF64748B),
                            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Bar
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((70 * ratio).dp)
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(
                                    if (isPeak) {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFFB7EAD4), Color(0xFF38BDF8))
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF0A84FF), Color(0xFF1E3A8A))
                                        )
                                    }
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Date
                        Text(
                            text = day.date.takeLast(5),
                            fontSize = 9.sp,
                            color = Color(0xFF98989D)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ErrorBanner(
    message: String,
    hasCachedData: Boolean = false,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (hasCachedData) Color(0x22FF9F0A) else Color(0xFF241417))
            .border(1.dp, if (hasCachedData) Color(0x66FF9F0A) else Color(0x55FF453A), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
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
                    text = if (hasCachedData) "连接已断开 (已保留离线数据)" else "网络通讯异常",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasCachedData) Color(0xFFFF9F0A) else TmError
                )
                Text(
                    text = message,
                    fontSize = 11.sp,
                    color = if (hasCachedData) Color(0xFFFFE5B4) else Color(0xFFFFD1D1),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "重试",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TmAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onRetry() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// Helpers
internal fun formatNumber(num: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(num)
}

internal fun formatCompactTokens(num: Long): String {
    return when {
        num >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", num / 1_000_000_000.0)
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString()
    }
}

private fun formatShortTokens(num: Long): String {
    return when {
        num >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", num / 1_000_000_000.0)
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.0fK", num / 1_000.0)
        else -> num.toString()
    }
}

private fun formatTime(ms: Long): String {
    return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(ms))
}

/**
 * Formats a reset timestamp (ISO-8601 or epoch) into the user's actual device timezone.
 * Example: "2026-09-11T07:39:29.000Z" -> "2026-09-11 15:39" (in Asia/Shanghai / CST)
 */
private fun formatResetTime(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val trimmed = raw.trim()

    // 1. Numeric timestamp (e.g. 1726040400000 or 1726040400)
    val numeric = trimmed.toLongOrNull()
    if (numeric != null && numeric > 1_000_000_000L) {
        return try {
            val epochMs = if (numeric > 100_000_000_000L) numeric else numeric * 1000L
            val instant = Instant.ofEpochMilli(epochMs)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
            instant.atZone(ZoneId.systemDefault()).format(formatter)
        } catch (_: Exception) {
            trimmed
        }
    }

    // 2. ISO-8601 or other standard date string
    return try {
        val instant = try {
            Instant.parse(trimmed)
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(trimmed).toInstant()
            } catch (_: Exception) {
                try {
                    ZonedDateTime.parse(trimmed).toInstant()
                } catch (_: Exception) {
                    val normalized = trimmed.replace(" ", "T")
                    val withTz = if (!normalized.endsWith("Z") && !normalized.contains("+") && !normalized.substringAfter("T").contains("-")) {
                        normalized + "Z"
                    } else {
                        normalized
                    }
                    Instant.parse(withTz)
                }
            }
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
        instant.atZone(ZoneId.systemDefault()).format(formatter)
    } catch (_: Exception) {
        trimmed
    }
}

private fun getProviderColor(provider: String): Color {
    return BrandHelper.getBrandColor(provider)
}

@SuppressLint("MissingPermission")
@Composable
private fun AmbientBackdrop() {
    val themeMode = LocalThemeMode.current
    val context = LocalContext.current

    when (themeMode) {
        AppThemeMode.WALLPAPER -> {
            val wallpaperBitmap = remember {
                try {
                    val wm = WallpaperManager.getInstance(context)
                    val drawable = wm.drawable ?: wm.peekDrawable()
                    drawable?.toBitmap()?.asImageBitmap()
                } catch (_: Throwable) {
                    null
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (wallpaperBitmap != null) {
                    Image(
                        bitmap = wallpaperBitmap,
                        contentDescription = "System Wallpaper",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Subtle transparent vignette/darkening veil to guarantee high contrast for text & gauges
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x35000000))
                )
            }
        }

        AppThemeMode.LIGHT -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFF8FAFC),
                                Color(0xFFEDF2F7),
                                Color(0xFFE2E8F0)
                            )
                        )
                    )
            )
        }

        AppThemeMode.DARK -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF060912),
                                Color(0xFF0A0F1D),
                                Color(0xFF05070D)
                            )
                        )
                    )
            )
        }
    }
}
