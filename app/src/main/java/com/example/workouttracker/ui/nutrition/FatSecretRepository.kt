package com.example.workouttracker.ui.nutrition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject

class FatSecretRepository(
    private val consumerKey: String,
    private val consumerSecret: String,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun lookupProduct(barcode: String): LookupAttempt<ProductLookupResult> = withContext(Dispatchers.IO) {
        if (consumerKey.isBlank() || consumerSecret.isBlank()) {
            return@withContext LookupAttempt(error = "FatSecret не настроен: отсутствуют consumer key/secret")
        }

        // Мы используем OAuth1 — просто пробуем один вызов (OAuth1 не требует token refresh)
        lookupWithOAuth1(barcode)
    }

    private suspend fun lookupWithOAuth1(barcode: String): LookupAttempt<ProductLookupResult> {
        val productIdResult = getProductIdForBarcode(barcode)
        val productId = productIdResult.data
            ?: return LookupAttempt(error = productIdResult.error ?: "FatSecret не вернул food_id")

        val apiParams = mapOf(
            "method" to "food.get.v4",
            "food_id" to productId,
            "format" to "json"
        )

        val baseUrl = "https://platform.fatsecret.com/rest/server.api"
        val bodyResult = runCatching {
            OAuth1FatSecret.callFatSecret(httpClient, baseUrl, apiParams, consumerKey, consumerSecret)
        }
        val body = bodyResult.getOrNull()
            ?: return LookupAttempt(error = "Ошибка FatSecret food.get.v4: ${bodyResult.exceptionOrNull()?.message.orEmpty()}")

        val root = runCatching { JSONObject(body) }.getOrNull()
            ?: return LookupAttempt(error = "FatSecret вернул невалидный JSON для food.get.v4")
        val food = root.optJSONObject("food")
            ?: return LookupAttempt(error = "FatSecret не вернул объект food")
        val servings = food.optJSONObject("servings")?.optJSONArray("serving")
        val serving = if (servings != null && servings.length() > 0) {
            servings.optJSONObject(0)
        } else {
            food.optJSONObject("servings")?.optJSONObject("serving")
        } ?: return LookupAttempt(error = "FatSecret не вернул данные порции")

        val calories = serving.optNullableFloat("calories")
        val protein = serving.optNullableFloat("protein")
        val fats = serving.optNullableFloat("fat")
        val carbs = serving.optNullableFloat("carbohydrate")
        val partial = listOf(calories, protein, fats, carbs).any { it == null }

        return LookupAttempt(
            data = ProductLookupResult(
                barcode = barcode,
                name = food.optString("food_name", "Неизвестный продукт"),
                calories100 = calories,
                protein100 = protein,
                fats100 = fats,
                carbs100 = carbs,
                source = "fatsecret",
                isPartial = partial,
                isSuspicious = false
            )
        )
    }

    private suspend fun getProductIdForBarcode(barcode: String): LookupAttempt<String> {
        val apiParams = mapOf(
            "method" to "food.find_id_for_barcode",
            "barcode" to barcode,
            "format" to "json"
        )
        val baseUrl = "https://platform.fatsecret.com/rest/server.api"
        val bodyResult = runCatching {
            OAuth1FatSecret.callFatSecret(httpClient, baseUrl, apiParams, consumerKey, consumerSecret)
        }
        val body = bodyResult.getOrNull()
            ?: return LookupAttempt(error = "Ошибка FatSecret food.find_id_for_barcode: ${bodyResult.exceptionOrNull()?.message.orEmpty()}")

        val root = runCatching { JSONObject(body) }.getOrNull()
            ?: return LookupAttempt(error = "FatSecret вернул невалидный JSON для food.find_id_for_barcode")
        val foodId = root.optString("food_id", null)
            ?: return LookupAttempt(error = "Продукт не найден в FatSecret по штрихкоду")
        return LookupAttempt(data = foodId)
    }
}
