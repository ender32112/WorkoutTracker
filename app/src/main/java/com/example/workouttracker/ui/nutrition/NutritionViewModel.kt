package com.example.workouttracker.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.ui.nutrition.Dish
import com.example.workouttracker.ui.nutrition.DishIngredient
import com.example.workouttracker.ui.nutrition.Ingredient
import com.example.workouttracker.ui.nutrition.MealPlan
import com.example.workouttracker.ui.nutrition.MealType
import com.example.workouttracker.ui.nutrition.NutritionEntry
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
                val entry = runCatching { parseNewEntry(obj) }
                    .getOrElse {
                        // Миграция старого формата (плоские поля: name, calories, protein, fats, carbs, weight, date, mealType?)
                        migrateLegacyEntry(obj)
                    }
                list.add(entry)
            }
            _entries.value = list
        } catch (e: Exception) {
            e.printStackTrace()
            prefs.edit().remove("entries").apply()
        }
    }

    private fun parseNewEntry(obj: JSONObject): NutritionEntry {
        val mealType = obj.optString("mealType", MealType.OTHER.name)
            .let { type -> MealType.values().firstOrNull { it.name == type } ?: MealType.OTHER }
        val dish = parseDish(obj.getJSONObject("dish"))
        return NutritionEntry(
            id = obj.optString("id", null)?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
            date = obj.getString("date"),
            mealType = mealType,
            dish = dish,
            portionWeight = obj.getInt("portionWeight")
        )
    }

    private fun parseDish(obj: JSONObject): Dish {
        val ingredientsArray = obj.optJSONArray("ingredients") ?: JSONArray()
        val ingredients = buildList {
            for (i in 0 until ingredientsArray.length()) {
                add(parseDishIngredient(ingredientsArray.getJSONObject(i)))
            }
        }
        return Dish(
            id = obj.optString("id", null)?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
            name = obj.optString("name", ""),
            ingredients = ingredients
        )
    }

    private fun parseDishIngredient(obj: JSONObject): DishIngredient {
        val ingredientObj = obj.getJSONObject("ingredient")
        return DishIngredient(
            id = obj.optString("id", null)?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
            ingredient = parseIngredient(ingredientObj),
            weightInDish = obj.optInt("weightInDish", 0)
        )
    }

    private fun parseIngredient(obj: JSONObject): Ingredient {
        return Ingredient(
            id = obj.optString("id", null)?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
            name = obj.optString("name", ""),
            caloriesPer100g = obj.optInt("caloriesPer100g", 0),
            proteinPer100g = obj.optInt("proteinPer100g", 0),
            fatsPer100g = obj.optInt("fatsPer100g", 0),
            carbsPer100g = obj.optInt("carbsPer100g", 0)
        )
    }

    private fun migrateLegacyEntry(obj: JSONObject): NutritionEntry {
        val weight = obj.optInt("weight", 0)
        val name = obj.optString("name", "")
        val calories = obj.optInt("calories", 0)
        val protein = obj.optInt("protein", 0)
        val fats = obj.optInt("fats", 0)
        val carbs = obj.optInt("carbs", 0)
        val ingredient = Ingredient(
            name = name,
            caloriesPer100g = if (weight > 0) calories * 100 / weight else 0,
            proteinPer100g = if (weight > 0) protein * 100 / weight else 0,
            fatsPer100g = if (weight > 0) fats * 100 / weight else 0,
            carbsPer100g = if (weight > 0) carbs * 100 / weight else 0
        )
        val dishIngredient = DishIngredient(
            ingredient = ingredient,
            weightInDish = weight
        )
        val dish = Dish(
            name = name,
            ingredients = listOf(dishIngredient)
        )
        val mealType = obj.optString("mealType", MealType.OTHER.name)
            .let { type -> MealType.values().firstOrNull { it.name == type } ?: MealType.OTHER }
        return NutritionEntry(
            id = obj.optString("id", null)?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
            date = obj.getString("date"),
            mealType = mealType,
            dish = dish,
            portionWeight = weight.takeIf { it > 0 } ?: dish.totalWeight
        )
    }

    private fun saveEntries() {
        try {
            val jsonArray = JSONArray()
            _entries.value.forEach { entry ->
                jsonArray.put(entry.toJson())
            }
            prefs.edit().putString("entries", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun NutritionEntry.toJson(): JSONObject = JSONObject().apply {
        put("id", id.toString())
        put("date", date)
        put("mealType", mealType.name)
        put("portionWeight", portionWeight)
        put("dish", dish.toJson())
    }

    private fun Dish.toJson(): JSONObject = JSONObject().apply {
        put("id", id.toString())
        put("name", name)
        put("ingredients", JSONArray().apply { ingredients.forEach { put(it.toJson()) } })
    }

    private fun DishIngredient.toJson(): JSONObject = JSONObject().apply {
        put("id", id.toString())
        put("weightInDish", weightInDish)
        put("ingredient", ingredient.toJson())
    }

    private fun Ingredient.toJson(): JSONObject = JSONObject().apply {
        put("id", id.toString())
        put("name", name)
        put("caloriesPer100g", caloriesPer100g)
        put("proteinPer100g", proteinPer100g)
        put("fatsPer100g", fatsPer100g)
        put("carbsPer100g", carbsPer100g)
    }

    fun addEntry(entry: NutritionEntry) {
        _entries.value = _entries.value + entry
        saveEntries()
    }

    fun addEntry(date: String, mealType: MealType, dish: Dish, portionWeight: Int) {
        addEntry(
            NutritionEntry(
                date = date,
                mealType = mealType,
                dish = dish,
                portionWeight = portionWeight
            )
        )
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
