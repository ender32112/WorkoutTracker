package com.example.workouttracker.ui.nutrition

enum class Sex {
    MALE,
    FEMALE
}

enum class Goal {
    LOSE_WEIGHT,
    MAINTAIN_WEIGHT,
    GAIN_WEIGHT,
    DIET
}

data class DietSettings(
    val calories: Int,
    val protein: Int,
    val fats: Int,
    val carbs: Int,
    val excludeOrLimit: String = "",
    val increase: String = "",
    val additionalRecommendations: String = ""
)

data class NutritionProfile(
    val sex: Sex,
    val age: Int,
    val heightCm: Int,
    val weightKg: Float,
    val goal: Goal,
    val dietSettings: DietSettings? = null,
    val favoriteIngredients: List<String> = emptyList(),
    val dislikedIngredients: List<String> = emptyList(),
    val allergies: List<String> = emptyList()
)
