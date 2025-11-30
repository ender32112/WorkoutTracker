package com.example.workouttracker.ui.nutrition_analytic

import com.example.workouttracker.ui.nutrition.MealPlan
import com.example.workouttracker.ui.nutrition.MealType
import com.example.workouttracker.ui.nutrition.NutritionEntry
import kotlin.math.min
import kotlin.math.roundToInt

class NutritionAnalyticsEngine(
    private val canonicalizer: FoodCanonicalizer
) {

    suspend fun computeDailyAnalytics(
        date: String,
        mealPlan: MealPlan?,
        entries: List<NutritionEntry>
    ): DailyAnalytics? {
        if (mealPlan == null) return null

        val plannedCalories = mealPlan.targetCalories
        val plannedProtein = mealPlan.targetProtein
        val plannedFats = mealPlan.targetFat
        val plannedCarbs = mealPlan.targetCarbs

        val eatenCalories = entries.sumOf { it.calories }
        val eatenProtein = entries.sumOf { it.protein }
        val eatenFats = entries.sumOf { it.fats }
        val eatenCarbs = entries.sumOf { it.carbs }

        val deviationCalories = eatenCalories - plannedCalories
        val deviationProtein = eatenProtein - plannedProtein
        val deviationFats = eatenFats - plannedFats
        val deviationCarbs = eatenCarbs - plannedCarbs

        fun adherence(eaten: Int, planned: Int): Int {
            val percent = eaten.toDouble() / maxOf(1, planned).toDouble() * 100.0
            return percent.roundToInt().coerceIn(0, 300)
        }

        val mealComparisons = MealType.values().map { mealType ->
            val plannedItems = mealPlan.meals
                .filter { it.type == mealType }
                .flatMap { meal ->
                    meal.items.map { item ->
                        CanonicalFoodItem(
                            nameCanonical = canonicalizer.canonicalize(item.name),
                            nameOriginal = item.name,
                            calories = item.calories,
                            protein = item.protein,
                            fats = item.fat,
                            carbs = item.carbs
                        )
                    }
                }

            val eatenItems = entries
                .filter { it.mealType == mealType }
                .map { entry ->
                    CanonicalFoodItem(
                        nameCanonical = canonicalizer.canonicalize(entry.name),
                        nameOriginal = entry.name,
                        calories = entry.calories,
                        protein = entry.protein,
                        fats = entry.fats,
                        carbs = entry.carbs
                    )
                }

            val plannedGrouped = plannedItems.groupBy { it.nameCanonical }
            val eatenGrouped = eatenItems.groupBy { it.nameCanonical }

            val matched = plannedGrouped.keys.intersect(eatenGrouped.keys).flatMap { key ->
                val plannedList = plannedGrouped[key].orEmpty()
                val eatenList = eatenGrouped[key].orEmpty()
                val count = min(plannedList.size, eatenList.size)
                (0 until count).map { index ->
                    MatchedFood(
                        planned = plannedList[index],
                        eaten = eatenList[index]
                    )
                }
            }

            val matchedCounts = matched.groupingBy { it.planned.nameCanonical }.eachCount()

            val missedFromPlan = plannedGrouped.flatMap { (key, list) ->
                val used = matchedCounts[key] ?: 0
                list.drop(used)
            }

            val extraFood = eatenGrouped.flatMap { (key, list) ->
                val used = matchedCounts[key] ?: 0
                list.drop(used)
            }

            MealComparison(
                mealType = mealType,
                plannedItems = plannedItems,
                eatenItems = eatenItems,
                matched = matched,
                missedFromPlan = missedFromPlan,
                extraFood = extraFood
            )
        }

        return DailyAnalytics(
            date = date,
            plannedCalories = plannedCalories,
            eatenCalories = eatenCalories,
            deviationCalories = deviationCalories,
            plannedProtein = plannedProtein,
            eatenProtein = eatenProtein,
            deviationProtein = deviationProtein,
            plannedFats = plannedFats,
            eatenFats = eatenFats,
            deviationFats = deviationFats,
            plannedCarbs = plannedCarbs,
            eatenCarbs = eatenCarbs,
            deviationCarbs = deviationCarbs,
            adherenceCaloriesPercent = adherence(eatenCalories, plannedCalories),
            adherenceProteinPercent = adherence(eatenProtein, plannedProtein),
            adherenceFatsPercent = adherence(eatenFats, plannedFats),
            adherenceCarbsPercent = adherence(eatenCarbs, plannedCarbs),
            mealComparisons = mealComparisons
        )
    }

    suspend fun computeWeeklyAnalytics(
        dates: List<String>,
        mealPlansByDate: Map<String, MealPlan?>,
        entriesByDate: Map<String, List<NutritionEntry>>
    ): WeeklyAnalytics? {
        if (dates.isEmpty()) return WeeklyAnalytics(
            startDate = "",
            endDate = "",
            days = emptyList(),
            avgAdherenceCaloriesPercent = 0,
            avgAdherenceProteinPercent = 0,
            avgAdherenceFatsPercent = 0,
            avgAdherenceCarbsPercent = 0,
            favoriteFoods = emptyList(),
            ignoredPlannedFoods = emptyList(),
            replacedFoods = emptyList()
        )

        val dailyAnalytics = dates.mapNotNull { date ->
            val plan = mealPlansByDate[date]
            val entries = entriesByDate[date].orEmpty()
            computeDailyAnalytics(date, plan, entries)
        }

        if (dailyAnalytics.isEmpty()) return WeeklyAnalytics(
            startDate = dates.minOrNull().orEmpty(),
            endDate = dates.maxOrNull().orEmpty(),
            days = emptyList(),
            avgAdherenceCaloriesPercent = 0,
            avgAdherenceProteinPercent = 0,
            avgAdherenceFatsPercent = 0,
            avgAdherenceCarbsPercent = 0,
            favoriteFoods = emptyList(),
            ignoredPlannedFoods = emptyList(),
            replacedFoods = emptyList()
        )

        fun avg(selector: (DailyAnalytics) -> Int): Int {
            return dailyAnalytics.map { selector(it) }.average().roundToInt()
        }

        val favoriteFoodsMap = mutableMapOf<String, Int>()
        val ignoredMap = mutableMapOf<String, Int>()
        val extraMap = mutableMapOf<String, Int>()

        dailyAnalytics.forEach { day ->
            day.mealComparisons.forEach { comparison ->
                comparison.eatenItems.forEach { item ->
                    if (item.nameCanonical.isNotBlank()) {
                        favoriteFoodsMap[item.nameCanonical] = favoriteFoodsMap.getOrDefault(item.nameCanonical, 0) + 1
                    }
                }
                comparison.missedFromPlan.forEach { item ->
                    if (item.nameCanonical.isNotBlank()) {
                        ignoredMap[item.nameCanonical] = ignoredMap.getOrDefault(item.nameCanonical, 0) + 1
                    }
                }
                comparison.extraFood.forEach { item ->
                    if (item.nameCanonical.isNotBlank()) {
                        extraMap[item.nameCanonical] = extraMap.getOrDefault(item.nameCanonical, 0) + 1
                    }
                }
            }
        }

        fun topKeys(map: Map<String, Int>, limit: Int = 10): List<String> =
            map.entries.sortedByDescending { it.value }.take(limit).map { it.key }

        val favoriteFoods = topKeys(favoriteFoodsMap)
        val ignoredPlannedFoods = topKeys(ignoredMap)
        val replacedFoods = topKeys(
            ignoredPlannedFoods.associateWith { key ->
                (ignoredMap[key] ?: 0) + (extraMap[key] ?: 0)
            }
        )

        val sortedDates = dailyAnalytics.map { it.date }.sorted()

        return WeeklyAnalytics(
            startDate = sortedDates.first(),
            endDate = sortedDates.last(),
            days = dailyAnalytics,
            avgAdherenceCaloriesPercent = avg { it.adherenceCaloriesPercent },
            avgAdherenceProteinPercent = avg { it.adherenceProteinPercent },
            avgAdherenceFatsPercent = avg { it.adherenceFatsPercent },
            avgAdherenceCarbsPercent = avg { it.adherenceCarbsPercent },
            favoriteFoods = favoriteFoods,
            ignoredPlannedFoods = ignoredPlannedFoods,
            replacedFoods = replacedFoods
        )
    }
}
