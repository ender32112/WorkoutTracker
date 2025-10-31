package com.example.workouttracker.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.workouttracker.ui.nutrition.NutritionEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * ViewModel для раздела питания.
 * Сохраняет записи в SharedPreferences (без Gson).
 * Поддерживает норму КБЖУ.
 */
class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    // SharedPreferences для хранения записей
    private val prefs = application.getSharedPreferences("nutrition_prefs", Context.MODE_PRIVATE)

    // Состояние списка записей
    private val _entries = MutableStateFlow<List<NutritionEntry>>(emptyList())
    val entries: StateFlow<List<NutritionEntry>> = _entries

    // Норма КБЖУ (можно вынести в настройки)
    val dailyNorm = mapOf(
        "calories" to 2500,
        "protein"  to 120,
        "carbs"    to 300,
        "fats"     to 80
    )

    init {
        loadEntries()
    }

    /**
     * Загружает записи из SharedPreferences
     */
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
                        calories = obj.getInt("calories"),
                        protein = obj.getInt("protein"),
                        carbs = obj.getInt("carbs"),
                        fats = obj.getInt("fats")
                    )
                )
            }
            _entries.value = list
        } catch (e: Exception) {
            e.printStackTrace()
            // При ошибке — очищаем
            prefs.edit().remove("entries").apply()
        }
    }

    /**
     * Сохраняет записи в SharedPreferences
     */
    private fun saveEntries() {
        try {
            val jsonArray = JSONArray()
            _entries.value.forEach { entry ->
                jsonArray.put(
                    JSONObject().apply {
                        put("id", entry.id.toString())
                        put("date", entry.date)
                        put("calories", entry.calories)
                        put("protein", entry.protein)
                        put("carbs", entry.carbs)
                        put("fats", entry.fats)
                    }
                )
            }
            prefs.edit().putString("entries", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Добавляет новую запись
     */
    fun addEntry(entry: NutritionEntry) {
        _entries.value = _entries.value + entry
        saveEntries()
    }

    /**
     * Удаляет запись по ID
     */
    fun removeEntry(id: UUID) {
        _entries.value = _entries.value.filterNot { it.id == id }
        saveEntries()
    }
}