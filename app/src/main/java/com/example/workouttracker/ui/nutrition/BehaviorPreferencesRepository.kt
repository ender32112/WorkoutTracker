package com.example.workouttracker.ui.nutrition

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class FoodBehaviorStats(
    val nameCanonical: String,
    var plannedCount: Int = 0,
    var skippedCount: Int = 0,
    var replacedCount: Int = 0,
    var eatenCount: Int = 0
)

class BehaviorPreferencesRepository(context: Context, userId: String) {

    private val prefs = context.getSharedPreferences("behavior_prefs_${'$'}userId", Context.MODE_PRIVATE)
    private val gson = Gson()

    private fun loadStats(): MutableMap<String, FoodBehaviorStats> {
        val json = prefs.getString(KEY_BEHAVIOR_STATS, null) ?: return mutableMapOf()
        val type = object : TypeToken<Map<String, FoodBehaviorStats>>() {}.type
        return runCatching {
            gson.fromJson<Map<String, FoodBehaviorStats>>(json, type)?.toMutableMap()
        }.getOrElse { mutableMapOf() } ?: mutableMapOf()
    }

    private fun saveStats(map: Map<String, FoodBehaviorStats>) {
        val json = gson.toJson(map)
        prefs.edit().putString(KEY_BEHAVIOR_STATS, json).apply()
    }

    private fun updateStat(nameCanonical: String, updater: (FoodBehaviorStats) -> Unit) {
        val key = nameCanonical.trim()
        if (key.isBlank()) return
        val stats = loadStats()
        val current = stats[key] ?: FoodBehaviorStats(nameCanonical = key)
        updater(current)
        stats[key] = current
        saveStats(stats)
    }

    fun registerPlannedFood(nameCanonical: String) {
        updateStat(nameCanonical) { it.plannedCount += 1 }
    }

    fun registerEatenFood(nameCanonical: String) {
        updateStat(nameCanonical) { it.eatenCount += 1 }
    }

    fun registerSkippedFood(nameCanonical: String) {
        updateStat(nameCanonical) { it.skippedCount += 1 }
    }

    fun registerReplacedFood(nameCanonical: String) {
        updateStat(nameCanonical) { it.replacedCount += 1 }
    }

    fun getDislikedByBehavior(): Set<String> {
        val stats = loadStats().values
        return stats.filter { stat ->
            stat.plannedCount >= 4 &&
                (stat.skippedCount + stat.replacedCount).toDouble() / stat.plannedCount >= 0.6
        }.map { it.nameCanonical }.toSet()
    }

    companion object {
        private const val KEY_BEHAVIOR_STATS = "behavior_food_stats"
    }
}
