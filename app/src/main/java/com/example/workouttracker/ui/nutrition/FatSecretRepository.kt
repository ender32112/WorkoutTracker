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
    suspend fun lookupProduct(barcode: String): ProductLookupResult? = withContext(Dispatchers.IO) {
        // Мы используем OAuth1 — просто пробуем один вызов (OAuth1 не требует token refresh)
        lookupWithOAuth1(barcode)
    }

    private suspend fun lookupWithOAuth1(barcode: String): ProductLookupResult? {
        val productId = getProductIdForBarcode(barcode) ?: return null

        val apiParams = mapOf(
            "method" to "food.get.v4",
            "food_id" to productId,
            "format" to "json"
        )

        val baseUrl = "https://platform.fatsecret.com/rest/server.api"
        val body = runCatching {
            OAuth1FatSecret.callFatSecret(httpClient, baseUrl, apiParams, consumerKey, consumerSecret)
        }.getOrNull() ?: return null

        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        val food = root.optJSONObject("food") ?: return null
        val servings = food.optJSONObject("servings")?.optJSONArray("serving")
        val serving = if (servings != null && servings.length() > 0) {
            servings.optJSONObject(0)
        } else {
            food.optJSONObject("servings")?.optJSONObject("serving")
        } ?: return null

        val calories = serving.optNullableFloat("calories")
        val protein = serving.optNullableFloat("protein")
        val fats = serving.optNullableFloat("fat")
        val carbs = serving.optNullableFloat("carbohydrate")
        val partial = listOf(calories, protein, fats, carbs).any { it == null }

        return ProductLookupResult(
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
    }

    private suspend fun getProductIdForBarcode(barcode: String): String? {
        val apiParams = mapOf(
            "method" to "food.find_id_for_barcode",
            "barcode" to barcode,
            "format" to "json"
        )
        val baseUrl = "https://platform.fatsecret.com/rest/server.api"
        val body = runCatching {
            OAuth1FatSecret.callFatSecret(httpClient, baseUrl, apiParams, consumerKey, consumerSecret)
        }.getOrNull() ?: return null

        val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
        return root.optString("food_id", null)
    }
}
