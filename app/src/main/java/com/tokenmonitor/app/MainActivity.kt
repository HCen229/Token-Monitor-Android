package com.tokenmonitor.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.tokenmonitor.app.service.TokenNotificationManager
import com.tokenmonitor.app.ui.HomeScreen
import com.tokenmonitor.app.ui.MainViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tokenmonitor.app.ui.theme.AppThemeMode
import com.tokenmonitor.app.ui.theme.TokenMonitorTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ensure notification channels are registered immediately
        TokenNotificationManager.init(this)

        // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        1001
                    )
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Permission request failed", e)
        }

        // Enable edge-to-edge with transparent system bars for seamless full-screen display
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.setFormat(android.graphics.PixelFormat.TRANSLUCENT)

        // Clear any fullscreen flags
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val insetsController = remember(window) {
                WindowCompat.getInsetsController(window, window.decorView)
            }

            LaunchedEffect(themeMode) {
                val isLight = themeMode == AppThemeMode.LIGHT
                insetsController.isAppearanceLightStatusBars = isLight
                insetsController.isAppearanceLightNavigationBars = isLight

                // Hardware-level system wallpaper binding:
                // When WALLPAPER mode is active, set FLAG_SHOW_WALLPAPER so Android's native compositor renders
                // the user's current live or static wallpaper directly behind the transparent window.
                if (themeMode == AppThemeMode.WALLPAPER) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
                    val bgColor = if (isLight) 0xFFF4F6F9.toInt() else 0xFF090D16.toInt()
                    window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgColor))
                }
            }

            TokenMonitorTheme(themeMode = themeMode) {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
        }
        viewModel.startPolling()
        viewModel.manualRefresh()

        // Activate persistent status & Super Island notification while in foreground
        if (TokenNotificationManager.isPermissionGranted(this)) {
            TokenNotificationManager.startMonitoring(this)
            TokenNotificationManager.postNotification(this)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopPolling()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            TokenNotificationManager.startMonitoring(this)
            TokenNotificationManager.postNotification(this)
        }
    }
}

