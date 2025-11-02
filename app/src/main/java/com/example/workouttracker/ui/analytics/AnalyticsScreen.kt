package com.example.workouttracker.ui.analytics

import android.Manifest
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
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
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

    // permission state
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
        // explicitly return Unit to avoid nullable Unit inference
        Unit
    }

    // --- STEPS ---
    var stepsToday by remember { mutableStateOf(prefs.getLong("steps_today", 0L)) }
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val stepListener = remember(prefs) {
        object : SensorEventListener {
            private var initialStepsCache: Long
                get() = prefs.getLong("initial_steps", -1L)
                set(value) {
                    prefs.edit().putLong("initial_steps", value).apply()
                }

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    val currentSteps = event.values[0].toLong()
                    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val lastSavedDay = prefs.getString("initial_steps_day", null)

                    if (lastSavedDay == null || lastSavedDay != todayKey) {
                        initialStepsCache = currentSteps
                        prefs.edit().putString("initial_steps_day", todayKey).apply()
                    }

                    if (initialStepsCache == -1L) {
                        initialStepsCache = currentSteps
                    }

                    val calculated = (currentSteps - initialStepsCache).coerceAtLeast(0L)
                    stepsToday = calculated
                    prefs.edit().putLong("steps_today", stepsToday).apply()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // no-op
            }
        }
    }

    LaunchedEffect(hasStepPermission, stepSensor) {
        if (hasStepPermission && stepSensor != null) {
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // unregister listener — explicit Unit
            sensorManager.unregisterListener(stepListener)
            Unit
        }
    }

    // === WEATHER ===
    var weather by remember { mutableStateOf("Загрузка...") }
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val apiKey = context.getString(R.string.openweather_api_key)
                if (apiKey.isBlank()) {
                    weather = "API key не настроен"
                    return@launch
                }
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://api.openweathermap.org/data/2.5/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                val api = retrofit.create(WeatherApi::class.java)
                val response = api.getCurrentWeather(47.2357, 39.7122, apiKey)
                val temp = response.main.temp.roundToInt()
                val desc = response.weather.getOrNull(0)?.description?.replaceFirstChar { it.uppercase() } ?: "-"
                weather = "$desc, $temp°C"
            } catch (e: Exception) {
                weather = "Нет сети / ошибка"
            }
        }
    }

    // === NUTRITION ===
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
            fats = 0
        )
    ) { acc, e ->
        acc.copy(
            calories = acc.calories + e.calories,
            protein = acc.protein + e.protein,
            carbs = acc.carbs + e.carbs,
            fats = acc.fats + e.fats
        )
    }

    // === WEIGHT HISTORY ===
    var weightInput by remember { mutableStateOf("") }

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
        } catch (e: Exception) {
            emptyList()
        }
    }

    var weightHistory by remember { mutableStateOf(loadWeightHistory()) }

    val onSaveWeight = {
        weightInput.toFloatOrNull()?.let { w ->
            val date = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())
            val newHistory = (weightHistory + (date to w)).takeLast(30)
            val array = JSONArray()
            newHistory.forEach { (d, weight) ->
                array.put(JSONObject().apply {
                    put("date", d)
                    put("weight", weight)
                })
            }
            prefs.edit().putString("weight_history", array.toString()).apply()
            weightHistory = newHistory
            weightInput = ""
        }
        Unit
    }

    // === BEST EXERCISES ===
    val sessions by trainingViewModel.sessions.collectAsState()
    val bestExercises = sessions
        .flatMap { it.exercises }
        .groupBy { it.name }
        .mapValues { (_, exercises) ->
            exercises.maxByOrNull { it.weight * it.reps } ?: exercises.first()
        }
        .values
        .sortedByDescending { it.weight * it.reps }
        .take(5)

    // === UI ===
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StepsCard(
                steps = stepsToday,
                hasPermission = hasStepPermission,
                onRequest = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
            )
        }
        item { WeatherCard(weather = weather) }
        item { NutritionTodayCard(total = total, norm = nutritionViewModel.dailyNorm) }
        item {
            WeightInputCard(
                input = weightInput,
                onInputChange = { weightInput = it },
                onSave = onSaveWeight,
                history = weightHistory
            )
        }
        item { BestExercisesCard(exercises = bestExercises) }
    }
}

// ===== components =====

@Composable
fun StepsCard(steps: Long, hasPermission: Boolean, onRequest: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DirectionsWalk, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Шаги сегодня", style = MaterialTheme.typography.titleMedium)
                Text("$steps", style = MaterialTheme.typography.headlineMedium)
            }
            Spacer(Modifier.weight(1f))
            if (!hasPermission) {
                TextButton(onClick = onRequest) { Text("Разрешить") }
            }
        }
    }
}

@Composable
fun WeatherCard(weather: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WbSunny, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Погода", style = MaterialTheme.typography.titleMedium)
                Text(weather, style = MaterialTheme.typography.bodyMedium)
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
                RingMacro("Б", total.protein, norm["protein"] ?: 100, Color.Green)
                RingMacro("Ж", total.fats, norm["fats"] ?: 70, Color.Yellow)
                RingMacro("У", total.carbs, norm["carbs"] ?: 250, Color.Blue)
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
fun WeightInputCard(input: String, onInputChange: (String) -> Unit, onSave: () -> Unit, history: List<Pair<String, Float>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Вес тела", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text("кг") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = onSave) { Text("Сохранить") }
            }
            if (history.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text("Вес, кг", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                // chart with labels at left (composables, not nativeCanvas)
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

/**
 * Chart implementation WITHOUT nativeCanvas.
 * Left column shows Y labels (as Text composables), right column contains pure Canvas (axis, grid, line, points).
 */
@Composable
fun WeightChartWithAxes(data: List<Pair<String, Float>>) {
    // hoist theme-dependent values out of the Canvas lambda
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
        // left Y labels column (composables — allowed)
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

        // right: canvas for axis/line/points — inside Canvas we DON'T call MaterialTheme.*
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
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

                // axes
                drawLine(axisColor, Offset(paddingLeft, paddingTop), Offset(paddingLeft, height - paddingBottom), 2f)
                drawLine(axisColor, Offset(paddingLeft, height - paddingBottom), Offset(width - paddingRight, height - paddingBottom), 2f)

                // horizontal grid lines
                for (i in 0..stepsY) {
                    val fy = i / stepsY.toFloat()
                    val y = paddingTop + graphHeight * (1f - fy)
                    drawLine(gridColor, Offset(paddingLeft, y), Offset(width - paddingRight, y), 1f)
                }

                // plot
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
                            if (i > 0) {
                                drawLine(primaryColor, Offset(prevX, prevY), Offset(x, y), 3f)
                            }
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
