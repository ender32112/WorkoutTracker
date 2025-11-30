package com.example.workouttracker.ui.nutrition

data class FridgeProduct(
    val name: String,
    val calories100: Int,
    val protein100: Int,
    val fats100: Int,
    val carbs100: Int,
    val availableGrams: Int? = null
)
