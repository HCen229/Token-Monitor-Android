package com.tokenmonitor.app.ui.glass

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop

/** The three material levels used by the app's glass surfaces. */
enum class LiquidMaterial {
    CLEAR,
    REGULAR,
    PROMINENT
}

/**
 * Optical and motion values for LiquidGlass surfaces and controls.
 */
data class LiquidGlassTokens(
    val clearBlurRadius: Dp,
    val regularBlurRadius: Dp,
    val prominentBlurRadius: Dp,
    val refractionHeight: Dp,
    val refractionAmount: Dp,
    val chromaticAberration: Boolean,
    val highlightWidth: Dp,
    val highlightBlur: Dp,
    val highlightAlphaLight: Float,
    val highlightAlphaDark: Float,
    val innerShadowRadius: Dp,
    val outerShadowRadius: Dp,
    val surfaceAlphaLight: Float,
    val surfaceAlphaDark: Float,
    val disabledAlpha: Float,
    val pressScale: Float,
    val springDamping: Float,
    val springStiffness: Float,
    val minimumTouchTarget: Dp
) {
    companion object {
        fun defaultTokens(): LiquidGlassTokens = LiquidGlassTokens(
            clearBlurRadius = 8.dp,
            regularBlurRadius = 16.dp,
            prominentBlurRadius = 24.dp,
            refractionHeight = 22.dp,
            refractionAmount = 30.dp,
            chromaticAberration = true,
            highlightWidth = 1.2.dp,
            highlightBlur = 1.5.dp,
            highlightAlphaLight = 0.45f,
            highlightAlphaDark = 0.38f,
            innerShadowRadius = 6.dp,
            outerShadowRadius = 12.dp,
            surfaceAlphaLight = 0.22f,
            surfaceAlphaDark = 0.32f,
            disabledAlpha = 0.46f,
            pressScale = 0.985f,
            springDamping = 0.75f,
            springStiffness = 500f,
            minimumTouchTarget = 44.dp
        )
    }
}

val LocalLiquidGlassTokens = staticCompositionLocalOf {
    LiquidGlassTokens.defaultTokens()
}

val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
