package com.tokenmonitor.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tokenmonitor.app.R
import com.tokenmonitor.app.data.IslandConfig
import com.tokenmonitor.app.data.IslandItemType
import com.tokenmonitor.app.data.TokenStats
import com.tokenmonitor.app.service.TokenNotificationManager
import com.tokenmonitor.app.ui.theme.TmAccent
import com.tokenmonitor.app.ui.theme.TmLive
import com.tokenmonitor.app.ui.theme.TmPrimary
import com.tokenmonitor.app.ui.theme.TmTextMuted
import com.tokenmonitor.app.ui.theme.TmTextPrimary
import com.tokenmonitor.app.ui.theme.TmTextSecondary
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuperIslandStudio(
    config: IslandConfig,
    stats: TokenStats?,
    isConnected: Boolean,
    onConfigChange: (IslandConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    // Studio root layout coordinate tracker for 100% scroll-immune drag & drop
    var studioCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var leftSlotBoundsInStudio by remember { mutableStateOf<Rect?>(null) }
    var rightSlotBoundsInStudio by remember { mutableStateOf<Rect?>(null) }

    // Active drag state
    var draggingItem by remember { mutableStateOf<IslandItemType?>(null) }
    var dragPositionInStudio by remember { mutableStateOf(Offset.Zero) }

    // Generous drop detection (expanded by 36px buffer so dragging over slots is effortless)
    val isHoveringLeft = draggingItem != null && leftSlotBoundsInStudio?.let {
        Rect(it.left - 36f, it.top - 36f, it.right + 36f, it.bottom + 36f).contains(dragPositionInStudio)
    } == true

    val isHoveringRight = draggingItem != null && rightSlotBoundsInStudio?.let {
        Rect(it.left - 36f, it.top - 36f, it.right + 36f, it.bottom + 36f).contains(dragPositionInStudio)
    } == true

    // Quick placement tap popover
    var activeQuickItem by remember { mutableStateOf<IslandItemType?>(null) }

    // Slot click-to-pick modal ("left" or "right")
    var slotPickerTarget by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                studioCoords = coords
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header - with weight(1f) to prevent "恢复默认" vertical character squishing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "自定义",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TmTextPrimary
                    )
                    Text(
                        text = "自由编排摄像头两侧的展示内容，支持拖拽放置或点击挑选",
                        fontSize = 11.sp,
                        color = TmTextMuted
                    )
                }

                // Reset default button - isolated with singleLine to guarantee layout integrity
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x18FFFFFF))
                        .clickable { onConfigChange(IslandConfig.DEFAULT) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "恢复默认",
                        fontSize = 11.sp,
                        color = TmAccent,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            // 1. 1:1 Live Island Capsule Preview (Accurately matches HyperOS physical status bar!)
            SuperIslandLivePreview(
                config = config,
                stats = stats,
                isConnected = isConnected
            )

            // 2. Interactive Drop Target Slots (Left locked to working status, Right customizable)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Slot (Locked to Working Status)
                SingleSlotTarget(
                    slotBadge = "左侧 (固定)",
                    slotName = "运行状态",
                    item = IslandItemType.STATUS,
                    isHovered = false,
                    onClickSlot = {},
                    onClear = null,
                    isLocked = true,
                    modifier = Modifier.weight(1f)
                )

                // Central Camera Punch Hole Indicator
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF000000))
                        .border(1.5.dp, Color(0x66FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    )
                }

                // Right Slot (User customizable)
                SingleSlotTarget(
                    slotBadge = "右侧 (自定义)",
                    slotName = "",
                    item = config.rightItem,
                    isHovered = isHoveringRight,
                    onClickSlot = { slotPickerTarget = "right" },
                    onClear = { onConfigChange(config.copy(rightItem = IslandItemType.NONE)) },
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { coords ->
                            studioCoords?.let { studio ->
                                if (coords.isAttached && studio.isAttached) {
                                    val topLeft = studio.localPositionOf(coords, Offset.Zero)
                                    rightSlotBoundsInStudio = Rect(topLeft.x, topLeft.y, topLeft.x + coords.size.width, topLeft.y + coords.size.height)
                                }
                            }
                        }
                )
            }

            // 3. Available Components Shelf
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x14FFFFFF))
                    .border(0.75.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "可用元件仓库",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TmTextSecondary
                    )
                    Text(
                        text = "按住 ⠿ 拖拽放入，或轻点快速放置",
                        fontSize = 10.sp,
                        color = TmTextMuted
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IslandItemType.entries.forEach { item ->
                        val isLeft = item == IslandItemType.STATUS
                        val isRight = config.rightItem == item
                        val isUsed = isLeft || isRight

                        DraggableComponentChip(
                            item = item,
                            isUsed = isUsed,
                            usedLocation = when {
                                isLeft && isRight -> "左右"
                                isLeft -> "左侧 (固定)"
                                isRight -> "右侧"
                                else -> null
                            },
                            studioCoords = studioCoords,
                            onDragStart = { startPosInStudio ->
                                draggingItem = item
                                dragPositionInStudio = startPosInStudio
                            },
                            onDrag = { dragAmount ->
                                dragPositionInStudio += dragAmount
                            },
                            onDragEnd = {
                                val currentPos = dragPositionInStudio
                                val hitRight = rightSlotBoundsInStudio?.let {
                                    Rect(it.left - 48f, it.top - 48f, it.right + 48f, it.bottom + 48f).contains(currentPos)
                                } == true

                                if (hitRight) {
                                    onConfigChange(config.copy(rightItem = item))
                                }
                                draggingItem = null
                            },
                            onClick = {
                                onConfigChange(config.copy(rightItem = item))
                            }
                        )
                    }
                }
            }

            // 4. AI Configuration Card (Provider + Quota Mode Selector)
            val hasAiQuota = config.rightItem == IslandItemType.AI_QUOTA
            AnimatedVisibility(visible = hasAiQuota) {
                AiConfigurationCard(
                    selectedProvider = config.selectedProvider,
                    quotaMode = config.quotaMode,
                    stats = stats,
                    onSelectProvider = { prov ->
                        onConfigChange(config.copy(selectedProvider = prov))
                    },
                    onSelectQuotaMode = { mode ->
                        onConfigChange(config.copy(quotaMode = mode))
                    }
                )
            }
        }

        // Floating Drag Overlay (Under user's touch finger)
        if (draggingItem != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (dragPositionInStudio.x - 45).roundToInt(),
                            (dragPositionInStudio.y - 30).roundToInt()
                        )
                    }
                    .zIndex(9999f)
                    .shadow(16.dp, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xF01C1C1E))
                    .border(1.5.dp, if (isHoveringLeft || isHoveringRight) TmLive else TmAccent, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = draggingItem!!.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Slot Click-to-Pick Modal Dialog (Right Slot only)
        if (slotPickerTarget != null) {
            AlertDialog(
                onDismissRequest = { slotPickerTarget = null },
                title = {
                    Text(text = "选择放入【右侧区域】的内容", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TmTextPrimary)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "右侧在挖孔右侧独立呈现，可自由配置",
                            fontSize = 11.sp,
                            color = TmTextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        IslandItemType.entries.forEach { item ->
                            val isCurrent = config.rightItem == item
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) Color(0x280A84FF) else Color(0x14FFFFFF))
                                    .clickable {
                                        onConfigChange(config.copy(rightItem = item))
                                        slotPickerTarget = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) TmAccent else TmTextPrimary
                                )
                                if (isCurrent) {
                                    Text(text = "当前", fontSize = 11.sp, color = TmAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { slotPickerTarget = null }) {
                        Text("取消", color = TmTextMuted)
                    }
                },
                containerColor = Color(0xFF1E2128),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * 1:1 Live HyperOS Super Island Capsule Preview
 * Accurately mimics Xiaomi status bar capsule:
 * [Brand Icon] [Left Text]   📷 Camera   [Right Text]
 */
@Composable
private fun SuperIslandLivePreview(
    config: IslandConfig,
    stats: TokenStats?,
    isConnected: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "islandDotPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F1115))
            .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(14.dp))
            .padding(vertical = 14.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Phone Status Bar Pill Capsule Mockup
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF000000))
                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LEFT AREA: [Brand Icon] + [Left Text]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (config.showIcon) {
                        val iconRes = if (config.leftItem == IslandItemType.AI_QUOTA || config.rightItem == IslandItemType.AI_QUOTA) {
                            resolveBrandIcon(stats, config.selectedProvider)
                        } else {
                            R.drawable.ic_brand_token_monitor
                        }
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = "App Icon",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    ItemRenderView(
                        item = IslandItemType.STATUS,
                        stats = stats,
                        isConnected = isConnected,
                        pulseAlpha = pulseAlpha,
                        selectedProvider = config.selectedProvider,
                        quotaMode = config.quotaMode
                    )
                }

                // CENTER CAMERA PUNCH HOLE
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF111111))
                        .border(1.dp, Color(0x55FFFFFF), CircleShape)
                )

                // RIGHT AREA: [Right Text]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (config.rightItem != IslandItemType.NONE) {
                        ItemRenderView(
                            item = config.rightItem,
                            stats = stats,
                            isConnected = isConnected,
                            pulseAlpha = pulseAlpha,
                            selectedProvider = config.selectedProvider,
                            quotaMode = config.quotaMode
                        )
                    } else {
                        Text(text = "空", fontSize = 11.sp, color = Color(0xFF555555))
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemRenderView(
    item: IslandItemType,
    stats: TokenStats?,
    isConnected: Boolean,
    pulseAlpha: Float,
    selectedProvider: String,
    quotaMode: String = "auto"
) {
    when (item) {
        IslandItemType.STATUS -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isWorking = isConnected && TokenNotificationManager.isWorkingState()
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !isConnected -> Color(0xFFFF453A)
                                isWorking -> TmLive.copy(alpha = pulseAlpha)
                                else -> Color(0xFF8E8E93)
                            }
                        )
                )
                Text(
                    text = when {
                        !isConnected -> "离线"
                        isWorking -> "工作中"
                        else -> "空闲中"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
        IslandItemType.TODAY_TOKENS -> {
            val tokens = stats?.today?.totalTokens ?: 72_200_000L
            Text(
                text = formatCompact(tokens),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TmAccent
            )
        }
        IslandItemType.AI_QUOTA -> {
            // Pure value display, no long provider name string to eliminate blur!
            val quotaStr = TokenNotificationManager.resolveAiQuotaValue(stats, selectedProvider, quotaMode)
            Text(
                text = quotaStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64D2FF)
            )
        }
        IslandItemType.TODAY_COST -> {
            val cost = stats?.today?.costUsd ?: 0.15
            Text(
                text = String.format(Locale.US, "$%.2f", cost),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFD60A)
            )
        }
        IslandItemType.MONTH_TOKENS -> {
            val tokens = stats?.month?.totalTokens ?: 18_500_000L
            Text(
                text = formatCompact(tokens),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFBF5AF2)
            )
        }
        IslandItemType.NONE -> {}
    }
}

@Composable
private fun SingleSlotTarget(
    slotBadge: String,
    slotName: String,
    item: IslandItemType,
    isHovered: Boolean,
    onClickSlot: () -> Unit,
    onClear: (() -> Unit)? = null,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isLocked -> Color(0x22FFFFFF)
            isHovered -> TmAccent
            item != IslandItemType.NONE -> Color(0x4DFFFFFF)
            else -> Color(0x22FFFFFF)
        },
        label = "singleSlotBorder"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isLocked -> Color(0x10FFFFFF)
            isHovered -> Color(0x28B7EAD4)
            item != IslandItemType.NONE -> Color(0x18000000)
            else -> Color(0x0CFFFFFF)
        },
        label = "singleSlotBg"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (isHovered && !isLocked) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Tag + Name row, immune to horizontal squeezing
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isHovered && !isLocked) TmAccent.copy(alpha = 0.25f) else Color(0x22FFFFFF))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = slotBadge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHovered && !isLocked) TmAccent else Color.White
                )
            }
            if (slotName.isNotBlank()) {
                Text(
                    text = slotName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHovered && !isLocked) TmAccent else TmTextMuted,
                    maxLines = 1
                )
            }
        }

        if (item == IslandItemType.NONE && !isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHovered) Color(0x2030D158) else Color(0x0AFFFFFF))
                    .clickable { onClickSlot() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isHovered) "松手放入此槽位" else "+ 拖入或点击挑选",
                    fontSize = 11.sp,
                    color = if (isHovered) TmAccent else Color(0xFF8E8E93),
                    fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHovered && !isLocked) Color(0x2030D158) else Color(0x22FFFFFF))
                    .then(if (!isLocked) Modifier.clickable { onClickSlot() } else Modifier)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHovered && !isLocked) "松手替换为当前" else item.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHovered && !isLocked) TmAccent else TmTextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                if (onClear != null && !isLocked) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .clickable { onClear() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 10.sp,
                            color = Color(0xCCFFFFFF),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isLocked) {
                    Text(
                        text = "已锁定",
                        fontSize = 10.sp,
                        color = TmTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DraggableComponentChip(
    item: IslandItemType,
    isUsed: Boolean,
    usedLocation: String?,
    studioCoords: LayoutCoordinates?,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit
) {
    var chipCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnClick by rememberUpdatedState(onClick)

    Box(
        modifier = Modifier
            .onGloballyPositioned { coords ->
                chipCoords = coords
            }
            .clip(RoundedCornerShape(10.dp))
            .background(if (isUsed) Color(0x1F0A84FF) else Color(0x1AFFFFFF))
            .border(
                1.dp,
                if (isUsed) Color(0x660A84FF) else Color(0x28FFFFFF),
                RoundedCornerShape(10.dp)
            )
            .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Instant Drag Handle Icon (⠿) - Direct drag without waiting for long press!
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x1EFFFFFF))
                    .pointerInput(item) {
                        detectDragGestures(
                            onDragStart = { localOffset ->
                                studioCoords?.let { studio ->
                                    chipCoords?.let { chip ->
                                        if (chip.isAttached && studio.isAttached) {
                                            val posInStudio = studio.localPositionOf(chip, localOffset)
                                            currentOnDragStart(posInStudio)
                                        }
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnDrag(dragAmount)
                            },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() }
                        )
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "⠿",
                    fontSize = 12.sp,
                    color = TmTextMuted,
                    fontWeight = FontWeight.Bold
                )
            }

            // Clickable content area
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clickable { currentOnClick() }
                    .pointerInput(item) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { localOffset ->
                                studioCoords?.let { studio ->
                                    chipCoords?.let { chip ->
                                        if (chip.isAttached && studio.isAttached) {
                                            val posInStudio = studio.localPositionOf(chip, localOffset)
                                            currentOnDragStart(posInStudio)
                                        }
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentOnDrag(dragAmount)
                            },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() }
                        )
                    }
            ) {
                Text(
                    text = item.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isUsed) Color.White else TmTextPrimary
                )
                if (usedLocation != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x330A84FF))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = usedLocation,
                            fontSize = 9.sp,
                            color = TmAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiConfigurationCard(
    selectedProvider: String,
    quotaMode: String,
    stats: TokenStats?,
    onSelectProvider: (String) -> Unit,
    onSelectQuotaMode: (String) -> Unit
) {
    val providers = stats?.providers.orEmpty()
    val providerOptions = mutableListOf("auto" to "自动选择 (最紧张)")
    if (providers.isNotEmpty()) {
        providers.forEach { p ->
            providerOptions.add(p.provider to "${p.provider.replaceFirstChar { it.uppercase() }}")
        }
    } else {
        providerOptions.add("antigravity" to "Antigravity")
        providerOptions.add("anthropic" to "Claude")
        providerOptions.add("openai" to "OpenAI")
        providerOptions.add("deepseek" to "DeepSeek")
    }

    val quotaModes = listOf(
        "auto" to "智能自动",
        "5h" to "5小时限制",
        "weekly" to "周限制",
        "balance" to "余额模式"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x140A84FF))
            .border(0.5.dp, Color(0x330A84FF), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section 1: Provider selection
        Text(
            text = "AI 厂商选择：",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64D2FF)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            providerOptions.forEach { (id, label) ->
                val isSelected = (id == "auto" && (selectedProvider.isBlank() || selectedProvider == "auto")) ||
                        selectedProvider.equals(id, ignoreCase = true)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) TmPrimary else Color(0x18FFFFFF))
                    .clickable { onSelectProvider(id) }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White else TmTextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Section 2: Quota Mode selection
        Text(
            text = "AI 配额计算模式：",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64D2FF)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quotaModes.forEach { (modeId, modeLabel) ->
                val isSelected = quotaMode.equals(modeId, ignoreCase = true) ||
                        (modeId == "auto" && (quotaMode.isBlank() || quotaMode == "auto"))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) TmAccent else Color(0x18FFFFFF))
                        .clickable { onSelectQuotaMode(modeId) }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = modeLabel,
                        fontSize = 11.sp,
                        color = if (isSelected) Color(0xFF0F1115) else TmTextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Text(
            text = when (quotaMode.lowercase()) {
                "5h" -> "优先匹配 5 小时滚动窗口剩余百分比（适合 Claude/Codex 快速窗口）"
                "weekly" -> "优先匹配每周额度限制剩余百分比（适合周周期重置模型）"
                "balance" -> "优先匹配现金余额（如 $12.50 或 ¥8.37）"
                else -> "智能自动匹配最紧俏的周期额度或现金余额（如 DeepSeek 自动显示 ¥8.37）"
            },
            fontSize = 10.sp,
            color = TmTextMuted
        )
    }
}

/**
 * Strips any approximate sign (≈) and formats numbers compactly.
 */
private fun formatCompact(num: Long): String {
    return when {
        num >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", num / 1_000_000_000.0)
        num >= 1_000_000 -> String.format(Locale.US, "%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format(Locale.US, "%.1fK", num / 1_000.0)
        else -> num.toString()
    }
}

private fun resolveBrandIcon(stats: TokenStats?, selectedProvider: String): Int {
    val target = TokenNotificationManager.resolveTargetProvider(stats, selectedProvider)
    return if (target != null) {
        TokenNotificationManager.resolveProviderIconRes(target.provider)
    } else {
        TokenNotificationManager.resolveProviderIconRes(selectedProvider)
    }
}


