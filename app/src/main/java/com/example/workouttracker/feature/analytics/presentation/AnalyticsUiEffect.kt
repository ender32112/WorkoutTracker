package com.example.workouttracker.feature.analytics.presentation

import com.example.workouttracker.core.presentation.FeatureUiEffect

sealed interface AnalyticsUiEffect : FeatureUiEffect {
    data class Snackbar(val message: String) : AnalyticsUiEffect
}

