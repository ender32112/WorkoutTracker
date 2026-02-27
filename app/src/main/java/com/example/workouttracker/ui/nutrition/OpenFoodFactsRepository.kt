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
                source = cached.source,
                isPartial = false,
                isSuspicious = false
            )
        }

        val ruResult = lookupFromHost("ru.openfoodfacts.org", barcode)
        val result = ruResult ?: lookupFromHost("world.openfoodfacts.org", barcode) ?: return@withContext null

        dao.upsertCachedProduct(
            ProductCacheEntity(
                barcode = result.barcode,
                name = result.name,
                calories100 = result.calories100 ?: 0f,
                protein100 = result.protein100 ?: 0f,
                fats100 = result.fats100 ?: 0f,
                carbs100 = result.carbs100 ?: 0f,
                source = result.source
            )
        )

        result
    }

    private fun lookupFromHost(host: String, barcode: String): ProductLookupResult? {
        val request = Request.Builder()
            .url("https://$host/api/v2/product/$barcode.json")
            .get()
            .build()

        val response = runCatching { httpClient.newCall(request).execute() }.getOrNull() ?: return null
        response.use {
            if (!it.isSuccessful) return null

            val body = it.body?.string().orEmpty()
            val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
            val product = root.optJSONObject("product") ?: return null
            val nutriments = product.optJSONObject("nutriments") ?: JSONObject()

            val calories = parseEnergyKcal(nutriments)
            val protein = nutriments.optNullableFloat("proteins_100g")
            val fats = nutriments.optNullableFloat("fat_100g")
            val carbs = nutriments.optNullableFloat("carbohydrates_100g")
            val partial = listOf(calories, protein, fats, carbs).any { it == null }

            return ProductLookupResult(
                barcode = barcode,
                name = product.optString("product_name", "Неизвестный продукт").ifBlank { "Неизвестный продукт" },
                calories100 = calories,
                protein100 = protein,
                fats100 = fats,
                carbs100 = carbs,
                source = "open_food_facts_$host",
                isPartial = partial,
                isSuspicious = isSuspicious(calories, protein, fats, carbs)
            )
        }
    }

    private fun parseEnergyKcal(nutriments: JSONObject): Float? {
        nutriments.optNullableFloat("energy-kcal_100g")?.let { return it }
        nutriments.optNullableFloat("energy_100g")?.let { return it / 4.184f }

        val protein = nutriments.optNullableFloat("proteins_100g")
        val fats = nutriments.optNullableFloat("fat_100g")
        val carbs = nutriments.optNullableFloat("carbohydrates_100g")
        if (protein == null || fats == null || carbs == null) return null
        return protein * 4f + carbs * 4f + fats * 9f
    }

    private fun isSuspicious(calories: Float?, protein: Float?, fats: Float?, carbs: Float?): Boolean {
        val atwater = (protein ?: 0f) * 4f + (carbs ?: 0f) * 4f + (fats ?: 0f) * 9f
        if (calories == null || atwater <= 0f) return false
        val diff = kotlin.math.abs(calories - atwater)
        return diff > 80f
    }
}
