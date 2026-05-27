package com.example.workouttracker.feature.nutrition.presentation

data class DailyNutritionSummary(
    val date: String,
    val calories: Int,
    val protein: Int,
    val fats: Int,
    val carbs: Int
)

