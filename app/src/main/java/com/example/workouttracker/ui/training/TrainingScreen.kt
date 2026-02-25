package com.example.workouttracker.ui.training

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.viewmodel.TrainingViewModel

@Composable
fun TrainingScreen(trainingViewModel: TrainingViewModel = viewModel()) {
    // Обёртка для совместимости навигации: новый UX вынесен в ImprovedTrainingScreen.
    ImprovedTrainingScreen(trainingViewModel = trainingViewModel)
}
