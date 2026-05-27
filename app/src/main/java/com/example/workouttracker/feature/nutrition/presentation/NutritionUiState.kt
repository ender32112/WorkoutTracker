package com.example.workouttracker.feature.nutrition.presentation

import com.example.workouttracker.core.presentation.FeatureUiState

data class NutritionUiState(
    val entries: List<NutritionEntry> = emptyList(),
    val groupedEntries: Map<String, List<NutritionEntry>> = emptyMap(),
    val todayTotal: NutritionEntry = NutritionEntry(
        date = "",
        name = "",
        calories = 0,
        protein = 0,
        carbs = 0,
        fats = 0,
        weight = 0
    ),
    val dailyNorm: Map<String, Int> = emptyMap(),
    val recommendedNorm: Norm? = null,
    val profile: NutritionProfile? = null,
    val profilePrefill: ProfilePrefillData = ProfilePrefillData(),
    val mealPlan: MealPlan? = null,
    val isPlanLoading: Boolean = false,
    val planError: String? = null,
    val fridgePrompt: List<FridgeProduct>? = null,
    val fridgeItems: List<FridgeItemUiModel> = emptyList()
) : FeatureUiState

