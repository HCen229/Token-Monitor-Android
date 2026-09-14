package com.tokenmonitor.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val id: String, val title: String, val subtitle: String) {
    DARK("dark", "暗色", "沉浸深邃黑夜"),
    LIGHT("light", "亮色", "清透现代浅色"),
    WALLPAPER("wallpaper", "系统壁纸", "透光高斯模糊毛玻璃");

    companion object {
        fun fromId(id: String?): AppThemeMode {
            return entries.firstOrNull { it.id == id } ?: DARK
        }
    }
}

data class TmColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val glass: Color,
    val glassBorder: Color,
    val primary: Color,
    val primarySoft: Color,
    val secondary: Color,
    val accent: Color,
    val accentSoft: Color,
    val warning: Color,
    val error: Color,
    val live: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val bgGradientStart: Color,
    val bgGradientEnd: Color
) {
    companion object {
        val Dark = TmColors(
            background = Color(0xFF090D16),
            surface = Color(0xFF131A29),
            surfaceElevated = Color(0xFF1C2436),
            glass = Color(0xFF1C2436),
            glassBorder = Color(0x28FFFFFF),
            primary = Color(0xFF2563EB),
            primarySoft = Color(0xFF1E3A8A),
            secondary = Color(0xFF94A3B8),
            accent = Color(0xFF10B981), // Fresh vibrant emerald green
            accentSoft = Color(0x2210B981),
            warning = Color(0xFFFF9F0A),
            error = Color(0xFFFF453A),
            live = Color(0xFF10B981),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            textMuted = Color(0xFF64748B),
            border = Color(0x1FFFFFFF),
            bgGradientStart = Color(0xFF090D16),
            bgGradientEnd = Color(0xFF0F172A)
        )

        val Light = TmColors(
            background = Color(0xFFF4F6F9),
            surface = Color(0xFFFFFFFF),
            surfaceElevated = Color(0xFFF8FAFC),
            glass = Color(0xF2FFFFFF),
            glassBorder = Color(0x18000000),
            primary = Color(0xFF007AFF),
            primarySoft = Color(0xFFE0F2FE),
            secondary = Color(0xFF475569),
            accent = Color(0xFF10B981),
            accentSoft = Color(0x2010B981),
            warning = Color(0xFFF59E0B),
            error = Color(0xFFEF4444),
            live = Color(0xFF10B981),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            textMuted = Color(0xFF64748B),
            border = Color(0xFFE2E8F0),
            bgGradientStart = Color(0xFFF8FAFC),
            bgGradientEnd = Color(0xFFF1F5F9)
        )

        val Wallpaper = TmColors(
            background = Color.Transparent,
            surface = Color(0x2210131E),
            surfaceElevated = Color(0x3510131E),
            glass = Color(0x24FFFFFF),
            glassBorder = Color(0x38FFFFFF),
            primary = Color(0xFF38BDF8),
            primarySoft = Color(0x330284C7),
            secondary = Color(0xCCFFFFFF),
            accent = Color(0xFF10B981),
            accentSoft = Color(0x3310B981),
            warning = Color(0xFFFFB020),
            error = Color(0xFFFF5252),
            live = Color(0xFF4ADE80),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xCCFFFFFF),
            textMuted = Color(0x99FFFFFF),
            border = Color(0x38FFFFFF),
            bgGradientStart = Color.Transparent,
            bgGradientEnd = Color.Transparent
        )
    }
}

val LocalTmColors = staticCompositionLocalOf { TmColors.Dark }
val LocalThemeMode = staticCompositionLocalOf { AppThemeMode.DARK }

val TmBackground: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.background
val TmSurface: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.surface
val TmSurfaceElevated: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.surfaceElevated
val TmPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.primary
val TmPrimarySoft: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.primarySoft
val TmAccent: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.accent
val TmAccentSoft: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.accentSoft
val TmLive: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.live
val TmError: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.error
val TmWarning: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.warning
val TmTextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.textPrimary
val TmTextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.textSecondary
val TmTextMuted: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.textMuted
val TmBorder: Color
    @Composable @ReadOnlyComposable get() = LocalTmColors.current.border
