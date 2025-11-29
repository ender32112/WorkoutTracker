package com.example.workouttracker.ui.nutrition

import kotlin.math.roundToInt

data class Norm(
    val calories: Int,
    val protein: Int,
    val fats: Int,
    val carbs: Int
)

object NutritionCalculator {
    private const val ACTIVITY_FACTOR = 1.45f
    private const val PROTEIN_PER_KG = 1.8f
    private const val FATS_PER_KG = 0.9f

    fun calculateRecommendedNorm(profile: NutritionProfile): Norm {
        val bmr = when (profile.sex) {
            Sex.MALE -> 10 * profile.weightKg + 6.25f * profile.heightCm - 5f * profile.age + 5
            Sex.FEMALE -> 10 * profile.weightKg + 6.25f * profile.heightCm - 5f * profile.age - 161
        }

        val maintenanceCalories = bmr * ACTIVITY_FACTOR
        val adjustedCalories = when (profile.goal) {
            Goal.LOSE_WEIGHT -> maintenanceCalories * 0.85f
            Goal.MAINTAIN_WEIGHT -> maintenanceCalories
            Goal.GAIN_WEIGHT -> maintenanceCalories * 1.12f
        }
        val calories = adjustedCalories.roundToInt()

        val proteinGrams = (profile.weightKg * PROTEIN_PER_KG).roundToInt()
        val fatsGrams = (profile.weightKg * FATS_PER_KG).roundToInt()
        val caloriesFromProtein = proteinGrams * 4
        val caloriesFromFats = fatsGrams * 9
        val carbsCalories = (calories - caloriesFromProtein - caloriesFromFats).coerceAtLeast(0)
        val carbsGrams = (carbsCalories / 4f).roundToInt()

        return Norm(
            calories = calories,
            protein = proteinGrams,
            fats = fatsGrams,
            carbs = carbsGrams
        )
    }
}
