package com.example.workouttracker.feature.analytics.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workouttracker.feature.analytics.presentation.AnalyticsViewModel
import com.example.workouttracker.feature.nutrition.presentation.NutritionViewModel
import com.example.workouttracker.feature.training.presentation.TrainingViewModel

@Composable
fun AnalyticsScreen(
    trainingViewModel: TrainingViewModel = hiltViewModel(),
    nutritionViewModel: NutritionViewModel = hiltViewModel(),
    analyticsViewModel: AnalyticsViewModel = hiltViewModel()
) {
    AnalyticsRoute(
        trainingViewModel = trainingViewModel,
        nutritionViewModel = nutritionViewModel,
        analyticsViewModel = analyticsViewModel
    )
}

