package com.example.workouttracker.feature.training.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workouttracker.feature.training.presentation.TrainingViewModel

@Composable
fun TrainingScreen(trainingViewModel: TrainingViewModel = hiltViewModel()) {
    // Обёртка для совместимости навигации: новый UX вынесен в ImprovedTrainingScreen.
    ImprovedTrainingScreen(trainingViewModel = trainingViewModel)
}

