package com.tokenmonitor.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.app.data.DailyHistory
import com.tokenmonitor.app.ui.theme.TmAccent
import com.tokenmonitor.app.ui.theme.TmTextMuted
import com.tokenmonitor.app.ui.theme.TmTextPrimary
import com.tokenmonitor.app.ui.theme.TmTextSecondary
import androidx.compose.foundation.layout.PaddingValues
import com.tokenmonitor.app.ui.glass.LiquidGlassSurface
import com.tokenmonitor.app.ui.glass.LiquidMaterial
import com.tokenmonitor.app.ui.glass.LocalLiquidGlassBackdrop
import com.tokenmonitor.app.ui.theme.AppThemeMode
import com.tokenmonitor.app.ui.theme.LocalThemeMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

/**
 * Responsive Card Container:
 * - Wallpaper mode: AGSL hardware Gaussian blur frosted glass (LiquidGlassSurface)
 * - Light mode: Crisp white card with subtle outline
 * - Dark mode: Deep obsidian card
 */
@Composable
fun TmCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val themeMode = LocalThemeMode.current
    if (themeMode == AppThemeMode.WALLPAPER) {
        LiquidGlassSurface(
            hazeState = LocalLiquidGlassBackdrop.current,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            variant = LiquidMaterial.REGULAR,
            tint = Color(0x3510131E),
            surfaceAlpha = 0.22f,
            enableBlur = true,
            enableRefraction = false,
            borderColor = Color(0x38FFFFFF),
            borderWidth = 1.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    } else {
        val isLight = themeMode == AppThemeMode.LIGHT
        val cardBg = if (isLight) Color.White else Color(0xFF131722)
        val cardBorder = if (isLight) Color(0xFFE2E8F0) else Color(0x1FFFFFFF)

        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(cardBg)
                .border(1.dp, cardBorder, shape)
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * Activity Heatmap Card (Image 3).
 * Rolling weeks grid (7 rows: Sun-Sat) with color intensity for tokens/cost.
 * Month labels at the bottom aligned under the 1st of each month.
 * Clicking a square displays that day's token and cost usage!
 */
@Composable
fun ActivityHeatmapCard(
    daily: List<DailyHistory>,
    activeDays: Int,
    modifier: Modifier = Modifier
) {
    val displayActiveDays = if (activeDays > 0) activeDays else daily.count { it.tokens > 0 }

    // Map date string "yyyy-MM-dd" to DailyHistory
    val dailyMap = remember(daily) {
        daily.associateBy { it.date.take(10) }
    }

    val maxTokens = remember(daily) {
        (daily.maxOfOrNull { it.tokens } ?: 1L).coerceAtLeast(1L)
    }

    var selectedCell by remember { mutableStateOf<HeatCell?>(null) }

    // Build rolling weeks grid (last 28 weeks ~ 6.5 months)
    val gridData = remember(daily) {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val monthFmt = SimpleDateFormat("M月", Locale.CHINESE)

        // Find Sunday of 51 weeks ago (52 weeks total ~ 1 full year)
        val weeks = 52
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
        cal.add(Calendar.DAY_OF_YEAR, -((weeks - 1) * 7 + currentDayOfWeek))

        val cols = mutableListOf<List<HeatCell>>()
        val monthLabels = mutableListOf<MonthLabel>()

        for (w in 0 until weeks) {
            val colCells = mutableListOf<HeatCell>()
            var monthLabelForCol: String? = null

            for (d in 0 until 7) {
                val dateStr = sdf.format(cal.time)
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                if (dayOfMonth == 1) {
                    monthLabelForCol = monthFmt.format(cal.time)
                }

                val item = dailyMap[dateStr]
                val tokens = item?.tokens ?: 0L
                val cost = item?.cost ?: 0.0

                val intensity = if (tokens <= 0L) {
                    0
                } else {
                    val ratio = tokens.toFloat() / maxTokens
                    when {
                        ratio > 0.65f -> 4
                        ratio > 0.35f -> 3
                        ratio > 0.12f -> 2
                        else -> 1
                    }
                }
                colCells.add(HeatCell(dateStr, tokens, cost, intensity))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            cols.add(colCells)
            if (monthLabelForCol != null) {
                monthLabels.add(MonthLabel(w, monthLabelForCol))
            }
        }
        HeatmapGrid(cols, monthLabels)
    }

    TmCard(modifier = modifier) {
        // Header: "活动" (left), "长按拖动查探用量 · 活跃 55 天 📈" (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "活动",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TmTextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "长按查看当日用量",
                    fontSize = 11.sp,
                    color = TmTextMuted
                )
                Text(
                    text = "·",
                    fontSize = 11.sp,
                    color = TmTextMuted
                )
                Text(
                    text = "活跃 $displayActiveDays 天",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TmTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Day Usage Banner (displays on tapping any cell)
        AnimatedVisibility(
            visible = selectedCell != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val cell = selectedCell
            val isLight = LocalThemeMode.current == AppThemeMode.LIGHT
            if (cell != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E2536))
                        .border(1.dp, if (isLight) Color(0xFF007AFF) else Color(0x4038BDF8), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = formatChineseDate(cell.date),
                            fontSize = 11.sp,
                            color = TmTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (cell.tokens > 0) formatCompactTokens(cell.tokens) else "无消耗",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cell.tokens > 0) (if (isLight) Color(0xFF007AFF) else Color(0xFF38BDF8)) else TmTextMuted
                            )
                            if (cell.tokens > 0) {
                                Text(
                                    text = "(${formatNumber(cell.tokens)} tokens)",
                                    fontSize = 11.sp,
                                    color = TmTextMuted
                                )
                            }
                            if (cell.cost > 0.0) {
                                Text(
                                    text = String.format(Locale.US, "· $%.2f", cell.cost),
                                    fontSize = 11.sp,
                                    color = TmAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isLight) Color(0xFFE2E8F0) else Color(0x22FFFFFF))
                            .clickable { selectedCell = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 11.sp,
                            color = if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }

        // Horizontally scrollable heatmap grid
        val scrollState = rememberScrollState()
        LaunchedEffect(gridData) {
            scrollState.scrollTo(scrollState.maxValue)
        }

        val cellSize = 9.dp
        val cellGap = 2.5.dp
        val density = LocalDensity.current
        val haptic = LocalHapticFeedback.current
        val cellStepPx = with(density) { (cellSize + cellGap).toPx() }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            val isLight = LocalThemeMode.current == AppThemeMode.LIGHT
            // 7 Rows of cells wrapped in interactive container (long-press then drag to probe usage)
            Box(
                modifier = Modifier
                    .width((cellSize + cellGap) * gridData.columns.size)
                    .pointerInput(gridData, cellStepPx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val col = (offset.x / cellStepPx).toInt().coerceIn(0, gridData.columns.size - 1)
                                val row = (offset.y / cellStepPx).toInt().coerceIn(0, 6)
                                val cell = gridData.columns.getOrNull(col)?.getOrNull(row)
                                if (cell != null && cell.date.isNotBlank()) {
                                    selectedCell = cell
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val col = (change.position.x / cellStepPx).toInt().coerceIn(0, gridData.columns.size - 1)
                                val row = (change.position.y / cellStepPx).toInt().coerceIn(0, 6)
                                val cell = gridData.columns.getOrNull(col)?.getOrNull(row)
                                if (cell != null && cell.date.isNotBlank() && cell != selectedCell) {
                                    selectedCell = cell
                                }
                            }
                        )
                    }
            ) {
                Column {
                    for (row in 0 until 7) {
                        Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                            for (col in gridData.columns) {
                                val cell = col.getOrNull(row) ?: HeatCell("", 0L, 0.0, 0)
                                val isSelected = selectedCell?.date == cell.date && cell.date.isNotBlank()
                                val cellColor = when (cell.intensity) {
                                    4 -> if (isLight) Color(0xFF0369A1) else Color(0xFFE0F2FE)
                                    3 -> if (isLight) Color(0xFF0284C7) else Color(0xFF60A5FA)
                                    2 -> if (isLight) Color(0xFF38BDF8) else Color(0xFF2563EB)
                                    1 -> if (isLight) Color(0xFFBAE6FD) else Color(0xFF1E3A5F)
                                    else -> if (isLight) Color(0xFFE2E8F0) else Color(0xFF1F2430)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(cellColor)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(1.5.dp, Color.White, RoundedCornerShape(2.dp))
                                            } else Modifier
                                        )
                                )
                            }
                        }
                        if (row < 6) Spacer(modifier = Modifier.height(cellGap))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Month labels below grid (aligned precisely under the 1st of each month)
            Box(
                modifier = Modifier
                    .width((cellSize + cellGap) * gridData.columns.size)
                    .height(16.dp)
            ) {
                for (ml in gridData.monthLabels) {
                    Text(
                        text = ml.label,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.offset {
                            IntOffset((ml.colIndex * cellStepPx).roundToInt(), 0)
                        }
                    )
                }
            }
        }
    }
}

private data class HeatCell(val date: String, val tokens: Long, val cost: Double, val intensity: Int)
private data class MonthLabel(val colIndex: Int, val label: String)
private data class HeatmapGrid(val columns: List<List<HeatCell>>, val monthLabels: List<MonthLabel>)

/**
 * Format date like "2026-09-08" -> "2026年9月8日 星期二"
 */
private fun formatChineseDate(dateStr: String): String {
    return try {
        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdfIn.parse(dateStr.take(10)) ?: return dateStr
        val sdfOut = SimpleDateFormat("yyyy年M月d日 E", Locale.CHINESE)
        sdfOut.format(date)
    } catch (e: Exception) {
        dateStr
    }
}

/**
 * Trend Area Spline Chart (Image 3).
 * Cubic Bezier spline curve with cyan line and smooth gradient area fill.
 * Header shows "趋势" (left) and "峰值 399.7M" (right).
 * X-axis shows 3 evenly spaced dates below: e.g. "7/23", "8/16", "9/11".
 */
@Composable
fun TrendSplineChartCard(
    daily: List<DailyHistory>,
    peakTokens: Long,
    modifier: Modifier = Modifier
) {
    // Extract points: last 30-50 days of daily history
    val points = remember(daily) {
        if (daily.isEmpty()) {
            emptyList()
        } else {
            val count = daily.size.coerceAtMost(50)
            daily.takeLast(count)
        }
    }

    val displayPeak = if (peakTokens > 0L) peakTokens else (points.maxOfOrNull { it.tokens } ?: 0L)

    // Three date labels: start, middle, end
    val dateLabels = remember(points) {
        if (points.size < 2) {
            listOf("", "", "")
        } else {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfOut = SimpleDateFormat("M/d", Locale.US)
            fun formatDate(dStr: String): String {
                return try {
                    val date = sdfIn.parse(dStr.take(10)) ?: return dStr
                    sdfOut.format(date)
                } catch (e: Exception) {
                    dStr
                }
            }
            val first = formatDate(points.first().date)
            val mid = formatDate(points[points.size / 2].date)
            val last = formatDate(points.last().date)
            listOf(first, mid, last)
        }
    }

    TmCard(modifier = modifier) {
        // Header: "趋势" (left), "峰值 399.7M" (right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "趋势",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TmTextPrimary
            )

            Text(
                text = "峰值 ${formatCompactTokens(displayPeak)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TmTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Smooth cubic spline Canvas
        val isLight = LocalThemeMode.current == AppThemeMode.LIGHT
        val strokeColor = if (isLight) Color(0xFF007AFF) else Color(0xFF38BDF8)
        val gradientFill = Brush.verticalGradient(
            listOf(
                strokeColor.copy(alpha = if (isLight) 0.25f else 0.40f),
                strokeColor.copy(alpha = if (isLight) 0.06f else 0.12f),
                Color.Transparent
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                if (points.size < 2) return@Canvas

                val maxVal = max(1L, points.maxOf { it.tokens }).toFloat()
                val w = size.width
                val h = size.height
                val padTop = 6.dp.toPx()
                val padBottom = 4.dp.toPx()
                val innerH = h - padTop - padBottom

                val coords = points.mapIndexed { i, p ->
                    val x = i * (w / (points.size - 1))
                    val y = padTop + innerH - (p.tokens.toFloat() / maxVal) * innerH
                    Offset(x, y)
                }

                // Build smooth cubic Bezier spline
                val linePath = Path()
                val areaPath = Path()

                linePath.moveTo(coords[0].x, coords[0].y)
                areaPath.moveTo(coords[0].x, h)
                areaPath.lineTo(coords[0].x, coords[0].y)

                for (i in 0 until coords.size - 1) {
                    val p0 = if (i > 0) coords[i - 1] else coords[i]
                    val p1 = coords[i]
                    val p2 = coords[i + 1]
                    val p3 = if (i + 2 < coords.size) coords[i + 2] else p2

                    // Catmull-Rom to Cubic Bezier control points
                    val cp1x = p1.x + (p2.x - p0.x) / 6f
                    val cp1y = p1.y + (p2.y - p0.y) / 6f
                    val cp2x = p2.x - (p3.x - p1.x) / 6f
                    val cp2y = p2.y - (p3.y - p1.y) / 6f

                    linePath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                    areaPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
                }

                areaPath.lineTo(coords.last().x, h)
                areaPath.close()

                // Draw area gradient fill
                drawPath(path = areaPath, brush = gradientFill)

                // Draw spline stroke
                drawPath(
                    path = linePath,
                    color = strokeColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-Axis date labels: 3 evenly spaced (left, center, right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dateLabels.getOrElse(0) { "" },
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = dateLabels.getOrElse(1) { "" },
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = dateLabels.getOrElse(2) { "" },
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
