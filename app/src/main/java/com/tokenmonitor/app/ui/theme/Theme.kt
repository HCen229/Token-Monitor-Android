package com.tokenmonitor.app.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.tokenmonitor.app.ui.glass.LiquidGlassTokens
import com.tokenmonitor.app.ui.glass.LocalLiquidGlassTokens

private val DarkColorScheme = darkColorScheme(
    primary = TmColors.Dark.primary,
    onPrimary = TmColors.Dark.textPrimary,
    primaryContainer = TmColors.Dark.primarySoft,
    secondary = TmColors.Dark.accent,
    background = TmColors.Dark.background,
    surface = TmColors.Dark.surface,
    surfaceVariant = TmColors.Dark.surfaceElevated,
    error = TmColors.Dark.error
)

private val LightColorScheme = lightColorScheme(
    primary = TmColors.Light.primary,
    onPrimary = Color.White,
    primaryContainer = TmColors.Light.primarySoft,
    secondary = TmColors.Light.accent,
    background = TmColors.Light.background,
    surface = TmColors.Light.surface,
    surfaceVariant = TmColors.Light.surfaceElevated,
    error = TmColors.Light.error
)

private val WallpaperColorScheme = darkColorScheme(
    primary = TmColors.Wallpaper.primary,
    onPrimary = TmColors.Wallpaper.textPrimary,
    primaryContainer = TmColors.Wallpaper.primarySoft,
    secondary = TmColors.Wallpaper.accent,
    background = TmColors.Wallpaper.background,
    surface = TmColors.Wallpaper.surface,
    surfaceVariant = TmColors.Wallpaper.surfaceElevated,
    error = TmColors.Wallpaper.error
)

@Composable
fun TokenMonitorTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val tmColors = when (themeMode) {
        AppThemeMode.DARK -> TmColors.Dark
        AppThemeMode.LIGHT -> TmColors.Light
        AppThemeMode.WALLPAPER -> TmColors.Wallpaper
    }

    val colorScheme = when (themeMode) {
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.WALLPAPER -> WallpaperColorScheme
    }

    val baseTextStyle = TextStyle(
        fontFamily = AppFontFamily,
        color = tmColors.textPrimary
    )

    CompositionLocalProvider(
        LocalTmColors provides tmColors,
        LocalThemeMode provides themeMode,
        LocalLiquidGlassTokens provides LiquidGlassTokens.defaultTokens(),
        LocalTextStyle provides baseTextStyle
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

