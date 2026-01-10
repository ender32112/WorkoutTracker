package com.example.workouttracker.ui.nutrition

enum class Sex {
    MALE,
    FEMALE
}

enum class Goal {
    LOSE_WEIGHT,
    MAINTAIN_WEIGHT,
    GAIN_WEIGHT
}

data class NutritionProfile(
    val sex: Sex,
    val age: Int,
    val heightCm: Int,
    val weightKg: Float,
    val goal: Goal,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val clinicalDiet: ClinicalDiet = ClinicalDiet.NONE,
    val dietConstraints: DietConstraints? = null,
    val favoriteIngredients: List<String> = emptyList(),
    val dislikedIngredients: List<String> = emptyList(),
    val allergies: List<String> = emptyList()
)
