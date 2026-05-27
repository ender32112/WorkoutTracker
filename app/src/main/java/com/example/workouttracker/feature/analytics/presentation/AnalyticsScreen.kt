package com.example.workouttracker.feature.analytics.presentation

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workouttracker.data.local.StepHistoryRecord
import com.example.workouttracker.data.settings.AppSettings
import com.example.workouttracker.data.settings.AppSettingsDataStore
import com.example.workouttracker.feature.analytics.runtime.ACTION_STEPS_UPDATED
import com.example.workouttracker.feature.analytics.runtime.PREF_HEALTH_CONNECT_ACTIVE
import com.example.workouttracker.feature.analytics.runtime.PREF_STEP_DATA_SOURCE
import com.example.workouttracker.feature.analytics.runtime.STEP_SOURCE_DEVICE_SENSOR
import com.example.workouttracker.feature.analytics.runtime.StepCounterService
import com.example.workouttracker.feature.analytics.runtime.StepDataSource
import com.example.workouttracker.feature.analytics.runtime.StepHistoryEntry
import com.example.workouttracker.feature.analytics.runtime.awaitHighAccuracyLocation
import com.example.workouttracker.feature.analytics.runtime.awaitLastLocationFallback
import com.example.workouttracker.feature.analytics.runtime.hasLocationPermission
import com.example.workouttracker.feature.analytics.runtime.refreshStepProgressNotification
import com.example.workouttracker.feature.analytics.runtime.resolveCityName
import com.example.workouttracker.feature.analytics.runtime.startStepCounterServiceSafely
import com.example.workouttracker.feature.analytics.runtime.todayIso
import com.example.workouttracker.feature.analytics.runtime.userAnalyticsPrefs
import com.example.workouttracker.feature.nutrition.presentation.NutritionViewModel
import com.example.workouttracker.feature.training.presentation.TrainingViewModel
import com.example.workouttracker.health.HealthConnectAvailability
import com.example.workouttracker.health.HealthConnectGateway
import com.example.workouttracker.workers.StepServiceWatchdogWorker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsRoute(
    trainingViewModel: TrainingViewModel = hiltViewModel(),
    nutritionViewModel: NutritionViewModel = hiltViewModel(),
    analyticsViewModel: AnalyticsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember(context) { userAnalyticsPrefs(context) }
    val appSettingsStore = remember(context) { AppSettingsDataStore(context) }
    val appSettings by appSettingsStore.settingsFlow.collectAsState(
        initial = AppSettings(
            themeVariant = "DARK",
            notificationsEnabled = true,
            stepGoal = 8000,
            exercisesLoaded = false,
            exercisesCatalogVersion = 0
        )
    )

    val healthConnect = remember(context) { HealthConnectGateway(context.applicationContext) }
    val healthAvailability = remember { healthConnect.availability() }
    val fusedLocationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    val persistedWeightHistory by analyticsViewModel.weightHistory.collectAsState(emptyList())
    val persistedStepHistory by analyticsViewModel.stepHistory.collectAsState(emptyList())
    val entries by nutritionViewModel.entries.collectAsState(emptyList())

    var hasStepPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    var hasHealthPermissions by remember { mutableStateOf(false) }
    var detectingCity by remember { mutableStateOf(false) }

    var city by rememberSaveable {
        mutableStateOf(prefs.getString(KEY_CITY, "Москва") ?: "Москва")
    }
    var stepGoal by rememberSaveable {
        mutableStateOf(prefs.getInt(KEY_STEP_GOAL, appSettings.stepGoal))
    }
    var notifyStepsEnabled by rememberSaveable {
        mutableStateOf(prefs.getBoolean(KEY_NOTIFY_ENABLED, appSettings.notificationsEnabled))
    }
    var stepDataSource by rememberSaveable {
        mutableStateOf(
            StepDataSource.fromStored(
                prefs.getString(PREF_STEP_DATA_SOURCE, STEP_SOURCE_DEVICE_SENSOR)
            )
        )
    }
    var stepsToday by remember {
        mutableStateOf(prefs.getLong(KEY_STEPS_TODAY, 0L))
    }
    var dialogState by remember { mutableStateOf(AnalyticsDialogState()) }
    var draftState by remember { mutableStateOf(AnalyticsDraftState()) }

    val stepPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStepPermission = granted
        prefs.edit().putBoolean("step_permission", granted).apply()
        if (granted && stepDataSource == StepDataSource.DEVICE_SENSOR) {
            startStepCounterServiceSafely(context, reason = "permission_granted")
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
    }

    val healthPermissionLauncher =
        if (healthAvailability == HealthConnectAvailability.AVAILABLE) {
            rememberLauncherForActivityResult(healthConnect.permissionContract()) { granted ->
                hasHealthPermissions = granted.containsAll(healthConnect.requiredPermissions)
                prefs.edit()
                    .putBoolean(
                        PREF_HEALTH_CONNECT_ACTIVE,
                        hasHealthPermissions && stepDataSource == StepDataSource.HEALTH_CONNECT
                    )
                    .apply()
                if (hasHealthPermissions && stepDataSource == StepDataSource.HEALTH_CONNECT) {
                    startStepCounterServiceSafely(context, reason = "health_connect_permission")
                } else if (!hasHealthPermissions) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (granted.isEmpty()) {
                                "Health Connect не выдал разрешения."
                            } else {
                                "Разрешения Health Connect не были выданы."
                            }
                        )
                    }
                }
            }
        } else {
            null
        }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.any { it.value }) {
            scope.launch {
                detectCity(
                    context = context,
                    fusedLocationClient = fusedLocationClient,
                    onStart = { detectingCity = true },
                    onFinish = { detectingCity = false },
                    onResolved = { resolvedCity ->
                        city = resolvedCity
                        snackbarHostState.showSnackbar("Город определён: $resolvedCity")
                    },
                    onFailure = {
                        snackbarHostState.showSnackbar("Не удалось определить город")
                    }
                )
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Разрешение на геолокацию не предоставлено")
            }
        }
    }

    val loadStepsHistory = remember(persistedStepHistory) {
        {
            persistedStepHistory.map {
                StepHistoryEntry(
                    isoDate = it.isoDate,
                    prettyDate = it.prettyDate,
                    steps = it.steps
                )
            }
        }
    }

    LaunchedEffect(persistedStepHistory, persistedWeightHistory) {
        draftState = draftState.copy(
            stepsHistory = loadStepsHistory(),
            weightHistory = persistedWeightHistory
        )
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter(ACTION_STEPS_UPDATED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                stepsToday = prefs.getLong(KEY_STEPS_TODAY, 0L)
                draftState = draftState.copy(stepsHistory = loadStepsHistory())
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(city) {
        val cachedWeather = loadWeatherFromCache(prefs, ::formatAnalyticsTime)
        draftState = if (cachedWeather != null) {
            draftState.copy(
                weatherSummary = cachedWeather.summary,
                weatherSubtitle = cachedWeather.subtitle
            )
        } else {
            draftState.copy(weatherSummary = "Загрузка...", weatherSubtitle = null)
        }

        if (!isWeatherCacheFresh(prefs)) {
            fetchAndCacheWeather(
                context = context,
                prefs = prefs,
                city = city,
                onWeatherChanged = { summary, subtitle ->
                    draftState = draftState.copy(
                        weatherSummary = summary,
                        weatherSubtitle = subtitle
                    )
                },
                onFallbackMessage = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
                formatTime = ::formatAnalyticsTime
            )
        }
    }

    LaunchedEffect(analyticsViewModel) {
        analyticsViewModel.effects.collectLatest { effect ->
            when (effect) {
                is AnalyticsUiEffect.Snackbar -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(healthAvailability) {
        if (healthAvailability != HealthConnectAvailability.AVAILABLE) {
            hasHealthPermissions = false
            if (stepDataSource == StepDataSource.HEALTH_CONNECT) {
                prefs.edit().putBoolean(PREF_HEALTH_CONNECT_ACTIVE, false).apply()
                StepServiceWatchdogWorker.schedule(context)
                context.stopService(Intent(context, StepCounterService::class.java))
            }
            return@LaunchedEffect
        }
        hasHealthPermissions = runCatching { healthConnect.hasAllPermissions() }.getOrDefault(false)
    }

    val useHealthConnectSteps =
        stepDataSource == StepDataSource.HEALTH_CONNECT &&
            healthAvailability == HealthConnectAvailability.AVAILABLE &&
            hasHealthPermissions

    LaunchedEffect(stepDataSource, hasHealthPermissions, healthAvailability, hasStepPermission) {
        if (useHealthConnectSteps) {
            prefs.edit().putBoolean(PREF_HEALTH_CONNECT_ACTIVE, true).apply()
            StepServiceWatchdogWorker.schedule(context)
            syncFromHealthConnect(
                context = context,
                prefs = prefs,
                healthConnect = healthConnect,
                analyticsViewModel = analyticsViewModel,
                showSuccessMessage = false,
                refreshHistory = true,
                onStepsTodayChanged = { stepsToday = it },
                onHistoryReload = { draftState = draftState.copy(stepsHistory = loadStepsHistory()) },
                showSnack = { snackbarHostState.showSnackbar(it) }
            )
            startStepCounterServiceSafely(context, reason = "source_health_connect")
        } else {
            prefs.edit().putBoolean(PREF_HEALTH_CONNECT_ACTIVE, false).apply()
            StepServiceWatchdogWorker.schedule(context)
            if (stepDataSource == StepDataSource.DEVICE_SENSOR && hasStepPermission) {
                startStepCounterServiceSafely(context, reason = "source_device_sensor")
            } else {
                context.stopService(Intent(context, StepCounterService::class.java))
            }
        }
    }

    LaunchedEffect(stepsToday, stepGoal, stepDataSource, hasStepPermission, hasHealthPermissions) {
        refreshStepProgressNotification(context)
    }

    LaunchedEffect(notifyStepsEnabled) {
        if (notifyStepsEnabled && Build.VERSION.SDK_INT >= 33 && !hasNotifPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val todayIsoValue = todayIso()
    val todayNutrition = remember(entries, todayIsoValue) {
        buildTodayNutritionTotal(entries, todayIsoValue)
    }

    val analyticsState = AnalyticsUiState(
        stepsToday = stepsToday,
        stepHistory = draftState.stepsHistory,
        weightHistory = draftState.weightHistory,
        weightInput = draftState.weightInput,
        weightError = draftState.weightError,
        weather = WeatherUiState(
            city = city,
            summary = draftState.weatherSummary,
            subtitle = draftState.weatherSubtitle
        ),
        todayNutrition = todayNutrition,
        nutritionNorm = nutritionViewModel.dailyNorm,
        settings = AnalyticsSettingsUiState(
            stepGoal = stepGoal,
            notificationsEnabled = notifyStepsEnabled,
            stepDataSource = stepDataSource,
            healthAvailability = healthAvailability,
            hasHealthPermissions = hasHealthPermissions,
            hasStepPermission = hasStepPermission,
            detectingCity = detectingCity
        ),
        showNotificationPermissionCta =
            notifyStepsEnabled && Build.VERSION.SDK_INT >= 33 && !hasNotifPermission
    )

    val routeCallbacks = buildAnalyticsRouteCallbacks(
        context = context,
        prefs = prefs,
        appSettingsStore = appSettingsStore,
        themeVariant = appSettings.themeVariant,
        analyticsViewModel = analyticsViewModel,
        scope = scope,
        todayKeyIso = ::todayIso,
        currentStepSource = stepDataSource,
        currentCity = city,
        onCityChanged = { city = it },
        onStepGoalChanged = { stepGoal = it },
        onNotifyChanged = { notifyStepsEnabled = it },
        onStepDataSourceChanged = { stepDataSource = it },
        onStepsTodayChanged = { stepsToday = it },
        onWeightHistoryChanged = { draftState = draftState.copy(weightHistory = it) },
        onWeatherRefreshRequested = { requestedCity ->
            scope.launch {
                fetchAndCacheWeather(
                    context = context,
                    prefs = prefs,
                    city = requestedCity,
                    onWeatherChanged = { summary, subtitle ->
                        draftState = draftState.copy(
                            weatherSummary = summary,
                            weatherSubtitle = subtitle
                        )
                    },
                    onFallbackMessage = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                    formatTime = ::formatAnalyticsTime
                )
            }
        },
        onStepsHistoryChanged = { draftState = draftState.copy(stepsHistory = loadStepsHistory()) },
        onMessage = analyticsViewModel::emitMessage
    )

    val requestStepPermission: () -> Unit = {
        when (stepDataSource) {
            StepDataSource.DEVICE_SENSOR -> {
                stepPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }

            StepDataSource.HEALTH_CONNECT -> {
                when (healthAvailability) {
                    HealthConnectAvailability.AVAILABLE -> {
                        healthPermissionLauncher?.launch(healthConnect.requiredPermissions)
                    }

                    HealthConnectAvailability.UPDATE_REQUIRED -> {
                        scope.launch {
                            snackbarHostState.showSnackbar("Обновите или установите Health Connect.")
                        }
                    }

                    HealthConnectAvailability.UNAVAILABLE -> {
                        scope.launch {
                            snackbarHostState.showSnackbar("Health Connect недоступен на этом устройстве.")
                        }
                    }
                }
            }
        }
    }

    AnalyticsContent(
        state = analyticsState,
        snackbarHostState = snackbarHostState,
        onOpenSettings = { dialogState = dialogState.copy(showSettings = true) },
        onRequestStepPermission = requestStepPermission,
        onRequestNotificationPermission = {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onOpenStepEditor = {
            if (stepDataSource == StepDataSource.DEVICE_SENSOR) {
                dialogState = dialogState.copy(showStepEditor = true)
            } else {
                analyticsViewModel.emitMessage(
                    "Ручное редактирование недоступно при источнике Health Connect."
                )
            }
        },
        onOpenStepsHistory = {
            draftState = draftState.copy(stepsHistory = loadStepsHistory())
            dialogState = dialogState.copy(showStepsHistory = true)
        },
        onWeightInputChange = { input ->
            draftState = draftState.copy(
                weightInput = input.filter { it.isDigit() || it == '.' || it == ',' },
                weightError = null
            )
        },
        onSaveWeight = {
            val error = validateAnalyticsWeight(draftState.weightInput)
            draftState = draftState.copy(weightError = error)
            if (error == null) {
                val value = draftState.weightInput.replace(',', '.').toFloat()
                analyticsViewModel.saveWeightForToday(value)
                draftState = draftState.copy(
                    weightInput = "",
                    weightError = null
                )
            } else {
                scope.launch { snackbarHostState.showSnackbar(error) }
            }
        },
        onOpenWeightEditor = { dialogState = dialogState.copy(showWeightEditor = true) }
    )

    AnalyticsOverlays(
        showSettings = dialogState.showSettings,
        showWeightEditor = dialogState.showWeightEditor,
        showStepEditor = dialogState.showStepEditor,
        showStepsHistory = dialogState.showStepsHistory,
        currentCity = city,
        currentGoal = stepGoal,
        currentNotify = notifyStepsEnabled,
        currentStepSource = stepDataSource,
        healthAvailability = healthAvailability,
        hasHealthPermissions = hasHealthPermissions,
        detectingCity = detectingCity,
        weightHistory = draftState.weightHistory,
        currentSteps = stepsToday,
        stepsHistory = draftState.stepsHistory,
        onDetectCity = {
            if (context.hasLocationPermission()) {
                scope.launch {
                    detectCity(
                        context = context,
                        fusedLocationClient = fusedLocationClient,
                        onStart = { detectingCity = true },
                        onFinish = { detectingCity = false },
                        onResolved = { resolvedCity ->
                            city = resolvedCity
                            snackbarHostState.showSnackbar("Город определён: $resolvedCity")
                        },
                        onFailure = {
                            snackbarHostState.showSnackbar("Не удалось определить город")
                        }
                    )
                }
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        onSaveSettings = { newCity, newGoal, notify, newSource ->
            routeCallbacks.saveSettings(newCity, newGoal, notify, newSource)
            dialogState = dialogState.copy(showSettings = false)
        },
        onRefreshWeather = routeCallbacks.refreshWeather,
        onDismissSettings = { dialogState = dialogState.copy(showSettings = false) },
        onSaveWeightHistory = { updated ->
            routeCallbacks.saveWeightHistory(updated)
            dialogState = dialogState.copy(showWeightEditor = false)
        },
        onDismissWeightEditor = { dialogState = dialogState.copy(showWeightEditor = false) },
        validateDate = ::validateAnalyticsShortDate,
        validateWeight = ::validateAnalyticsWeight,
        onApplyStepEdit = { manualSteps ->
            routeCallbacks.applyManualStepEdit(manualSteps)
            dialogState = dialogState.copy(showStepEditor = false)
        },
        onDismissStepEditor = { dialogState = dialogState.copy(showStepEditor = false) },
        onAddStepHistoryEntry = { dateIso, steps ->
            routeCallbacks.addStepHistoryEntry(dateIso, steps)
        },
        onDismissStepsHistory = { dialogState = dialogState.copy(showStepsHistory = false) }
    )
}

private suspend fun detectCity(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onResolved: suspend (String) -> Unit,
    onFailure: suspend () -> Unit
) {
    onStart()
    val location = runCatching {
        fusedLocationClient.awaitHighAccuracyLocation() ?: fusedLocationClient.awaitLastLocationFallback()
    }.getOrNull()
    val city = location?.let { resolveCityName(context, it) }?.trim().orEmpty()
    onFinish()
    if (city.isNotBlank()) onResolved(city) else onFailure()
}

private suspend fun syncFromHealthConnect(
    context: Context,
    prefs: SharedPreferences,
    healthConnect: HealthConnectGateway,
    analyticsViewModel: AnalyticsViewModel,
    showSuccessMessage: Boolean,
    refreshHistory: Boolean,
    onStepsTodayChanged: (Long) -> Unit,
    onHistoryReload: () -> Unit,
    showSnack: suspend (String) -> Unit
) {
    val today = todayIso()
    var importedSteps = false

    runCatching {
        if (refreshHistory) {
            val stepsByDate = healthConnect.readStepsByDate(days = 30)
            val todaySteps = stepsByDate[today] ?: 0L
            prefs.edit()
                .putLong(KEY_STEPS_TODAY, todaySteps)
                .putString(KEY_TODAY_DATE, today)
                .apply()
            analyticsViewModel.replaceStepHistory(
                stepsByDate.map { (isoDate, stepCount) ->
                    StepHistoryRecord(isoDate, stepCount)
                }
            )
            onStepsTodayChanged(todaySteps)
            onHistoryReload()
        } else {
            val todaySteps = healthConnect.readTodaySteps()
            prefs.edit()
                .putLong(KEY_STEPS_TODAY, todaySteps)
                .putString(KEY_TODAY_DATE, today)
                .apply()
            analyticsViewModel.saveStepsForDate(today, todaySteps)
            onStepsTodayChanged(todaySteps)
        }

        refreshStepProgressNotification(context)
        context.sendBroadcast(Intent(ACTION_STEPS_UPDATED).setPackage(context.packageName))
        importedSteps = true
    }

    if (showSuccessMessage) {
        showSnack(
            if (importedSteps) {
                "Шаги из Health Connect синхронизированы"
            } else {
                "Не удалось получить шаги из Health Connect"
            }
        )
    }
}

private const val KEY_TODAY_DATE = "steps_today_date"
private const val KEY_STEPS_TODAY = "steps_today"
private const val KEY_CITY = "weather_city"
private const val KEY_STEP_GOAL = "step_goal"
private const val KEY_NOTIFY_ENABLED = "notify_steps_enabled"
