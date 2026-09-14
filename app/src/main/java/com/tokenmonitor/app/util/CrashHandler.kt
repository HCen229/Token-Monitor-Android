package com.tokenmonitor.app.util

import android.app.Application
import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "TokenMonitorCrash"
    private var previous: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null

    fun install(app: Application) {
        appContext = app.applicationContext
        previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        Log.i(TAG, "CrashHandler successfully installed")
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stack = sw.toString()
            Log.e(TAG, "FATAL CRASH on thread ${thread.name}:\n$stack")

            val ctx = appContext
            if (ctx != null) {
                val dir = ctx.getExternalFilesDir(null) ?: ctx.filesDir
                val file = File(dir, "tokenmonitor_crash.log")
                file.writeText("Time: ${System.currentTimeMillis()}\nThread: ${thread.name}\n$stack")
            }
        } catch (_: Throwable) {
        }

        val prev = previous
        if (prev != null) {
            prev.uncaughtException(thread, throwable)
        } else {
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(10)
        }
    }
}
