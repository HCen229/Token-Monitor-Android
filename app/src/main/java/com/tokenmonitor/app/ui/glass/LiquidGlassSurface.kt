package com.tokenmonitor.app.ui.glass

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.tokenmonitor.app.ui.theme.AppThemeMode
import com.tokenmonitor.app.ui.theme.LocalThemeMode

/**
 * Optical LiquidGlass surface inspired by Niskle-Link.
 * Uses backdrop lens refraction & chromatic blur on API 33+, with fallback to
 * acrylic frosted glass styling on earlier Android versions.
 */
@Composable
fun LiquidGlassSurface(
    hazeState: Backdrop? = LocalLiquidGlassBackdrop.current,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    variant: LiquidMaterial = LiquidMaterial.REGULAR,
    tint: Color = Color(0xFF1C1C1E),
    pressed: Boolean = false,
    selected: Boolean = false,
    surfaceAlpha: Float? = null,
    borderColor: Color? = null,
    borderBrush: Brush? = null,
    borderWidth: Dp = 1.dp,
    enableBlur: Boolean = true,
    blurRadius: Dp? = null,
    enableRefraction: Boolean = true,
    refractionScale: Float = 1f,
    refractionHeight: Dp? = null,
    refractionAmount: Dp? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalLiquidGlassTokens.current
    val themeMode = LocalThemeMode.current
    val isLight = themeMode == AppThemeMode.LIGHT
    val isWallpaper = themeMode == AppThemeMode.WALLPAPER
    val supportsOpticalGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = tokens.springDamping, stiffness = tokens.springStiffness),
        label = "glassPress"
    )

    val tintAlpha = when (variant) {
        LiquidMaterial.CLEAR -> 0.14f
        LiquidMaterial.REGULAR -> 0.28f
        LiquidMaterial.PROMINENT -> 0.44f
    }
    val resolvedSurfaceAlpha = surfaceAlpha ?: tintAlpha
    val resolvedBlurRadius = blurRadius ?: when (variant) {
        LiquidMaterial.CLEAR -> tokens.clearBlurRadius
        LiquidMaterial.REGULAR -> tokens.regularBlurRadius
        LiquidMaterial.PROMINENT -> tokens.prominentBlurRadius
    }
    val resolvedRefractionHeight = refractionHeight ?: tokens.refractionHeight
    val resolvedRefractionAmount = refractionAmount ?: tokens.refractionAmount

    val defaultBorderBrush = if (isLight) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x35000000),
                Color(0x18000000),
                Color(0x0C000000)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0x55FFFFFF),
                Color(0x24FFFFFF),
                Color(0x10FFFFFF)
            )
        )
    }

    val opticalModifier = if (hazeState != null && supportsOpticalGlass) {
        Modifier.drawBackdrop(
            backdrop = hazeState,
            shape = { shape },
            effects = {
                if (enableBlur) {
                    vibrancy()
                    blur(resolvedBlurRadius.toPx())
                }
                if (enableRefraction && refractionScale > 0.001f) {
                    lens(
                        refractionHeight = (resolvedRefractionHeight * refractionScale).toPx(),
                        refractionAmount = (resolvedRefractionAmount * refractionScale).toPx(),
                        depthEffect = true,
                        chromaticAberration = tokens.chromaticAberration && (pressed || refractionScale > 0.85f)
                    )
                }
            },
            highlight = {
                if (enableRefraction && refractionScale > 0.001f) {
                    val baseAlpha = if (isLight) tokens.highlightAlphaLight else 0.28f
                    Highlight.Default.copy(
                        width = 1.1.dp,
                        blurRadius = 1.5.dp,
                        alpha = (baseAlpha + pressProgress * 0.18f) * refractionScale
                    )
                } else {
                    Highlight.Default.copy(alpha = 0f)
                }
            },
            shadow = {
                if (enableRefraction && refractionScale > 0.001f) {
                    Shadow(
                        radius = tokens.outerShadowRadius + (if (pressed) 5.dp else 0.dp),
                        color = Color.Black.copy(alpha = (if (pressed) 0.35f else 0.22f) * refractionScale)
                    )
                } else {
                    Shadow(radius = 0.dp, color = Color.Transparent)
                }
            },
            innerShadow = {
                if (enableRefraction && refractionScale > 0.001f) {
                    InnerShadow(
                        radius = tokens.innerShadowRadius,
                        color = Color.White.copy(alpha = 0.14f * refractionScale),
                        alpha = 0.40f * refractionScale
                    )
                } else {
                    InnerShadow(radius = 0.dp, color = Color.Transparent, alpha = 0f)
                }
            },
            onDrawSurface = {
                if (resolvedSurfaceAlpha > 0f) {
                    drawRect(tint.copy(alpha = resolvedSurfaceAlpha))
                }
            },
            layerBlock = {
                val scale = 1f + (tokens.pressScale - 1f) * pressProgress
                scaleX = scale
                scaleY = scale
            }
        ).then(
            when {
                borderBrush != null -> Modifier.border(width = borderWidth, brush = borderBrush, shape = shape)
                borderColor != null -> Modifier.border(width = borderWidth, color = borderColor, shape = shape)
                else -> Modifier.border(width = borderWidth, brush = defaultBorderBrush, shape = shape)
            }
        )
    } else {
        // Fallback for non-optical devices: deep frosted acrylic glass
        val fallbackBgAlpha = if (resolvedSurfaceAlpha <= 0f) 0f else (resolvedSurfaceAlpha + 0.32f).coerceAtMost(0.85f)
        val fallbackColors = when {
            isLight -> listOf(
                Color(0xF0FFFFFF).copy(alpha = (fallbackBgAlpha + 0.20f).coerceAtMost(0.95f)),
                Color(0xE6F8FAFC).copy(alpha = (fallbackBgAlpha + 0.25f).coerceAtMost(0.98f))
            )
            isWallpaper -> listOf(
                Color(0x3510131E).copy(alpha = fallbackBgAlpha.coerceAtMost(0.40f)),
                Color(0x45141722).copy(alpha = (fallbackBgAlpha + 0.10f).coerceAtMost(0.50f))
            )
            else -> listOf(
                Color(0xFF242735).copy(alpha = fallbackBgAlpha),
                Color(0xFF141722).copy(alpha = (fallbackBgAlpha + 0.10f).coerceAtMost(0.92f))
            )
        }

        Modifier
            .shadow(
                elevation = if (pressed) 8.dp else 4.dp,
                shape = shape,
                clip = false,
                ambientColor = if (isLight) Color(0x18000000) else Color.Black.copy(alpha = 0.3f),
                spotColor = if (isLight) Color(0x20000000) else Color.Black.copy(alpha = 0.4f)
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(colors = fallbackColors),
                shape = shape
            )
            .then(
                when {
                    borderBrush != null -> Modifier.border(width = borderWidth, brush = borderBrush, shape = shape)
                    borderColor != null -> Modifier.border(width = borderWidth, color = borderColor, shape = shape)
                    else -> Modifier.border(width = borderWidth, brush = defaultBorderBrush, shape = shape)
                }
            )
    }

    Box(
        modifier = modifier
            .then(opticalModifier)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Pinned title layer directly ported from Niskle-Link's ProgressiveBlurHeader.
 * Stays optically absent at the top of a page, then progressively
 * captures and blurs scrolling content as it moves underneath. The title content is drawn after
 * the backdrop pass, so labels and controls remain sharp while only the page below is obscured.
 */
@Composable
fun ProgressiveBlurHeader(
    backdrop: Backdrop?,
    progress: Float,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    uniformOverlay: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val strength = progress.coerceIn(0f, 1f)
    val supportsOpticalGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val themeMode = LocalThemeMode.current
    val surfaceColor = when (themeMode) {
        AppThemeMode.LIGHT -> Color(0xFFFFFFFF)
        AppThemeMode.WALLPAPER -> Color(0xFF0F131D)
        AppThemeMode.DARK -> Color(0xFF131A29)
    }
    val darkTheme = surfaceColor.luminance() < 0.5f

    Box(modifier = modifier.clip(shape)) {
        if (backdrop != null && supportsOpticalGlass && strength > 0.001f) {
            val activeBlurDp = (24f * strength).coerceAtLeast(0.5f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            blur(activeBlurDp.dp.toPx())
                        },
                        highlight = {
                            Highlight.Default.copy(
                                width = 0.dp,
                                blurRadius = 0.dp,
                                alpha = 0f
                            )
                        },
                        shadow = {
                            Shadow(radius = 0.dp, color = Color.Transparent)
                        },
                        innerShadow = {
                            InnerShadow(
                                radius = 0.dp,
                                color = Color.Transparent,
                                alpha = 0f
                            )
                        }
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (uniformOverlay) {
                            Modifier.background(
                                color = surfaceColor.copy(
                                    alpha = (if (darkTheme) 0.38f else 0.44f) * strength
                                )
                            )
                        } else {
                            Modifier.background(
                                brush = Brush.verticalGradient(
                                    colors = if (darkTheme) {
                                        listOf(
                                            surfaceColor.copy(alpha = 0.62f * strength),
                                            surfaceColor.copy(alpha = 0.38f * strength),
                                            surfaceColor.copy(alpha = 0.16f * strength)
                                        )
                                    } else {
                                        listOf(
                                            surfaceColor.copy(alpha = 0.68f * strength),
                                            surfaceColor.copy(alpha = 0.44f * strength),
                                            surfaceColor.copy(alpha = 0.18f * strength)
                                        )
                                    }
                                )
                            )
                        }
                    )
            )
        } else if (strength > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (uniformOverlay) {
                            Modifier.background(
                                color = surfaceColor.copy(
                                    alpha = (if (darkTheme) 0.82f else 0.88f) * strength
                                )
                            )
                        } else {
                            Modifier.background(
                                brush = Brush.verticalGradient(
                                    colors = if (darkTheme) {
                                        listOf(
                                            surfaceColor.copy(alpha = 0.96f * strength),
                                            surfaceColor.copy(alpha = 0.82f * strength),
                                            surfaceColor.copy(alpha = 0.58f * strength)
                                        )
                                    } else {
                                        listOf(
                                            surfaceColor.copy(alpha = 0.98f * strength),
                                            surfaceColor.copy(alpha = 0.88f * strength),
                                            surfaceColor.copy(alpha = 0.64f * strength)
                                        )
                                    }
                                )
                            )
                        }
                    )
            )
        }

        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}

