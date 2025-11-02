package com.example.workouttracker.ui.nutrition

import java.util.*

data class NutritionEntry(
    val id: UUID = UUID.randomUUID(),
    val date: String,
    val name: String, // ← НОВОЕ
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fats: Int
)