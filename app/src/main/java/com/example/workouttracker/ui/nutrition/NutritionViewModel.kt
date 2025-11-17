package com.example.workouttracker.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.ui.nutrition.NutritionEntry
import com.example.workouttracker.ui.nutrition.MealPlan
import com.example.workouttracker.llm.NutritionAiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val authPrefs = application.getSharedPreferences(AuthViewModel.AUTH_PREFS_NAME, Context.MODE_PRIVATE)
    private val userId = authPrefs.getString(AuthViewModel.KEY_CURRENT_USER_ID, null) ?: "guest"
    private val prefs = application.getSharedPreferences("nutrition_prefs_" + userId, Context.MODE_PRIVATE)

    private val _entries = MutableStateFlow<List<NutritionEntry>>(emptyList())
    val entries: StateFlow<List<NutritionEntry>> = _entries

    // ---- состояние плана питания ----
    private val _mealPlan = MutableStateFlow<MealPlan?>(null)
    val mealPlan: StateFlow<MealPlan?> = _mealPlan

    private val _isPlanLoading = MutableStateFlow(false)
    val isPlanLoading: StateFlow<Boolean> = _isPlanLoading

    private val _planError = MutableStateFlow<String?>(null)
    val planError: StateFlow<String?> = _planError

    var dailyNorm = mapOf<String, Int>()

    init {
        dailyNorm = mapOf(
            "calories" to prefs.getInt("norm_calories", 2500),
            "protein"  to prefs.getInt("norm_protein", 120),
            "carbs"    to prefs.getInt("norm_carbs", 300),
            "fats"     to prefs.getInt("norm_fats", 80)
        )
        loadEntries()
    }

    private fun loadEntries() {
        val jsonString = prefs.getString("entries", null) ?: return
        try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<NutritionEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    NutritionEntry(
                        id = UUID.fromString(obj.getString("id")),
                        date = obj.getString("date"),
                        name = obj.getString("name"),
                        calories = obj.getInt("calories"),
                        protein = obj.getInt("protein"),
                        carbs = obj.getInt("carbs"),
                        fats = obj.getInt("fats"),
                        weight = obj.optInt("weight", 100)
                    )
                )
            }
            _entries.value = list
        } catch (e: Exception) {
            e.printStackTrace()
            prefs.edit().remove("entries").apply()
        }
    }

    private fun saveEntries() {
        try {
            val jsonArray = JSONArray()
            _entries.value.forEach { entry ->
                jsonArray.put(
                    JSONObject().apply {
                        put("id", entry.id.toString())
                        put("date", entry.date)
                        put("name", entry.name)
                        put("calories", entry.calories)
                        put("protein", entry.protein)
                        put("carbs", entry.carbs)
                        put("fats", entry.fats)
                        put("weight", entry.weight)
                    }
                )
            }
            prefs.edit().putString("entries", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addEntry(entry: NutritionEntry) {
        _entries.value = _entries.value + entry
        saveEntries()
    }

    fun removeEntry(id: UUID) {
        _entries.value = _entries.value.filterNot { it.id == id }
        saveEntries()
    }

    fun updateEntry(updated: NutritionEntry) {
        _entries.value = _entries.value.map { if (it.id == updated.id) updated else it }
        saveEntries()
    }

    fun updateNorm(norm: Map<String, Int>) {
        dailyNorm = norm
        with(prefs.edit()) {
            putInt("norm_calories", norm["calories"] ?: 2500)
            putInt("norm_protein",  norm["protein"]  ?: 120)
            putInt("norm_carbs",    norm["carbs"]    ?: 300)
            putInt("norm_fats",     norm["fats"]     ?: 80)
            apply()
        }
    }

    // ---- ГЕНЕРАЦИЯ ПЛАНА С LLM ----

    fun generatePlanForToday() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val entriesToday = _entries.value.filter { it.date == today }

        viewModelScope.launch {
            _isPlanLoading.value = true
            _planError.value = null
            try {
                val plan = NutritionAiRepository.instance.generateMealPlanForDay(
                    date = today,
                    dailyNorm = dailyNorm,
                    entriesToday = entriesToday
                )
                _mealPlan.value = plan
            } catch (e: Exception) {
                e.printStackTrace()
                _planError.value = e.message ?: "Ошибка при генерации плана"
            } finally {
                _isPlanLoading.value = false
            }
        }
    }

    fun clearPlanError() {
        _planError.value = null
    }
}
