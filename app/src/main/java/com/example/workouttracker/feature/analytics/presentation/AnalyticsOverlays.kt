package com.example.workouttracker.feature.analytics.presentation

import androidx.compose.runtime.Composable
import com.example.workouttracker.feature.analytics.runtime.StepDataSource
import com.example.workouttracker.feature.analytics.runtime.StepHistoryEntry
import com.example.workouttracker.health.HealthConnectAvailability

@Composable
fun AnalyticsOverlays(
    showSettings: Boolean,
    showWeightEditor: Boolean,
    showStepEditor: Boolean,
    showStepsHistory: Boolean,
    currentCity: String,
    currentGoal: Int,
    currentNotify: Boolean,
    currentStepSource: StepDataSource,
    healthAvailability: HealthConnectAvailability,
    hasHealthPermissions: Boolean,
    detectingCity: Boolean,
    weightHistory: List<Pair<String, Float>>,
    currentSteps: Long,
    stepsHistory: List<StepHistoryEntry>,
    onDetectCity: () -> Unit,
    onSaveSettings: (String, String, Boolean, StepDataSource) -> Unit,
    onRefreshWeather: () -> Unit,
    onDismissSettings: () -> Unit,
    onSaveWeightHistory: (List<Pair<String, Float>>) -> Unit,
    onDismissWeightEditor: () -> Unit,
    validateDate: (String) -> String?,
    validateWeight: (String) -> String?,
    onApplyStepEdit: (Long) -> Unit,
    onDismissStepEditor: () -> Unit,
    onAddStepHistoryEntry: (String, Long) -> Unit,
    onDismissStepsHistory: () -> Unit
) {
    if (showSettings) {
        AnalyticsSettingsDialogPretty(
            currentCity = currentCity,
            currentGoal = currentGoal.toString(),
            currentNotify = currentNotify,
            currentStepSource = currentStepSource,
            healthAvailability = healthAvailability,
            hasHealthPermissions = hasHealthPermissions,
            detectingCity = detectingCity,
            onDetectCity = onDetectCity,
            onSave = onSaveSettings,
            onRefreshWeather = onRefreshWeather,
            onDismiss = onDismissSettings
        )
    }

    if (showWeightEditor) {
        EditWeightHistoryDialogPretty(
            initial = weightHistory,
            validateDate = validateDate,
            validateWeight = validateWeight,
            onSave = onSaveWeightHistory,
            onDismiss = onDismissWeightEditor
        )
    }

    if (showStepEditor) {
        StepEditDialog(
            currentSteps = currentSteps,
            onApply = onApplyStepEdit,
            onDismiss = onDismissStepEditor
        )
    }

    if (showStepsHistory) {
        StepsHistoryBottomSheet(
            history = stepsHistory,
            todaySteps = currentSteps,
            goal = currentGoal,
            onAddEntry = onAddStepHistoryEntry,
            onDismiss = onDismissStepsHistory
        )
    }
}
