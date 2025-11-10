package com.example.workouttracker.ui.nutrition

import java.util.UUID

data class NutritionEntry(
    val id: UUID = UUID.randomUUID(),
    val date: String,
    val name: String,
    val calories: Int,   // вычисляется: 4*Б + 4*У + 9*Ж
    val protein: Int,    // итог за порцию
    val carbs: Int,      // итог за порцию
    val fats: Int,       // итог за порцию
    val weight: Int      // вес порции, г (НОВОЕ)
)
