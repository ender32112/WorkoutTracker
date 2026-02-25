package com.example.workouttracker.ui.analytics

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

const val EXTRA_START_REASON = "extra_step_service_start_reason"
const val DIAG_LAST_SERVICE_START_TS = "diag_last_service_start_ts"
const val DIAG_LAST_SENSOR_EVENT_TS = "diag_last_sensor_event_ts"
const val DIAG_LAST_SENSOR_EVENT_UPTIME_NS = "diag_last_sensor_uptime_ns"
const val DIAG_LAST_NON_START_REASON = "diag_last_non_start_reason"

fun startStepCounterServiceSafely(context: Context, reason: String, prefs: SharedPreferences = userAnalyticsPrefs(context)): Boolean {
    val hasActivityPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACTIVITY_RECOGNITION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasActivityPermission) {
        prefs.edit().putString(DIAG_LAST_NON_START_REASON, "permission_missing:$reason").apply()
        return false
    }

    val intent = Intent(context, StepCounterService::class.java).apply {
        putExtra(EXTRA_START_REASON, reason)
    }

    return runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
        true
    }.getOrElse {
        prefs.edit().putString(DIAG_LAST_NON_START_REASON, "exception_${it.javaClass.simpleName}:$reason").apply()
        false
    }
}
