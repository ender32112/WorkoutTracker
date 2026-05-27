package com.example.workouttracker.feature.nutrition.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workouttracker.feature.nutrition.presentation.NutritionViewModel

@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = hiltViewModel()
) {
    NutritionRoute(viewModel = viewModel)
}

