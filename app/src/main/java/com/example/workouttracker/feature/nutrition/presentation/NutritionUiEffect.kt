package com.example.workouttracker.feature.nutrition.presentation

import com.example.workouttracker.core.presentation.FeatureUiEffect

sealed interface NutritionUiEffect : FeatureUiEffect {
    data class Snackbar(val message: String) : NutritionUiEffect
}

