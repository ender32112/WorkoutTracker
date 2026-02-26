package com.example.workouttracker.ui.nutrition

data class FridgeProduct(
    val name: String,
    val calories100: Float,
    val protein100: Float,
    val fats100: Float,
    val carbs100: Float,
    val availableGrams: Int? = null
)
