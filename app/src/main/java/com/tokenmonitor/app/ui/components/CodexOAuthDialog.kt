package com.tokenmonitor.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tokenmonitor.app.data.provider.DirectProviderManager
import com.tokenmonitor.app.ui.BrandIcon
import com.tokenmonitor.app.ui.i18n.LocalAppStrings
import com.tokenmonitor.app.ui.theme.AppThemeMode
import com.tokenmonitor.app.ui.theme.LocalThemeMode
import com.tokenmonitor.app.ui.theme.TmAccent
import com.tokenmonitor.app.ui.theme.TmPrimary
import com.tokenmonitor.app.ui.theme.TmTextMuted
import com.tokenmonitor.app.ui.theme.TmTextPrimary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CodexOAuthDialog(
    isExchanging: Boolean = false,
    onDismiss: () -> Unit,
    onSuccess: (code: String, codeVerifier: String) -> Unit,
    onError: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isThemeLight = LocalThemeMode.current == AppThemeMode.LIGHT

    val codeVerifier = remember { DirectProviderManager.PkceHelper.generateCodeVerifier() }
    val codeChallenge = remember(codeVerifier) { DirectProviderManager.PkceHelper.generateCodeChallenge(codeVerifier) }
    val authUrl = remember(codeChallenge) { DirectProviderManager.PkceHelper.buildAuthorizeUrl(codeChallenge) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var intercepted by remember { mutableStateOf(false) }

    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isExchanging) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isExchanging,
            dismissOnClickOutside = false
        )
    ) {
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        val bgColor = if (isThemeLight) Color(0xFFF8FAFC) else Color(0xFF0F172A)
        val headerBg = if (isThemeLight) Color(0xFFFFFFFF) else Color(0xFF1E293B)
        val borderColor = if (isThemeLight) Color(0xFFE2E8F0) else Color(0x22FFFFFF)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .border(width = 0.5.dp, color = borderColor)
                ) {
                    Spacer(modifier = Modifier.height(topPadding))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x1510A37F)),
                                contentAlignment = Alignment.Center
                            ) {
                                BrandIcon(name = "codex", size = 20.dp)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = strings.codexLoginWithChatGPT,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TmTextPrimary
                                )
                                Text(
                                    text = "auth.openai.com",
                                    fontSize = 11.sp,
                                    color = TmTextMuted
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Reload Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { webViewRef?.reload() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "↻",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TmTextMuted
                                )
                            }

                            // Close Button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TmTextPrimary
                                )
                            }
                        }
                    }

                    // Progress Bar
                    if (isLoadingPage || isExchanging) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp),
                            color = TmAccent,
                            trackColor = Color.Transparent
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.5.dp))
                    }
                }

                // WebView Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    setSupportMultipleWindows(false)
                                    javaScriptCanOpenWindowsAutomatically = true
                                    userAgentString = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
                                }

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                fun handleIntercept(url: String?): Boolean {
                                    if (url == null) return false
                                    if (url.startsWith(DirectProviderManager.PkceHelper.CODEX_REDIRECT_URI)) {
                                        if (intercepted) return true
                                        intercepted = true
                                        val uri = Uri.parse(url)
                                        val code = uri.getQueryParameter("code")
                                        val error = uri.getQueryParameter("error")
                                        val errorDesc = uri.getQueryParameter("error_description")

                                        if (!code.isNullOrBlank()) {
                                            onSuccess(code, codeVerifier)
                                        } else {
                                            onError(errorDesc ?: error ?: "Authorization canceled")
                                            onDismiss()
                                        }
                                        return true
                                    }
                                    return false
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url?.toString()
                                        if (handleIntercept(url)) {
                                            return true
                                        }
                                        return super.shouldOverrideUrlLoading(view, request)
                                    }

                                    @Deprecated("Deprecated in Java")
                                    @Suppress("DEPRECATION")
                                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                        if (handleIntercept(url)) {
                                            return true
                                        }
                                        return super.shouldOverrideUrlLoading(view, url)
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        if (handleIntercept(url)) {
                                            view?.stopLoading()
                                            return
                                        }
                                        isLoadingPage = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoadingPage = false
                                    }
                                }

                                loadUrl(authUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Exchanging code overlay
                    if (isExchanging) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isThemeLight) Color(0xCCFFFFFF) else Color(0xCC0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = TmPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = strings.codexLoggingIn,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TmTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
