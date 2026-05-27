package com.example.workouttracker.data.local

import com.example.workouttracker.feature.nutrition.presentation.DietSettings
import com.example.workouttracker.feature.nutrition.presentation.Dish
import com.example.workouttracker.feature.nutrition.presentation.DishIngredient
import com.example.workouttracker.feature.nutrition.presentation.Goal
import com.example.workouttracker.feature.nutrition.presentation.Ingredient
import com.example.workouttracker.feature.nutrition.presentation.MealPlan
import com.example.workouttracker.feature.nutrition.presentation.MealType
import com.example.workouttracker.feature.nutrition.presentation.NutritionEntry
import com.example.workouttracker.feature.nutrition.presentation.NutritionProfile
import com.example.workouttracker.feature.nutrition.presentation.PlannedFoodItem
import com.example.workouttracker.feature.nutrition.presentation.PlannedMeal
import com.example.workouttracker.feature.nutrition.presentation.Sex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NutritionRepository(
    private val dao: WorkoutTrackerDao
) {
    fun observeEntries(userId: String): Flow<List<NutritionEntry>> =
        dao.observeNutritionEntries(userId).map { list -> list.map(::entryFromEntity) }

    suspend fun getEntriesOnce(userId: String): List<NutritionEntry> =
        dao.getNutritionEntriesOnce(userId).map(::entryFromEntity)

    suspend fun upsertEntry(userId: String, entry: NutritionEntry) {
        dao.upsertNutritionEntry(entry.toEntity(userId))
    }

    suspend fun deleteEntry(userId: String, entryId: UUID) {
        dao.deleteNutritionEntry(userId, entryId.toString())
    }

    fun observeFridgeItems(userId: String): Flow<List<FridgeItemEntity>> =
        dao.observeFridgeItems(userId)

    suspend fun upsertFridgeItem(item: FridgeItemEntity): Long = dao.upsertFridgeItem(item)

    suspend fun updateFridgeItem(item: FridgeItemEntity) = dao.updateFridgeItem(item)

    suspend fun deleteFridgeItem(userId: String, itemId: Long) = dao.deleteFridgeItem(userId, itemId)

    fun observeProfile(userId: String): Flow<NutritionProfile?> =
        dao.observeNutritionProfile(userId).map { it?.toDomain() }

    suspend fun getProfile(userId: String): NutritionProfile? = dao.getNutritionProfile(userId)?.toDomain()

    suspend fun upsertProfile(userId: String, profile: NutritionProfile, customNorm: Map<String, Int>?) {
        dao.upsertNutritionProfile(profile.toEntity(userId, customNorm))
    }

    suspend fun updateCustomNorm(userId: String, currentProfile: NutritionProfile?, norm: Map<String, Int>) {
        val existing = dao.getNutritionProfile(userId)
        dao.upsertNutritionProfile(
            NutritionProfileEntity(
                userId = userId,
                sex = existing?.sex ?: currentProfile?.sex?.name,
                age = existing?.age ?: currentProfile?.age,
                heightCm = existing?.heightCm ?: currentProfile?.heightCm,
                weightKg = existing?.weightKg ?: currentProfile?.weightKg,
                goal = existing?.goal ?: currentProfile?.goal?.name,
                favoriteIngredientsJson = existing?.favoriteIngredientsJson ?: "[]",
                dislikedIngredientsJson = existing?.dislikedIngredientsJson ?: "[]",
                allergiesJson = existing?.allergiesJson ?: "[]",
                dietSettingsJson = existing?.dietSettingsJson,
                customCalories = norm["calories"],
                customProtein = norm["protein"],
                customFats = norm["fats"],
                customCarbs = norm["carbs"],
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getCustomNorm(userId: String): Map<String, Int>? {
        val profile = dao.getNutritionProfile(userId) ?: return null
        val calories = profile.customCalories ?: return null
        return mapOf(
            "calories" to calories,
            "protein" to (profile.customProtein ?: 0),
            "fats" to (profile.customFats ?: 0),
            "carbs" to (profile.customCarbs ?: 0)
        )
    }

    fun observeMealPlan(userId: String, dateIso: String): Flow<MealPlan?> =
        dao.observeMealPlan(userId, dateIso).map { it?.toDomainMealPlan() }

    suspend fun getMealPlan(userId: String, dateIso: String): MealPlan? =
        dao.getMealPlan(userId, dateIso)?.toDomainMealPlan()

    suspend fun saveMealPlan(userId: String, plan: MealPlan) {
        dao.upsertMealPlan(
            MealPlanEntity(
                userId = userId,
                dateIso = plan.date,
                payloadJson = JSONObject().apply { put("plan", plan.toJson()) }.toString(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMealPlan(userId: String, dateIso: String) {
        dao.deleteMealPlan(userId, dateIso)
    }

    suspend fun getMealPlans(userId: String): List<MealPlanEntity> = dao.getMealPlans(userId)

    private fun NutritionEntry.toEntity(userId: String): NutritionEntryEntity =
        NutritionEntryEntity(
            id = id.toString(),
            userId = userId,
            dateIso = date,
            mealType = mealType.name,
            dishJson = dish.toJson().toString(),
            portionWeight = portionWeight
        )

    private fun entryFromEntity(entity: NutritionEntryEntity): NutritionEntry {
        val root = JSONObject(entity.dishJson)
        return NutritionEntry(
            id = UUID.fromString(entity.id),
            date = entity.dateIso,
            mealType = MealType.entries.firstOrNull { it.name == entity.mealType } ?: MealType.OTHER,
            dish = dishFromJson(root),
            portionWeight = entity.portionWeight
        )
    }

    private fun NutritionProfileEntity.toDomain(): NutritionProfile? {
        val sexValue = sex?.let { Sex.entries.firstOrNull { candidate -> candidate.name == it } } ?: return null
        val goalValue = goal?.let { Goal.entries.firstOrNull { candidate -> candidate.name == it } } ?: return null
        val ageValue = age ?: return null
        val heightValue = heightCm ?: return null
        val weightValue = weightKg ?: return null
        return NutritionProfile(
            sex = sexValue,
            age = ageValue,
            heightCm = heightValue,
            weightKg = weightValue,
            goal = goalValue,
            dietSettings = dietSettingsJson?.let(::dietFromJson),
            favoriteIngredients = stringListFromJson(favoriteIngredientsJson),
            dislikedIngredients = stringListFromJson(dislikedIngredientsJson),
            allergies = stringListFromJson(allergiesJson)
        )
    }

    private fun NutritionProfile.toEntity(userId: String, customNorm: Map<String, Int>?): NutritionProfileEntity =
        NutritionProfileEntity(
            userId = userId,
            sex = sex.name,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            goal = goal.name,
            favoriteIngredientsJson = JSONArray(favoriteIngredients).toString(),
            dislikedIngredientsJson = JSONArray(dislikedIngredients).toString(),
            allergiesJson = JSONArray(allergies).toString(),
            dietSettingsJson = dietSettings?.let(::dietToJson),
            customCalories = customNorm?.get("calories"),
            customProtein = customNorm?.get("protein"),
            customFats = customNorm?.get("fats"),
            customCarbs = customNorm?.get("carbs"),
            updatedAt = System.currentTimeMillis()
        )

    private fun dishFromJson(obj: JSONObject): Dish {
        val ingredientsArray = obj.optJSONArray("ingredients") ?: JSONArray()
        val ingredients = buildList {
            for (index in 0 until ingredientsArray.length()) {
                val item = ingredientsArray.getJSONObject(index)
                add(
                    DishIngredient(
                        id = item.optString("id").takeIf { it.isNotBlank() }?.let(UUID::fromString) ?: UUID.randomUUID(),
                        ingredient = ingredientFromJson(item.getJSONObject("ingredient")),
                        weightInDish = item.optInt("weightInDish", 0)
                    )
                )
            }
        }
        return Dish(
            id = obj.optString("id").takeIf { it.isNotBlank() }?.let(UUID::fromString) ?: UUID.randomUUID(),
            name = obj.optString("name", ""),
            ingredients = ingredients
        )
    }

    private fun ingredientFromJson(obj: JSONObject): Ingredient =
        Ingredient(
            id = obj.optString("id").takeIf { it.isNotBlank() }?.let(UUID::fromString) ?: UUID.randomUUID(),
            name = obj.optString("name", ""),
            caloriesPer100g = obj.optDouble("caloriesPer100g", 0.0).toFloat(),
            proteinPer100g = obj.optDouble("proteinPer100g", 0.0).toFloat(),
            fatsPer100g = obj.optDouble("fatsPer100g", 0.0).toFloat(),
            carbsPer100g = obj.optDouble("carbsPer100g", 0.0).toFloat()
        )

    private fun Dish.toJson(): JSONObject = JSONObject().apply {
        put("id", id.toString())
        put("name", name)
        put("ingredients", JSONArray().apply {
            ingredients.forEach { ingredient ->
                put(JSONObject().apply {
                    put("id", ingredient.id.toString())
                    put("weightInDish", ingredient.weightInDish)
                    put(
                        "ingredient",
                        JSONObject().apply {
                            put("id", ingredient.ingredient.id.toString())
                            put("name", ingredient.ingredient.name)
                            put("caloriesPer100g", ingredient.ingredient.caloriesPer100g)
                            put("proteinPer100g", ingredient.ingredient.proteinPer100g)
                            put("fatsPer100g", ingredient.ingredient.fatsPer100g)
                            put("carbsPer100g", ingredient.ingredient.carbsPer100g)
                        }
                    )
                })
            }
        })
    }

    private fun MealPlanEntity.toDomainMealPlan(): MealPlan {
        val wrapper = JSONObject(payloadJson)
        return mealPlanFromJson(wrapper.getJSONObject("plan"))
    }

    private fun MealPlan.toJson(): JSONObject = JSONObject().apply {
        put("date", date)
        put("targetCalories", targetCalories)
        put("targetProtein", targetProtein)
        put("targetFat", targetFat)
        put("targetCarbs", targetCarbs)
        put("meals", JSONArray().apply {
            meals.forEach { meal ->
                put(JSONObject().apply {
                    put("type", meal.type.name)
                    put("items", JSONArray().apply {
                        meal.items.forEach { item ->
                            put(JSONObject().apply {
                                put("name", item.name)
                                put("grams", item.grams)
                                put("calories", item.calories)
                                put("protein", item.protein)
                                put("fat", item.fat)
                                put("carbs", item.carbs)
                            })
                        }
                    })
                })
            }
        })
    }

    private fun mealPlanFromJson(obj: JSONObject): MealPlan =
        MealPlan(
            date = obj.getString("date"),
            targetCalories = obj.optInt("targetCalories"),
            targetProtein = obj.optInt("targetProtein"),
            targetFat = obj.optInt("targetFat"),
            targetCarbs = obj.optInt("targetCarbs"),
            meals = buildList {
                val mealsArray = obj.optJSONArray("meals") ?: JSONArray()
                for (mealIndex in 0 until mealsArray.length()) {
                    val mealObj = mealsArray.getJSONObject(mealIndex)
                    add(
                        PlannedMeal(
                            type = MealType.entries.firstOrNull { it.name == mealObj.optString("type") }
                                ?: MealType.entries.firstOrNull { it.name == mealObj.optString("mealType") }
                                ?: MealType.OTHER,
                            items = buildList {
                                val itemsArray = mealObj.optJSONArray("items") ?: JSONArray()
                                for (itemIndex in 0 until itemsArray.length()) {
                                    val itemObj = itemsArray.getJSONObject(itemIndex)
                                    add(
                                        PlannedFoodItem(
                                            name = itemObj.optString("name"),
                                            grams = itemObj.optInt("grams"),
                                            calories = itemObj.optInt("calories"),
                                            protein = itemObj.optInt("protein"),
                                            fat = itemObj.optInt("fat", itemObj.optInt("fats")),
                                            carbs = itemObj.optInt("carbs")
                                        )
                                    )
                                }
                            }
                        )
                    )
                }
            }
        )

    private fun dietToJson(diet: DietSettings): String =
        JSONObject().apply {
            put("calories", diet.calories)
            put("protein", diet.protein)
            put("fats", diet.fats)
            put("carbs", diet.carbs)
            put("excludeOrLimit", diet.excludeOrLimit)
            put("increase", diet.increase)
            put("additionalRecommendations", diet.additionalRecommendations)
        }.toString()

    private fun dietFromJson(raw: String): DietSettings =
        JSONObject(raw).let { obj ->
            DietSettings(
                calories = obj.optInt("calories", 0),
                protein = obj.optInt("protein", 0),
                fats = obj.optInt("fats", 0),
                carbs = obj.optInt("carbs", 0),
                excludeOrLimit = obj.optString("excludeOrLimit", ""),
                increase = obj.optString("increase", ""),
                additionalRecommendations = obj.optString("additionalRecommendations", "")
            )
        }

    private fun stringListFromJson(raw: String): List<String> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.optString(index)
                if (value.isNotBlank()) add(value)
            }
        }
    }
}

