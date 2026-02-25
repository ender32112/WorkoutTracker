package com.example.workouttracker.ui.nutrition

import com.example.workouttracker.data.local.ProductCacheEntity
import com.example.workouttracker.data.local.WorkoutTrackerDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class OpenFoodFactsRepository(
    private val dao: WorkoutTrackerDao,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun lookupByBarcode(barcode: String): ProductLookupResult? = withContext(Dispatchers.IO) {
        dao.getCachedProductByBarcode(barcode)?.let { cached ->
            return@withContext ProductLookupResult(
                barcode = cached.barcode,
                name = cached.name,
                calories100 = cached.calories100,
                protein100 = cached.protein100,
                fats100 = cached.fats100,
                carbs100 = cached.carbs100,
                source = cached.source
            )
        }

        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
            .get()
            .build()

        val response = runCatching { httpClient.newCall(request).execute() }.getOrNull() ?: return@withContext null
        if (!response.isSuccessful) return@withContext null

        val body = response.body?.string().orEmpty()
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return@withContext null
        val product = root.optJSONObject("product") ?: return@withContext null
        val nutriments = product.optJSONObject("nutriments") ?: JSONObject()

        val result = ProductLookupResult(
            barcode = barcode,
            name = product.optString("product_name", "Неизвестный продукт").ifBlank { "Неизвестный продукт" },
            calories100 = nutriments.optDouble("energy-kcal_100g", 0.0).toInt(),
            protein100 = nutriments.optDouble("proteins_100g", 0.0).toInt(),
            fats100 = nutriments.optDouble("fat_100g", 0.0).toInt(),
            carbs100 = nutriments.optDouble("carbohydrates_100g", 0.0).toInt(),
            source = "open_food_facts"
        )

        dao.upsertCachedProduct(
            ProductCacheEntity(
                barcode = result.barcode,
                name = result.name,
                calories100 = result.calories100,
                protein100 = result.protein100,
                fats100 = result.fats100,
                carbs100 = result.carbs100,
                source = result.source
            )
        )

        result
    }
}
