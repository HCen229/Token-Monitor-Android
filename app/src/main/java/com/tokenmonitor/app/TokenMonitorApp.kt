package com.tokenmonitor.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.tokenmonitor.app.service.TokenNotificationManager
import com.tokenmonitor.app.util.CrashHandler

class TokenMonitorApp : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        CrashHandler.install(this)
        try {
            com.tokenmonitor.app.service.ScreenStateManager.init(this)
            TokenNotificationManager.init(this)
        } catch (e: Throwable) {
            Log.e("TokenMonitorApp", "Failed to init services", e)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivityCount++
        isAppInForeground = startedActivityCount > 0
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
        isAppInForeground = startedActivityCount > 0
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        private var startedActivityCount = 0

        @Volatile
        var isAppInForeground: Boolean = false
            private set
    }
}
