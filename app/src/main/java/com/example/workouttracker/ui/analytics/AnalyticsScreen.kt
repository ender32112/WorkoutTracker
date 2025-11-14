package com.example.workouttracker.ui.analytics

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.workouttracker.R
import com.example.workouttracker.ui.components.SectionHeader
import com.example.workouttracker.ui.nutrition.NutritionEntry
import com.example.workouttracker.ui.training.ExerciseEntry
import com.example.workouttracker.viewmodel.NutritionViewModel
import com.example.workouttracker.viewmodel.TrainingViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/* ===================== Weather DTO & API ===================== */
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

/* ===================== Const ===================== */
private const val WEATHER_TTL_MS = 30 * 60 * 1000L
private const val NOTIF_CHANNEL_ID_GOAL = "steps_goal_channel"
private const val NOTIF_CHANNEL_ID_SERVICE = "step_tracking_channel"
private const val NOTIF_ID_GOAL = 1001
private const val NOTIF_ID_SERVICE = 1002
private const val ACTION_STEPS_UPDATED = "com.example.workouttracker.STEPS_UPDATED"

/* ===================== Analytics Screen ===================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    trainingViewModel: TrainingViewModel = viewModel(),
    nutritionViewModel: NutritionViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs: SharedPreferences = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)

    // ---- Keys ----
    val K_TODAY_DATE = "steps_today_date"      // дата, для которой считаем stepsToday
    val K_LAST_RAW = "steps_last_raw"         // последнее "сырое" значение датчика
    val K_LAST_TS = "steps_last_ts"           // время последнего события датчика
    val K_STEPS_TODAY = "steps_today"         // накопленные шаги за сегодня

    val K_WEATHER_JSON = "weather_cache_json"
    val K_WEATHER_TIME = "weather_cache_time"
    val K_CITY = "weather_city"
    val K_STEP_GOAL = "step_goal"
    val K_WEIGHT_JSON = "weight_history"
    val K_NOTIFY_ENABLED = "notify_steps_enabled"
    val K_GOAL_SENT_FOR_DAY = "goal_sent_day"
    val K_BEST_EX_LIMIT = "best_ex_limit"
    val K_LAST_GOAL_NOTIFIED = "steps_last_goal_notified"
    val K_LAST_NOTIFY_DAY = "steps_last_notify_day"

    fun todayKeyIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    fun todayPrettyShort(): String = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())
    fun timePretty(ts: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

    /* ---------- Permissions ---------- */
    var hasStepPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val stepPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStepPermission = granted
        prefs.edit().putBoolean("step_permission", granted).apply()
        if (granted) {
            val serviceIntent = Intent(context, StepCounterService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 33)
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotifPermission = granted }

    /* ---------- Settings state ---------- */
    var stepGoal by rememberSaveable { mutableStateOf(prefs.getInt(K_STEP_GOAL, 8000)) }
    var city by rememberSaveable { mutableStateOf(prefs.getString(K_CITY, "Москва") ?: "Москва") }
    var notifyStepsEnabled by rememberSaveable { mutableStateOf(prefs.getBoolean(K_NOTIFY_ENABLED, true)) }
    var bestExercisesLimit by rememberSaveable { mutableStateOf(prefs.getInt(K_BEST_EX_LIMIT, 5).coerceIn(1, 10)) }

    /* ---------- Snackbar ---------- */
    val snackbarHost = remember { SnackbarHostState() }
    suspend fun showSnack(msg: String) { snackbarHost.showSnackbar(msg) }

    /* ---------- Steps state ---------- */
    var stepsToday by remember {
        mutableStateOf(prefs.getLong(K_STEPS_TODAY, 0L))
    }

    /* ---------- Realtime updates via broadcast ---------- */
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                stepsToday = prefs.getLong(K_STEPS_TODAY, 0L)
            }
        }

        // Регистрация
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter(ACTION_STEPS_UPDATED))

        // Инициализация значения
        stepsToday = prefs.getLong(K_STEPS_TODAY, 0L)

        // Планирование сброса в полночь
        MidnightResetReceiver.scheduleNext(context)

        // Отмена при выходе из composable
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        }
    }

    /* ---------- Weather with cache ---------- */
    var weather by remember { mutableStateOf("Загрузка...") }
    var weatherSubtitle by remember { mutableStateOf<String?>(null) }

    val retrofit = remember {
        Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val api = remember { retrofit.create(WeatherApi::class.java) }

    fun setWeatherFromCache(): Boolean {
        val cached = prefs.getString(K_WEATHER_JSON, null) ?: return false
        val time = prefs.getLong(K_WEATHER_TIME, 0L)
        return try {
            val obj = JSONObject(cached)
            val temp = obj.getDouble("temp").roundToInt()
            val desc = obj.getString("desc")
            val ts = if (time > 0) timePretty(time) else "-"
            weather = "$desc, $temp°C"
            weatherSubtitle = "Обновлено $ts"
            true
        } catch (_: Exception) { false }
    }
    fun cacheIsFresh(): Boolean {
        val t = prefs.getLong(K_WEATHER_TIME, 0L)
        return t > 0 && (System.currentTimeMillis() - t) < WEATHER_TTL_MS
    }
    suspend fun fetchAndCacheWeather(currentCity: String) {
        try {
            val apiKey = context.getString(R.string.openweather_api_key)
            if (apiKey.isBlank()) {
                weather = "API key не настроен"
                weatherSubtitle = null
                return
            }
            val response = api.getCurrentWeatherByCity(currentCity, apiKey)
            val temp = response.main.temp.roundToInt()
            val desc = response.weather.getOrNull(0)?.description?.replaceFirstChar { it.uppercase() } ?: "-"

            weather = "$desc, $temp°C"
            weatherSubtitle = "Обновлено " + timePretty(System.currentTimeMillis())

            val cached = JSONObject().apply {
                put("temp", response.main.temp)
                put("desc", desc)
            }.toString()
            prefs.edit()
                .putString(K_WEATHER_JSON, cached)
                .putLong(K_WEATHER_TIME, System.currentTimeMillis())
                .apply()
        } catch (_: Exception) {
            if (!setWeatherFromCache()) {
                weather = "Нет сети / нет кэша"
                weatherSubtitle = null
            } else {
                scope.launch { showSnack("Показаны последние сохранённые данные погоды") }
            }
        }
    }
    LaunchedEffect(city) {
        val hadCache = setWeatherFromCache()
        if (!hadCache) weather = "Загрузка..."
        if (!cacheIsFresh()) scope.launch { fetchAndCacheWeather(city) }
    }

    /* ---------- Nutrition Today ---------- */
    val todayIso = todayKeyIso()
    val entries by nutritionViewModel.entries.collectAsState()
    val todayNutrition = entries.filter { it.date == todayIso || it.date == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayTotal = todayNutrition.fold(
        NutritionEntry(
            id = UUID.randomUUID(),
            date = todayIso,
            name = "",
            calories = 0, protein = 0, carbs = 0, fats = 0, weight = 0
        )
    ) { acc, e ->
        acc.copy(
            calories = acc.calories + e.calories,
            protein = acc.protein + e.protein,
            carbs = acc.carbs + e.carbs,
            fats = acc.fats + e.fats,
            weight = acc.weight + e.weight
        )
    }

    /* ---------- Weight history ---------- */
    fun loadWeightHistory(): List<Pair<String, Float>> {
        val json = prefs.getString(K_WEIGHT_JSON, "[]") ?: "[]"
        return try {
            val list = mutableListOf<Pair<String, Float>>()
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val d = o.optString("date")
                val pretty = when {
                    d.length == 5 -> d
                    d.length == 10 && d[2] == '.' && d[5] == '.' -> d.substring(0, 5) // dd.MM.yyyy -> dd.MM
                    else -> d
                }
                list.add(pretty to o.getDouble("weight").toFloat())
            }
            list
        } catch (_: Exception) { emptyList() }
    }
    var weightHistory by remember { mutableStateOf(loadWeightHistory()) }

    var weightInput by rememberSaveable { mutableStateOf("") }
    var weightError by remember { mutableStateOf<String?>(null) }

    fun validateDateShort(text: String): String? {
        // Требуем ДД.MM; проверяем корректность дня/месяца
        val re = Regex("""^\d{2}\.\d{2}$""")
        if (!re.matches(text)) return "Дата в формате ДД.ММ"
        val day = text.substring(0, 2).toIntOrNull() ?: return "Неверный день"
        val mon = text.substring(3, 5).toIntOrNull() ?: return "Неверный месяц"
        if (mon !in 1..12) return "Месяц 01–12"
        val maxDay = when (mon) {
            1,3,5,7,8,10,12 -> 31
            4,6,9,11 -> 30
            else -> 29 // для февраля — допустим 29 без учёта года
        }
        if (day !in 1..maxDay) return "День 01–$maxDay"
        return null
    }
    fun validateWeight(text: String): String? {
        if (text.isBlank()) return "Введите вес"
        val normalized = text.replace(',', '.')
        val value = normalized.toFloatOrNull() ?: return "Неверный формат (пример: 72.4)"
        if (value < 30f || value > 300f) return "Диапазон 30–300 кг"
        return null
    }

    val onSaveWeight: () -> Unit = {
        val err = validateWeight(weightInput)
        weightError = err
        if (err == null) {
            val value = weightInput.replace(',', '.').toFloat()
            val pretty = todayPrettyShort()
            // upsert по дате dd.MM
            val arrOld = JSONArray(prefs.getString(K_WEIGHT_JSON, "[]") ?: "[]")
            val list = mutableListOf<JSONObject>()
            for (i in 0 until arrOld.length()) list += arrOld.getJSONObject(i)
            val updated = list.filterNot { it.optString("date").take(5) == pretty }.toMutableList()
            updated += JSONObject().apply {
                put("date", pretty)
                put("weight", value)
            }
            // сортировка по dd.MM (для графика не критично, но стабильнее)
            updated.sortBy {
                val d = it.optString("date").take(5)
                val day = d.substring(0, 2).toIntOrNull() ?: 0
                val mon = d.substring(3, 5).toIntOrNull() ?: 0
                mon * 31 + day
            }
            val arr = JSONArray()
            updated.takeLast(60).forEach { arr.put(it) }
            prefs.edit().putString(K_WEIGHT_JSON, arr.toString()).apply()

            weightHistory = updated.takeLast(60).map { it.optString("date").take(5) to it.getDouble("weight").toFloat() }
            weightInput = ""
            scope.launch { showSnack("Вес сохранён") }
        } else {
            scope.launch { showSnack(err!!) }
        }
    }

    /* ---------- Best exercises ---------- */
    val sessions by trainingViewModel.sessions.collectAsState()
    val bestExercises = sessions
        .flatMap { it.exercises }
        .groupBy { it.name }
        .mapValues { (_, list) -> list.maxByOrNull { it.weight * it.reps * max(1, it.sets) } ?: list.first() }
        .values
        .sortedByDescending { it.weight * it.reps * max(1, it.sets) }
        .take(bestExercisesLimit)

    /* ---------- Dialogs state ---------- */
    var showSettings by remember { mutableStateOf(false) }
    var showWeightEditor by remember { mutableStateOf(false) }

    /* ===================== UI ===================== */
    Scaffold(
        topBar = {
            SectionHeader(
                title = "Аналитика",
                titleStyle = MaterialTheme.typography.headlineSmall,
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Настройки аналитики")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StepsCardPretty(
                    steps = stepsToday,
                    goal = stepGoal,
                    hasPermission = hasStepPermission,
                    onRequest = { stepPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
                )
            }
            item { WeatherCardPretty(city = city, weather = weather, subtitle = weatherSubtitle) }
            item { NutritionTodayCardPretty(total = todayTotal, norm = nutritionViewModel.dailyNorm) }
            item {
                WeightInputCardPretty(
                    input = weightInput,
                    error = weightError,
                    onInputChange = {
                        weightInput = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                        weightError = null
                    },
                    onSave = onSaveWeight,
                    history = weightHistory,
                    onEditClick = { showWeightEditor = true }
                )
            }
            item { BestExercisesCardPretty(exercises = bestExercises) }
            item {
                if (notifyStepsEnabled && Build.VERSION.SDK_INT >= 33 && !hasNotifPermission) {
                    FilledTonalButton(onClick = { notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Включить уведомления")
                    }
                }
            }
        }
    }

    if (showSettings) {
        AnalyticsSettingsDialogPretty(
            currentCity = city,
            currentGoal = stepGoal.toString(),
            currentNotify = notifyStepsEnabled,
            bestLimit = bestExercisesLimit,
            onSave = { newCity, newGoal, notify, bestLimitNew ->
                val c = newCity.trim().ifBlank { "Москва" }
                val g = newGoal.toIntOrNull()?.coerceIn(1_000, 50_000) ?: 8_000
                val bl = bestLimitNew.coerceIn(1, 10)

                prefs.edit()
                    .putString(K_CITY, c)
                    .putInt(K_STEP_GOAL, g)
                    .putBoolean(K_NOTIFY_ENABLED, notify)
                    .putInt(K_BEST_EX_LIMIT, bl)
                    // СБРОС статуса уведомления, чтобы новая цель могла сработать снова
                    .remove(K_LAST_GOAL_NOTIFIED)
                    .putString(K_LAST_NOTIFY_DAY, todayKeyIso())
                    .apply()

                city = c
                stepGoal = g
                notifyStepsEnabled = notify
                bestExercisesLimit = bl

                scope.launch { fetchAndCacheWeather(c) }
                showSettings = false
                scope.launch { showSnack("Настройки сохранены") }
            },
            onRefreshWeather = { scope.launch { fetchAndCacheWeather(city) } },
            onDismiss = { showSettings = false }
        )
    }

    if (showWeightEditor) {
        EditWeightHistoryDialogPretty(
            initial = weightHistory,
            validateDate = ::validateDateShort,
            validateWeight = ::validateWeight,
            onSave = { updated ->
                // сортируем и сохраняем
                val sorted = updated.sortedBy {
                    val d = it.first
                    val day = d.substring(0, 2).toIntOrNull() ?: 0
                    val mon = d.substring(3, 5).toIntOrNull() ?: 0
                    mon * 31 + day
                }
                val arr = JSONArray()
                sorted.forEach { (pretty, w) ->
                    arr.put(JSONObject().apply {
                        put("date", pretty)
                        put("weight", w)
                    })
                }
                prefs.edit().putString(K_WEIGHT_JSON, arr.toString()).apply()
                weightHistory = sorted
                showWeightEditor = false
                scope.launch { showSnack("История веса обновлена") }
            },
            onDismiss = { showWeightEditor = false }
        )
    }

    LaunchedEffect(notifyStepsEnabled) {
        if (notifyStepsEnabled && Build.VERSION.SDK_INT >= 33 && !hasNotifPermission) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/* ===================== Step Counter Service ===================== */
class StepCounterService : Service() {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var prefs: SharedPreferences

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("analytics_prefs", MODE_PRIVATE)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        // Register listener if sensor available
        if (stepSensor != null) {
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Start foreground with notification
        ensureServiceChannel()
        val notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID_SERVICE)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle("Шагомер")
            .setContentText("Отслеживание шагов в фоне")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID_SERVICE, notification)

        // Schedule midnight reset
        MidnightResetReceiver.scheduleNext(this)
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(stepListener)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private val stepListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

            val raw = event.values[0].toLong()
            val now = System.currentTimeMillis()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))

            val lastRaw = prefs.getLong("steps_last_raw", -1L)
            val lastTs = prefs.getLong("steps_last_ts", -1L)
            val storedDate = prefs.getString("steps_today_date", today) ?: today
            var todaySteps = prefs.getLong("steps_today", 0L)

            // Handle first run or reboot (delta < 0)
            var delta = if (lastRaw >= 0L) raw - lastRaw else 0L
            if (lastRaw < 0L || lastTs < 0L || delta < 0L) {
                delta = raw  // Add the current raw since boot/reboot
            }

            // If day changed, reset todaySteps
            val effectiveDate: String
            if (storedDate != today) {
                todaySteps = 0L
                effectiveDate = today
            } else {
                effectiveDate = storedDate
            }

            // Add delta
            todaySteps = (todaySteps + delta).coerceAtLeast(0L)

            // Save
            prefs.edit()
                .putLong("steps_today", todaySteps)
                .putString("steps_today_date", effectiveDate)
                .putLong("steps_last_raw", raw)
                .putLong("steps_last_ts", now)
                .apply()

            // Broadcast update to UI
            LocalBroadcastManager.getInstance(this@StepCounterService).sendBroadcast(Intent(ACTION_STEPS_UPDATED))

            // Check and send goal notification
            sendGoalNotificationIfNeeded(this@StepCounterService, prefs, todaySteps)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun ensureServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID_SERVICE,
                "Отслеживание шагов",
                NotificationManager.IMPORTANCE_MIN
            ).apply { description = "Постоянное уведомление для фонового отслеживания шагов" }
            nm.createNotificationChannel(channel)
        }
    }
}

/* ===================== Midnight Reset Receiver ===================== */
class MidnightResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        prefs.edit()
            .putLong("steps_today", 0L)
            .putString("steps_today_date", today)
            .apply()

        // Schedule next
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
                add(Calendar.DAY_OF_MONTH, 1)  // Next midnight
            }
            val intent = Intent(context, MidnightResetReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pi)
            }
        }
    }
}

/* ===================== Boot Receiver ===================== */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("step_permission", false)) {
                val serviceIntent = Intent(context, StepCounterService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            MidnightResetReceiver.scheduleNext(context)
        }
    }
}

/* ===================== Notifications ===================== */
private fun sendGoalNotificationIfNeeded(context: Context, prefs: SharedPreferences, stepsToday: Long) {
    val notifyStepsEnabled = prefs.getBoolean("notify_steps_enabled", true)
    if (!notifyStepsEnabled) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
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
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round))
            .setContentTitle("Цель достигнута 🎉")
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

private fun ensureGoalChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIF_CHANNEL_ID_GOAL,
            "Достижение цели по шагам",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Уведомления при выполнении дневной цели шагов" }
        nm.createNotificationChannel(channel)
    }
}

/* ===================== Pretty helpers ===================== */

@Composable
private fun gradientPrimary(): Brush = Brush.linearGradient(
    listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    )
)
@Composable
private fun gradientSecondary(): Brush = Brush.linearGradient(
    listOf(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    )
)

/* ====== Толстая, скруглённая линейка прогресса ====== */
@Composable
fun FatLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
    cornerRadius: Dp = 10.dp
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val gradient = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
    )
    val rPx = with(LocalDensity.current) { cornerRadius.toPx() }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = trackColor,
            size = Size(w, h),
            cornerRadius = CornerRadius(rPx, rPx)
        )
        val pw = (w * progress.coerceIn(0f, 1f))
        if (pw > 0f) {
            drawRoundRect(
                brush = gradient,
                size = Size(pw, h),
                cornerRadius = CornerRadius(rPx, rPx)
            )
        }
    }
}

/* ===================== Cards ===================== */

@Composable
fun StepsCardPretty(
    steps: Long,
    goal: Int,
    hasPermission: Boolean,
    onRequest: () -> Unit
) {
    val progress = (steps.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .background(gradientPrimary(), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.width(12.dp))
                Text("Шаги сегодня", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if (!hasPermission) {
                    FilledTonalButton(onClick = onRequest, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("Разрешить")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("$steps / $goal", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            FatLinearProgress(progress = progress)
        }
    }
}

@Composable
fun WeatherCardPretty(city: String, weather: String, subtitle: String?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .background(gradientSecondary())
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Погода • $city", style = MaterialTheme.typography.titleMedium)
                Text(weather, style = MaterialTheme.typography.bodyMedium)
                if (subtitle != null)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/* ====== Равные круглые кольца КБЖУ ====== */
@Composable
fun NutritionTodayCardPretty(total: NutritionEntry, norm: Map<String, Int>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("КБЖУ сегодня", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RingMacro(
                    label = "Кал",
                    value = total.calories,
                    norm = norm["calories"] ?: 2000,
                    gradient = Brush.sweepGradient(listOf(Color(0xFFFF8A65), Color(0xFFFF7043), Color(0xFFFF8A65)))
                )
                RingMacro(
                    label = "Б",
                    value = total.protein,
                    norm = norm["protein"] ?: 100,
                    gradient = Brush.sweepGradient(listOf(Color(0xFF66BB6A), Color(0xFF2E7D32), Color(0xFF66BB6A)))
                )
                RingMacro(
                    label = "Ж",
                    value = total.fats,
                    norm = norm["fats"] ?: 70,
                    gradient = Brush.sweepGradient(listOf(Color(0xFFFFD54F), Color(0xFFF9A825), Color(0xFFFFD54F)))
                )
                RingMacro(
                    label = "У",
                    value = total.carbs,
                    norm = norm["carbs"] ?: 250,
                    gradient = Brush.sweepGradient(listOf(Color(0xFF64B5F6), Color(0xFF1565C0), Color(0xFF64B5F6)))
                )
            }
        }
    }
}

@Composable
fun RingMacro(label: String, value: Int, norm: Int, gradient: Brush) {
    val progress = (value.toFloat() / norm.coerceAtLeast(1)).coerceIn(0f, 1f)
    val pct = ((progress * 100f).coerceIn(0f, 100f)).roundToInt()

    val ringTrack = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val innerShade = MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)

    val ringSize = 72.dp
    val strokeWidthDp = 10.dp
    val strokePx = with(LocalDensity.current) { strokeWidthDp.toPx() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(ringSize) // фиксированный квадрат
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val d = min(size.width, size.height)
            val inset = strokePx / 2f
            val rect = Rect(
                left = (size.width - d) / 2f + inset,
                top = (size.height - d) / 2f + inset,
                right = (size.width + d) / 2f - inset,
                bottom = (size.height + d) / 2f - inset
            )
            val arcSize = Size(rect.width, rect.height)

            drawArc(
                color = ringTrack,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = Offset(rect.left, rect.top),
                size = arcSize
            )
            drawArc(
                brush = gradient,
                startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = Offset(rect.left, rect.top),
                size = arcSize
            )
            drawCircle(
                color = innerShade,
                radius = d / 2.6f,
                center = rect.center
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$pct%", style = MaterialTheme.typography.labelSmall)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun WeightInputCardPretty(
    input: String,
    error: String?,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit,
    history: List<Pair<String, Float>>,
    onEditClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Вес тела", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEditClick) { Text("Редактировать") }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text("кг") },
                    isError = error != null,
                    supportingText = { if (error != null) Text(error, color = MaterialTheme.colorScheme.error) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSave, enabled = input.isNotBlank(), modifier = Modifier.height(48.dp)) { Text("Сохранить") }
            }
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Вес, кг", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                WeightChartWithAxes(history) // подписи X делает сама функция (макс. 3)
            }
        }
    }
}

/* ====== График веса: максимум 3 подписи по X (начало/середина/конец) ====== */
@Composable
fun WeightChartWithAxes(data: List<Pair<String, Float>>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val axisColor = Color.Gray
    val gridColor = Color.LightGray
    val labelStyle = MaterialTheme.typography.labelSmall
    val paddingDp = 24.dp
    val density = LocalDensity.current

    val values = data.map { it.second }
    val maxVal = values.maxOrNull() ?: 0f
    val minVal = values.minOrNull() ?: 0f
    val displayMax = if (maxVal == minVal) maxVal + 1f else maxVal
    val displayMin = if (maxVal == minVal) minVal - 1f else minVal
    val range = kotlin.math.max(0.1f, displayMax - displayMin)
    val stepsY = 4

    // индексы X: максимум 3 (начало/середина/конец)
    val labelIndices: List<Int> = when {
        data.isEmpty() -> emptyList()
        data.size == 1 -> listOf(0)
        data.size == 2 -> listOf(0, 1)
        else -> listOf(0, data.size / 2, data.lastIndex)
    }
    val xLabels = labelIndices.map { data[it].first }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val paddingPx = with(density) { paddingDp.toPx() }
            val xLabelPadPx = with(density) { 8.dp.toPx() }

            // paint для Y-меток — РИСУЕМ ИХ В CANVAS (точное совпадение по Y)
            val yPaint = remember {
                android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            }.also {
                it.textSize = with(density) { 10.sp.toPx() }
                it.color = android.graphics.Color.GRAY
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // оставляем место слева под подписи Y
                val maxYLabel = listOf(displayMin, displayMax).maxBy { "%.1f".format(it).length }
                val approxYTextWidth = yPaint.measureText("%.1f".format(maxYLabel))
                val paddingLeft = kotlin.math.max(approxYTextWidth + xLabelPadPx, paddingPx * 0.8f)
                val paddingRight = paddingPx.coerceAtMost(width * 0.05f)
                val paddingTop = paddingPx.coerceAtMost(height * 0.1f)
                val paddingBottom = (paddingPx * 1.2f).coerceAtMost(height * 0.22f)

                val graphWidth = (width - paddingLeft - paddingRight).coerceAtLeast(1f)
                val graphHeight = (height - paddingTop - paddingBottom).coerceAtLeast(1f)

                // Оси
                drawLine(axisColor, Offset(paddingLeft, paddingTop), Offset(paddingLeft, height - paddingBottom), 2f)
                drawLine(axisColor, Offset(paddingLeft, height - paddingBottom), Offset(width - paddingRight, height - paddingBottom), 2f)

                // Горизонтальная сетка + МЕТКИ Y (в Canvas, по тем же Y)
                for (i in 0..stepsY) {
                    val fy = i / stepsY.toFloat()
                    val y = paddingTop + graphHeight * (1f - fy)
                    drawLine(gridColor, Offset(paddingLeft, y), Offset(width - paddingRight, y), 1f)

                    // текст Y
                    val value = displayMin + range * fy
                    val label = "%.1f".format(value)
                    val fm = yPaint.fontMetrics
                    val baseline = y - (fm.ascent + fm.descent) / 2f
                    drawContext.canvas.nativeCanvas.drawText(label, paddingLeft - xLabelPadPx, baseline, yPaint)
                }

                // График
                if (data.isNotEmpty()) {
                    if (data.size == 1) {
                        val x = paddingLeft + graphWidth / 2f
                        val y = paddingTop + graphHeight * (1f - (data[0].second - displayMin) / range)
                        drawCircle(primaryColor, 6f, Offset(x, y))
                    } else {
                        val stepX = graphWidth / (data.size - 1)
                        var prevX = 0f
                        var prevY = 0f
                        data.forEachIndexed { i, (_, w) ->
                            val x = paddingLeft + i * stepX
                            val y = paddingTop + graphHeight * (1f - (w - displayMin) / range)
                            if (i > 0) drawLine(primaryColor, Offset(prevX, prevY), Offset(x, y), 3f)
                            drawCircle(primaryColor, 4f, Offset(x, y))
                            prevX = x
                            prevY = y
                        }

                        // риски под выбранные подписи X
                        labelIndices.forEach { idx ->
                            val x = paddingLeft + idx * stepX
                            drawLine(
                                color = gridColor,
                                start = Offset(x, height - paddingBottom),
                                end = Offset(x, height - paddingBottom + 6f),
                                strokeWidth = 1f
                            )
                        }
                    }
                }
            }
        }

        // Подписи X (1–3 шт), в одну строку
        if (xLabels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp), // Y-метки уже в Canvas, поэтому маленький отступ
                horizontalArrangement = when (xLabels.size) {
                    1 -> Arrangement.Center
                    2 -> Arrangement.SpaceBetween
                    else -> Arrangement.SpaceBetween
                }
            ) {
                xLabels.forEach { lbl ->
                    Text(lbl, style = labelStyle, maxLines = 1)
                }
            }
        }
    }
}



@Composable
fun BestExercisesCardPretty(exercises: Collection<ExerciseEntry>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Лучшие результаты", style = MaterialTheme.typography.titleMedium)
            if (exercises.isEmpty()) {
                Text("Нет данных", style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exercises.forEach { ex ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(ex.name, style = MaterialTheme.typography.titleSmall)
                                Text("${ex.weight} кг × ${ex.reps} × ${max(1, ex.sets)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ===================== Settings (компактные и стабильные) ===================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsSettingsDialogPretty(
    currentCity: String,
    currentGoal: String,
    currentNotify: Boolean,
    bestLimit: Int,
    onSave: (newCity: String, newGoal: String, notify: Boolean, bestExercisesLimit: Int) -> Unit,
    onRefreshWeather: () -> Unit,
    onDismiss: () -> Unit
) {
    var city by remember { mutableStateOf(currentCity) }
    var goal by remember { mutableStateOf(currentGoal) }
    var notifyEnabled by remember { mutableStateOf(currentNotify) }
    var bestLimitState by remember { mutableStateOf(bestLimit.coerceIn(1, 10)) }

    var goalError by remember { mutableStateOf<String?>(null) }
    fun validateGoal(s: String): String? {
        if (s.isBlank()) return "Укажите цель по шагам"
        val v = s.toIntOrNull() ?: return "Только целое число"
        if (v !in 1000..50000) return "Диапазон 1 000–50 000"
        return null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Tune, contentDescription = null) },
        title = { Text("Настройки аналитики") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Баннер
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Настройте город, шаги и уведомления.")
                    }
                }

                OutlinedTextField(
                    value = goal,
                    onValueChange = {
                        goal = it.filter { ch -> ch.isDigit() }
                        goalError = null
                    },
                    label = { Text("Цель по шагам (шт.)") },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                    singleLine = true,
                    isError = goalError != null,
                    supportingText = { if (goalError != null) Text(goalError!!, color = MaterialTheme.colorScheme.error) }
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Город для погоды") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                    singleLine = true
                )

                // Переключатель уведомлений
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (notifyEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (notifyEnabled) "Уведомления: вкл." else "Уведомления: выкл.")
                    }
                    Switch(checked = notifyEnabled, onCheckedChange = { notifyEnabled = it })
                }

                // Заголовок блока
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Показывать топ упражнений")
                }
                // Отдельной строкой:  −  число  +
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedIconButton(
                        onClick = { bestLimitState = max(1, bestLimitState - 1) },
                        enabled = bestLimitState > 1
                    ) { Icon(Icons.Default.Remove, contentDescription = "Уменьшить") }

                    Text(
                        "$bestLimitState",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    OutlinedIconButton(
                        onClick = { bestLimitState = min(10, bestLimitState + 1) },
                        enabled = bestLimitState < 10
                    ) { Icon(Icons.Default.Add, contentDescription = "Увеличить") }
                }

                FilledTonalButton(onClick = onRefreshWeather) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Обновить погоду сейчас")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val err = validateGoal(goal)
                goalError = err
                if (err == null) onSave(city, goal, notifyEnabled, bestLimitState)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}



/* ===================== Weight Editor (валидация dd.MM + компакт) ===================== */

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWeightHistoryDialogPretty(
    initial: List<Pair<String, Float>>,
    validateDate: (String) -> String?,
    validateWeight: (String) -> String?,
    onSave: (List<Pair<String, Float>>) -> Unit,
    onDismiss: () -> Unit
) {
    class RowItem(
        val id: String = UUID.randomUUID().toString(),
        date: String,
        weight: String
    ) {
        var date by mutableStateOf(date)
        var weight by mutableStateOf(weight)
    }

    val rows = remember {
        mutableStateListOf<RowItem>().apply {
            initial.forEach { add(RowItem(date = it.first.trim(), weight = it.second.toString())) }
        }
    }

    fun hasDuplicateDates(): Boolean {
        val set = HashSet<String>()
        rows.forEach { if (!set.add(it.date.trim())) return true }
        return false
    }

    val canSave by derivedStateOf {
        rows.isNotEmpty()
                && rows.all { validateDate(it.date) == null && validateWeight(it.weight) == null }
                && !hasDuplicateDates()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.EditCalendar, contentDescription = null) },
        title = { Text("Редактирование веса") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            ),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TipsAndUpdates, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Дата — ДД.ММ (корректные значения). Вес — 30–300 кг. Без повторов дат.")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(onClick = {
                        val todayShort = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())
                        rows.add(RowItem(date = todayShort, weight = ""))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Добавить")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 380.dp)
                ) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rows, key = { it.id }) { item ->
                            val errDate = validateDate(item.date)
                            val errWeight = validateWeight(item.weight)
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = item.date,
                                        onValueChange = { v ->
                                            val filtered = v.filter { ch -> ch.isDigit() || ch == '.' }
                                            item.date = filtered.take(5)
                                        },
                                        label = { Text("Дата (ДД.ММ)") },
                                        singleLine = true,
                                        isError = errDate != null,
                                        supportingText = { if (errDate != null) Text(errDate) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = item.weight,
                                        onValueChange = { v -> item.weight = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                                        label = { Text("Вес (кг)") },
                                        singleLine = true,
                                        isError = errWeight != null,
                                        supportingText = { if (errWeight != null) Text(errWeight) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { rows.removeAll { it.id == item.id } },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Icon(Icons.Default.Delete, contentDescription = "Удалить") }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = hasDuplicateDates(), enter = expandVertically(), exit = shrinkVertically()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text("В списке есть повторяющиеся даты", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = rows.map { it.date.trim() to it.weight.trim().replace(',', '.').toFloat() }
                    onSave(result)
                },
                enabled = canSave
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
