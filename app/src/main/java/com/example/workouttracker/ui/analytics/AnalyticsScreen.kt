package com.example.workouttracker.ui.analytics

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.R
import com.example.workouttracker.ui.components.SectionHeader
import com.example.workouttracker.ui.nutrition.NutritionEntry
import com.example.workouttracker.ui.training.ExerciseEntry
import com.example.workouttracker.viewmodel.NutritionViewModel
import com.example.workouttracker.viewmodel.TrainingViewModel
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

// === Weather DTOs & API ===
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    trainingViewModel: TrainingViewModel = viewModel(),
    nutritionViewModel: NutritionViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs: SharedPreferences = context.getSharedPreferences("analytics_prefs", Context.MODE_PRIVATE)

    // ===== Pref keys
    val K_DAY_KEY = "steps_day_key"
    val K_COUNTER_BASELINE = "steps_counter_base"
    val K_COUNTER_LAST_SEEN = "steps_counter_last"
    val K_STEPS_TODAY = "steps_today"
    val K_WEATHER_JSON = "weather_cache_json"
    val K_WEATHER_TIME = "weather_cache_time"
    val K_CITY = "weather_city"
    val K_STEP_GOAL = "step_goal"

    // ===== Permission for steps
    var hasStepPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACTIVITY_RECOGNITION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        hasStepPermission = granted
        prefs.edit().putBoolean("step_permission", granted).apply()
        Unit
    }

    fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // ===== Step goal
    var stepGoal by remember { mutableStateOf(prefs.getInt(K_STEP_GOAL, 8000)) }

    // ===== STEPS (TYPE_STEP_COUNTER)
    var stepsToday by remember { mutableStateOf(prefs.getLong(K_STEPS_TODAY, 0L)) }
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val stepListener = remember(prefs) {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
                val currentCounter = event.values[0].toLong()
                val today = todayKey()
                val savedDay = prefs.getString(K_DAY_KEY, null)
                val lastSeenCounter = prefs.getLong(K_COUNTER_LAST_SEEN, -1L)
                var base = prefs.getLong(K_COUNTER_BASELINE, -1L)

                if (savedDay == null || savedDay != today) {
                    val newBase = if (lastSeenCounter >= 0L) lastSeenCounter else currentCounter
                    prefs.edit().putString(K_DAY_KEY, today).putLong(K_COUNTER_BASELINE, newBase).apply()
                    base = newBase
                }

                if (base < 0L) {
                    prefs.edit().putLong(K_COUNTER_BASELINE, currentCounter).apply()
                    stepsToday = 0L
                } else {
                    val calc = (currentCounter - base).coerceAtLeast(0L)
                    stepsToday = calc
                    prefs.edit().putLong(K_STEPS_TODAY, calc).apply()
                }
                prefs.edit().putLong(K_COUNTER_LAST_SEEN, currentCounter).apply()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
    }

    LaunchedEffect(hasStepPermission, stepSensor) {
        if (hasStepPermission && stepSensor != null) {
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }
    DisposableEffect(Unit) {
        onDispose { sensorManager.unregisterListener(stepListener); Unit }
    }

    // ===== WEATHER with cache + city
    var city by remember { mutableStateOf(prefs.getString(K_CITY, "Москва") ?: "Москва") }
    var weather by remember { mutableStateOf("Загрузка...") }
    var weatherSubtitle by remember { mutableStateOf<String?>(null) }

    fun setWeatherFromCache(): Boolean {
        val cached = prefs.getString(K_WEATHER_JSON, null) ?: return false
        val time = prefs.getLong(K_WEATHER_TIME, 0L)
        return try {
            val obj = JSONObject(cached)
            val temp = obj.getDouble("temp").roundToInt()
            val desc = obj.getString("desc")
            val ts = if (time > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time)) else "-"
            weather = "$desc, $temp°C"
            weatherSubtitle = "Обновлено $ts"
            true
        } catch (_: Exception) { false }
    }

    suspend fun fetchAndCacheWeather(currentCity: String) {
        try {
            val apiKey = context.getString(R.string.openweather_api_key)
            if (apiKey.isBlank()) {
                weather = "API key не настроен"
                weatherSubtitle = null
                return
            }
            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/data/2.5/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val api = retrofit.create(WeatherApi::class.java)

            val response = api.getCurrentWeatherByCity(currentCity, apiKey)
            val temp = response.main.temp.roundToInt()
            val desc = response.weather.getOrNull(0)?.description?.replaceFirstChar { it.uppercase() } ?: "-"

            weather = "$desc, $temp°C"
            weatherSubtitle = "Обновлено " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

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
            }
        }
    }

    LaunchedEffect(city) {
        val hadCache = setWeatherFromCache()
        if (!hadCache) weather = "Загрузка..."
        scope.launch { fetchAndCacheWeather(city) }
    }

    // ===== NUTRITION today
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayEntries by nutritionViewModel.entries.collectAsState()
    val todayNutrition = todayEntries.filter { it.date == today }
    val total = todayNutrition.fold(
        NutritionEntry(
            id = UUID.randomUUID(),
            date = today,
            name = "",
            calories = 0,
            protein = 0,
            carbs = 0,
            fats = 0,
            weight = 0
        )
    ) { acc, e ->
        acc.copy(
            calories = acc.calories + e.calories,
            protein = acc.protein + e.protein,
            carbs = acc.carbs + e.carbs,
            fats = acc.fats + e.fats
        )
    }

    // ===== WEIGHT HISTORY + validation + editing
    var weightInput by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf<String?>(null) }

    fun loadWeightHistory(): List<Pair<String, Float>> {
        val json = prefs.getString("weight_history", "[]") ?: "[]"
        return try {
            val list = mutableListOf<Pair<String, Float>>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(obj.getString("date") to obj.getDouble("weight").toFloat())
            }
            list
        } catch (_: Exception) { emptyList() }
    }
    var weightHistory by remember { mutableStateOf(loadWeightHistory()) }

    fun validateWeight(text: String): String? {
        if (text.isBlank()) return "Введите вес"
        val normalized = text.replace(',', '.')
        val value = normalized.toFloatOrNull() ?: return "Неверный формат (пример: 72.4)"
        if (value < 30f || value > 300f) return "Диапазон 30–300 кг"
        return null
    }

    val onSaveWeight = {
        val err = validateWeight(weightInput)
        weightError = err
        if (err == null) {
            val value = weightInput.replace(',', '.').toFloat()
            val date = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())
            val newHistory = (weightHistory + (date to value)).takeLast(30)
            val array = JSONArray()
            newHistory.forEach { (d, w) ->
                array.put(JSONObject().apply { put("date", d); put("weight", w) })
            }
            prefs.edit().putString("weight_history", array.toString()).apply()
            weightHistory = newHistory
            weightInput = ""
        }
        Unit
    }

    // ===== BEST EXERCISES
    val sessions by trainingViewModel.sessions.collectAsState()
    val bestExercises = sessions
        .flatMap { it.exercises }
        .groupBy { it.name }
        .mapValues { (_, exercises) -> exercises.maxByOrNull { it.weight * it.reps } ?: exercises.first() }
        .values
        .sortedByDescending { it.weight * it.reps }
        .take(5)

    // ===== Settings + Weight editor dialogs
    var showSettings by remember { mutableStateOf(false) }
    var showWeightEditor by remember { mutableStateOf(false) }

    // ===== UI =====
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StepsCard(
                    steps = stepsToday,
                    goal = stepGoal,
                    hasPermission = hasStepPermission,
                    onRequest = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
                )
            }
            item { WeatherCard(weather = weather, subtitle = weatherSubtitle, city = city) }
            item { NutritionTodayCard(total = total, norm = nutritionViewModel.dailyNorm) }
            item {
                WeightInputCard(
                    input = weightInput,
                    error = weightError,
                    onInputChange = { text ->
                        weightInput = text.filter { it.isDigit() || it == '.' || it == ',' }
                        weightError = null
                    },
                    onSave = onSaveWeight,
                    history = weightHistory,
                    onEditClick = { showWeightEditor = true }
                )
            }
            item { BestExercisesCard(exercises = bestExercises) }
        }
    }

    if (showSettings) {
        AnalyticsSettingsDialog(
            currentCity = city,
            currentGoal = stepGoal.toString(),
            onSave = { newCity, newGoal ->
                val cityTrim = newCity.trim().ifBlank { "Москва" }
                val goalInt = newGoal.toIntOrNull()?.coerceIn(1000, 50000) ?: 8000
                prefs.edit().putString(K_CITY, cityTrim).putInt(K_STEP_GOAL, goalInt).apply()
                city = cityTrim
                stepGoal = goalInt
                scope.launch { fetchAndCacheWeather(cityTrim) }
                showSettings = false
            },
            onRefreshWeather = { scope.launch { fetchAndCacheWeather(city) } },
            onDismiss = { showSettings = false }
        )
    }

    if (showWeightEditor) {
        EditWeightHistoryDialog(
            initial = weightHistory,
            onSave = { updated ->
                val array = JSONArray()
                updated.forEach { (d, w) ->
                    array.put(JSONObject().apply { put("date", d); put("weight", w) })
                }
                prefs.edit().putString("weight_history", array.toString()).apply()
                weightHistory = updated
                showWeightEditor = false
            },
            onDismiss = { showWeightEditor = false }
        )
    }
}

// ===== components =====

@Composable
fun StepsCard(steps: Long, goal: Int, hasPermission: Boolean, onRequest: () -> Unit) {
    val progress = (steps.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DirectionsWalk, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Шаги сегодня", style = MaterialTheme.typography.titleMedium)
                Text("$steps / $goal", style = MaterialTheme.typography.headlineSmall)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            if (!hasPermission) {
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = onRequest) { Text("Разрешить") }
            }
        }
    }
}

@Composable
fun WeatherCard(weather: String, subtitle: String?, city: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WbSunny, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Погода • $city", style = MaterialTheme.typography.titleMedium)
                Text(weather, style = MaterialTheme.typography.bodyMedium) // ← фикс опечатки
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun NutritionTodayCard(total: NutritionEntry, norm: Map<String, Int>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("КБЖУ сегодня", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                RingMacro("Кал", total.calories, norm["calories"] ?: 2000, Color.Red)
                RingMacro("Б", total.protein, norm["protein"] ?: 100, Color(0xFF2E7D32))
                RingMacro("Ж", total.fats, norm["fats"] ?: 70, Color(0xFFF9A825))
                RingMacro("У", total.carbs, norm["carbs"] ?: 250, Color(0xFF1565C0))
            }
        }
    }
}

@Composable
fun RingMacro(label: String, value: Int, norm: Int, color: Color) {
    val progress = (value.toFloat() / norm).coerceIn(0f, 1f)
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = color.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(12f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun WeightInputCard(
    input: String,
    error: String?,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit,
    history: List<Pair<String, Float>>,
    onEditClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSave, enabled = input.isNotBlank()) { Text("Сохранить") }
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
                WeightChartWithAxes(history)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    history.forEach { (date, _) ->
                        Text(text = date, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

/** Настройки: цель по шагам, город, обновление погоды. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsSettingsDialog(
    currentCity: String,
    currentGoal: String,
    onSave: (newCity: String, newGoal: String) -> Unit,
    onRefreshWeather: () -> Unit,
    onDismiss: () -> Unit
) {
    var city by remember { mutableStateOf(currentCity) }
    var goal by remember { mutableStateOf(currentGoal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Tune, contentDescription = null) },
        title = { Text("Настройки аналитики") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Цель по шагам (шт.)") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) }
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Город для погоды") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) }
                )
                OutlinedButton(onClick = onRefreshWeather) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Обновить погоду сейчас")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(city, goal) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

/** Редактор истории веса c устойчивыми ключами и удалением по id. */
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWeightHistoryDialog(
    initial: List<Pair<String, Float>>,
    onSave: (List<Pair<String, Float>>) -> Unit,
    onDismiss: () -> Unit
) {
    // State-holder для строки с устойчивым ключом
    class RowItem(
        val id: String = UUID.randomUUID().toString(),
        date: String,
        weight: String
    ) {
        var date by mutableStateOf(date)
        var weight by mutableStateOf(weight)
    }

    // Локальный snapshotStateList — любые изменения полей RowItem перерисуют UI
    val rows = remember {
        mutableStateListOf<RowItem>().apply {
            initial.forEach { add(RowItem(date = it.first.trim(), weight = it.second.toString())) }
        }
    }

    val dateRegex = Regex("""\d{2}\.\d{2}""")
    fun validateRow(date: String, weightStr: String): String? {
        val d = date.trim()
        if (!dateRegex.matches(d)) return "Дата в формате ДД.ММ"
        val normalized = weightStr.trim().replace(',', '.')
        val v = normalized.toFloatOrNull() ?: return "Формат веса (пример: 72.4)"
        if (v < 30f || v > 300f) return "Диапазон 30–300"
        return null
    }

    val canSave by derivedStateOf {
        rows.all { validateRow(it.date, it.weight) == null }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.EditCalendar, contentDescription = null) },
        title = { Text("Редактирование веса") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Измените дату и/или вес. Можно удалять строки.")
                Spacer(Modifier.height(4.dp))

                rows.forEach { item ->
                    val err = validateRow(item.date, item.weight)

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = item.date,
                            onValueChange = { v ->
                                item.date = v.filter { ch -> ch.isDigit() || ch == '.' }.take(5)
                            },
                            label = { Text("Дата (ДД.ММ)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = item.weight,
                            onValueChange = { v ->
                                item.weight = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                            },
                            label = { Text("Вес (кг)") },
                            singleLine = true,
                            isError = err != null,
                            supportingText = { if (err != null) Text(err) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { rows.removeAll { it.id == item.id } },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Icon(Icons.Default.Delete, contentDescription = "Удалить") }
                    }
                }

                if (rows.isEmpty()) {
                    Text("Нет записей", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = rows.map { it.date.trim() to it.weight.trim().replace(',', '.').toFloat() }
                    onSave(result)
                },
                enabled = canSave // теперь корректно считается и разблокируется
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}


/**
 * Диаграмма без nativeCanvas — в Canvas не вызываем @Composable API.
 */
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
    val range = max(0.1f, displayMax - displayMin)
    val stepsY = 4

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Column(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in stepsY downTo 0) {
                val fy = i / stepsY.toFloat()
                val value = displayMin + range * fy
                Text(
                    text = "%.1f".format(value),
                    style = labelStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp)
                )
            }
        }

        Box(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
            val paddingPx = with(density) { paddingDp.toPx() }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingLeft = paddingPx.coerceAtMost(width * 0.2f)
                val paddingRight = paddingPx.coerceAtMost(width * 0.05f)
                val paddingTop = paddingPx.coerceAtMost(height * 0.1f)
                val paddingBottom = paddingPx.coerceAtMost(height * 0.12f)
                val graphWidth = (width - paddingLeft - paddingRight).coerceAtLeast(1f)
                val graphHeight = (height - paddingTop - paddingBottom).coerceAtLeast(1f)

                drawLine(axisColor, Offset(paddingLeft, paddingTop), Offset(paddingLeft, height - paddingBottom), 2f)
                drawLine(axisColor, Offset(paddingLeft, height - paddingBottom), Offset(width - paddingRight, height - paddingBottom), 2f)

                for (i in 0..stepsY) {
                    val fy = i / stepsY.toFloat()
                    val y = paddingTop + graphHeight * (1f - fy)
                    drawLine(gridColor, Offset(paddingLeft, y), Offset(width - paddingRight, y), 1f)
                }

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
                    }
                }
            }
        }
    }
}

@Composable
fun BestExercisesCard(exercises: Collection<ExerciseEntry>) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                                Text("${ex.weight} кг × ${ex.reps} × ${ex.sets}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
