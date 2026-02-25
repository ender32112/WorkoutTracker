package com.example.workouttracker.ui.nutrition

import java.util.UUID
import kotlin.math.roundToInt

data class Ingredient(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val fatsPer100g: Float,
    val carbsPer100g: Float
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

    val caloriesPer100g: Float
        get() = weightedMacro { caloriesPer100g }

    val proteinPer100g: Float
        get() = weightedMacro { proteinPer100g }

    val fatsPer100g: Float
        get() = weightedMacro { fatsPer100g }

    val carbsPer100g: Float
        get() = weightedMacro { carbsPer100g }

    private fun weightedMacro(selector: Ingredient.() -> Float): Float {
        val total = totalWeight
        if (total == 0) return 0f
        val numerator = ingredients.sumOf { (it.ingredient.selector() * it.weightInDish).toDouble() }
        return (numerator / total).toFloat()
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
        get() = (dish.caloriesPer100g * portionWeight / 100f).roundToInt()

    val protein: Int
        get() = (dish.proteinPer100g * portionWeight / 100f).roundToInt()

    val fats: Int
        get() = (dish.fatsPer100g * portionWeight / 100f).roundToInt()

    val carbs: Int
        get() = (dish.carbsPer100g * portionWeight / 100f).roundToInt()

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
                        caloriesPer100g = if (weight > 0) calories * 100f / weight else 0f,
                        proteinPer100g = if (weight > 0) protein * 100f / weight else 0f,
                        fatsPer100g = if (weight > 0) fats * 100f / weight else 0f,
                        carbsPer100g = if (weight > 0) carbs * 100f / weight else 0f
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

fun MealType.displayName(): String = when (this) {
    MealType.BREAKFAST -> "Завтрак"
    MealType.LUNCH -> "Обед"
    MealType.DINNER -> "Ужин"
    MealType.SNACK -> "Перекус"
    MealType.OTHER -> "Другое"
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
