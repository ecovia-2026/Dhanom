package com.example.domain.agent

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Periodic background job that wakes once a day and asks [MonthlyAutopilot] to
 * produce its monthly analysis. The autopilot itself only writes a report when
 * a NEW calendar month has started, so this is safe to run daily.
 */
class MonthlyAnalysisWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Auto-post any due recurring transactions, then the monthly analysis.
            RecurringEngine.processDue(applicationContext)
            MonthlyAutopilot.run(applicationContext, force = false)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "dhanom-monthly-autopilot"

        /** Schedules the monthly agent (idempotent — keeps the existing job). */
        fun schedule(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<MonthlyAnalysisWorker>(1, TimeUnit.DAYS).build()
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            } catch (_: Exception) {
                // WorkManager unavailable — the app still runs fine.
            }
        }
    }
}
