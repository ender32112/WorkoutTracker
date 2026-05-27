package com.example.workouttracker.feature.analytics.presentation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.example.workouttracker.R
import com.example.workouttracker.data.settings.AppSettingsDataStore
import com.example.workouttracker.feature.analytics.runtime.ACTION_STEPS_UPDATED
import com.example.workouttracker.feature.analytics.runtime.StepDataSource
import com.example.workouttracker.feature.analytics.runtime.WeatherApi
import com.example.workouttracker.feature.analytics.runtime.WEATHER_TTL_MS
import com.example.workouttracker.feature.analytics.runtime.refreshStepProgressNotification
import com.example.workouttracker.feature.analytics.runtime.sendGoalNotificationIfNeeded
import com.example.workouttracker.feature.analytics.runtime.startStepCounterServiceSafely
import com.example.workouttracker.workers.StepServiceWatchdogWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

private const val KEY_TODAY_DATE = "steps_today_date"
private const val KEY_STEPS_TODAY = "steps_today"
private const val KEY_LAST_TS = "steps_last_ts"
private const val KEY_CITY = "weather_city"
private const val KEY_STEP_GOAL = "step_goal"
private const val KEY_NOTIFY_ENABLED = "notify_steps_enabled"
private const val KEY_WEATHER_JSON = "weather_cache_json"
private const val KEY_WEATHER_TIME = "weather_cache_time"

data class CachedWeatherUi(
    val summary: String,
    val subtitle: String?
)

data class AnalyticsRouteCallbacks(
    val saveSettings: (String, String, Boolean, StepDataSource) -> Unit,
    val refreshWeather: () -> Unit,
    val saveWeightHistory: (List<Pair<String, Float>>) -> Unit,
    val applyManualStepEdit: (Long) -> Unit,
    val addStepHistoryEntry: (String, Long) -> Unit
)

fun loadWeatherFromCache(
    prefs: SharedPreferences,
    formatTime: (Long) -> String
): CachedWeatherUi? {
    val cached = prefs.getString(KEY_WEATHER_JSON, null) ?: return null
    val time = prefs.getLong(KEY_WEATHER_TIME, 0L)
    return runCatching {
        val obj = JSONObject(cached)
        val temp = obj.getDouble("temp").roundToInt()
        val desc = obj.getString("desc")
        CachedWeatherUi(
            summary = "$desc, $temp°C",
            subtitle = if (time > 0L) "Обновлено ${formatTime(time)}" else null
        )
    }.getOrNull()
}

fun isWeatherCacheFresh(prefs: SharedPreferences): Boolean {
    val ts = prefs.getLong(KEY_WEATHER_TIME, 0L)
    return ts > 0L && (System.currentTimeMillis() - ts) < WEATHER_TTL_MS
}

suspend fun fetchAndCacheWeather(
    context: Context,
    prefs: SharedPreferences,
    city: String,
    onWeatherChanged: (String, String?) -> Unit,
    onFallbackMessage: (String) -> Unit,
    formatTime: (Long) -> String
) {
    val normalizedCity = city.trim().ifBlank { "Москва" }
    val apiKey = runCatching { context.getString(R.string.openweather_api_key) }.getOrDefault("")

    if (apiKey.isBlank()) {
        val cached = loadWeatherFromCache(prefs, formatTime)
        if (cached != null) {
            onWeatherChanged(cached.summary, cached.subtitle)
            onFallbackMessage("Показаны последние сохранённые данные погоды")
        } else {
            onWeatherChanged("Погода недоступна", "Добавьте API-ключ OpenWeather в ресурсы")
        }
        return
    }

    runCatching {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(WeatherApi::class.java)
        api.getCurrentWeatherByCity(normalizedCity, apiKey)
    }.onSuccess { response ->
        val temp = response.main.temp.roundToInt()
        val desc = response.weather.firstOrNull()?.description
            ?.replaceFirstChar { it.titlecase() }
            ?: "Нет данных"
        val now = System.currentTimeMillis()
        onWeatherChanged("$desc, $temp°C", "Обновлено ${formatTime(now)}")
        val cachedJson = JSONObject().apply {
            put("temp", response.main.temp)
            put("desc", desc)
        }.toString()
        prefs.edit()
            .putString(KEY_WEATHER_JSON, cachedJson)
            .putLong(KEY_WEATHER_TIME, now)
            .apply()
    }.onFailure {
        val cached = loadWeatherFromCache(prefs, formatTime)
        if (cached != null) {
            onWeatherChanged(cached.summary, cached.subtitle)
            onFallbackMessage("Показаны последние сохранённые данные погоды")
        } else {
            onWeatherChanged("Нет сети / нет кэша", null)
        }
    }
}

fun buildAnalyticsRouteCallbacks(
    context: Context,
    prefs: SharedPreferences,
    appSettingsStore: AppSettingsDataStore,
    themeVariant: String,
    analyticsViewModel: AnalyticsViewModel,
    scope: CoroutineScope,
    todayKeyIso: () -> String,
    currentStepSource: StepDataSource,
    currentCity: String,
    onCityChanged: (String) -> Unit,
    onStepGoalChanged: (Int) -> Unit,
    onNotifyChanged: (Boolean) -> Unit,
    onStepDataSourceChanged: (StepDataSource) -> Unit,
    onStepsTodayChanged: (Long) -> Unit,
    onWeightHistoryChanged: (List<Pair<String, Float>>) -> Unit,
    onWeatherRefreshRequested: (String) -> Unit,
    onStepsHistoryChanged: () -> Unit,
    onMessage: (String) -> Unit
): AnalyticsRouteCallbacks {
    return AnalyticsRouteCallbacks(
        saveSettings = { newCity, newGoal, notify, newSource ->
            val resolvedCity = newCity.trim().ifBlank { "Москва" }
            val resolvedGoal = newGoal.toIntOrNull()?.coerceIn(1_000, 50_000) ?: 8_000

            prefs.edit()
                .putString(KEY_CITY, resolvedCity)
                .putInt(KEY_STEP_GOAL, resolvedGoal)
                .putBoolean(KEY_NOTIFY_ENABLED, notify)
                .putString(
                    com.example.workouttracker.feature.analytics.runtime.PREF_STEP_DATA_SOURCE,
                    newSource.storageValue
                )
                .apply()

            onCityChanged(resolvedCity)
            onStepGoalChanged(resolvedGoal)
            onNotifyChanged(notify)
            onStepDataSourceChanged(newSource)

            scope.launch {
                appSettingsStore.updateFromLegacyPreferences(themeVariant, notify, resolvedGoal)
            }

            if (resolvedCity != currentCity) {
                onWeatherRefreshRequested(resolvedCity)
            }

            if (newSource != currentStepSource) {
                StepServiceWatchdogWorker.schedule(context)
                startStepCounterServiceSafely(context, reason = "analytics_settings_changed", prefs = prefs)
                refreshStepProgressNotification(context)
            }

            onMessage("Настройки аналитики сохранены")
        },
        refreshWeather = {
            onWeatherRefreshRequested(currentCity.trim().ifBlank { "Москва" })
        },
        saveWeightHistory = { updated ->
            analyticsViewModel.replaceWeightHistory(updated)
            onWeightHistoryChanged(updated)
        },
        applyManualStepEdit = { manualSteps ->
            val todayIso = todayKeyIso()
            val safeSteps = manualSteps.coerceAtLeast(0L)
            prefs.edit()
                .putLong(KEY_STEPS_TODAY, safeSteps)
                .putString(KEY_TODAY_DATE, todayIso)
                .putLong(KEY_LAST_TS, System.currentTimeMillis())
                .apply()

            analyticsViewModel.saveStepsForDate(todayIso, safeSteps)
            onStepsTodayChanged(safeSteps)
            onStepsHistoryChanged()
            refreshStepProgressNotification(context)
            sendGoalNotificationIfNeeded(context, prefs, safeSteps)
            context.sendBroadcast(Intent(ACTION_STEPS_UPDATED).setPackage(context.packageName))
            onMessage("Значение шагов обновлено")
        },
        addStepHistoryEntry = { dateIso, steps ->
            analyticsViewModel.saveStepsForDate(dateIso, steps.coerceAtLeast(0L))
            if (dateIso == todayKeyIso()) {
                prefs.edit()
                    .putLong(KEY_STEPS_TODAY, steps.coerceAtLeast(0L))
                    .putString(KEY_TODAY_DATE, dateIso)
                    .apply()
                onStepsTodayChanged(steps.coerceAtLeast(0L))
                refreshStepProgressNotification(context)
            }
            onStepsHistoryChanged()
            context.sendBroadcast(Intent(ACTION_STEPS_UPDATED).setPackage(context.packageName))
            onMessage("История шагов обновлена")
        }
    )
}
