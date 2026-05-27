package com.example.workouttracker.feature.analytics.presentation

import com.example.workouttracker.feature.analytics.runtime.StepHistoryEntry

data class AnalyticsDialogState(
    val showSettings: Boolean = false,
    val showWeightEditor: Boolean = false,
    val showStepEditor: Boolean = false,
    val showStepsHistory: Boolean = false
)

data class AnalyticsDraftState(
    val stepsHistory: List<StepHistoryEntry> = emptyList(),
    val weightHistory: List<Pair<String, Float>> = emptyList(),
    val weightInput: String = "",
    val weightError: String? = null,
    val weatherSummary: String = "Загрузка...",
    val weatherSubtitle: String? = null
)
