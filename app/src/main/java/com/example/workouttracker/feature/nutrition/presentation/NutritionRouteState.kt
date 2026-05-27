package com.example.workouttracker.feature.nutrition.presentation

data class NutritionDialogState(
    val showAddDialog: Boolean = false,
    val editEntry: NutritionEntry? = null,
    val showSettings: Boolean = false,
    val showProfileDialog: Boolean = false,
    val showFridgeManagerDialog: Boolean = false,
    val showReplaceDialog: Boolean = false,
    val replaceMealType: MealType? = null,
    val replaceComment: String = "",
    val showFridgeChoiceDialog: Boolean = false,
    val showRegenerateWarning: Boolean = false,
    val showFridgeDialog: Boolean = false,
    val showMealPlanSheet: Boolean = false,
    val showSavedProducts: Boolean = false
)

