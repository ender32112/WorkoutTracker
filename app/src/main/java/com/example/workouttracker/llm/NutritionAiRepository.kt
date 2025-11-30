package com.example.workouttracker.llm

import android.content.Context
import com.example.workouttracker.ui.nutrition.MealPlan
import com.example.workouttracker.ui.nutrition.MealType
import com.example.workouttracker.ui.nutrition.NutritionProfile
import com.example.workouttracker.ui.nutrition.Norm
import com.example.workouttracker.ui.nutrition.PlannedFoodItem
import com.example.workouttracker.ui.nutrition.PlannedMeal
import com.example.workouttracker.ui.nutrition.Goal
import com.example.workouttracker.viewmodel.NutritionViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import kotlin.math.roundToInt
import java.util.Locale

// ---------- DTO для OpenAI-совместимого API ----------

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val response_format: ResponseFormat? = ResponseFormat("json_object"),
    val temperature: Double = 0.4
)

data class ResponseFormat(
    val type: String
)

data class ChatCompletionResponse(
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val message: ChatMessageContent
)

data class ChatMessageContent(
    val role: String,
    val content: String
)

/**
 * Репозиторий, который общается с LLM через HTTP и
 * возвращает MealPlan для ViewModel.
 */
class NutritionAiRepository private constructor(context: Context) {

    private val gson = Gson()
    private val prefs = context.applicationContext.getSharedPreferences("nutrition_cache", Context.MODE_PRIVATE)

    // ---------- OkHttp клиент ----------

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .header("Authorization", "Bearer ${LlmConfig.API_KEY}")
                    .header("Content-Type", "application/json")
                    // рекомендовано OpenRouter: идентификатор приложения и сайта
                    .header("HTTP-Referer", "https://your-app-or-github.com") // можешь указать свой сайт или GitHub
                    .header("X-Title", "WorkoutTracker Nutrition AI")         // имя приложения
                    .build()
                chain.proceed(newRequest)
            }
            .build()
    }


    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    fun loadCachedPlan(date: String): MealPlan? {
        val key = "meal_plan_" + date
        val json = prefs.getString(key, null) ?: return null
        return gson.fromJson(json, MealPlan::class.java)
    }

    fun saveCachedPlan(date: String, plan: MealPlan) {
        val key = "meal_plan_" + date
        val json = gson.toJson(plan)
        prefs.edit().putString(key, json).apply()
    }

    fun copyPlanToDate(fromDate: String, toDate: String) {
        val plan = loadCachedPlan(fromDate) ?: return
        saveCachedPlan(toDate, plan.copy(date = toDate))
    }

    // ---------- Публичный метод ----------

    suspend fun generatePersonalizedPlan(
        date: String,
        profile: NutritionProfile?,
        recommendedNorm: Norm?,
        userNorm: Map<String, Int>?,
        history: List<NutritionViewModel.DailyNutritionSummary>
    ): MealPlan = withContext(Dispatchers.IO) {

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

// коэффициент, во сколько раз изменилась калорийность
        val scale = adjustedCaloriesDouble / targetCaloriesBase.toDouble()

// масштабируем макросы пропорционально
        val adjustedProtein = (targetProteinBase * scale).roundToInt().coerceAtLeast(0)
        val adjustedFats    = (targetFatsBase * scale).roundToInt().coerceAtLeast(0)
        val adjustedCarbs   = (targetCarbsBase * scale).roundToInt().coerceAtLeast(0)

        val profileDescription = profile?.let {
            "Пол: ${if (it.sex.name == "MALE") "мужчина" else "женщина"}, " +
                "${it.age} лет, рост ${it.heightCm} см, вес ${it.weightKg} кг, цель — ${goalToText(it.goal)}.\n" +
                "Любимые продукты: ${it.favoriteIngredients.joinToString().ifEmpty { "не указаны" }}.\n" +
                "Нелюбимые продукты: ${it.dislikedIngredients.joinToString().ifEmpty { "нет" }}.\n" +
                "Аллергии: ${it.allergies.joinToString().ifEmpty { "нет" }}."
        } ?: "Профиль пользователя неизвестен (пол, возраст, рост, вес, цель не указаны)."

        val recommendedNormText = recommendedNorm?.let {
            "Рекомендуемая норма: ${it.calories} ккал, белки ${it.protein} г, жиры ${it.fats} г, углеводы ${it.carbs} г."
        } ?: "Рекомендуемая норма неизвестна."

        val userNormText = userNorm?.let {
            "Пользовательская норма: ${it["calories"] ?: "?"} ккал, белки ${it["protein"] ?: "?"} г, жиры ${it["fats"] ?: "?"} г, углеводы ${it["carbs"] ?: "?"} г."
        } ?: "Пользовательская норма не задана."

        val effectiveNormText =
            "Эффективная цель с учётом истории: $adjustedCalories ккал, белки $adjustedProtein г, жиры $adjustedFats г, углеводы $adjustedCarbs г."


        val historyDetails = if (historyWindow.isNotEmpty()) {
            historyWindow.joinToString(separator = "\n") { h ->
                "- ${h.date}: ${h.calories} ккал (Б:${h.protein}, Ж:${h.fats}, У:${h.carbs})"
            }
        } else {
            "История питания за последние дни отсутствует."
        }

        val historySummary = calorieDiff?.let {
            when {
                it > 50 -> "За последние ${historyWindow.size} дней пользователь в среднем переедал примерно на ${it.roundToInt()} ккал. Сделай план сегодня примерно на ${calorieAdjustment.roundToInt().let { adj -> if (adj < 0) -adj else 0 }} ккал ниже базовой цели, но не опускайся ниже 1500 ккал."
                it < -50 -> "За последние ${historyWindow.size} дней пользователь в среднем недоедал примерно на ${(-it).roundToInt()} ккал. Добавь немного калорий к плану, но оставайся в разумных пределах здоровья."
                else -> "Последние дни близки к целевой норме, придерживайся базовой цели."
            }
        } ?: "История отсутствует, используй базовую цель."

        val systemPrompt = """
            Ты диетолог и нутриционист. Тебе нужно составить персонализированный план питания на день.
            Ты ДОЛЖЕН ответить строго в формате JSON, без Markdown и без дополнительного текста.
            Формат ответа:
            {
              "meals": [
                {
                  "mealType": "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK" | "OTHER",
                  "items": [
                    {
                      "name": "Овсянка с ягодами",
                      "grams": 250,
                      "calories": 350,
                      "protein": 15,
                      "fats": 10,
                      "carbs": 50
                    }
                  ]
                }
              ]
            }

            Обязательные требования:
            - Верни только JSON без пояснений.
            - Суммарные калории и макроэлементы по всем блюдам должны быть близки к целевой норме.
            - Используй несколько приёмов пищи (завтрак, обед, ужин, перекусы), в каждом 1–3 блюда.
            - Никогда не предлагай блюда, содержащие указанные аллергены.
            - По возможности избегай нелюбимых продуктов.
        """.trimIndent()

        val userPrompt = """
            Дата: $date

            Профиль:
            $profileDescription

            Целевые нормы:
            $recommendedNormText
            $userNormText
            $effectiveNormText

            Считай именно эту эффективную цель основной при составлении плана.

            Старайся ориентироваться на пользовательскую норму, но учитывай рекомендации и цель (похудение/набор/поддержание).

            История питания (последние ${historyWindow.size} дней):
            $historyDetails
            $historySummary

            Задача: составь план питания на сегодняшний день, приблизься к целевой норме (калории и макросы), слегка скорректировав калорийность с учётом истории, без экстремальных ограничений.
            Верни только JSON в указанном формате.
        """.trimIndent()

        val requestBody = ChatCompletionRequest(
            model = LlmConfig.MODEL_ID,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            )
        )

        val jsonBody = gson.toJson(requestBody)
        val body = jsonBody.toRequestBody(mediaTypeJson)

        val url = LlmConfig.BASE_URL.trimEnd('/') + "/chat/completions"

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorText = response.body?.string()
            throw IllegalStateException("Ошибка LLM API: ${response.code} ${response.message} $errorText")
        }

        val responseText = response.body?.string()
            ?: throw IllegalStateException("Пустой ответ от LLM API")

        val chatResponse = gson.fromJson(responseText, ChatCompletionResponse::class.java)

        val content = chatResponse.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Пустой content в ответе модели")

// content может быть либо уже JSON-объектом, либо строкой с JSON внутри
        val rootElement = gson.fromJson(content, com.google.gson.JsonElement::class.java)

        val json: JsonObject = when {
            rootElement.isJsonObject -> rootElement.asJsonObject
            rootElement.isJsonPrimitive && rootElement.asJsonPrimitive.isString -> {
                // content = "\"{...}\"" → достаём строку и парсим ещё раз как объект
                val inner = rootElement.asJsonPrimitive.asString
                gson.fromJson(inner, JsonObject::class.java)
            }
            else -> {
                throw IllegalStateException("Ожидался JSON-объект плана, но пришло: $rootElement")
            }
        }

        val respCals = adjustedCalories
        val respProt = adjustedProtein
        val respFat  = adjustedFats
        val respCarb = adjustedCarbs

        val mealsJson = json.getAsJsonArray("meals")
            ?: throw IllegalStateException("Не удалось найти массив meals в ответе модели")
        val meals = mealsJson.map { mealElement ->
            val mealObj = mealElement.asJsonObject
            val typeStr = when {
                mealObj.has("mealType") -> mealObj.get("mealType").asString
                mealObj.has("type") -> mealObj.get("type").asString
                else -> throw IllegalStateException("У приёма пищи отсутствует поле mealType/type")
            }
            val type = MealType.valueOf(typeStr.uppercase(Locale.ROOT))

            val itemsJson = mealObj.getAsJsonArray("items")
                ?: throw IllegalStateException("У приёма пищи нет массива items")
            val items = itemsJson.map { itemElement ->
                val it = itemElement.asJsonObject
                PlannedFoodItem(
                    name = it.get("name")?.asString
                        ?: throw IllegalStateException("У блюда нет имени"),
                    grams = it.get("grams")?.asInt ?: 0,
                    calories = it.get("calories")?.asInt
                        ?: throw IllegalStateException("У блюда нет калорийности"),
                    protein = it.get("protein")?.asInt
                        ?: throw IllegalStateException("У блюда нет белков"),
                    fat = it.get("fat")?.asInt ?: it.get("fats")?.asInt
                        ?: throw IllegalStateException("У блюда нет жиров"),
                    carbs = it.get("carbs")?.asInt
                        ?: throw IllegalStateException("У блюда нет углеводов")
                )
            }

            PlannedMeal(
                type = type,
                items = items
            )
        }

        MealPlan(
            date = date,
            targetCalories = respCals,
            targetProtein = respProt,
            targetFat = respFat,
            targetCarbs = respCarb,
            meals = meals
        )
    }

    suspend fun replaceMeal(
        date: String,
        mealType: MealType,
        currentPlan: MealPlan,
        profile: NutritionProfile?,
        recommendedNorm: Norm?,
        userNorm: Map<String, Int>?,
        comment: String?
    ): PlannedMeal = withContext(Dispatchers.IO) {

        val profileDescription = profile?.let {
            "Пол: ${if (it.sex.name == "MALE") "мужчина" else "женщина"}, " +
                "${it.age} лет, рост ${it.heightCm} см, вес ${it.weightKg} кг, цель — ${goalToText(it.goal)}.\n" +
                "Любимые продукты: ${it.favoriteIngredients.joinToString().ifEmpty { "не указаны" }}.\n" +
                "Нелюбимые продукты: ${it.dislikedIngredients.joinToString().ifEmpty { "нет" }}.\n" +
                "Аллергии: ${it.allergies.joinToString().ifEmpty { "нет" }}."
        } ?: "Профиль пользователя неизвестен (пол, возраст, рост, вес, цель не указаны)."

        val recommendedNormText = recommendedNorm?.let {
            "Рекомендуемая норма: ${it.calories} ккал, белки ${it.protein} г, жиры ${it.fats} г, углеводы ${it.carbs} г."
        } ?: "Рекомендуемая норма неизвестна."

        val userNormText = userNorm?.let {
            "Пользовательская норма: ${it["calories"] ?: "?"} ккал, белки ${it["protein"] ?: "?"} г, жиры ${it["fats"] ?: "?"} г, углеводы ${it["carbs"] ?: "?"} г."
        } ?: "Пользовательская норма не задана."

        val systemPrompt = """
            Ты нутриционист. Нужна ЗАМЕНА одного приёма пищи.
            Верни строго JSON.
            Формат:
            {
              "mealType": "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK" | "OTHER",
              "items": [
                 {"name": "...", "grams": N, "calories": N, "protein": N, "fats": N, "carbs": N}
              ]
            }
        """.trimIndent()

        val userPrompt = """
            Дата: $date

            Нужно заменить приём пищи: ${mealType.name}
            Комментарий пользователя: ${comment ?: "нет"}

            Профиль:
            $profileDescription

            $recommendedNormText
            $userNormText

            Целевые макросы текущего плана: ${currentPlan.targetCalories} ккал, белки ${currentPlan.targetProtein} г, жиры ${currentPlan.targetFat} г, углеводы ${currentPlan.targetCarbs} г.

            Текущий план (meals в JSON): ${gson.toJson(currentPlan.meals)}

            Подбери аналогичный по калорийности и макросам приём, но учти комментарий пользователя и избегай аллергии/нелюбимых продуктов.
            Верни только один приём пищи строго в JSON без дополнительного текста.
        """.trimIndent()

        val requestBody = ChatCompletionRequest(
            model = LlmConfig.MODEL_ID,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            )
        )

        val jsonBody = gson.toJson(requestBody)
        val body = jsonBody.toRequestBody(mediaTypeJson)

        val url = LlmConfig.BASE_URL.trimEnd('/') + "/chat/completions"

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorText = response.body?.string()
            throw IllegalStateException("Ошибка LLM API: ${response.code} ${response.message} $errorText")
        }

        val responseText = response.body?.string()
            ?: throw IllegalStateException("Пустой ответ от LLM API")

        val chatResponse = gson.fromJson(responseText, ChatCompletionResponse::class.java)

        val content = chatResponse.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Пустой content в ответе модели")

        val rootElement = gson.fromJson(content, com.google.gson.JsonElement::class.java)

        val json: JsonObject = when {
            rootElement.isJsonObject -> rootElement.asJsonObject
            rootElement.isJsonPrimitive && rootElement.asJsonPrimitive.isString -> {
                val inner = rootElement.asJsonPrimitive.asString
                gson.fromJson(inner, JsonObject::class.java)
            }
            else -> {
                throw IllegalStateException("Ожидался JSON-объект приёма пищи, но пришло: $rootElement")
            }
        }

        val typeStr = when {
            json.has("mealType") -> json.get("mealType").asString
            json.has("type") -> json.get("type").asString
            else -> throw IllegalStateException("В ответе отсутствует поле mealType/type")
        }
        val type = MealType.valueOf(typeStr.uppercase(Locale.ROOT))

        val itemsJson = json.getAsJsonArray("items")
            ?: throw IllegalStateException("В ответе нет массива items")

        val items = itemsJson.map { element ->
            val obj = element.asJsonObject
            PlannedFoodItem(
                name = obj.get("name")?.asString
                    ?: throw IllegalStateException("У блюда нет имени"),
                grams = obj.get("grams")?.asInt ?: 0,
                calories = obj.get("calories")?.asInt
                    ?: throw IllegalStateException("У блюда нет калорийности"),
                protein = obj.get("protein")?.asInt
                    ?: throw IllegalStateException("У блюда нет белков"),
                fat = obj.get("fat")?.asInt ?: obj.get("fats")?.asInt
                    ?: throw IllegalStateException("У блюда нет жиров"),
                carbs = obj.get("carbs")?.asInt
                    ?: throw IllegalStateException("У блюда нет углеводов")
            )
        }

        PlannedMeal(
            type = type,
            items = items
        )
    }

    private fun goalToText(goal: Goal): String = when (goal) {
        Goal.LOSE_WEIGHT -> "похудение"
        Goal.MAINTAIN_WEIGHT -> "поддержание веса"
        Goal.GAIN_WEIGHT -> "набор веса"
    }

    companion object {
        @Volatile
        private var instance: NutritionAiRepository? = null

        fun getInstance(context: Context): NutritionAiRepository {
            return instance ?: synchronized(this) {
                instance ?: NutritionAiRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
