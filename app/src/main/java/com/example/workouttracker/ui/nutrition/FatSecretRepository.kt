package com.example.workouttracker.ui.nutrition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class FatSecretRepository(
    private val tokenManager: FatSecretTokenManager,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun lookupProduct(barcode: String): ProductLookupResult? = withContext(Dispatchers.IO) {
        val firstTry = lookupWithToken(barcode, tokenManager.getValidAccessToken() ?: return@withContext null)
        if (firstTry?.first == 401) {
            val refreshed = tokenManager.getValidAccessToken(forceRefresh = true) ?: return@withContext null
            return@withContext lookupWithToken(barcode, refreshed)?.second
        }
        firstTry?.second
    }

    private fun lookupWithToken(barcode: String, token: String): Pair<Int, ProductLookupResult?>? {
        val productId = getProductIdForBarcode(barcode, token) ?: return null
        val url = "https://platform.fatsecret.com/rest/server.api".toHttpUrl().newBuilder()
            .addQueryParameter("method", "food.get.v4")
            .addQueryParameter("food_id", productId)
            .addQueryParameter("format", "json")
            .build()

        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .build()
        val response = runCatching { httpClient.newCall(request).execute() }.getOrNull() ?: return null
        if (response.code == 401) return 401 to null
        if (!response.isSuccessful) return response.code to null

        val root = runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrNull() ?: return response.code to null
        val food = root.optJSONObject("food") ?: return response.code to null
        val servings = food.optJSONObject("servings")?.optJSONArray("serving")
        val serving = if (servings != null && servings.length() > 0) servings.optJSONObject(0) else food.optJSONObject("servings")?.optJSONObject("serving")
            ?: return response.code to null

        val calories = serving.optNullableFloat("calories")
        val protein = serving.optNullableFloat("protein")
        val fats = serving.optNullableFloat("fat")
        val carbs = serving.optNullableFloat("carbohydrate")
        val partial = listOf(calories, protein, fats, carbs).any { it == null }

        return response.code to ProductLookupResult(
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

    private fun getProductIdForBarcode(barcode: String, token: String): String? {
        val url = "https://platform.fatsecret.com/rest/server.api".toHttpUrl().newBuilder()
            .addQueryParameter("method", "food.find_id_for_barcode")
            .addQueryParameter("barcode", barcode)
            .addQueryParameter("format", "json")
            .build()

        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer $token")
            .build()
        val response = runCatching { httpClient.newCall(request).execute() }.getOrNull() ?: return null
        if (!response.isSuccessful) return null
        val root = runCatching { JSONObject(response.body?.string().orEmpty()) }.getOrNull() ?: return null
        return root.optString("food_id", null)
    }
}
