package com.example.workouttracker.feature.analytics.presentation

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.workouttracker.feature.analytics.presentation.sections.NutritionSection
import com.example.workouttracker.feature.analytics.presentation.sections.StepsSection
import com.example.workouttracker.feature.analytics.presentation.sections.WeatherSection
import com.example.workouttracker.feature.analytics.presentation.sections.WeightSection
import com.example.workouttracker.feature.analytics.runtime.StepDataSource
import com.example.workouttracker.ui.designsystem.AppTopBar

@Composable
fun AnalyticsContent(
    state: AnalyticsUiState,
    snackbarHostState: SnackbarHostState,
    onOpenSettings: () -> Unit,
    onRequestStepPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenStepEditor: () -> Unit,
    onOpenStepsHistory: () -> Unit,
    onWeightInputChange: (String) -> Unit,
    onSaveWeight: () -> Unit,
    onOpenWeightEditor: () -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Аналитика",
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Tune, contentDescription = "Настройки аналитики")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                StepsSection(
                    steps = state.stepsToday,
                    goal = state.settings.stepGoal,
                    stepDataSource = state.settings.stepDataSource,
                    hasPermission = when (state.settings.stepDataSource) {
                        StepDataSource.DEVICE_SENSOR -> state.settings.hasStepPermission
                        StepDataSource.HEALTH_CONNECT -> state.settings.hasHealthPermissions
                    },
                    onRequest = onRequestStepPermission,
                    onLongPressEdit = onOpenStepEditor,
                    onHistoryClick = onOpenStepsHistory
                )
            }
            item {
                WeatherSection(
                    city = state.weather.city,
                    weather = state.weather.summary,
                    subtitle = state.weather.subtitle
                )
            }
            item {
                NutritionSection(
                    total = state.todayNutrition,
                    norm = state.nutritionNorm
                )
            }
            item {
                WeightSection(
                    input = state.weightInput,
                    error = state.weightError,
                    history = state.weightHistory,
                    onInputChange = onWeightInputChange,
                    onSave = onSaveWeight,
                    onEditClick = onOpenWeightEditor
                )
            }
            if (state.showNotificationPermissionCta && Build.VERSION.SDK_INT >= 33) {
                item {
                    FilledTonalButton(onClick = onRequestNotificationPermission) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Включить уведомления")
                    }
                }
            }
        }
    }
}
