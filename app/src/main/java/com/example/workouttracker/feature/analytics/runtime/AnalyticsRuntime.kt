package com.example.workouttracker.feature.analytics.runtime

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.workouttracker.MainActivity
import com.example.workouttracker.R
import com.example.workouttracker.health.HealthConnectAvailability
import com.example.workouttracker.health.HealthConnectGateway
import com.example.workouttracker.workers.StepServiceWatchdogWorker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

data class WeatherResponse(val main: Main, val weather: List<WeatherDesc>)
data class Main(val temp: Double)
data class WeatherDesc(val description: String)

interface WeatherApi {
    @GET("weather")
    suspend fun getCurrentWeatherByCity(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "ru"
    ): WeatherResponse
}

const val WEATHER_TTL_MS = 30 * 60 * 1000L
const val PREF_KEY_SENSOR_BASELINE_RAW = "steps_sensor_baseline_raw"
const val HEALTH_CONNECT_POLL_MS = 5_000L


class StepCounterService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var stepDetector: Sensor? = null
    private lateinit var prefs: SharedPreferences
    private lateinit var healthConnectGateway: HealthConnectGateway
    private var healthConnectPollingStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = userAnalyticsPrefs(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        healthConnectGateway = HealthConnectGateway(applicationContext)

        prefs.edit().putLong(DIAG_LAST_SERVICE_START_TS, System.currentTimeMillis()).apply()

        ensureServiceChannel()
        val initialSteps = prefs.getLong("steps_today", 0L)
        startForeground(NOTIF_ID_SERVICE, buildForegroundNotification(initialSteps))

        configureStepSource()
        MidnightResetReceiver.scheduleNext(this)
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(stepListener)
        sensorManager.unregisterListener(stepDetectorListener)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reason = intent?.getStringExtra(EXTRA_START_REASON) ?: "unknown"
        prefs.edit()
            .putLong(DIAG_LAST_SERVICE_START_TS, System.currentTimeMillis())
            .putString(DIAG_LAST_NON_START_REASON, "started:$reason")
            .apply()
        configureStepSource()
        return START_STICKY
    }

    private fun configureStepSource() {
        sensorManager.unregisterListener(stepListener)
        sensorManager.unregisterListener(stepDetectorListener)

        when (prefs.getString(PREF_STEP_DATA_SOURCE, STEP_SOURCE_DEVICE_SENSOR) ?: STEP_SOURCE_DEVICE_SENSOR) {
            STEP_SOURCE_HEALTH_CONNECT -> startHealthConnectTracking()
            STEP_SOURCE_DEVICE_SENSOR -> startDeviceSensorTracking()
            else -> {
                prefs.edit().putString(DIAG_LAST_NON_START_REASON, "unknown_source").apply()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun startDeviceSensorTracking() {
        healthConnectPollingStarted = false

        val listenerRegistered = when {
            stepSensor != null -> sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)
            stepDetector != null -> sensorManager.registerListener(stepDetectorListener, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
            else -> false
        }

        if (!listenerRegistered) {
            prefs.edit().putString(DIAG_LAST_NON_START_REASON, "sensor_listener_not_registered").apply()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        prefs.edit()
            .putLong("steps_boot_elapsed_realtime_ns", SystemClock.elapsedRealtimeNanos())
            .putString(DIAG_LAST_NON_START_REASON, "started_and_listening")
            .apply()
        refreshForegroundNotification(prefs.getLong("steps_today", 0L))
    }

    private fun startHealthConnectTracking() {
        if (healthConnectGateway.availability() != HealthConnectAvailability.AVAILABLE) {
            prefs.edit().putString(DIAG_LAST_NON_START_REASON, "health_connect_unavailable").apply()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        if (healthConnectPollingStarted) return
        healthConnectPollingStarted = true
        prefs.edit().putString(DIAG_LAST_NON_START_REASON, "started_health_connect_polling").apply()

        serviceScope.launch {
            syncHealthConnectSteps(refreshHistory = true)
            while (isActive && isHealthConnectSelected()) {
                delay(HEALTH_CONNECT_POLL_MS)
                syncHealthConnectSteps(refreshHistory = false)
            }
            healthConnectPollingStarted = false
        }
    }

    private fun isHealthConnectSelected(): Boolean {
        return (prefs.getString(PREF_STEP_DATA_SOURCE, STEP_SOURCE_DEVICE_SENSOR) ?: STEP_SOURCE_DEVICE_SENSOR) ==
            STEP_SOURCE_HEALTH_CONNECT
    }

    private suspend fun syncHealthConnectSteps(refreshHistory: Boolean) {
        if (!isHealthConnectSelected()) return
        if (!healthConnectGateway.hasAllPermissions()) {
            prefs.edit().putString(DIAG_LAST_NON_START_REASON, "health_connect_permissions_missing").apply()
            return
        }

        runCatching {
            val today = todayIso()
            if (refreshHistory) {
                val stepsByDate = healthConnectGateway.readStepsByDate(days = 30)
                val todaySteps = stepsByDate[today] ?: 0L
                AnalyticsRuntimeStorage.from(this@StepCounterService).replaceStepHistory(stepsByDate)
                persistTodaySteps(todaySteps, now = System.currentTimeMillis(), sensorTimestampNs = null)
            } else {
                val todaySteps = healthConnectGateway.readTodaySteps()
                persistTodaySteps(todaySteps, now = System.currentTimeMillis(), sensorTimestampNs = null)
            }
            prefs.edit().putString(DIAG_LAST_NON_START_REASON, "health_connect_poll_success").apply()
        }.onFailure { error ->
            Log.w("HealthConnect", "Background Health Connect sync failed", error)
            prefs.edit().putString(DIAG_LAST_NON_START_REASON, "health_connect_poll_failed").apply()
        }
    }

    private fun persistTodaySteps(
        targetSteps: Long,
        now: Long,
        sensorTimestampNs: Long?
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
        val storedDate = prefs.getString("steps_today_date", today) ?: today
        val previousSteps = prefs.getLong("steps_today", 0L)

        if (storedDate != today) {
            appendStepsHistory(this@StepCounterService, storedDate, previousSteps)
        }

        val safeSteps = targetSteps.coerceAtLeast(0L)
        prefs.edit()
            .putLong("steps_today", safeSteps)
            .putString("steps_today_date", today)
            .putLong("steps_last_ts", now)
            .apply {
                if (sensorTimestampNs != null) {
                    putLong(DIAG_LAST_SENSOR_EVENT_UPTIME_NS, sensorTimestampNs)
                }
            }
            .putLong(DIAG_LAST_SENSOR_EVENT_TS, now)
            .apply()

        refreshForegroundNotification(safeSteps)
        sendBroadcast(Intent(ACTION_STEPS_UPDATED).setPackage(packageName))
        sendGoalNotificationIfNeeded(this, prefs, safeSteps)
    }

    private fun refreshForegroundNotification(stepsToday: Long) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_SERVICE, buildForegroundNotification(stepsToday))
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

            val raw = event.values[0].toLong()
            val now = System.currentTimeMillis()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))

            val lastRaw = prefs.getLong("steps_last_raw", -1L)
            val storedDate = prefs.getString("steps_today_date", today) ?: today
            val storedSteps = prefs.getLong("steps_today", 0L)
            val currentElapsedNs = SystemClock.elapsedRealtimeNanos()
            val baseSteps = if (storedDate == today) storedSteps else 0L
            var baselineRaw = prefs.getLong(PREF_KEY_SENSOR_BASELINE_RAW, Long.MIN_VALUE)

            if (storedDate != today) {
                baselineRaw = raw
            } else if (baselineRaw == Long.MIN_VALUE) {
                baselineRaw = raw - baseSteps
            } else if (raw < lastRaw || raw < baselineRaw) {
                baselineRaw = raw - baseSteps
            }

            val computedTodaySteps = (raw - baselineRaw).coerceAtLeast(0L)
            val newTodaySteps = max(baseSteps, computedTodaySteps)

            prefs.edit()
                .putLong(PREF_KEY_SENSOR_BASELINE_RAW, baselineRaw)
                .putLong("steps_last_raw", raw)
                .putLong("steps_last_sensor_ts", event.timestamp)
                .putLong("steps_boot_elapsed_realtime_ns", currentElapsedNs)
                .apply()

            persistTodaySteps(
                targetSteps = newTodaySteps,
                now = now,
                sensorTimestampNs = event.timestamp
            )
            Log.d(
                "Steps",
                "raw=$raw lastRaw=$lastRaw baseline=$baselineRaw stored=$storedSteps computed=$computedTodaySteps final=$newTodaySteps date=$storedDate->$today"
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val stepDetectorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_STEP_DETECTOR) return

            val increment = event.values.getOrNull(0)?.roundToInt()?.toLong() ?: 0L
            if (increment <= 0L) return

            val now = System.currentTimeMillis()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))
            val todaySteps = prefs.getLong("steps_today", 0L)
            val storedDate = prefs.getString("steps_today_date", today) ?: today
            val baseSteps = if (storedDate == today) todaySteps else 0L

            prefs.edit()
                .putLong("steps_boot_elapsed_realtime_ns", SystemClock.elapsedRealtimeNanos())
                .apply()

            persistTodaySteps(
                targetSteps = (baseSteps + increment).coerceAtLeast(0L),
                now = now,
                sensorTimestampNs = event.timestamp
            )
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun ensureServiceChannel() {
        ensureStepProgressChannel(this)
    }

    private fun buildForegroundNotification(stepsToday: Long): Notification {
        return buildStepProgressNotification(this, stepsToday, prefs)
    }
}

class MidnightResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = userAnalyticsPrefs(context)
        val storedDate = prefs.getString("steps_today_date", todayIso()) ?: todayIso()
        val previousSteps = prefs.getLong("steps_today", 0L)
        appendStepsHistory(context, storedDate, previousSteps)

        prefs.edit()
            .putLong("steps_today", 0L)
            .putString("steps_today_date", todayIso())
            .remove(PREF_KEY_SENSOR_BASELINE_RAW)
            .putLong("steps_last_raw", -1L)
            .putLong("steps_last_sensor_ts", -1L)
            .apply()

        refreshStepProgressNotification(context)
        context.sendBroadcast(Intent(ACTION_STEPS_UPDATED).setPackage(context.packageName))
        scheduleNext(context)
    }

    companion object {
        fun scheduleNext(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, 1)
            }

            val intent = Intent(context, MidnightResetReceiver::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pi = PendingIntent.getBroadcast(context, 0, intent, flags)

            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                    } else {
                        am.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                }
            }.onFailure { error ->
                Log.w("Analytics", "Failed to schedule exact midnight reset, using inexact fallback", error)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
                }
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            startStepCounterServiceSafely(context, reason = action)
            MidnightResetReceiver.scheduleNext(context)
            StepServiceWatchdogWorker.schedule(context)
            refreshStepProgressNotification(context)
        }
    }
}

@SuppressLint("MissingPermission")
suspend fun FusedLocationProviderClient.awaitHighAccuracyLocation(): Location? = suspendCancellableCoroutine { cont ->
    val tokenSource = CancellationTokenSource()
    cont.invokeOnCancellation { tokenSource.cancel() }
    getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
        .addOnSuccessListener { location -> if (cont.isActive) cont.resume(location) }
        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
}

@SuppressLint("MissingPermission")
suspend fun FusedLocationProviderClient.awaitLastLocationFallback(): Location? = suspendCancellableCoroutine { cont ->
    lastLocation
        .addOnSuccessListener { location -> if (cont.isActive) cont.resume(location) }
        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
}

suspend fun resolveCityName(context: Context, location: Location): String? = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) return@withContext null
    val geocoder = Geocoder(context, Locale.getDefault())
    if (Build.VERSION.SDK_INT >= 33) {
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(location.latitude, location.longitude, 5, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    val name = addresses.firstOrNull()?.let { pickCityName(it) }
                    if (cont.isActive) cont.resume(name)
                }

                override fun onError(errorMessage: String?) {
                    if (cont.isActive) cont.resume(null)
                }
            })
        }
    } else {
        try {
            val result = geocoder.getFromLocation(location.latitude, location.longitude, 5)
            result?.firstOrNull()?.let { pickCityName(it) }
        } catch (_: Exception) {
            null
        }
    }
}

private fun pickCityName(address: Address): String? {
    return address.locality
        ?: address.subAdminArea
        ?: address.adminArea
        ?: address.countryName
}

fun Context.hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}


