package com.example.workouttracker.feature.analytics.presentation

import com.example.workouttracker.core.presentation.FeatureUiState
import com.example.workouttracker.feature.analytics.runtime.StepDataSource
import com.example.workouttracker.feature.analytics.runtime.StepHistoryEntry
import com.example.workouttracker.feature.nutrition.presentation.NutritionEntry
import com.example.workouttracker.health.HealthConnectAvailability

data class WeatherUiState(
    val city: String,
    val summary: String,
    val subtitle: String?
)

data class AnalyticsSettingsUiState(
    val stepGoal: Int,
    val notificationsEnabled: Boolean,
    val stepDataSource: StepDataSource,
    val healthAvailability: HealthConnectAvailability,
    val hasHealthPermissions: Boolean,
    val hasStepPermission: Boolean,
    val detectingCity: Boolean
)

data class AnalyticsUiState(
    val stepsToday: Long,
    val stepHistory: List<StepHistoryEntry>,
    val weightHistory: List<Pair<String, Float>>,
    val weightInput: String,
    val weightError: String?,
    val weather: WeatherUiState,
    val todayNutrition: NutritionEntry,
    val nutritionNorm: Map<String, Int>,
    val settings: AnalyticsSettingsUiState,
    val showNotificationPermissionCta: Boolean
) : FeatureUiState
