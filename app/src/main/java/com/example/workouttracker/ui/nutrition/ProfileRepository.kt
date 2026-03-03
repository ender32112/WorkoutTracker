package com.example.workouttracker.ui.nutrition

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ProfileRepository(context: Context) {
    private val prefs = context.getSharedPreferences("nutrition_profile_prefs", Context.MODE_PRIVATE)

    fun loadProfile(): NutritionProfile? {
        val sexName = prefs.getString(KEY_SEX, null) ?: return null
        val goalName = prefs.getString(KEY_GOAL, null) ?: return null
        val age = prefs.getInt(KEY_AGE, -1)
        val heightCm = prefs.getInt(KEY_HEIGHT, -1)
        val weightKg = prefs.getFloat(KEY_WEIGHT, -1f)

        if (age <= 0 || heightCm <= 0 || weightKg <= 0f) return null

        val sex = Sex.values().firstOrNull { it.name == sexName } ?: return null
        val goal = Goal.values().firstOrNull { it.name == goalName } ?: return null

        return NutritionProfile(
            sex = sex,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            goal = goal,
            dietSettings = readDietSettings(),
            favoriteIngredients = readStringList(KEY_FAVORITES),
            dislikedIngredients = readStringList(KEY_DISLIKED),
            allergies = readStringList(KEY_ALLERGIES)
        )
    }

    fun saveProfile(profile: NutritionProfile) {
        prefs.edit()
            .putString(KEY_SEX, profile.sex.name)
            .putString(KEY_GOAL, profile.goal.name)
            .putString(KEY_DIET_SETTINGS, profile.dietSettings?.let { toDietJson(it) })
            .putInt(KEY_AGE, profile.age)
            .putInt(KEY_HEIGHT, profile.heightCm)
            .putFloat(KEY_WEIGHT, profile.weightKg)
            .putString(KEY_FAVORITES, JSONArray(profile.favoriteIngredients).toString())
            .putString(KEY_DISLIKED, JSONArray(profile.dislikedIngredients).toString())
            .putString(KEY_ALLERGIES, JSONArray(profile.allergies).toString())
            .apply()
    }


    private fun readDietSettings(): DietSettings? {
        val raw = prefs.getString(KEY_DIET_SETTINGS, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            DietSettings(
                calories = obj.optInt("calories", 0),
                protein = obj.optInt("protein", 0),
                fats = obj.optInt("fats", 0),
                carbs = obj.optInt("carbs", 0),
                excludeOrLimit = obj.optString("excludeOrLimit", ""),
                increase = obj.optString("increase", ""),
                additionalRecommendations = obj.optString("additionalRecommendations", "")
            )
        }.getOrNull()?.takeIf { it.calories > 0 }
    }

    private fun toDietJson(diet: DietSettings): String {
        return JSONObject().apply {
            put("calories", diet.calories)
            put("protein", diet.protein)
            put("fats", diet.fats)
            put("carbs", diet.carbs)
            put("excludeOrLimit", diet.excludeOrLimit)
            put("increase", diet.increase)
            put("additionalRecommendations", diet.additionalRecommendations)
        }.toString()
    }

    private fun readStringList(key: String): List<String> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val value = array.optString(i)
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrElse { emptyList() }
    }

    companion object {
        private const val KEY_SEX = "sex"
        private const val KEY_AGE = "age"
        private const val KEY_HEIGHT = "heightCm"
        private const val KEY_WEIGHT = "weightKg"
        private const val KEY_GOAL = "goal"
        private const val KEY_FAVORITES = "favoriteIngredients"
        private const val KEY_DISLIKED = "dislikedIngredients"
        private const val KEY_ALLERGIES = "allergies"
        private const val KEY_DIET_SETTINGS = "dietSettings"
    }
}
