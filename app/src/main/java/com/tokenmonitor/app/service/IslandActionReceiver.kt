package com.tokenmonitor.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver triggered by Xiaomi HyperOS Super Island interactions
 * (e.g. clicking the island capsule to expand, or clicking cards inside the expanded island).
 * Upon receiving this broadcast, it triggers 1-second high-frequency polling for the expanded island.
 */
class IslandActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ISLAND_EXPAND = "com.tokenmonitor.app.ACTION_ISLAND_EXPAND"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.i("IslandActionReceiver", "Received island action broadcast: $action")
        if (action == ACTION_ISLAND_EXPAND) {
            TokenMonitorService.triggerFastRefresh(60_000L)
            if (!TokenMonitorService.isRunning()) {
                TokenNotificationManager.startMonitoring(context)
            }
        }
    }
}
