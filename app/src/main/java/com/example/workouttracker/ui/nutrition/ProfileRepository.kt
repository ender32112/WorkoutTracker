package com.example.workouttracker.ui.nutrition

import android.content.Context
import com.google.gson.Gson
import org.json.JSONArray

class ProfileRepository(context: Context) {
    private val prefs = context.getSharedPreferences("nutrition_profile_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadProfile(): NutritionProfile? {
        val sexName = prefs.getString(KEY_SEX, null) ?: return null
        val goalName = prefs.getString(KEY_GOAL, null) ?: return null
        val age = prefs.getInt(KEY_AGE, -1)
        val heightCm = prefs.getInt(KEY_HEIGHT, -1)
        val weightKg = prefs.getFloat(KEY_WEIGHT, -1f)
        val activityLevelName = prefs.getString(KEY_ACTIVITY_LEVEL, null)
        val clinicalDietName = prefs.getString(KEY_CLINICAL_DIET, null)
        val dietConstraintsJson = prefs.getString(KEY_DIET_CONSTRAINTS, null)

        if (age <= 0 || heightCm <= 0 || weightKg <= 0f) return null

        val sex = Sex.values().firstOrNull { it.name == sexName } ?: return null
        val goal = Goal.values().firstOrNull { it.name == goalName } ?: return null
        val activityLevel = activityLevelName
            ?.let { name -> ActivityLevel.values().firstOrNull { it.name == name } }
            ?: ActivityLevel.MODERATE
        val clinicalDiet = clinicalDietName
            ?.let { name -> ClinicalDiet.values().firstOrNull { it.name == name } }
            ?: ClinicalDiet.NONE
        val dietConstraints = dietConstraintsJson?.let { json ->
            runCatching { gson.fromJson(json, DietConstraints::class.java) }.getOrNull()
        }

        return NutritionProfile(
            sex = sex,
            age = age,
            heightCm = heightCm,
            weightKg = weightKg,
            goal = goal,
            activityLevel = activityLevel,
            clinicalDiet = clinicalDiet,
            dietConstraints = dietConstraints,
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
            .putString(KEY_ACTIVITY_LEVEL, profile.activityLevel.name)
            .putString(KEY_CLINICAL_DIET, profile.clinicalDiet.name)
            .putString(KEY_DIET_CONSTRAINTS, profile.dietConstraints?.let { gson.toJson(it) })
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
        private const val KEY_ACTIVITY_LEVEL = "activity_level"
        private const val KEY_CLINICAL_DIET = "clinical_diet"
        private const val KEY_DIET_CONSTRAINTS = "diet_constraints_json"
        private const val KEY_FAVORITES = "favoriteIngredients"
        private const val KEY_DISLIKED = "dislikedIngredients"
        private const val KEY_ALLERGIES = "allergies"
    }
}
