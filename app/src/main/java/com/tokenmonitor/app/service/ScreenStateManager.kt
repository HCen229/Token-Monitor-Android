package com.tokenmonitor.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors the device screen display state (Screen ON / Screen OFF).
 * When screen turns OFF (息屏): freezes background polling and retains the last data.
 * When screen turns ON (亮屏): unfreezes polling and immediately triggers a data fetch.
 */
object ScreenStateManager {

    private const val TAG = "ScreenStateManager"

    private val _isScreenOn = MutableStateFlow(true)
    val isScreenOn: StateFlow<Boolean> = _isScreenOn.asStateFlow()

    private var isInitialized = false

    var onScreenTurnedOn: (() -> Unit)? = null

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val appContext = context.applicationContext
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        _isScreenOn.value = pm?.isInteractive ?: true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.d(TAG, "Screen turned OFF (息屏). Freezing data updates.")
                        _isScreenOn.value = false
                    }
                    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                        val wasOff = !_isScreenOn.value
                        Log.d(TAG, "Screen turned ON (亮屏). Resuming data updates.")
                        _isScreenOn.value = true
                        if (wasOff) {
                            try {
                                onScreenTurnedOn?.invoke()
                            } catch (e: Throwable) {
                                Log.e(TAG, "Error invoking onScreenTurnedOn callback", e)
                            }
                        }
                    }
                }
            }
        }

        try {
            appContext.registerReceiver(receiver, filter)
            Log.i(TAG, "ScreenStateManager initialized. Initial isScreenOn = ${_isScreenOn.value}")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register screen state receiver", e)
        }
    }

    fun isInteractive(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val interactive = pm?.isInteractive ?: _isScreenOn.value
        if (_isScreenOn.value != interactive) {
            _isScreenOn.value = interactive
        }
        return interactive
    }
}
