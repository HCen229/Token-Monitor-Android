package com.tokenmonitor.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.app.R
import com.tokenmonitor.app.ui.theme.TmTextPrimary

object BrandHelper {
    fun getBrandDrawable(name: String): Int {
        val lower = name.lowercase().trim()
        return when {
            lower.contains("deepseek") -> R.drawable.ic_brand_deepseek
            lower.contains("gemini") || lower.contains("google") -> R.drawable.ic_brand_gemini
            lower.contains("claude") || lower.contains("anthropic") -> R.drawable.ic_brand_claude
            lower.contains("gpt") || lower.contains("o1") || lower.contains("o3") || lower.contains("codex") || lower.contains("openai") || lower.contains("chatgpt") -> R.drawable.ic_brand_codex
            lower.contains("qwen") || lower.contains("qwq") || lower.contains("tongyi") -> R.drawable.ic_brand_qwen
            lower.contains("kimi") || lower.contains("moonshot") -> R.drawable.ic_brand_kimi
            lower.contains("doubao") || lower.contains("skylark") || lower.contains("volcengine") || lower.contains("arkcli") || lower.contains("bytedance") -> R.drawable.ic_brand_doubao
            lower.contains("minimax") || lower.contains("abab") -> R.drawable.ic_brand_minimax
            lower.contains("grok") || lower.contains("xai") -> R.drawable.ic_brand_grok
            lower.contains("llama") || lower.contains("mistral") || lower.contains("ollama") -> R.drawable.ic_brand_ollama
            lower.contains("antigravity") -> R.drawable.ic_brand_antigravity
            lower.contains("opencode") -> R.drawable.ic_brand_opencode
            lower.contains("workbuddy") -> R.drawable.ic_brand_workbuddy
            lower.contains("cursor") -> R.drawable.ic_brand_cursor
            lower.contains("cline") -> R.drawable.ic_brand_cline
            lower.contains("copilot") || lower.contains("github") -> R.drawable.ic_brand_copilot
            lower.contains("alibaba") || lower.contains("aliyun") -> R.drawable.ic_brand_alibaba
            lower.contains("dsh") -> R.drawable.ic_brand_dsh
            lower.contains("droid") || lower.contains("factory") -> R.drawable.ic_brand_droid
            lower.contains("openrouter") -> R.drawable.ic_brand_openrouter
            lower.contains("trae") -> R.drawable.ic_brand_trae
            else -> R.drawable.ic_brand_token_monitor
        }
    }

    fun getBrandColor(name: String): Color {
        val lower = name.lowercase().trim()
        return when {
            lower.contains("deepseek") -> Color(0xFF1E88E5)
            lower.contains("gemini") || lower.contains("google") -> Color(0xFF4285F4)
            lower.contains("claude") || lower.contains("anthropic") -> Color(0xFFD97706)
            lower.contains("gpt") || lower.contains("o1") || lower.contains("o3") || lower.contains("codex") || lower.contains("openai") || lower.contains("chatgpt") -> Color(0xFF10A37F)
            lower.contains("qwen") || lower.contains("qwq") || lower.contains("tongyi") || lower.contains("alibaba") -> Color(0xFFFF6A00)
            lower.contains("kimi") || lower.contains("moonshot") -> Color(0xFF3B82F6)
            lower.contains("doubao") || lower.contains("skylark") || lower.contains("volcengine") || lower.contains("arkcli") -> Color(0xFF3B82F6)
            lower.contains("minimax") || lower.contains("abab") -> Color(0xFFFF3366)
            lower.contains("grok") || lower.contains("xai") -> Color(0xFFE2E8F0)
            lower.contains("llama") || lower.contains("mistral") || lower.contains("ollama") -> Color(0xFFE2E8F0)
            lower.contains("antigravity") -> Color(0xFF3186FF)
            lower.contains("opencode") -> Color(0xFF00C853)
            lower.contains("workbuddy") -> Color(0xFFFF9100)
            lower.contains("cursor") -> Color(0xFF60A5FA)
            lower.contains("cline") -> Color(0xFF00E5FF)
            lower.contains("copilot") || lower.contains("github") -> Color(0xFF8B5CF6)
            lower.contains("dsh") -> Color(0xFF1E88E5)
            lower.contains("droid") || lower.contains("factory") -> Color(0xFF10B981)
            lower.contains("openrouter") -> Color(0xFF6366F1)
            lower.contains("trae") -> Color(0xFF00E676)
            else -> Color(0xFF64D2FF)
        }
    }

    /**
     * Resolves the brand name to use for displaying a model's icon.
     * Prioritizes the model name itself (e.g. deepseek, gemini, claude, gpt).
     * Falls back to client name only when the model name has no recognized AI provider brand.
     */
    fun resolveModelBrandName(modelName: String, fallbackClient: String = ""): String {
        val modelDrawable = getBrandDrawable(modelName)
        if (modelDrawable != R.drawable.ic_brand_token_monitor) {
            return modelName
        }
        if (fallbackClient.isNotBlank() && fallbackClient != "all") {
            val clientDrawable = getBrandDrawable(fallbackClient)
            if (clientDrawable != R.drawable.ic_brand_token_monitor) {
                return fallbackClient
            }
        }
        return modelName
    }
}

@Composable
fun BrandIcon(
    name: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 18.dp
) {
    val resId = BrandHelper.getBrandDrawable(name)
    val color = tint ?: BrandHelper.getBrandColor(name)
    Icon(
        painter = painterResource(resId),
        contentDescription = name,
        tint = color,
        modifier = modifier.size(size)
    )
}

@Composable
fun BrandBadge(
    name: String,
    label: String = name,
    modifier: Modifier = Modifier
) {
    val brandColor = BrandHelper.getBrandColor(name)
    val resId = BrandHelper.getBrandDrawable(name)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(brandColor.copy(alpha = 0.12f))
            .border(0.75.dp, brandColor.copy(alpha = 0.28f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(resId),
            contentDescription = null,
            tint = brandColor,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = brandColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
