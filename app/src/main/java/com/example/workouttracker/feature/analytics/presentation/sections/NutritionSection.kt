package com.example.workouttracker.feature.analytics.presentation.sections

import androidx.compose.runtime.Composable
import com.example.workouttracker.feature.analytics.presentation.NutritionTodayCardPretty
import com.example.workouttracker.feature.nutrition.presentation.NutritionEntry

@Composable
fun NutritionSection(
    total: NutritionEntry,
    norm: Map<String, Int>
) {
    NutritionTodayCardPretty(total = total, norm = norm)
}

