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



