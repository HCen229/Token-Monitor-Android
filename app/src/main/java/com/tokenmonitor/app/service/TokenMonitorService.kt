package com.tokenmonitor.app.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.tokenmonitor.app.data.TokenRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TokenMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var pollJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var repository: TokenRepository

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = TokenRepository(applicationContext)
        TokenNotificationManager.ensureChannels(this)
        ScreenStateManager.init(this)
        ScreenStateManager.onScreenTurnedOn = {
            triggerImmediatePoll()
        }
        initWakeLock()
        Log.i(TAG, "TokenMonitorService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notification = TokenNotificationManager.buildCurrentNotification(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        TokenNotificationManager.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (t: Throwable) {
                    try {
                        startForeground(
                            TokenNotificationManager.NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } catch (_: Throwable) {
                        startForeground(TokenNotificationManager.NOTIFICATION_ID, notification)
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        TokenNotificationManager.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } catch (_: Throwable) {
                    startForeground(TokenNotificationManager.NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(TokenNotificationManager.NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "startForeground failed", e)
        }

        startBackgroundPolling()
        return START_STICKY
    }

    fun restartPollLoop() {
        startBackgroundPolling()
    }

    suspend fun performPoll(forceNotification: Boolean = false) {
        withTransientWakeLock(5000L) {
            try {
                val result = repository.fetchStats(forceDirectRefresh = false)
                result.onSuccess { stats ->
                    TokenNotificationManager.updateStats(applicationContext, stats, isConnected = true, force = forceNotification)
                }.onFailure {
                    TokenNotificationManager.updateStats(applicationContext, TokenNotificationManager.cachedStats, isConnected = false, force = forceNotification)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "performPoll error: ${e.message}")
            }
        }
    }

    private fun startBackgroundPolling() {
        pollJob?.cancel()
        pollJob = serviceScope.launch {
            while (isActive) {
                try {
                    // Screen OFF handling - freeze polling and retain previous data
                    if (!ScreenStateManager.isInteractive(applicationContext)) {
                        Log.d(TAG, "Screen is OFF (息屏). Retaining data and pausing background sync.")
                        // Suspend until screen turns ON
                        ScreenStateManager.isScreenOn.first { it }
                        Log.d(TAG, "Screen turned ON (亮屏). Resuming sync.")
                    }

                    val isForeground = com.tokenmonitor.app.TokenMonitorApp.isAppInForeground

                    if (isForeground) {
                        // When app is in foreground, MainViewModel is actively fetching data.
                        delay(1000L)
                        continue
                    }

                    // Continuous 1-second refresh for Super Island & Notification when screen is interactive
                    performPoll(forceNotification = true)
                    delay(1000L)
                } catch (e: Throwable) {
                    Log.w(TAG, "Background poll loop error: ${e.message}")
                    delay(2000L)
                }
            }
        }
    }

    private inline fun <T> withTransientWakeLock(timeoutMs: Long = 8000L, block: () -> T): T {
        try {
            wakeLock?.acquire(timeoutMs)
        } catch (_: Throwable) {
        }
        try {
            return block()
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun initWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TokenMonitor:TransientWakeLock")?.apply {
                setReferenceCounted(false)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to initialize wake lock", e)
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Throwable) {
        }
        wakeLock = null
    }

    override fun onDestroy() {
        pollJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        instance = null
        Log.i(TAG, "TokenMonitorService destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "TokenMonitorService"

        @Volatile
        var instance: TokenMonitorService? = null
            private set

        @Volatile
        var fastRefreshUntil: Long = 0L

        fun isRunning(): Boolean = instance != null

        fun isFastRefreshActive(): Boolean {
            val ctx = instance?.applicationContext ?: return true
            return ScreenStateManager.isInteractive(ctx)
        }

        fun triggerFastRefresh(durationMs: Long = 60_000L) {
            val until = System.currentTimeMillis() + durationMs
            if (until > fastRefreshUntil) {
                fastRefreshUntil = until
            }
            Log.i(TAG, "triggerFastRefresh: fast 1s refresh triggered")

            instance?.let { service ->
                service.serviceScope.launch {
                    service.performPoll(forceNotification = true)
                }
                service.restartPollLoop()
            }
        }

        fun start(context: Context) {
            try {
                val intent = Intent(context, TokenMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start TokenMonitorService", e)
            }
        }

        fun triggerImmediatePoll() {
            instance?.let { service ->
                service.serviceScope.launch {
                    service.performPoll(forceNotification = true)
                }
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, TokenMonitorService::class.java)
                context.stopService(intent)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to stop TokenMonitorService", e)
            }
        }
    }
}
