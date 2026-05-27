package com.example.workouttracker.feature.articles.presentation

import com.example.workouttracker.core.presentation.FeatureUiEffect

sealed interface ArticlesUiEffect : FeatureUiEffect {
    data class Snackbar(val message: String) : ArticlesUiEffect
}
