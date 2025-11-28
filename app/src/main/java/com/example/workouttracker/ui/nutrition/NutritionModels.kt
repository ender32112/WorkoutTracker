package com.example.workouttracker.ui.nutrition

import java.util.UUID

data class Ingredient(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val caloriesPer100g: Int,
    val proteinPer100g: Int,
    val fatsPer100g: Int,
    val carbsPer100g: Int
)

data class DishIngredient(
    val id: UUID = UUID.randomUUID(),
    val ingredient: Ingredient,
    val weightInDish: Int
)

data class Dish(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val ingredients: List<DishIngredient>
) {
    val totalWeight: Int
        get() = ingredients.sumOf { it.weightInDish }

    val caloriesPer100g: Int
        get() = weightedMacro { caloriesPer100g }

    val proteinPer100g: Int
        get() = weightedMacro { proteinPer100g }

    val fatsPer100g: Int
        get() = weightedMacro { fatsPer100g }

    val carbsPer100g: Int
        get() = weightedMacro { carbsPer100g }

    private fun weightedMacro(selector: Ingredient.() -> Int): Int {
        val total = totalWeight
        if (total == 0) return 0
        val numerator = ingredients.sumOf { it.ingredient.selector() * it.weightInDish }
        return numerator / total
    }
}

data class NutritionEntry(
    val id: UUID = UUID.randomUUID(),
    val date: String,
    val mealType: MealType = MealType.OTHER,
    val dish: Dish,
    val portionWeight: Int
) {
    val calories: Int
        get() = dish.caloriesPer100g * portionWeight / 100

    val protein: Int
        get() = dish.proteinPer100g * portionWeight / 100

    val fats: Int
        get() = dish.fatsPer100g * portionWeight / 100

    val carbs: Int
        get() = dish.carbsPer100g * portionWeight / 100

    val name: String
        get() = dish.name

    val weight: Int
        get() = portionWeight

    constructor(
        id: UUID = UUID.randomUUID(),
        date: String,
        name: String,
        calories: Int,
        protein: Int,
        carbs: Int,
        fats: Int,
        weight: Int,
        mealType: MealType = MealType.OTHER
    ) : this(
        id = id,
        date = date,
        mealType = mealType,
        dish = Dish(
            name = name,
            ingredients = listOf(
                DishIngredient(
                    ingredient = Ingredient(
                        name = name,
                        caloriesPer100g = if (weight > 0) calories * 100 / weight else 0,
                        proteinPer100g = if (weight > 0) protein * 100 / weight else 0,
                        fatsPer100g = if (weight > 0) fats * 100 / weight else 0,
                        carbsPer100g = if (weight > 0) carbs * 100 / weight else 0
                    ),
                    weightInDish = weight
                )
            )
        ),
        portionWeight = weight
    )
}


//LLM-интеграция для диплома:
enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK, OTHER
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
