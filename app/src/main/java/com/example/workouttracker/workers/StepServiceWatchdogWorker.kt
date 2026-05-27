package com.example.workouttracker.workers

import com.example.workouttracker.feature.analytics.runtime.*

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.workouttracker.health.HealthConnectAvailability
import com.example.workouttracker.health.HealthConnectGateway
import com.example.workouttracker.feature.analytics.runtime.DIAG_LAST_NON_START_REASON
import com.example.workouttracker.feature.analytics.runtime.PREF_HEALTH_CONNECT_ACTIVE
import com.example.workouttracker.feature.analytics.runtime.PREF_STEP_DATA_SOURCE
import com.example.workouttracker.feature.analytics.runtime.STEP_SOURCE_DEVICE_SENSOR
import com.example.workouttracker.feature.analytics.runtime.STEP_SOURCE_HEALTH_CONNECT
import com.example.workouttracker.feature.analytics.runtime.refreshStepProgressNotification
import com.example.workouttracker.feature.analytics.runtime.startStepCounterServiceSafely
import com.example.workouttracker.feature.analytics.runtime.userAnalyticsPrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val WATCHDOG_STALE_TIMEOUT_MS = 2 * 60 * 60 * 1000L

class StepServiceWatchdogWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = userAnalyticsPrefs(applicationContext)
        val selectedSource = prefs.getString(PREF_STEP_DATA_SOURCE, STEP_SOURCE_DEVICE_SENSOR) ?: STEP_SOURCE_DEVICE_SENSOR
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()

        if (selectedSource == STEP_SOURCE_HEALTH_CONNECT) {
            val gateway = HealthConnectGateway(applicationContext)
            val canSyncFromHealthConnect =
                prefs.getBoolean(PREF_HEALTH_CONNECT_ACTIVE, false) &&
                    gateway.availability() == HealthConnectAvailability.AVAILABLE &&
                    gateway.hasAllPermissions()
            val lastUpdateTs = prefs.getLong("steps_last_ts", 0L)
            val stale = lastUpdateTs == 0L || now - lastUpdateTs > WATCHDOG_STALE_TIMEOUT_MS

            if (canSyncFromHealthConnect) {
                if (stale) {
                    startStepCounterServiceSafely(applicationContext, reason = "watchdog_health_connect", prefs = prefs)
                }
                val stepsToday = gateway.readTodaySteps()
                prefs.edit()
                    .putLong("steps_today", stepsToday)
                    .putString("steps_today_date", todayIso)
                    .putString(DIAG_LAST_NON_START_REASON, "watchdog_health_connect_refresh")
                    .apply()
            } else {
                prefs.edit().putString(DIAG_LAST_NON_START_REASON, "watchdog_health_connect_cached").apply()
            }

            refreshStepProgressNotification(applicationContext)
            return Result.success()
        }

        if (prefs.getBoolean(PREF_HEALTH_CONNECT_ACTIVE, false)) {
            prefs.edit().putString(DIAG_LAST_NON_START_REASON, "watchdog_skipped_health_connect_active").apply()
            refreshStepProgressNotification(applicationContext)
            return Result.success()
        }

        val lastSensorTs = prefs.getLong("steps_last_ts", 0L)
        val stale = lastSensorTs == 0L || now - lastSensorTs > WATCHDOG_STALE_TIMEOUT_MS

        if (stale) {
            val started = startStepCounterServiceSafely(applicationContext, reason = "watchdog", prefs = prefs)
            if (!started) {
                prefs.edit().putString(DIAG_LAST_NON_START_REASON, "watchdog_start_failed").apply()
            }
        }
        refreshStepProgressNotification(applicationContext)
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

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }
}




