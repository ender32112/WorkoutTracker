package com.example.workouttracker.data.local

import android.content.Context
import com.example.workouttracker.core.auth.AuthSessionStore
import com.example.workouttracker.data.settings.AppSettingsDataStore
import com.example.workouttracker.feature.articles.presentation.defaultArticleCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyDataMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: WorkoutTrackerDao,
    private val settings: AppSettingsDataStore
) {
    suspend fun migrateAllKnownUsers() {
        knownUserIds().forEach { userId ->
            migrateUserProfile(userId)
            migrateNutrition(userId)
            migrateAnalytics(userId)
            migrateArticles(userId)
        }
    }

    private suspend fun migrateUserProfile(userId: String) {
        val token = "$userId:user_profile"
        if (settings.hasCompletedMigration(token)) return
        val prefs = context.getSharedPreferences("user_profile_$userId", Context.MODE_PRIVATE)
        if (prefs.all.isNotEmpty()) {
            val existing = dao.getUserById(userId)
            dao.upsertUser(
                UserEntity(
                    id = userId,
                    name = listOf(prefs.getString("firstName", ""), prefs.getString("lastName", ""))
                        .joinToString(" ")
                        .trim()
                        .ifBlank { existing?.name.orEmpty() },
                    email = prefs.getString("email", existing?.email).orEmpty(),
                    password = prefs.getString("password", existing?.password).orEmpty(),
                    firstName = prefs.getString("firstName", existing?.firstName).orEmpty(),
                    lastName = prefs.getString("lastName", existing?.lastName).orEmpty(),
                    age = prefs.getInt("age", existing?.age ?: 0),
                    gender = prefs.getString("gender", existing?.gender).orEmpty(),
                    avatarUri = prefs.getString("avatarUri", existing?.avatarUri),
                    height = prefs.getFloat("height", existing?.height ?: 0f),
                    weight = prefs.getFloat("weight", existing?.weight ?: 0f),
                    shoulders = prefs.getFloat("shoulders", existing?.shoulders ?: 0f),
                    waist = prefs.getFloat("waist", existing?.waist ?: 0f),
                    hips = prefs.getFloat("hips", existing?.hips ?: 0f),
                    chest = prefs.getFloat("chest", existing?.chest ?: 0f),
                    measurementDate = prefs.getString("measurementDate", existing?.measurementDate).orEmpty(),
                    goalName = prefs.getString("goalName", existing?.goalName).orEmpty(),
                    goalDeadline = prefs.getString("goalDeadline", existing?.goalDeadline).orEmpty(),
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        settings.markMigrationCompleted(token)
    }

    private suspend fun migrateNutrition(userId: String) {
        migrateNutritionEntries(userId)
        migrateNutritionProfile(userId)
        migrateMealPlans(userId)
    }

    private suspend fun migrateNutritionEntries(userId: String) {
        val token = "$userId:nutrition_entries"
        if (settings.hasCompletedMigration(token)) return
        val prefs = context.getSharedPreferences("nutrition_prefs_$userId", Context.MODE_PRIVATE)
        val raw = prefs.getString("entries", null)
        if (!raw.isNullOrBlank() && dao.getNutritionEntriesOnce(userId).isEmpty()) {
            runCatching {
                val arr = JSONArray(raw)
                for (index in 0 until arr.length()) {
                    val obj = arr.getJSONObject(index)
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                    val date = obj.optString("date")
                    if (date.isBlank()) continue
                    val mealType = obj.optString("mealType", "OTHER")
                    val portionWeight = when {
                        obj.has("portionWeight") -> obj.optInt("portionWeight", 0)
                        else -> obj.optInt("weight", 0)
                    }.coerceAtLeast(0)
                    val dishJson = when {
                        obj.has("dish") -> obj.getJSONObject("dish").toString()
                        else -> legacyDishJson(obj)
                    }
                    dao.upsertNutritionEntry(
                        NutritionEntryEntity(
                            id = id,
                            userId = userId,
                            dateIso = date,
                            mealType = mealType,
                            dishJson = dishJson,
                            portionWeight = portionWeight
                        )
                    )
                }
            }
        }
        settings.markMigrationCompleted(token)
    }

    private suspend fun migrateNutritionProfile(userId: String) {
        val token = "$userId:nutrition_profile"
        if (settings.hasCompletedMigration(token)) return
        val profilePrefs = context.getSharedPreferences("nutrition_profile_prefs_$userId", Context.MODE_PRIVATE)
        val nutritionPrefs = context.getSharedPreferences("nutrition_prefs_$userId", Context.MODE_PRIVATE)
        if (profilePrefs.all.isNotEmpty() || nutritionPrefs.contains("norm_calories")) {
            dao.upsertNutritionProfile(
                NutritionProfileEntity(
                    userId = userId,
                    sex = profilePrefs.getString("sex", null),
                    age = profilePrefs.getInt("age", -1).takeIf { it > 0 },
                    heightCm = profilePrefs.getInt("heightCm", -1).takeIf { it > 0 },
                    weightKg = profilePrefs.getFloat("weightKg", -1f).takeIf { it > 0f },
                    goal = profilePrefs.getString("goal", null),
                    favoriteIngredientsJson = profilePrefs.getString("favoriteIngredients", "[]") ?: "[]",
                    dislikedIngredientsJson = profilePrefs.getString("dislikedIngredients", "[]") ?: "[]",
                    allergiesJson = profilePrefs.getString("allergies", "[]") ?: "[]",
                    dietSettingsJson = profilePrefs.getString("dietSettings", null),
                    customCalories = nutritionPrefs.getInt("norm_calories", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                    customProtein = nutritionPrefs.getInt("norm_protein", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                    customFats = nutritionPrefs.getInt("norm_fats", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                    customCarbs = nutritionPrefs.getInt("norm_carbs", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE },
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        settings.markMigrationCompleted(token)
    }

    private suspend fun migrateMealPlans(userId: String) {
        val token = "$userId:meal_plans"
        if (settings.hasCompletedMigration(token)) return
        val prefs = context.getSharedPreferences("nutrition_cache_$userId", Context.MODE_PRIVATE)
        val existing = dao.getMealPlans(userId).map { it.dateIso }.toSet()
        prefs.all.forEach { (key, value) ->
            if (!key.startsWith("meal_plan_")) return@forEach
            val dateIso = key.removePrefix("meal_plan_")
            if (dateIso in existing) return@forEach
            val raw = value as? String ?: return@forEach
            val wrapped = wrapMealPlanJson(raw)
            dao.upsertMealPlan(
                MealPlanEntity(
                    userId = userId,
                    dateIso = dateIso,
                    payloadJson = wrapped,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        settings.markMigrationCompleted(token)
    }

    private suspend fun migrateAnalytics(userId: String) {
        migrateWeightHistory(userId)
        migrateStepHistory(userId)
    }

    private suspend fun migrateWeightHistory(userId: String) {
        val token = "$userId:weight_history"
        val prefs = context.getSharedPreferences("analytics_prefs_$userId", Context.MODE_PRIVATE)
        val raw = prefs.getString("weight_history", null)
        if (raw.isNullOrBlank()) {
            settings.markMigrationCompleted(token)
            return
        }

        val completed = settings.hasCompletedMigration(token)
        val migrated = runCatching {
            val legacyEntries = buildList {
                val arr = JSONArray(raw)
                for (index in 0 until arr.length()) {
                    val obj = arr.getJSONObject(index)
                    val dateIso = prettyOrIsoToIso(obj.optString("date"))
                    val weight = obj.optDouble("weight", 0.0).toFloat()
                    if (dateIso != null && weight > 0f) {
                        add(dateIso to weight)
                    }
                }
            }

            val existingDates = dao.getWeightEntriesOnce(userId)
                .map { timestampToIso(it.loggedAt) }
                .toSet()
            val missingEntries = legacyEntries
                .distinctBy { it.first }
                .filter { (dateIso, _) -> dateIso !in existingDates }

            if (completed && missingEntries.isEmpty()) return
            if (missingEntries.isNotEmpty()) ensureUserExists(userId)

            missingEntries.forEach { (dateIso, weight) ->
                dao.insertWeightEntry(
                    WeightEntryEntity(
                        userId = userId,
                        weightKg = weight,
                        loggedAt = isoToTimestamp(dateIso)
                    )
                )
            }
        }

        if (migrated.isSuccess) {
            settings.markMigrationCompleted(token)
        }
    }

    private suspend fun migrateStepHistory(userId: String) {
        val token = "$userId:step_history"
        if (settings.hasCompletedMigration(token)) return
        val prefs = context.getSharedPreferences("analytics_prefs_$userId", Context.MODE_PRIVATE)
        val raw = prefs.getString("steps_history", null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(raw)
                for (index in 0 until arr.length()) {
                    val obj = arr.getJSONObject(index)
                    val dateIso = obj.optString("date")
                    val steps = obj.optLong("steps", 0L)
                    if (dateIso.isNotBlank()) {
                        dao.deleteStepEntryByDate(userId, dateIso)
                        dao.upsertStepEntry(StepEntryEntity(userId = userId, dateIso = dateIso, steps = steps))
                    }
                }
            }
        }
        val todayDate = prefs.getString("steps_today_date", null)
        val todaySteps = prefs.getLong("steps_today", -1L)
        if (!todayDate.isNullOrBlank() && todaySteps >= 0L) {
            dao.deleteStepEntryByDate(userId, todayDate)
            dao.upsertStepEntry(StepEntryEntity(userId = userId, dateIso = todayDate, steps = todaySteps))
        }
        settings.markMigrationCompleted(token)
    }

    private suspend fun migrateArticles(userId: String) {
        val token = "$userId:articles"
        if (settings.hasCompletedMigration(token)) return
        val prefs = context.getSharedPreferences("article_prefs_$userId", Context.MODE_PRIVATE)
        val existingIds = dao.getArticlePurchasesOnce(userId).map { it.articleId }.toSet()
        defaultArticleCatalog().forEach { article ->
            val key = "purchased_${article.id}"
            if (prefs.getBoolean(key, false) && article.id.toString() !in existingIds) {
                dao.upsertArticlePurchase(
                    ArticlePurchaseEntity(
                        userId = userId,
                        articleId = article.id.toString(),
                        cost = article.cost,
                        purchasedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        settings.markMigrationCompleted(token)
    }

    private fun knownUserIds(): Set<String> {
        val authPrefs = context.getSharedPreferences(AuthSessionStore.PREFS_NAME, Context.MODE_PRIVATE)
        val ids = mutableSetOf<String>()
        authPrefs.getString(AuthSessionStore.KEY_CURRENT_USER_ID, null)?.let(ids::add)
        authPrefs.getStringSet(AuthSessionStore.KEY_ACCOUNTS, emptySet()).orEmpty().forEach { entry ->
            val userId = entry.substringAfter('|', "")
            if (userId.isNotBlank()) ids += userId
        }
        if (ids.isEmpty()) ids += "guest"
        return ids
    }

    private fun legacyDishJson(obj: JSONObject): String {
        val weight = obj.optInt("weight", 0)
        val name = obj.optString("name", "")
        val calories = obj.optInt("calories", 0)
        val protein = obj.optInt("protein", 0)
        val fats = obj.optInt("fats", 0)
        val carbs = obj.optInt("carbs", 0)
        return JSONObject().apply {
            put("id", UUID.randomUUID().toString())
            put("name", name)
            put(
                "ingredients",
                JSONArray().put(
                    JSONObject().apply {
                        put("id", UUID.randomUUID().toString())
                        put("weightInDish", weight)
                        put(
                            "ingredient",
                            JSONObject().apply {
                                put("id", UUID.randomUUID().toString())
                                put("name", name)
                                put("caloriesPer100g", if (weight > 0) calories * 100f / weight else 0f)
                                put("proteinPer100g", if (weight > 0) protein * 100f / weight else 0f)
                                put("fatsPer100g", if (weight > 0) fats * 100f / weight else 0f)
                                put("carbsPer100g", if (weight > 0) carbs * 100f / weight else 0f)
                            }
                        )
                    }
                )
            )
        }.toString()
    }

    private fun wrapMealPlanJson(raw: String): String = runCatching {
        val parsed = JSONObject(raw)
        if (parsed.has("plan")) raw else JSONObject().put("plan", parsed).toString()
    }.getOrElse {
        JSONObject().put("plan", JSONObject(raw)).toString()
    }

    private fun prettyOrIsoToIso(raw: String): String? {
        if (raw.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) return raw
        if (!raw.matches(Regex("""\d{2}\.\d{2}"""))) return null
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return "$year-${raw.substring(3, 5)}-${raw.substring(0, 2)}"
    }

    private fun isoToTimestamp(dateIso: String): Long =
        runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateIso)?.time
        }.getOrNull() ?: Date().time

    private fun timestampToIso(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))

    private suspend fun ensureUserExists(userId: String) {
        if (dao.getUserById(userId) != null) return

        dao.upsertUser(
            UserEntity(
                id = userId,
                name = userId.takeIf { it != AuthSessionStore.DEFAULT_USER_ID }.orEmpty(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

