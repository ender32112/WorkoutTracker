package com.example.workouttracker.llm

import android.content.Context
import com.example.workouttracker.feature.nutrition.presentation.FridgeProduct
import com.example.workouttracker.feature.nutrition.presentation.Goal
import com.example.workouttracker.feature.nutrition.presentation.MealPlan
import com.example.workouttracker.feature.nutrition.presentation.MealType
import com.example.workouttracker.feature.nutrition.presentation.Norm
import com.example.workouttracker.feature.nutrition.presentation.NutritionProfile
import com.example.workouttracker.feature.nutrition.presentation.PlannedFoodItem
import com.example.workouttracker.feature.nutrition.presentation.PlannedMeal
import com.example.workouttracker.feature.nutrition.presentation.QuantityUnit
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.StringReader
import java.util.Locale

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val response_format: ResponseFormat? = ResponseFormat("json_object"),
    val temperature: Double = 0.35
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

class NutritionAiRepository private constructor(context: Context, private val userId: String) {

    private val gson = Gson()
    private val prefs = context.applicationContext.getSharedPreferences("nutrition_cache_$userId", Context.MODE_PRIVATE)

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
                    .header("HTTP-Referer", LlmConfig.HTTP_REFERER)
                    .header("X-Title", LlmConfig.APP_TITLE)
                    .build()
                chain.proceed(newRequest)
            }
            .build()
    }

    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    fun loadCachedPlan(date: String): MealPlan? {
        val key = "meal_plan_$date"
        val json = prefs.getString(key, null) ?: return null
        val plan = gson.fromJson(json, MealPlan::class.java)
        return if (containsLatinResult(plan.meals)) {
            prefs.edit().remove(key).apply()
            null
        } else {
            plan
        }
    }

    fun saveCachedPlan(date: String, plan: MealPlan) {
        pruneCachedPlans(setOf(date))
        prefs.edit().putString("meal_plan_$date", gson.toJson(plan)).apply()
    }

    fun copyPlanToDate(fromDate: String, toDate: String) {
        val plan = loadCachedPlan(fromDate) ?: return
        saveCachedPlan(toDate, plan.copy(date = toDate))
    }

    fun clearCachedPlan(date: String) {
        prefs.edit().remove("meal_plan_$date").apply()
    }

    fun pruneCachedPlans(keepDates: Set<String>) {
        val keepKeys = keepDates.mapTo(mutableSetOf()) { "meal_plan_$it" }
        val keysToRemove = prefs.all.keys.filter { key ->
            key.startsWith("meal_plan_") && key !in keepKeys
        }
        if (keysToRemove.isNotEmpty()) {
            val editor = prefs.edit()
            keysToRemove.forEach(editor::remove)
            editor.apply()
        }
    }

    suspend fun generatePersonalizedPlan(
        date: String,
        profile: NutritionProfile?,
        recommendedNorm: Norm?,
        userNorm: Map<String, Int>?
    ): MealPlan = withContext(Dispatchers.IO) {
        val targets = resolveTargets(recommendedNorm, userNorm)
        val systemPrompt = buildPersonalizedSystemPrompt()
        val userPrompt = buildPersonalizedUserPrompt(
            date = date,
            profile = profile,
            recommendedNorm = recommendedNorm,
            userNorm = userNorm,
            targets = targets
        )

        val meals = requestRussianResponse(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            parse = ::parseMeals
        )

        MealPlan(
            date = date,
            targetCalories = targets.calories,
            targetProtein = targets.protein,
            targetFat = targets.fats,
            targetCarbs = targets.carbs,
            meals = meals
        )
    }

    suspend fun generatePlanFromFridge(
        date: String,
        fridge: List<FridgeProduct>,
        profile: NutritionProfile?,
        recommendedNorm: Norm?,
        userNorm: Map<String, Int>?,
        allowExtraProducts: Boolean,
        goalCalories: Int,
        goalProtein: Int,
        goalFats: Int,
        goalCarbs: Int
    ): MealPlan = withContext(Dispatchers.IO) {
        val systemPrompt = buildFridgeSystemPrompt()
        val userPrompt = buildFridgeUserPrompt(
            date = date,
            fridge = fridge,
            profile = profile,
            recommendedNorm = recommendedNorm,
            userNorm = userNorm,
            allowExtraProducts = allowExtraProducts,
            targets = PlanTargets(
                calories = goalCalories,
                protein = goalProtein,
                fats = goalFats,
                carbs = goalCarbs
            )
        )

        val meals = requestRussianResponse(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            parse = ::parseMeals
        )

        MealPlan(
            date = date,
            targetCalories = goalCalories,
            targetProtein = goalProtein,
            targetFat = goalFats,
            targetCarbs = goalCarbs,
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
        val systemPrompt = buildReplaceMealSystemPrompt()
        val userPrompt = buildReplaceMealUserPrompt(
            date = date,
            mealType = mealType,
            currentPlan = currentPlan,
            profile = profile,
            recommendedNorm = recommendedNorm,
            userNorm = userNorm,
            comment = comment
        )

        requestRussianResponse(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            parse = { root -> parseSingleMeal(root, mealType) }
        )
    }

    private suspend fun <T> requestRussianResponse(
        systemPrompt: String,
        userPrompt: String,
        parse: (JsonObject) -> T
    ): T {
        val first = parse(requestJson(systemPrompt, userPrompt))
        if (!containsLatinResult(first)) return first

        val retry = parse(requestJson(buildStrictRussianPrompt(systemPrompt), buildStrictRetryPrompt(userPrompt)))
        if (!containsLatinResult(retry)) return retry

        throw IllegalStateException("Не удалось получить полностью русскоязычный ответ от модели. Повторите генерацию ещё раз.")
    }

    private suspend fun requestJson(systemPrompt: String, userPrompt: String): JsonObject {
        val requestBody = ChatCompletionRequest(
            model = LlmConfig.MODEL_ID,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            )
        )

        val responseText = client.newCall(
            Request.Builder()
                .url(LlmConfig.BASE_URL.trimEnd('/') + "/chat/completions")
                .post(gson.toJson(requestBody).toRequestBody(mediaTypeJson))
                .build()
        ).execute().use { response ->
            if (!response.isSuccessful) {
                val errorText = response.body?.string().orEmpty()
                throw IllegalStateException("Ошибка LLM API: ${response.code} ${response.message} $errorText")
            }
            response.body?.string() ?: throw IllegalStateException("Пустой ответ от LLM API")
        }

        val chatResponse = gson.fromJson(responseText, ChatCompletionResponse::class.java)
        val contentResponse = chatResponse.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Модель вернула пустой контент")

        return parseJsonObject(contentResponse)
    }

    private fun parseJsonObject(content: String): JsonObject {
        return parseJsonObjectCandidate(content)
            ?: parseJsonObjectCandidate(extractJsonCandidate(content))
            ?: throw IllegalStateException(
                "Не удалось распознать корректный JSON в ответе модели. Получено: ${content.take(300)}"
            )
    }

    private fun parseJsonObjectCandidate(content: String): JsonObject? {
        val normalized = content.trim()
        if (normalized.isBlank()) return null

        return runCatching {
            val reader = JsonReader(StringReader(normalized)).apply { isLenient = true }
            val rootElement = JsonParser.parseReader(reader)
            when {
                rootElement.isJsonObject -> rootElement.asJsonObject
                rootElement.isJsonPrimitive && rootElement.asJsonPrimitive.isString ->
                    parseJsonObjectCandidate(rootElement.asJsonPrimitive.asString)
                else -> null
            }
        }.getOrNull()
    }

    private fun extractJsonCandidate(content: String): String {
        var normalized = content.trim()

        if (normalized.startsWith("```")) {
            normalized = normalized
                .removePrefix("```json")
                .removePrefix("```JSON")
                .removePrefix("```")
                .substringBeforeLast("```")
                .trim()
        }

        val firstBrace = normalized.indexOf('{')
        val lastBrace = normalized.lastIndexOf('}')
        return if (firstBrace >= 0 && lastBrace > firstBrace) {
            normalized.substring(firstBrace, lastBrace + 1)
        } else {
            normalized
        }
    }

    private fun parseMeals(root: JsonObject): List<PlannedMeal> {
        val mealsJson = root.getAsJsonArray("meals")
            ?: throw IllegalStateException("В ответе модели отсутствует массив meals")

        return mealsJson.map { mealElement ->
            val mealObject = mealElement.asJsonObject
            val mealTypeValue = when {
                mealObject.has("mealType") -> mealObject.get("mealType").asString
                mealObject.has("type") -> mealObject.get("type").asString
                else -> throw IllegalStateException("У приёма пищи нет поля mealType/type")
            }
            val mealType = resolveMealType(mealTypeValue)

            val itemsJson = mealObject.getAsJsonArray("items")
                ?: throw IllegalStateException("У приёма пищи отсутствует массив items")

            PlannedMeal(
                type = mealType,
                items = itemsJson.map { parseFoodItem(it.asJsonObject) }
            )
        }
    }

    private fun parseSingleMeal(root: JsonObject, fallbackType: MealType): PlannedMeal {
        val mealTypeValue = when {
            root.has("mealType") -> root.get("mealType").asString
            root.has("type") -> root.get("type").asString
            else -> fallbackType.name
        }
        val mealType = resolveMealType(mealTypeValue, fallbackType)

        val itemsJson = root.getAsJsonArray("items")
            ?: throw IllegalStateException("В ответе замены отсутствует массив items")

        return PlannedMeal(
            type = mealType,
            items = itemsJson.map { parseFoodItem(it.asJsonObject) }
        )
    }

    private fun parseFoodItem(itemObject: JsonObject): PlannedFoodItem {
        val name = itemObject.string("name")
            ?: throw IllegalStateException("В элементе плана отсутствует name")

        return PlannedFoodItem(
            name = name,
            grams = itemObject.int("grams") ?: 0,
            calories = itemObject.int("calories")
                ?: throw IllegalStateException("В элементе плана отсутствует calories"),
            protein = itemObject.int("protein")
                ?: throw IllegalStateException("В элементе плана отсутствует protein"),
            fat = itemObject.int("fat") ?: itemObject.int("fats")
                ?: throw IllegalStateException("В элементе плана отсутствует fat/fats"),
            carbs = itemObject.int("carbs")
                ?: throw IllegalStateException("В элементе плана отсутствует carbs")
        )
    }

    private fun resolveMealType(rawValue: String?, fallback: MealType = MealType.OTHER): MealType {
        val normalized = rawValue
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.replace('ё', 'е')
            ?: return fallback

        return when (normalized) {
            "breakfast", "завтрак", "утро", "morning" -> MealType.BREAKFAST
            "lunch", "обед", "день", "afternoon" -> MealType.LUNCH
            "dinner", "ужин", "вечер", "supper", "evening" -> MealType.DINNER
            "snack", "перекус" -> MealType.SNACK
            "other", "другое", "иной", "прочее" -> MealType.OTHER
            else -> MealType.entries.firstOrNull {
                it.name.equals(normalized, ignoreCase = true)
            } ?: fallback
        }
    }

    private fun buildPersonalizedSystemPrompt(): String = """
        Ты нутрициолог и составляешь реалистичный план питания на один день.
        Возвращай только валидный JSON без Markdown и пояснений.

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

        Правила:
        - Все названия блюд и продуктов пиши только на русском языке.
        - Не используй английские слова, если есть общеупотребимый русский вариант.
        - Для каждого приёма пищи обязательно указывай mealType только из списка BREAKFAST, LUNCH, DINNER, SNACK. Значение OTHER используй только в крайнем случае.
        - Держи калории в пределах +/-5% от цели, а белки, жиры и углеводы в пределах +/-10%.
        - Проверяй арифметику: калории должны примерно соответствовать формуле 4*белки + 9*жиры + 4*углеводы с допуском +/-15%.
        - Используй 3-5 реалистичных приёмов пищи в течение дня.
        - Полностью исключай аллергены.
        - По возможности избегай нелюбимых продуктов.
        - Любимые продукты учитывай только как мягкое предпочтение: можно включать отдельные из них, но нельзя строить весь день только вокруг любимых продуктов и нельзя повторять один и тот же продукт в большинстве приёмов пищи.
        - Делай сочетания продуктов естественными и бытовыми, без странных или однообразных комбинаций.
        - Порции должны быть практичными: обычно от 80 до 450 граммов на продукт.
    """.trimIndent()

    private fun buildFridgeSystemPrompt(): String = """
        Ты нутрициолог и составляешь реалистичный план питания на один день, максимально используя продукты из холодильника.
        Возвращай только валидный JSON без Markdown и пояснений.

        Формат ответа:
        {
          "meals": [
            {
              "mealType": "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK" | "OTHER",
              "items": [
                {
                  "name": "Название блюда",
                  "grams": 250,
                  "calories": 350,
                  "protein": 15,
                  "fat": 10,
                  "carbs": 50
                }
              ]
            }
          ]
        }

        Правила:
        - Все названия блюд и продуктов пиши только на русском языке.
        - Для каждого приёма пищи обязательно указывай mealType только из списка BREAKFAST, LUNCH, DINNER, SNACK. Значение OTHER используй только в крайнем случае.
        - Обязательно используй продукты из холодильника хотя бы в части блюд.
        - Никогда не превышай доступное количество продукта.
        - Если количество указано в штуках, считай 1 штуку примерно как 100 г.
        - Не строй весь день вокруг одного и того же продукта.
        - Если allowExtraProducts = true, можно добавить дополнительные продукты, чтобы реалистично добрать цель.
        - Если allowExtraProducts = false, используй только продукты из холодильника.
        - Итоговые суточные значения должны быть близки к цели: калории +/-5%, БЖУ +/-10%.
        - Проверяй арифметику калорий по макросам с допуском +/-15%.
        - Любимые продукты учитывай только как мягкое предпочтение, а не как обязательную основу каждого приёма пищи.
        - Делай блюда и сочетания продуктов естественными и правдоподобными.
    """.trimIndent()

    private fun buildReplaceMealSystemPrompt(): String = """
        Ты нутрициолог и заменяешь ровно один приём пищи в уже существующем плане.
        Возвращай только валидный JSON без Markdown и пояснений.

        Формат ответа:
        {
          "mealType": "BREAKFAST" | "LUNCH" | "DINNER" | "SNACK" | "OTHER",
          "items": [
            {
              "name": "Название блюда",
              "grams": 250,
              "calories": 350,
              "protein": 15,
              "fat": 10,
              "carbs": 50
            }
          ]
        }

        Правила:
        - Все названия блюд и продуктов пиши только на русском языке.
        - Сохраняй тот же тип приёма пищи.
        - В поле mealType возвращай только одно из значений BREAKFAST, LUNCH, DINNER, SNACK. OTHER используй только в крайнем случае.
        - Держись близко к заменяемому приёму по калориям и БЖУ с допуском +/-15%.
        - Полностью исключай аллергены.
        - По возможности избегай нелюбимых продуктов.
        - Любимые продукты учитывай как пожелание, но не делай замену однообразной или странной по сочетанию продуктов.
        - Замена должна быть практичной и правдоподобной.
    """.trimIndent()

    private fun buildStrictRussianPrompt(basePrompt: String): String = """
        $basePrompt

        Критично:
        - Ответ должен быть полностью на русском языке.
        - Запрещено использовать латиницу в названиях блюд и продуктов.
        - Если сомневаешься, всё равно выбирай русский общеупотребимый вариант.
    """.trimIndent()

    private fun buildStrictRetryPrompt(basePrompt: String): String = """
        $basePrompt

        В прошлый раз модель использовала английские названия. Повтори ответ строго на русском языке.
        Верни только JSON в той же схеме, без дополнительных комментариев.
    """.trimIndent()

    private fun buildPersonalizedUserPrompt(
        date: String,
        profile: NutritionProfile?,
        recommendedNorm: Norm?,
        userNorm: Map<String, Int>?,
        targets: PlanTargets
    ): String = """
        Дата: $date

        Профиль:
        ${profileDescription(profile)}

        Нормы:
        ${recommendedNormText(recommendedNorm)}
        ${userNormText(userNorm)}
        Итоговая цель для этого плана: ${targets.calories} ккал, белки ${targets.protein} г, жиры ${targets.fats} г, углеводы ${targets.carbs} г.

        Составь практичный план питания на весь день для указанной даты.
        Используй итоговую цель как основную.
        Верни только JSON в указанной схеме.
    """.trimIndent()

    private fun buildFridgeUserPrompt(
        date: String,
        fridge: List<FridgeProduct>,
        profile: NutritionProfile?,
        recommendedNorm: Norm?,
        userNorm: Map<String, Int>?,
        allowExtraProducts: Boolean,
        targets: PlanTargets
    ): String = """
        Дата: $date

        Продукты из холодильника:
        ${fridgeDescription(fridge)}

        Цель на день:
        Калории: ${targets.calories}
        Белки: ${targets.protein}
        Жиры: ${targets.fats}
        Углеводы: ${targets.carbs}

        Профиль:
        ${profileDescription(profile)}

        Нормы:
        ${recommendedNormText(recommendedNorm)}
        ${userNormText(userNorm)}

        Разрешено добавлять продукты вне холодильника: ${if (allowExtraProducts) "да" else "нет"}.
        Комментарий: избегай продуктов из списка «Нелюбимые», если это возможно.

        Собери реалистичный план на 3-5 приёмов пищи.
        Верни только JSON.
    """.trimIndent()

    private fun buildReplaceMealUserPrompt(
        date: String,
        mealType: MealType,
        currentPlan: MealPlan,
        profile: NutritionProfile?,
        recommendedNorm: Norm?,
        userNorm: Map<String, Int>?,
        comment: String?
    ): String = """
        Дата: $date

        Нужно заменить приём пищи: ${mealType.name}
        Комментарий пользователя: ${comment?.takeIf { it.isNotBlank() } ?: "без комментария"}

        Профиль:
        ${profileDescription(profile)}

        Нормы:
        ${recommendedNormText(recommendedNorm)}
        ${userNormText(userNorm)}

        Цель на день:
        ${currentPlan.targetCalories} ккал, белки ${currentPlan.targetProtein} г, жиры ${currentPlan.targetFat} г, углеводы ${currentPlan.targetCarbs} г.

        Текущий план за день JSON:
        ${gson.toJson(currentPlan.meals)}

        Замени только указанный приём пищи и верни только JSON.
    """.trimIndent()

    private fun profileDescription(profile: NutritionProfile?): String = profile?.let {
        buildString {
            append("Пол: ${if (it.sex.name == "MALE") "мужской" else "женский"}, ")
            append("возраст ${it.age}, рост ${it.heightCm} см, вес ${it.weightKg} кг, цель ${goalToText(it.goal)}.\n")
            append("Любимые продукты: ${it.favoriteIngredients.joinToString().ifEmpty { "не указаны" }}.\n")
            append("Нелюбимые продукты: ${it.dislikedIngredients.joinToString().ifEmpty { "нет" }}.\n")
            append("Аллергии и ограничения: ${it.allergies.joinToString().ifEmpty { "нет" }}.")
        }
    } ?: "Профиль пользователя недоступен."

    private fun recommendedNormText(norm: Norm?): String = norm?.let {
        "Рекомендованная цель: ${it.calories} ккал, белки ${it.protein} г, жиры ${it.fats} г, углеводы ${it.carbs} г."
    } ?: "Рекомендованная цель недоступна."

    private fun userNormText(userNorm: Map<String, Int>?): String = userNorm?.let {
        "Пользовательская цель: ${it["calories"] ?: "?"} ккал, белки ${it["protein"] ?: "?"} г, жиры ${it["fats"] ?: "?"} г, углеводы ${it["carbs"] ?: "?"} г."
    } ?: "Пользовательская цель не задана."

    private fun fridgeDescription(fridge: List<FridgeProduct>): String =
        if (fridge.isEmpty()) {
            "Список холодильника пуст."
        } else {
            fridge.joinToString(separator = "\n") { product ->
                val unitLabel = if (product.unitType == QuantityUnit.PIECES) "шт." else "г"
                val available = product.availableGrams?.let { "$it $unitLabel" } ?: "количество не указано"
                "${product.name}: на 100 г ${product.calories100}/${product.protein100}/${product.fats100}/${product.carbs100}, доступно $available"
            }
        }

    private fun resolveTargets(recommendedNorm: Norm?, userNorm: Map<String, Int>?): PlanTargets {
        val defaultNorm = mapOf(
            "calories" to 2000,
            "protein" to 120,
            "fats" to 70,
            "carbs" to 250
        )

        return PlanTargets(
            calories = userNorm?.get("calories") ?: recommendedNorm?.calories ?: defaultNorm.getValue("calories"),
            protein = (userNorm?.get("protein") ?: recommendedNorm?.protein ?: defaultNorm.getValue("protein")).coerceAtLeast(0),
            fats = (userNorm?.get("fats") ?: recommendedNorm?.fats ?: defaultNorm.getValue("fats")).coerceAtLeast(0),
            carbs = (userNorm?.get("carbs") ?: recommendedNorm?.carbs ?: defaultNorm.getValue("carbs")).coerceAtLeast(0)
        )
    }

    private fun containsLatinText(value: String): Boolean = LATIN_REGEX.containsMatchIn(value)

    private fun containsLatinResult(plan: Any?): Boolean = when (plan) {
        is PlannedMeal -> plan.items.any { containsLatinText(it.name) }
        is List<*> -> plan.filterIsInstance<PlannedMeal>().any { containsLatinResult(it) }
        else -> false
    }

    private fun goalToText(goal: Goal): String = when (goal) {
        Goal.LOSE_WEIGHT -> "снижение веса"
        Goal.MAINTAIN_WEIGHT -> "поддержание"
        Goal.GAIN_WEIGHT -> "набор массы"
        Goal.DIET -> "лечебное питание"
    }

    companion object {
        private val LATIN_REGEX = Regex("[A-Za-z]")

        @Volatile
        private var instances: MutableMap<String, NutritionAiRepository> = mutableMapOf()

        fun getInstance(context: Context, userId: String): NutritionAiRepository {
            return instances[userId] ?: synchronized(this) {
                instances[userId] ?: NutritionAiRepository(context.applicationContext, userId).also {
                    instances[userId] = it
                }
            }
        }
    }
}

private data class PlanTargets(
    val calories: Int,
    val protein: Int,
    val fats: Int,
    val carbs: Int
)

private fun JsonObject.string(name: String): String? =
    if (has(name) && !get(name).isJsonNull) get(name).asString else null

private fun JsonObject.int(name: String): Int? =
    if (has(name) && !get(name).isJsonNull) get(name).asInt else null
