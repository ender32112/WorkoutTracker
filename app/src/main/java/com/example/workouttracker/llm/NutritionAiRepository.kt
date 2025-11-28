package com.example.workouttracker.llm

import com.example.workouttracker.ui.nutrition.MealPlan
import com.example.workouttracker.ui.nutrition.MealType
import com.example.workouttracker.ui.nutrition.NutritionEntry
import com.example.workouttracker.ui.nutrition.PlannedFoodItem
import com.example.workouttracker.ui.nutrition.PlannedMeal
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
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
class NutritionAiRepository private constructor() {

    private val gson = Gson()

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

    // ---------- Публичный метод ----------

    suspend fun generateMealPlanForDay(
        date: String,
        dailyNorm: Map<String, Int>,
        entriesToday: List<NutritionEntry>
    ): MealPlan = withContext(Dispatchers.IO) {

        val calories = dailyNorm["calories"] ?: 2000
        val protein  = dailyNorm["protein"]  ?: 120
        val fats     = dailyNorm["fats"]     ?: 70
        val carbs    = dailyNorm["carbs"]    ?: 250

        // История за день в JSON, чтобы модель видела, что уже съедено
        val historyJson = gson.toJson(entriesToday.map {
            mapOf(
                "name" to it.name,
                "calories" to it.calories,
                "protein" to it.protein,
                "fat" to it.fats,
                "carbs" to it.carbs,
                "weight" to it.weight
            )
        })

        val systemPrompt = """
            Ты — диетолог и нутриционист.
            Твоя задача — СТРОГО вернуть ОДИН JSON-объект плана питания на день, без пояснений и текста вокруг.
            
            Формат JSON:
            {
              "date": "2025-11-19",
              "targetCalories": 2000,
              "targetProtein": 120,
              "targetFat": 70,
              "targetCarbs": 250,
              "meals": [
                {
                  "type": "BREAKFAST",
                  "items": [
                    {
                      "name": "Овсяная каша на молоке",
                      "grams": 250,
                      "calories": 350,
                      "protein": 15,
                      "fat": 8,
                      "carbs": 55
                    }
                  ]
                }
              ]
            }
            
            Правила:
            - "type" только из: "BREAKFAST", "LUNCH", "DINNER", "SNACK".
            - "grams" — целое число.
            - "calories", "protein", "fat", "carbs" — целые числа.
            - Суммарные калории и макросы за день должны быть БЛИЗКИ к целевым, но не сильно превышать норму.
            ВАЖНО: верни ТОЛЬКО JSON, без комментариев, без Markdown, без текста до или после.
        """.trimIndent()

        val userPrompt = """
            Дата: $date
            
            Целевые нормы на день:
            - Калории: $calories
            - Белки: $protein
            - Жиры: $fats
            - Углеводы: $carbs
            
            Уже съедено за этот день (может быть пустым массивом):
            $historyJson
            
            Составь план питания на оставшийся день так, чтобы суммарно за день
            максимально приблизиться к целям по калориям и макросам.
            Если история пустая — составь полный план на весь день.
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

        // content должен быть чистым JSON. Парсим его.
        val json = gson.fromJson(content, JsonObject::class.java)

        val respDate = json.get("date")?.asString ?: date
        val respCals = json.get("targetCalories")?.asInt ?: calories
        val respProt = json.get("targetProtein")?.asInt ?: protein
        val respFat  = json.get("targetFat")?.asInt ?: fats
        val respCarb = json.get("targetCarbs")?.asInt ?: carbs

        val mealsJson = json.getAsJsonArray("meals")
        val meals = mealsJson.map { mealElement ->
            val mealObj = mealElement.asJsonObject
            val typeStr = mealObj.get("type").asString
            val type = MealType.valueOf(typeStr.uppercase(Locale.ROOT))

            val itemsJson = mealObj.getAsJsonArray("items")
            val items = itemsJson.map { itemElement ->
                val it = itemElement.asJsonObject
                PlannedFoodItem(
                    name = it.get("name").asString,
                    grams = it.get("grams").asInt,
                    calories = it.get("calories").asInt,
                    protein = it.get("protein").asInt,
                    fat = it.get("fat").asInt,
                    carbs = it.get("carbs").asInt
                )
            }

            PlannedMeal(
                type = type,
                items = items
            )
        }

        MealPlan(
            date = respDate,
            targetCalories = respCals,
            targetProtein = respProt,
            targetFat = respFat,
            targetCarbs = respCarb,
            meals = meals
        )
    }

    companion object {
        val instance: NutritionAiRepository by lazy { NutritionAiRepository() }
    }
}
