package com.example.workouttracker.ui.nutrition

import android.content.Context
import org.json.JSONArray

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
            favoriteIngredients = readStringList(KEY_FAVORITES),
            dislikedIngredients = readStringList(KEY_DISLIKED),
            allergies = readStringList(KEY_ALLERGIES)
        )
    }

    fun saveProfile(profile: NutritionProfile) {
        prefs.edit()
            .putString(KEY_SEX, profile.sex.name)
            .putString(KEY_GOAL, profile.goal.name)
            .putInt(KEY_AGE, profile.age)
            .putInt(KEY_HEIGHT, profile.heightCm)
            .putFloat(KEY_WEIGHT, profile.weightKg)
            .putString(KEY_FAVORITES, JSONArray(profile.favoriteIngredients).toString())
            .putString(KEY_DISLIKED, JSONArray(profile.dislikedIngredients).toString())
            .putString(KEY_ALLERGIES, JSONArray(profile.allergies).toString())
            .apply()
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
    }
}
