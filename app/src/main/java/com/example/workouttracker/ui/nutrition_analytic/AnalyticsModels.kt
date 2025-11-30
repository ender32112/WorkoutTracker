package com.example.workouttracker.ui.nutrition_analytic

import com.example.workouttracker.ui.nutrition.MealType

data class CanonicalFoodItem(
    val nameCanonical: String,
    val nameOriginal: String,
    val calories: Int,
    val protein: Int,
    val fats: Int,
    val carbs: Int
)

data class MatchedFood(
    val planned: CanonicalFoodItem,
    val eaten: CanonicalFoodItem
)

data class MealComparison(
    val mealType: MealType,
    val plannedItems: List<CanonicalFoodItem>,
    val eatenItems: List<CanonicalFoodItem>,
    val matched: List<MatchedFood>,
    val missedFromPlan: List<CanonicalFoodItem>,
    val extraFood: List<CanonicalFoodItem>
)

data class DailyAnalytics(
    val date: String,
    val plannedCalories: Int,
    val eatenCalories: Int,
    val deviationCalories: Int,
    val plannedProtein: Int,
    val eatenProtein: Int,
    val deviationProtein: Int,
    val plannedFats: Int,
    val eatenFats: Int,
    val deviationFats: Int,
    val plannedCarbs: Int,
    val eatenCarbs: Int,
    val deviationCarbs: Int,
    val adherenceCaloriesPercent: Int,
    val adherenceProteinPercent: Int,
    val adherenceFatsPercent: Int,
    val adherenceCarbsPercent: Int,
    val mealComparisons: List<MealComparison>
)

data class WeeklyAnalytics(
    val startDate: String,
    val endDate: String,
    val days: List<DailyAnalytics>,
    val avgAdherenceCaloriesPercent: Int,
    val avgAdherenceProteinPercent: Int,
    val avgAdherenceFatsPercent: Int,
    val avgAdherenceCarbsPercent: Int,
    val favoriteFoods: List<String>,
    val ignoredPlannedFoods: List<String>,
    val replacedFoods: List<String>
)
