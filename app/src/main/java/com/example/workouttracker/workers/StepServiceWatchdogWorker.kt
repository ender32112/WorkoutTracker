package com.example.workouttracker.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.workouttracker.ui.analytics.DIAG_LAST_NON_START_REASON
import com.example.workouttracker.ui.analytics.startStepCounterServiceSafely
import com.example.workouttracker.ui.analytics.userAnalyticsPrefs
import java.util.concurrent.TimeUnit

private const val WATCHDOG_STALE_TIMEOUT_MS = 2 * 60 * 60 * 1000L

class StepServiceWatchdogWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = userAnalyticsPrefs(applicationContext)
        val lastSensorTs = prefs.getLong("steps_last_ts", 0L)
        val now = System.currentTimeMillis()
        val stale = lastSensorTs == 0L || now - lastSensorTs > WATCHDOG_STALE_TIMEOUT_MS

        if (stale) {
            val started = startStepCounterServiceSafely(applicationContext, reason = "watchdog", prefs = prefs)
            if (!started) {
                prefs.edit().putString(DIAG_LAST_NON_START_REASON, "watchdog_start_failed").apply()
            }
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "step_service_watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StepServiceWatchdogWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
