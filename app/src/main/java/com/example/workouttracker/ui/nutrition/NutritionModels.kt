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


//LLM-интеграция для диплома:
enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK
}

data class PlannedFoodItem(
    val name: String,
    val grams: Int,
    val calories: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int
)

data class PlannedMeal(
    val type: MealType,
    val items: List<PlannedFoodItem>
)

data class MealPlan(
    val date: String,              // yyyy-MM-dd
    val targetCalories: Int,
    val targetProtein: Int,
    val targetFat: Int,
    val targetCarbs: Int,
    val meals: List<PlannedMeal>
)
