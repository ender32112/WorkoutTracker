package com.example.workouttracker.ui.nutrition

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BehaviorPreferencesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("behavior_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadDislikedScores(): Map<String, Int> {
        val json = prefs.getString(KEY_DISLIKED_SCORES, null) ?: return emptyMap()
        return runCatching {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson<Map<String, Int>>(json, type) ?: emptyMap()
        }.getOrElse { emptyMap() }
    }

    fun saveDislikedScores(map: Map<String, Int>) {
        val json = gson.toJson(map)
        prefs.edit().putString(KEY_DISLIKED_SCORES, json).apply()
    }

    fun incrementDislike(dishNames: List<String>) {
        if (dishNames.isEmpty()) return

        val scores = loadDislikedScores().toMutableMap()
        dishNames.forEach { name ->
            if (name.isNotBlank()) {
                scores[name] = (scores[name] ?: 0) + 1
            }
        }
        saveDislikedScores(scores)
    }

    fun getDislikedByBehavior(threshold: Int = 3): Set<String> {
        return loadDislikedScores()
            .filterValues { it >= threshold }
            .keys
    }

    companion object {
        private const val KEY_DISLIKED_SCORES = "disliked_scores"
    }
}
