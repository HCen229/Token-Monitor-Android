package com.tokenmonitor.app.ui.glass

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.tokenmonitor.app.ui.theme.AppThemeMode
import com.tokenmonitor.app.ui.theme.LocalThemeMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class LiquidButtonTone {
    PRIMARY,
    SECONDARY,
    NEUTRAL,
    DANGER
}

@Composable
fun LiquidActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: Backdrop? = null,
    tint: Color = Color(0xFF2563EB),
    tone: LiquidButtonTone = LiquidButtonTone.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val bgColor = when (tone) {
        LiquidButtonTone.PRIMARY -> tint
        LiquidButtonTone.SECONDARY -> Color(0xFF1E2433)
        LiquidButtonTone.NEUTRAL -> Color(0xFF161B26)
        LiquidButtonTone.DANGER -> Color(0xFFDC2626)
    }

    val borderColor = when (tone) {
        LiquidButtonTone.PRIMARY -> Color.Transparent
        LiquidButtonTone.SECONDARY -> Color(0x33FFFFFF)
        LiquidButtonTone.NEUTRAL -> Color(0x22FFFFFF)
        LiquidButtonTone.DANGER -> Color.Transparent
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !loading) 0.96f else 1f,
        animationSpec = tween(120),
        label = "btnScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) bgColor else bgColor.copy(alpha = 0.5f))
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clickable(
                enabled = enabled && !loading,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            }
            content()
        }
    }
}

/**
 * 1:1 Desktop Replica Segmented Period Tabs:
 * Displays DAY, MONTH, TOTAL with a sliding liquid indicator pill.
 */
@Composable
fun <T> LiquidSegmentedTabs(
    tabs: List<T>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: Backdrop? = LocalLiquidGlassBackdrop.current,
    tabLabel: (T) -> String
) {
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val haptics = LocalHapticFeedback.current

    LiquidGlassSurface(
        hazeState = hazeState,
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        variant = LiquidMaterial.CLEAR,
        tint = Color(0xFF16181E),
        borderWidth = 0.8.dp,
        borderColor = Color(0x28FFFFFF),
        contentPadding = PaddingValues(2.5.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabCount = tabs.size
            val tabWidth = maxWidth / tabCount

            val indicatorOffset by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "pillIndicator"
            )

            // Sliding indicator pill matching desktop `.tab-indicator`
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .offset { IntOffset((indicatorOffset * tabWidth.toPx()).roundToInt(), 0) }
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0x33FFFFFF))
                    .border(0.75.dp, Color(0x3DFFFFFF), RoundedCornerShape(7.dp))
            )

            // Tab labels
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFFB7EAD4) else Color(0xFF98989D),
                        animationSpec = tween(150),
                        label = "tabText"
                    )

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
                            text = tabLabel(tab),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2-Tab Navigation (Home vs Settings)
 */
enum class HomeNavTab {
    HOME,
    SETTINGS
}

/**
 * iOS-style narrowed floating liquid dock with a sliding optical lens (1:1 Niskle-Link design).
 * Features smooth drag physics, rubber-band overshooting, spring settling, and tactile haptics.
 */
@Composable
fun LiquidBottomBar(
    selected: HomeNavTab,
    onSelect: (HomeNavTab) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: Backdrop? = LocalLiquidGlassBackdrop.current
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val animationScope = rememberCoroutineScope()
    val supportsOpticalGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val dockContentBackdrop = if (supportsOpticalGlass) rememberLayerBackdrop() else null
    val combinedLensBackdrop = if (hazeState != null && dockContentBackdrop != null) {
        rememberCombinedBackdrop(hazeState, dockContentBackdrop)
    } else {
        dockContentBackdrop ?: hazeState
    }
    val barSurfaceColor = Color(0xFF141722)

    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragVelocity by remember { mutableFloatStateOf(0f) }
    var fluidMomentumVelocity by remember { mutableFloatStateOf(0f) }
    var settleInitialVelocity by remember { mutableFloatStateOf(0f) }
    var activeTouchPosition by remember { mutableStateOf<Offset?>(null) }
    var settleTarget by remember {
        mutableFloatStateOf(if (selected == HomeNavTab.HOME) 0f else 1f)
    }
    var settleRequestId by remember { mutableIntStateOf(0) }
    var isAnimatingSettle by remember { mutableStateOf(false) }
    val selectionPosition = remember {
        Animatable(if (selected == HomeNavTab.HOME) 0f else 1f)
    }

    LaunchedEffect(selected) {
        val destination = if (selected == HomeNavTab.HOME) 0f else 1f
        if (destination != settleTarget) {
            settleTarget = destination
            settleInitialVelocity = 0f
            settleRequestId += 1
        }
    }

    LaunchedEffect(settleRequestId) {
        if (!dragging) {
            isAnimatingSettle = true
            try {
                // Graceful gliding settle with initial velocity from fling gesture
                selectionPosition.animateTo(
                    targetValue = settleTarget,
                    initialVelocity = settleInitialVelocity,
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = 260f
                    )
                )
            } finally {
                isAnimatingSettle = false
                settleInitialVelocity = 0f
            }
        }
    }

    fun requestSelection(target: HomeNavTab) {
        val dest = if (target == HomeNavTab.HOME) 0f else 1f
        if (dest != settleTarget) {
            settleTarget = dest
            settleInitialVelocity = 0f
            settleRequestId += 1
            onSelect(target)
        }
    }

    val barContentInset = 6.dp
    val barContentInsetPx = with(density) { barContentInset.toPx() }
    val barWidthPx = with(density) { 184.dp.toPx() }
    val innerWidthPx = (barWidthPx - barContentInsetPx * 2f).coerceAtLeast(1f)
    val itemWidthPx = innerWidthPx / 2f

    // Real-time velocity and momentum tracking for fluid physics deformation
    val currentVx = if (dragging) dragVelocity else (selectionPosition.velocity * itemWidthPx)
    val fluidVx = if (dragging) fluidMomentumVelocity else currentVx

    // Fluid deformation target:
    // Positive (> 0): Stretch along movement direction (变长、变扁)
    // Negative (< 0): Squash against opposing momentum / reversal / braking (变窄、变高)
    val targetDeform = if (!dragging && !isAnimatingSettle) {
        0f
    } else {
        val isReversing = (currentVx * fluidVx) < -5000f
        val isBraking = abs(fluidVx) > 250f && abs(currentVx) < abs(fluidVx) * 0.30f

        if (isReversing) {
            // Sudden counter-movement: e.g. dragging left and suddenly pulling right!
            // Compressive collision squashes horizontally (narrow) and bulges vertically (tall)
            val reversalSpeed = abs(currentVx - fluidVx)
            val squashIntensity = (reversalSpeed / 1800f).coerceIn(0f, 1f)
            -squashIntensity
        } else if (isBraking) {
            // Deceleration: fluid inertia presses into stationary point
            val brakeDiff = abs(fluidVx) - abs(currentVx)
            val brakeSquash = (brakeDiff / 1400f).coerceIn(0f, 0.65f)
            -brakeSquash
        } else {
            // Steady motion: fluid stretches along velocity axis
            val speed = abs(currentVx)
            (speed / 1900f).coerceIn(0f, 1f)
        }
    }

    val fluidDeform by animateFloatAsState(
        targetValue = targetDeform,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 450f
        ),
        label = "fluidDeform"
    )

    // Slow, luxurious fading for the 3D optical lens upon release
    val dragActivation by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 220f),
        label = "tabDragActivation"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (dragging) 1.38f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 220f),
        label = "tabLensScale"
    )

    // Slow, gentle, organic breathing contraction & release for bottom bar
    val carrierScaleY by animateFloatAsState(
        targetValue = if (dragging) {
            if (fluidDeform >= 0f) 0.85f - fluidDeform * 0.03f
            else 0.85f + (-fluidDeform) * 0.05f
        } else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 180f
        ),
        label = "carrierScaleY"
    )
    val carrierScaleX by animateFloatAsState(
        targetValue = if (dragging) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 180f
        ),
        label = "carrierScaleX"
    )

    val barActivation = dragActivation
    val isActivated = dragging

    val minimumLensCenter = itemWidthPx / 2f
    val maximumLensCenter = innerWidthPx - itemWidthPx / 2f
    val edgeOvershoot = when {
        !dragging -> 0f
        dragX < minimumLensCenter -> dragX - minimumLensCenter
        dragX > maximumLensCenter -> dragX - maximumLensCenter
        else -> 0f
    }
    val maximumBarPullPx = with(density) { 24.dp.toPx() }
    val barPullTarget = if (edgeOvershoot == 0f) {
        0f
    } else {
        val direction = if (edgeOvershoot < 0f) -1f else 1f
        val magnitude = abs(edgeOvershoot)
        direction * maximumBarPullPx * (magnitude / (magnitude + maximumBarPullPx * 2.5f))
    }
    val barPullX by animateFloatAsState(
        targetValue = barPullTarget,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 240f),
        label = "tabBarEdgePull"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(barPullX.roundToInt(), 0) }
            .width(184.dp)
            .height(64.dp)
            .pointerInput(selected, barContentInsetPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val gestureInnerWidth = (size.width.toFloat() - barContentInsetPx * 2f).coerceAtLeast(1f)
                    val gestureItemWidth = gestureInnerWidth / 2f
                    val currentCenter = gestureItemWidth / 2f + selectionPosition.value * gestureItemWidth
                    val downInsideBar = down.position.x - barContentInsetPx
                    val startedOnLens = abs(downInsideBar - currentCenter) <= gestureItemWidth / 2f

                    if (!startedOnLens) return@awaitEachGesture

                    down.consume()
                    animationScope.launch { selectionPosition.stop() }
                    dragging = true
                    dragX = currentCenter
                    activeTouchPosition = down.position
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val fingerOffsetFromCenter = downInsideBar - currentCenter
                    var cancelled = false

                    var lastTime = down.uptimeMillis
                    var lastX = down.position.x
                    var smoothedVx = 0f
                    var fluidMomentumVx = 0f

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null) {
                            cancelled = true
                            break
                        }
                        activeTouchPosition = change.position
                        if (!change.pressed) {
                            change.consume()
                            break
                        }

                        val now = change.uptimeMillis
                        val dt = (now - lastTime).coerceAtLeast(1L)
                        val dx = change.position.x - lastX
                        if (dt < 100L) {
                            val instantVx = (dx / (dt / 1000f)) // px/s
                            smoothedVx = smoothedVx * 0.35f + instantVx * 0.65f
                            fluidMomentumVx = fluidMomentumVx * 0.82f + instantVx * 0.18f
                            dragVelocity = smoothedVx
                            fluidMomentumVelocity = fluidMomentumVx
                        }
                        lastTime = now
                        lastX = change.position.x

                        dragX = change.position.x - barContentInsetPx - fingerOffsetFromCenter
                        change.consume()
                    }

                    val releaseVx = smoothedVx
                    val clampedCenter = dragX.coerceIn(
                        gestureItemWidth / 2f,
                        gestureInnerWidth - gestureItemWidth / 2f
                    )
                    val releasePosition = ((clampedCenter - gestureItemWidth / 2f) / gestureItemWidth).coerceIn(0f, 1f)

                    // Velocity-aware target selection: rapid flick triggers directional tab switch
                    val targetTab = when {
                        cancelled -> if (selected == HomeNavTab.HOME) HomeNavTab.HOME else HomeNavTab.SETTINGS
                        releaseVx > 450f -> HomeNavTab.SETTINGS
                        releaseVx < -450f -> HomeNavTab.HOME
                        clampedCenter < gestureInnerWidth / 2f -> HomeNavTab.HOME
                        else -> HomeNavTab.SETTINGS
                    }
                    val targetPos = if (targetTab == HomeNavTab.HOME) 0f else 1f
                    val flingUnits = (releaseVx / gestureItemWidth).coerceIn(-14f, 14f)

                    animationScope.launch {
                        selectionPosition.snapTo(releasePosition)
                        activeTouchPosition = null
                        dragging = false
                        dragVelocity = 0f
                        fluidMomentumVelocity = 0f

                        settleInitialVelocity = flingUnits
                        settleTarget = targetPos
                        settleRequestId += 1
                        if (!cancelled) {
                            onSelect(targetTab)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val dockContentModifier = if (dockContentBackdrop != null) {
            Modifier.layerBackdrop(dockContentBackdrop)
        } else {
            Modifier
        }

        // Dock Content Layer (Carrier + Idle Indicator Mask + Navigation Items)
        // Captured by dockContentBackdrop so the top optical lens can refract ALL of them!
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(dockContentModifier)
        ) {
            // 1. Base carrier (contracts on touch/drag with slow, elegant spring)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = carrierScaleX
                        scaleY = carrierScaleY
                    }
                    .shadow(
                        elevation = 10.dp * (1f - dragActivation * 0.20f),
                        shape = RoundedCornerShape(32.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.22f),
                        spotColor = Color.Black.copy(alpha = 0.25f)
                    )
            ) {
                val themeMode = LocalThemeMode.current
                val isLight = themeMode == AppThemeMode.LIGHT

                val carrierBorderBrush = if (isLight) {
                    Brush.verticalGradient(
                        listOf(
                            Color(0x30000000),
                            Color(0x12000000),
                            Color(0x04000000)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color(0x55FFFFFF),
                            Color(0x22FFFFFF),
                            Color(0x0EFFFFFF)
                        )
                    )
                }

                val greyMaskColor = if (isLight) {
                    Color(0xFF64748B).copy(alpha = 0.06f)
                } else {
                    Color(0xFF64748B).copy(alpha = 0.12f)
                }

                // Pure liquid optical glass base surface (纯液态玻璃)
                LiquidGlassSurface(
                    hazeState = hazeState,
                    modifier = Modifier.matchParentSize(),
                    shape = RoundedCornerShape(32.dp),
                    variant = LiquidMaterial.CLEAR,
                    blurRadius = 6.dp,
                    tint = Color.White,
                    surfaceAlpha = 0.001f, // pure optical glass
                    enableBlur = true,
                    enableRefraction = true,
                    refractionScale = 0.85f,
                    selected = false,
                    pressed = false,
                    borderBrush = carrierBorderBrush,
                    borderWidth = 1.dp
                ) {
                    // Transparent grey mask layer (在玻璃层放一层透明的灰色遮罩)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                            .background(greyMaskColor)
                    )
                }
            }

            // 2. Idle translucent mask pill (未激活态：稍微有些遮罩透明底，位于文字图标下方)
            if (dragActivation < 0.999f) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(barContentInset),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val itemWidth = maxWidth / 2
                    val itemWidthPx = with(density) { itemWidth.toPx() }
                    val lensX = if (dragging) {
                        (dragX - itemWidthPx / 2f).coerceIn(0f, itemWidthPx)
                    } else {
                        selectionPosition.value * itemWidthPx
                    }
                    val currentScaleX = if (fluidDeform >= 0f) {
                        1f + fluidDeform * 0.12f
                    } else {
                        (1f - (-fluidDeform) * 0.08f).coerceAtLeast(0.92f)
                    }
                    val lensWidth = itemWidth * currentScaleX
                    val lensHeight = maxHeight
                    val lensWidthPx = with(density) { lensWidth.toPx() }
                    val lensLeftPx = lensX - (lensWidthPx - itemWidthPx) / 2f

                    val isLight = LocalThemeMode.current == AppThemeMode.LIGHT
                    val idleMaskAlpha = if (isLight) 0.10f else 0.20f
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(lensLeftPx.roundToInt(), 0) }
                            .requiredSize(width = lensWidth, height = lensHeight)
                            .alpha(1f - dragActivation)
                            .clip(RoundedCornerShape(26.dp))
                            .background(
                                if (isLight) Color.Black.copy(alpha = idleMaskAlpha)
                                else Color.White.copy(alpha = idleMaskAlpha)
                            )
                    )
                }
            }

            // 3. Navigation items (主页 / 设置 图标与文字)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(barContentInset)
            ) {
                LiquidNavItem(
                    label = "主页",
                    icon = com.tokenmonitor.app.R.drawable.ic_nav_home,
                    selected = selected == HomeNavTab.HOME,
                    isActivated = isActivated,
                    onClick = { requestSelection(HomeNavTab.HOME) }
                )
                LiquidNavItem(
                    label = "设置",
                    icon = com.tokenmonitor.app.R.drawable.ic_nav_settings,
                    selected = selected == HomeNavTab.SETTINGS,
                    isActivated = isActivated,
                    onClick = { requestSelection(HomeNavTab.SETTINGS) }
                )
            }
        }

        // 4. Activated 3D Optical Convex Lens (TOPMOST LAYER)
        // Placed on top of EVERYTHING: Refracts Carrier + Nav Items + Screen underneath!
        if (dragActivation > 0.001f) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(barContentInset),
                contentAlignment = Alignment.CenterStart
            ) {
                val itemWidth = maxWidth / 2
                val itemWidthPx = with(density) { itemWidth.toPx() }
                val rawLensX = if (dragging) {
                    (dragX - itemWidthPx / 2f)
                } else {
                    selectionPosition.value * itemWidthPx
                }
                // Elastic rubber-banding when dragged beyond dock bounds
                val clampedLensX = rawLensX.coerceIn(0f, itemWidthPx)
                val edgeExcess = rawLensX - clampedLensX
                val rubberBandExcess = if (edgeExcess != 0f) {
                    val sign = if (edgeExcess > 0f) 1f else -1f
                    val mag = abs(edgeExcess)
                    val maxPull = with(density) { 16.dp.toPx() }
                    sign * maxPull * (mag / (mag + maxPull * 1.8f))
                } else 0f
                val baseLensX = clampedLensX + rubberBandExcess

                // Fluid inertia lag: center trails slightly behind rapid touch movement
                val maxLagPx = with(density) { 12.dp.toPx() }
                val normalizedVx = (fluidVx / 2000f).coerceIn(-1f, 1f)
                val fluidLagX = normalizedVx * maxLagPx

                // Speed & directional reversal deformation:
                // When fluidDeform >= 0: stretches horizontally (wide), squashes vertically (flat)
                // When fluidDeform < 0: squashes horizontally (narrow), bulges vertically (tall)
                val currentScaleX: Float
                val currentScaleY: Float
                if (fluidDeform >= 0f) {
                    val stretchX = fluidDeform * 0.48f
                    val squashY = fluidDeform * 0.18f
                    currentScaleX = pressScale + stretchX
                    currentScaleY = 1f + (pressScale - 1f) * (0.95f - squashY)
                } else {
                    val compression = -fluidDeform // 0f .. 1f
                    val shrinkX = compression * 0.36f // scaleX drops to ~1.02 (显著变窄!)
                    val bulgeY = compression * 0.28f  // scaleY rises to ~1.64 (显著变高!)
                    currentScaleX = (pressScale - shrinkX).coerceAtLeast(1.0f)
                    currentScaleY = 1f + (pressScale - 1f) * 0.95f + bulgeY
                }

                val lensWidth = itemWidth * currentScaleX
                val lensHeight = maxHeight * currentScaleY
                val lensWidthPx = with(density) { lensWidth.toPx() }
                val lensLeftPx = (baseLensX - fluidLagX) - (lensWidthPx - itemWidthPx) / 2f
                val fluidDroopY = if (fluidDeform >= 0f) {
                    (fluidDeform * with(density) { 2.dp.toPx() }).roundToInt()
                } else {
                    0
                }

                val isLight = LocalThemeMode.current == AppThemeMode.LIGHT
                val sliderBorderBrush = if (isLight) {
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = (0.35f * dragActivation).coerceAtMost(0.40f)),
                            Color.Black.copy(alpha = 0.18f * dragActivation)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = (0.55f * dragActivation).coerceAtMost(0.65f)),
                            Color.White.copy(alpha = 0.22f * dragActivation)
                        )
                    )
                }

                val dynamicRefractionAmount = (14 + abs(fluidDeform) * 5).dp
                val dynamicRefractionScale = 1.0f + abs(fluidDeform) * 0.20f

                LiquidGlassSurface(
                    hazeState = combinedLensBackdrop, // Refracts Screen + Carrier + Icons & Text!
                    modifier = Modifier
                        .offset { IntOffset(lensLeftPx.roundToInt(), fluidDroopY) }
                        .requiredSize(width = lensWidth, height = lensHeight)
                        .alpha(dragActivation),
                    shape = RoundedCornerShape(999.dp),
                    variant = LiquidMaterial.CLEAR,
                    tint = Color.White,
                    selected = true,
                    pressed = true,
                    surfaceAlpha = 0.012f,
                    enableBlur = false,
                    enableRefraction = true,
                    refractionScale = dynamicRefractionScale,
                    refractionHeight = 16.dp,
                    refractionAmount = dynamicRefractionAmount,
                    borderBrush = sliderBorderBrush,
                    borderWidth = 1.dp
                ) {}
            }
        }
    }
}

@Composable
private fun RowScope.LiquidNavItem(
    label: String,
    icon: Int,
    selected: Boolean,
    isActivated: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (selected && isActivated) 1.08f else if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 240f),
        label = "navItemPress"
    )
    val themeMode = LocalThemeMode.current
    val unselectedColor = if (themeMode == AppThemeMode.LIGHT) Color(0xFF64748B) else Color(0xFF8E8E93)
    val color by animateColorAsState(
        targetValue = if (selected) com.tokenmonitor.app.ui.theme.TmAccent else unselectedColor,
        animationSpec = tween(durationMillis = 180),
        label = "navItemColor"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .scale(scale)
            .clip(RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

