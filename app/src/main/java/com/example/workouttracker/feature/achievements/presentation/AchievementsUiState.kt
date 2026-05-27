package com.example.workouttracker.feature.achievements.presentation

import com.example.workouttracker.core.presentation.FeatureUiState

data class AchievementsUiState(
    val achievements: List<Achievement> = emptyList()
) : FeatureUiState
