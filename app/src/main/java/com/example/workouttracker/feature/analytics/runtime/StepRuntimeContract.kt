package com.example.workouttracker.feature.analytics.runtime

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.workouttracker.MainActivity
import com.example.workouttracker.R
import com.example.workouttracker.health.HealthConnectAvailability
import com.example.workouttracker.health.HealthConnectGateway
import com.example.workouttracker.core.auth.AuthSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

const val EXTRA_START_REASON = "extra_step_service_start_reason"
const val DIAG_LAST_SERVICE_START_TS = "diag_last_service_start_ts"
const val DIAG_LAST_SENSOR_EVENT_TS = "diag_last_sensor_event_ts"
const val DIAG_LAST_SENSOR_EVENT_UPTIME_NS = "diag_last_sensor_uptime_ns"
const val DIAG_LAST_NON_START_REASON = "diag_last_non_start_reason"
const val PREF_HEALTH_CONNECT_ACTIVE = "pref_health_connect_active"
const val PREF_STEP_DATA_SOURCE = "step_data_source"
const val STEP_SOURCE_DEVICE_SENSOR = "device_sensor"
const val STEP_SOURCE_HEALTH_CONNECT = "health_connect"
const val ACTION_STEPS_UPDATED = "com.example.workouttracker.STEPS_UPDATED"
const val NOTIF_ID_SERVICE = 1002

private const val NOTIF_CHANNEL_ID_GOAL = "steps_goal_channel"
private const val NOTIF_CHANNEL_ID_SERVICE = "step_tracking_channel"
private const val NOTIF_ID_GOAL = 1001

fun userAnalyticsPrefs(context: Context): SharedPreferences {
    val authPrefs = context.getSharedPreferences(AuthSessionStore.PREFS_NAME, Context.MODE_PRIVATE)
    val userId = authPrefs.getString(AuthSessionStore.KEY_CURRENT_USER_ID, null) ?: AuthSessionStore.DEFAULT_USER_ID
    return context.getSharedPreferences("analytics_prefs_" + userId, Context.MODE_PRIVATE)
}

fun todayIso(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

data class StepHistoryEntry(
    val isoDate: String,
    val prettyDate: String,
    val steps: Long
)

enum class StepDataSource(val storageValue: String) {
    DEVICE_SENSOR(STEP_SOURCE_DEVICE_SENSOR),
    HEALTH_CONNECT(STEP_SOURCE_HEALTH_CONNECT);

    companion object {
        fun fromStored(value: String?): StepDataSource {
            return entries.firstOrNull { it.storageValue == value } ?: DEVICE_SENSOR
        }
    }
}

fun StepDataSource.uiTitle(): String = when (this) {
    StepDataSource.DEVICE_SENSOR -> "Датчик устройства"
    StepDataSource.HEALTH_CONNECT -> "Health Connect"
}

fun StepDataSource.uiDescription(): String = when (this) {
    StepDataSource.DEVICE_SENSOR -> "Шаги считаются по датчику телефона в реальном времени."
    StepDataSource.HEALTH_CONNECT -> "Шаги берутся из дневной суммы, сохраненной в Health Connect."
}

fun appendStepsHistory(context: Context, dateIso: String, steps: Long) {
    if (dateIso.isBlank()) return
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        AnalyticsRuntimeStorage.from(context).saveStepEntry(dateIso = dateIso, steps = steps)
    }
}

fun isoToPrettyDate(iso: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("dd.MM", Locale.getDefault())
        formatter.format(parser.parse(iso) ?: return iso)
    } catch (_: Exception) {
        iso
    }
}

fun validateIsoDate(date: String): String? {
    val regex = Regex("""\d{4}-\d{2}-\d{2}""")
    if (!regex.matches(date)) return "Неверный формат даты"

    return try {
        val (yyyy, mm, dd) = date.split("-").map { it.toInt() }
        if (mm !in 1..12) return "Месяц должен быть от 01 до 12"
        if (dd !in 1..31) return "День должен быть от 01 до 31"
        LocalDate.of(yyyy, mm, dd)
        null
    } catch (_: Exception) {
        "Некорректная дата"
    }
}

fun startStepCounterServiceSafely(
    context: Context,
    reason: String,
    prefs: SharedPreferences = userAnalyticsPrefs(context)
): Boolean {
    val selectedSource = prefs.getString(PREF_STEP_DATA_SOURCE, STEP_SOURCE_DEVICE_SENSOR) ?: STEP_SOURCE_DEVICE_SENSOR
    when (selectedSource) {
        STEP_SOURCE_DEVICE_SENSOR -> {
            val hasActivityPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasActivityPermission) {
                prefs.edit().putString(DIAG_LAST_NON_START_REASON, "permission_missing:$reason").apply()
                return false
            }
        }

        STEP_SOURCE_HEALTH_CONNECT -> {
            val gateway = HealthConnectGateway(context.applicationContext)
            val healthConnectActive = prefs.getBoolean(PREF_HEALTH_CONNECT_ACTIVE, false)
            if (!healthConnectActive) {
                prefs.edit().putString(DIAG_LAST_NON_START_REASON, "health_connect_inactive:$reason").apply()
                return false
            }
            if (gateway.availability() != HealthConnectAvailability.AVAILABLE) {
                prefs.edit().putString(DIAG_LAST_NON_START_REASON, "health_connect_unavailable:$reason").apply()
                return false
            }
        }

        else -> {
            prefs.edit().putString(DIAG_LAST_NON_START_REASON, "unknown_source_$selectedSource:$reason").apply()
            return false
        }
    }

    val intent = Intent(context, StepCounterService::class.java).apply {
        putExtra(EXTRA_START_REASON, reason)
        putExtra(PREF_STEP_DATA_SOURCE, selectedSource)
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

fun sendGoalNotificationIfNeeded(context: Context, prefs: SharedPreferences, stepsToday: Long) {
    val notifyStepsEnabled = prefs.getBoolean("notify_steps_enabled", true)
    if (!notifyStepsEnabled) return

    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return

    val stepGoal = prefs.getInt("step_goal", 8000)
    val day = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val lastGoalNotified = prefs.getInt("steps_last_goal_notified", -1)
    val lastNotifyDay = prefs.getString("steps_last_notify_day", "")

    val reached = stepsToday >= stepGoal
    val notSentForThisGoalToday = (lastGoalNotified != stepGoal || lastNotifyDay != day)

    if (reached && notSentForThisGoalToday) {
        ensureGoalChannel(context)
        val notif = NotificationCompat.Builder(context, NOTIF_CHANNEL_ID_GOAL)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round))
            .setContentTitle("Цель достигнута")
            .setContentText("Вы прошли $stepsToday шагов из $stepGoal. Отличная работа!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_GOAL, notif)

        prefs.edit()
            .putInt("steps_last_goal_notified", stepGoal)
            .putString("steps_last_notify_day", day)
            .apply()
    }
}

fun refreshStepProgressNotification(context: Context) {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return

    val prefs = userAnalyticsPrefs(context)
    ensureStepProgressChannel(context)
    NotificationManagerCompat.from(context).notify(
        NOTIF_ID_SERVICE,
        buildStepProgressNotification(context, prefs.getLong("steps_today", 0L), prefs)
    )
}

fun buildStepProgressNotification(
    context: Context,
    stepsToday: Long,
    prefs: SharedPreferences
): Notification {
    val goal = prefs.getInt("step_goal", 8000).coerceAtLeast(1)
    val source = StepDataSource.fromStored(
        prefs.getString(PREF_STEP_DATA_SOURCE, STEP_SOURCE_DEVICE_SENSOR)
    )
    val progressPercent = ((stepsToday.toFloat() / goal) * 100f).toInt().coerceIn(0, 100)
    val sourceLabel = when (source) {
        StepDataSource.DEVICE_SENSOR -> "Источник: датчик устройства"
        StepDataSource.HEALTH_CONNECT -> "Источник: Health Connect"
    }
    val motivation = when {
        progressPercent >= 100 -> "Цель достигнута"
        progressPercent >= 75 -> "Финиш уже близко"
        progressPercent >= 50 -> "Больше половины пройдено"
        progressPercent >= 25 -> "Хороший темп на сегодня"
        else -> "Прогресс появится по мере движения"
    }

    val intent = Intent(context, MainActivity::class.java)
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

    return NotificationCompat.Builder(context, NOTIF_CHANNEL_ID_SERVICE)
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle("Шаги сегодня")
        .setContentText("$stepsToday из $goal шагов")
        .setSubText(sourceLabel)
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                "$stepsToday из $goal шагов\n$motivation"
            )
        )
        .setProgress(goal, stepsToday.coerceAtMost(goal.toLong()).toInt(), false)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(pendingIntent)
        .build()
}

private fun ensureGoalChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIF_CHANNEL_ID_GOAL,
            "Достижение цели по шагам",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Уведомления при выполнении дневной цели шагов"
        }
        nm.createNotificationChannel(channel)
    }
}

fun ensureStepProgressChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIF_CHANNEL_ID_SERVICE,
            "Прогресс шагов",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Постоянное уведомление с текущим прогрессом по шагам"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }
}
