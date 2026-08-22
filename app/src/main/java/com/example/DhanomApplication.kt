package com.example

import android.app.Application
import android.content.Intent
import android.os.Process
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Self-healing application: any uncaught crash is logged to a local crash log
 * and the app restarts itself (with a short guard to avoid restart loops).
 * The log is viewable/clearable from Profile → Self-heal.
 */
class DhanomApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logCrash(throwable)
                val last = lastCrashTime()
                if (System.currentTimeMillis() - last > 5000L) {
                    saveCrashTime()
                    restartApp()
                }
            } catch (_: Throwable) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
                Process.killProcess(Process.myPid())
            }
        }

        // Start the in-app monthly analysis agent (analyzes data + savings every month).
        try {
            com.example.domain.agent.MonthlyAnalysisWorker.schedule(this)
        } catch (_: Throwable) {
        }
    }

    private fun logCrash(t: Throwable) {
        try {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            crashLogFile().appendText("=== ${System.currentTimeMillis()} ===\n${sw}\n\n")
        } catch (_: Throwable) {
        }
    }

    private fun restartApp() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (intent != null) startActivity(intent)
        } catch (_: Throwable) {
        }
    }

    private fun crashLogFile(): File = File(filesDir, "crash-log.txt")

    private fun lastCrashTime(): Long =
        getSharedPreferences("dhanom_prefs", MODE_PRIVATE).getLong("last_crash_time", 0L)

    private fun saveCrashTime() {
        getSharedPreferences("dhanom_prefs", MODE_PRIVATE)
            .edit().putLong("last_crash_time", System.currentTimeMillis()).apply()
    }

    companion object {
        fun readCrashLog(context: Application): String =
            try {
                File(context.filesDir, "crash-log.txt").readText().takeLast(4000)
            } catch (_: Throwable) {
                "No crashes recorded. 🙏"
            }

        fun clearCrashLog(context: Application) {
            try {
                File(context.filesDir, "crash-log.txt").delete()
            } catch (_: Throwable) {
            }
        }
    }
}
