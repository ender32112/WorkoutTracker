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
import com.example.workouttracker.ui.nutrition.NutritionCalculator
import com.example.workouttracker.ui.nutrition.NutritionProfile
import com.example.workouttracker.ui.nutrition.Norm
import com.example.workouttracker.ui.nutrition.ProfileRepository
import com.example.workouttracker.ui.nutrition.Goal
import com.example.workouttracker.ui.nutrition.Sex
import com.example.workouttracker.llm.NutritionAiRepository
import com.example.workouttracker.ui.nutrition.BehaviorPreferencesRepository
import com.example.workouttracker.ui.nutrition.FridgeProduct
import com.example.workouttracker.ui.nutrition_analytic.DailyAnalytics
import com.example.workouttracker.ui.nutrition_analytic.FoodRating
import com.example.workouttracker.ui.nutrition_analytic.FoodCanonicalizer
import com.example.workouttracker.ui.nutrition_analytic.NutritionAnalyticsEngine
import com.example.workouttracker.ui.nutrition_analytic.WeeklyAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class NutritionViewModel(application: Application) : AndroidViewModel(application) {

    private val authPrefs = application.getSharedPreferences(AuthViewModel.AUTH_PREFS_NAME, Context.MODE_PRIVATE)
    private val userId = authPrefs.getString(AuthViewModel.KEY_CURRENT_USER_ID, null) ?: "guest"
    private val prefs = application.getSharedPreferences("nutrition_prefs_" + userId, Context.MODE_PRIVATE)
    private val profileRepository = ProfileRepository(application.applicationContext)
    private val nutritionAiRepository = NutritionAiRepository.getInstance(application.applicationContext, userId)
    private val behaviorRepository = BehaviorPreferencesRepository(application.applicationContext, userId)
    private val foodCanonicalizer = FoodCanonicalizer(application.applicationContext, nutritionAiRepository)
    private val analyticsEngine = NutritionAnalyticsEngine(foodCanonicalizer)

    private val _entries = MutableStateFlow<List<NutritionEntry>>(emptyList())
    val entries: StateFlow<List<NutritionEntry>> = _entries

    // ---- состояние плана питания ----
    private val _mealPlan = MutableStateFlow<MealPlan?>(null)
    val mealPlan: StateFlow<MealPlan?> = _mealPlan

    private val _isPlanLoading = MutableStateFlow(false)
    val isPlanLoading: StateFlow<Boolean> = _isPlanLoading

    private val _planError = MutableStateFlow<String?>(null)
    val planError: StateFlow<String?> = _planError

    private val _planMessage = MutableStateFlow<String?>(null)
    val planMessage: StateFlow<String?> = _planMessage

    private val _profile = MutableStateFlow<NutritionProfile?>(null)
    val profile: StateFlow<NutritionProfile?> = _profile

    private val _recommendedNorm = MutableStateFlow<Norm?>(null)
    val recommendedNorm: StateFlow<Norm?> = _recommendedNorm

    private val _fridgeExtraPrompt = MutableStateFlow<FridgeExtraPrompt?>(null)
    val fridgeExtraPrompt: StateFlow<FridgeExtraPrompt?> = _fridgeExtraPrompt

    private val _todayAnalytics = MutableStateFlow<DailyAnalytics?>(null)
    val todayAnalytics: StateFlow<DailyAnalytics?> = _todayAnalytics

    private val _weeklyAnalytics = MutableStateFlow<WeeklyAnalytics?>(null)
    val weeklyAnalytics: StateFlow<WeeklyAnalytics?> = _weeklyAnalytics

    private val _foodRatings = MutableStateFlow<List<FoodRating>>(emptyList())
    val foodRatings: StateFlow<List<FoodRating>> = _foodRatings

    private val _autoAdjustEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_ADJUST, true))
    val autoAdjustEnabled: StateFlow<Boolean> = _autoAdjustEnabled

    var dailyNorm = mapOf<String, Int>()

    data class DailyNutritionSummary(
        val date: String,
        val calories: Int,
        val protein: Int,
        val fats: Int,
        val carbs: Int
    )

    data class AdjustedGoal(
        val calories: Int,
        val protein: Int,
        val fats: Int,
        val carbs: Int
    )

    data class FridgeExtraPrompt(
        val fridge: List<FridgeProduct>
    )

    private data class MainProfileFields(
        val sex: Sex,
        val age: Int,
        val heightCm: Int,
        val weightKg: Float,
        val goal: Goal
    )

    init {
        dailyNorm = mapOf(
            "calories" to prefs.getInt("norm_calories", 2500),
            "protein"  to prefs.getInt("norm_protein", 120),
            "carbs"    to prefs.getInt("norm_carbs", 300),
            "fats"     to prefs.getInt("norm_fats", 80)
        )
        _profile.value = mergeWithMainProfile(profileRepository.loadProfile(), loadMainProfileFields())
        _recommendedNorm.value = _profile.value?.let { NutritionCalculator.calculateRecommendedNorm(it) }
        loadEntries()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cached = nutritionAiRepository.loadCachedPlan(today)
        if (cached != null) {
            _mealPlan.value = cached
        }
    }

    private fun mergeWithMainProfile(
        localProfile: NutritionProfile?,
        mainProfile: MainProfileFields?
    ): NutritionProfile? {
        if (localProfile != null) return localProfile
        if (mainProfile == null) return null
        return NutritionProfile(
            sex = mainProfile.sex,
            age = mainProfile.age,
            heightCm = mainProfile.heightCm,
            weightKg = mainProfile.weightKg,
            goal = mainProfile.goal
        )
    }

    private fun loadMainProfileFields(): MainProfileFields? {
        val mainPrefs = application.getSharedPreferences("user_profile_" + userId, Context.MODE_PRIVATE)
        val age = mainPrefs.getInt("age", 0)
        val height = mainPrefs.getFloat("height", 0f)
        val weight = mainPrefs.getFloat("weight", 0f)
        val gender = mainPrefs.getString("gender", "").orEmpty()
        val goalName = mainPrefs.getString("goalName", "").orEmpty()

        if (age <= 0 || height <= 0f || weight <= 0f || gender.isBlank()) return null

        val sex = when (gender.trim().lowercase(Locale.getDefault())) {
            "мужчина", "male", "m" -> Sex.MALE
            "женщина", "female", "f" -> Sex.FEMALE
            else -> Sex.MALE
        }

        val goal = when {
            goalName.contains("похуд", ignoreCase = true) -> Goal.LOSE_WEIGHT
            goalName.contains("набор", ignoreCase = true) -> Goal.GAIN_WEIGHT
            goalName.contains("gain", ignoreCase = true) -> Goal.GAIN_WEIGHT
            goalName.contains("loss", ignoreCase = true) -> Goal.LOSE_WEIGHT
            else -> Goal.MAINTAIN_WEIGHT
        }

        return MainProfileFields(
            sex = sex,
            age = age,
            heightCm = height.roundToInt(),
            weightKg = weight,
            goal = goal
        )
    }

    fun syncFromMainProfile() {
        val mainProfile = loadMainProfileFields() ?: return
        val current = _profile.value
        val updated = if (current == null) {
            NutritionProfile(
                sex = mainProfile.sex,
                age = mainProfile.age,
                heightCm = mainProfile.heightCm,
                weightKg = mainProfile.weightKg,
                goal = mainProfile.goal
            )
        } else {
            current.copy(
                sex = mainProfile.sex,
                age = mainProfile.age,
                heightCm = mainProfile.heightCm,
                weightKg = mainProfile.weightKg,
                goal = mainProfile.goal
            )
        }
        _profile.value = updated
        _recommendedNorm.value = NutritionCalculator.calculateRecommendedNorm(updated)
        profileRepository.saveProfile(updated)
    }

    fun setAutoAdjustEnabled(enabled: Boolean) {
        _autoAdjustEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_ADJUST, enabled).apply()
    }

    fun getEntriesByDate(date: String): Map<MealType, List<NutritionEntry>> {
        return entries.value
            .filter { it.date == date }
            .groupBy { it.mealType }
    }

    fun getDailySummary(date: String): DailyNutritionSummary {
        val list = entries.value.filter { it.date == date }
        return DailyNutritionSummary(
            date = date,
            calories = list.sumOf { it.calories },
            protein = list.sumOf { it.protein },
            fats = list.sumOf { it.fats },
            carbs = list.sumOf { it.carbs }
        )
    }

    fun getDailySummaries(): List<DailyNutritionSummary> {
        val currentEntries = entries.value

        return currentEntries
            .groupBy { it.date }
            .map { (date, list) ->
                DailyNutritionSummary(
                    date = date,
                    calories = list.sumOf { it.calories },
                    protein = list.sumOf { it.protein },
                    fats = list.sumOf { it.fats },
                    carbs = list.sumOf { it.carbs }
                )
            }
            .sortedByDescending { it.date }
    }

    fun computeTodayAnalytics() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val plan = _mealPlan.value
        val entriesToday = entries.value.filter { it.date == today }
        viewModelScope.launch {
            _todayAnalytics.value = analyticsEngine.computeDailyAnalytics(today, plan, entriesToday)
        }
    }

    fun computeWeeklyAnalytics() {
        val recentDates = getDailySummaries()
            .sortedByDescending { it.date }
            .take(7)
            .map { it.date }

        val mealPlans = recentDates.associateWith { date ->
            nutritionAiRepository.loadCachedPlan(date)
        }

        val entriesByDate = recentDates.associateWith { date ->
            entries.value.filter { it.date == date }
        }

        viewModelScope.launch {
            val result = analyticsEngine.computeWeeklyAnalytics(recentDates, mealPlans, entriesByDate)
            _weeklyAnalytics.value = result.weeklyAnalytics
            _foodRatings.value = analyticsEngine.buildFoodRatings(result.foodStats)

            result.weeklyAnalytics?.days.orEmpty().forEach { day ->
                day.mealComparisons.forEach { comparison ->
                    comparison.plannedItems.forEach { item ->
                        behaviorRepository.registerPlannedFood(item.nameCanonical)
                    }
                    comparison.matched.forEach { matched ->
                        behaviorRepository.registerEatenFood(matched.planned.nameCanonical)
                    }
                    comparison.missedFromPlan.forEach { item ->
                        behaviorRepository.registerSkippedFood(item.nameCanonical)
                    }
                }
            }
        }
    }

    private fun calculateAdjustedGoal(
        profile: NutritionProfile?,
        recommendedNorm: Norm?,
        userNorm: Map<String, Int>?,
        history: List<DailyNutritionSummary>
    ): AdjustedGoal {
        val defaultNorm = mapOf(
            "calories" to 2000,
            "protein" to 120,
            "fats" to 70,
            "carbs" to 250
        )

        val targetCaloriesBase = userNorm?.get("calories")
            ?: recommendedNorm?.calories
            ?: defaultNorm.getValue("calories")

        val targetProteinBase = userNorm?.get("protein")
            ?: recommendedNorm?.protein
            ?: defaultNorm.getValue("protein")

        val targetFatsBase = userNorm?.get("fats")
            ?: recommendedNorm?.fats
            ?: defaultNorm.getValue("fats")

        val targetCarbsBase = userNorm?.get("carbs")
            ?: recommendedNorm?.carbs
            ?: defaultNorm.getValue("carbs")

        val historyWindow = history.take(7)
        val avgCalories = historyWindow.takeIf { it.isNotEmpty() }?.map { it.calories }?.average()
        val calorieDiff = avgCalories?.minus(targetCaloriesBase)
        val calorieAdjustment = calorieDiff?.let { (-it).coerceIn(-400.0, 400.0) } ?: 0.0
        val adjustedCaloriesDouble = (targetCaloriesBase + calorieAdjustment).coerceAtLeast(1500.0)
        val adjustedCalories = adjustedCaloriesDouble.roundToInt()

        val scale = adjustedCaloriesDouble / targetCaloriesBase.toDouble()

        val adjustedProtein = (targetProteinBase * scale).roundToInt().coerceAtLeast(0)
        val adjustedFats = (targetFatsBase * scale).roundToInt().coerceAtLeast(0)
        val adjustedCarbs = (targetCarbsBase * scale).roundToInt().coerceAtLeast(0)

        return AdjustedGoal(
            calories = adjustedCalories,
            protein = adjustedProtein,
            fats = adjustedFats,
            carbs = adjustedCarbs
        )
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
            caloriesPer100g = obj.optDouble("caloriesPer100g", 0.0).toFloat(),
            proteinPer100g = obj.optDouble("proteinPer100g", 0.0).toFloat(),
            fatsPer100g = obj.optDouble("fatsPer100g", 0.0).toFloat(),
            carbsPer100g = obj.optDouble("carbsPer100g", 0.0).toFloat()
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
            caloriesPer100g = if (weight > 0) calories * 100f / weight else 0f,
            proteinPer100g = if (weight > 0) protein * 100f / weight else 0f,
            fatsPer100g = if (weight > 0) fats * 100f / weight else 0f,
            carbsPer100g = if (weight > 0) carbs * 100f / weight else 0f
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

    fun updateProfile(profile: NutritionProfile) {
        _profile.value = profile
        _recommendedNorm.value = NutritionCalculator.calculateRecommendedNorm(profile)
        profileRepository.saveProfile(profile)
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

    fun generatePlanFromFridge(fridge: List<FridgeProduct>, allowExtraProducts: Boolean) {
        _fridgeExtraPrompt.value = null
        generatePlanFromFridgeInternal(fridge, allowExtraProducts, forceProceed = false)
    }

    private fun generatePlanFromFridgeInternal(
        fridge: List<FridgeProduct>,
        allowExtraProducts: Boolean,
        forceProceed: Boolean
    ) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (hasCachedPlanForDate(today)) {
            _planError.value = "План на сегодня уже создан"
            return
        }

        val history = getDailySummaries().take(7)
        val dislikedByBehavior = getDislikedByBehavior()
        val adjustedGoal = calculateAdjustedGoal(profile.value, recommendedNorm.value, dailyNorm, history)

        viewModelScope.launch {
            _isPlanLoading.value = true
            _planError.value = null
            try {
                if (!allowExtraProducts && !forceProceed) {
                    val maxCaloriesFromFridge = fridge.sumOf { product ->
                        val available = product.availableGrams ?: 0
                        available * product.calories100 / 100
                    }
                    if (maxCaloriesFromFridge < (adjustedGoal.calories * 0.9).roundToInt()) {
                        _fridgeExtraPrompt.value = FridgeExtraPrompt(fridge)
                        return@launch
                    }
                }

                val plan = nutritionAiRepository.generatePlanFromFridge(
                    date = today,
                    fridge = fridge,
                    profile = profile.value,
                    recommendedNorm = recommendedNorm.value,
                    userNorm = dailyNorm,
                    history = history,
                    dislikedByBehavior = dislikedByBehavior,
                    allowExtraProducts = allowExtraProducts,
                    goalCalories = adjustedGoal.calories,
                    goalProtein = adjustedGoal.protein,
                    goalFats = adjustedGoal.fats,
                    goalCarbs = adjustedGoal.carbs
                )
                _mealPlan.value = plan
                nutritionAiRepository.saveCachedPlan(today, plan)
            } catch (e: Exception) {
                e.printStackTrace()
                _planError.value = e.message ?: "Ошибка при генерации плана"
            } finally {
                _isPlanLoading.value = false
            }
        }
    }

    fun allowExtraProductsForFridgePlan() {
        val fridge = _fridgeExtraPrompt.value?.fridge ?: return
        _fridgeExtraPrompt.value = null
        generatePlanFromFridgeInternal(fridge, allowExtraProducts = true, forceProceed = true)
    }

    fun continueWithoutExtraProducts() {
        val fridge = _fridgeExtraPrompt.value?.fridge ?: return
        _fridgeExtraPrompt.value = null
        generatePlanFromFridgeInternal(fridge, allowExtraProducts = false, forceProceed = true)
    }

    fun generateTodayPlan() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val history = getDailySummaries().take(7)
        val dislikedByBehavior = getDislikedByBehavior()

        viewModelScope.launch {
            _isPlanLoading.value = true
            _planError.value = null
            try {
                val plan = nutritionAiRepository.generatePersonalizedPlan(
                    date = today,
                    profile = profile.value,
                    recommendedNorm = recommendedNorm.value,
                    userNorm = dailyNorm,
                    history = history,
                    dislikedByBehavior = dislikedByBehavior
                )
                _mealPlan.value = plan
                nutritionAiRepository.saveCachedPlan(today, plan)
            } catch (e: Exception) {
                e.printStackTrace()
                _planError.value = e.message ?: "Ошибка при генерации плана"
            } finally {
                _isPlanLoading.value = false
            }
        }
    }

    fun replaceMeal(mealType: MealType, comment: String?) {
        val currentPlan = _mealPlan.value
        if (currentPlan == null) {
            _planError.value = "План на сегодня не создан"
            return
        }

        val oldMeal = currentPlan.meals.firstOrNull { it.type == mealType }
        val dislikedByBehavior = getDislikedByBehavior()

        viewModelScope.launch {
            _isPlanLoading.value = true
            _planError.value = null
            try {
                val newMeal = nutritionAiRepository.replaceMeal(
                    date = currentPlan.date,
                    mealType = mealType,
                    currentPlan = currentPlan,
                    profile = profile.value,
                    recommendedNorm = recommendedNorm.value,
                    userNorm = dailyNorm,
                    dislikedByBehavior = dislikedByBehavior,
                    comment = comment
                )
                oldMeal?.let { meal ->
                    meal.items.map { it.name }.forEach { name ->
                        val canonical = foodCanonicalizer.canonicalize(name)
                        behaviorRepository.registerReplacedFood(canonical)
                    }
                }
                val updatedMeals = currentPlan.meals.map { existing ->
                    if (existing.type == mealType) newMeal else existing
                }
                val updatedPlan = currentPlan.copy(meals = updatedMeals)
                _mealPlan.value = updatedPlan
                nutritionAiRepository.saveCachedPlan(currentPlan.date, updatedPlan)
            } catch (e: Exception) {
                e.printStackTrace()
                _planError.value = e.message ?: "Ошибка при замене приёма"
            } finally {
                _isPlanLoading.value = false
            }
        }
    }

    fun reuseYesterdayPlan() {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = formatter.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = formatter.format(calendar.time)

        viewModelScope.launch {
            _isPlanLoading.value = true
            _planError.value = null
            try {
                val yesterdayPlan = nutritionAiRepository.loadCachedPlan(yesterday)
                if (yesterdayPlan == null) {
                    _planError.value = "План за вчера отсутствует"
                    return@launch
                }
                nutritionAiRepository.copyPlanToDate(yesterday, today)
                val todayPlan = nutritionAiRepository.loadCachedPlan(today)
                _mealPlan.value = todayPlan
                _planMessage.value = "План перенесён с вчерашнего дня"
            } catch (e: Exception) {
                e.printStackTrace()
                _planError.value = e.message ?: "Не удалось перенести план"
            } finally {
                _isPlanLoading.value = false
            }
        }
    }

    fun resetTodayPlan() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _mealPlan.value = null
        nutritionAiRepository.clearCachedPlan(today)
        _planError.value = null
    }

    fun hasCachedPlanForDate(date: String): Boolean {
        return nutritionAiRepository.loadCachedPlan(date) != null
    }

    fun setPlanError(message: String) {
        _planError.value = message
    }

    fun consumePlanMessage() {
        _planMessage.value = null
    }

    fun clearPlanError() {
        _planError.value = null
    }

    private fun getDislikedByBehavior(): Set<String> {
        return behaviorRepository.getDislikedByBehavior()
    }

    companion object {
        private const val KEY_AUTO_ADJUST = "auto_adjust_enabled"
    }
}
