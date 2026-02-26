package com.example.workouttracker.ui.nutrition

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject

class FatSecretRepository(
    private val consumerKey: String,
    private val consumerSecret: String,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun lookupProduct(barcode: String): ProductLookupResult? = withContext(Dispatchers.IO) {
        if (consumerKey.isBlank() || consumerSecret.isBlank()) {
            Log.d("FatSecretRepository", "lookupProduct: consumerKey/consumerSecret is blank")
            return@withContext null
        }

        lookupWithOAuth1(barcode)
    }

    private suspend fun lookupWithOAuth1(barcode: String): ProductLookupResult? {
        Log.d("FatSecretRepository", "lookupWithOAuth1: start for barcode=$barcode")
        val productId = getProductIdForBarcode(barcode)
        if (productId == null) {
            Log.d("FatSecretRepository", "lookupWithOAuth1: no productId for barcode $barcode")
            return null
        }

        val apiParams = mapOf(
            "method" to "food.get.v4",
            "food_id" to productId,
            "format" to "json"
        )

        val baseUrl = "https://platform.fatsecret.com/rest/server.api"
        Log.d("FatSecretRepository", "calling OAuth1FatSecret for method=food.get.v4 productId=$productId")
        val body = runCatching {
            OAuth1FatSecret.callFatSecret(httpClient, baseUrl, apiParams, consumerKey, consumerSecret)
        }.onFailure {
            Log.d("FatSecretRepository", "lookupWithOAuth1: food.get request failed for productId $productId: ${it.message}")
        }.getOrNull()

        if (body.isNullOrEmpty()) {
            Log.d("FatSecretRepository", "lookupWithOAuth1: empty food.get response for productId $productId")
            return null
        }

        Log.d("FatSecretRepository", "food.get response for productId $productId: $body")

        val root = runCatching { JSONObject(body) }
            .onFailure {
                Log.d("FatSecretRepository", "lookupWithOAuth1: failed to parse food.get JSON for productId $productId")
            }
            .getOrNull()
            ?: return null

        val food = root.optJSONObject("food") ?: run {
            Log.d("FatSecretRepository", "lookupWithOAuth1: no food object in food.get response for productId $productId")
            return null
        }

        val servingsNode = food.optJSONObject("servings")
        val servingsArray = servingsNode?.optJSONArray("serving")
        val serving = if (servingsArray != null && servingsArray.length() > 0) {
            servingsArray.optJSONObject(0)
        } else {
            servingsNode?.optJSONObject("serving")
        } ?: run {
            Log.d("FatSecretRepository", "lookupWithOAuth1: no serving data in food.get response for productId $productId")
            return null
        }

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

        Log.d("FatSecretRepository", "calling OAuth1FatSecret for method=food.find_id_for_barcode barcode=$barcode")
        val body = runCatching {
            OAuth1FatSecret.callFatSecret(httpClient, baseUrl, apiParams, consumerKey, consumerSecret)
        }.onFailure {
            Log.d("FatSecretRepository", "getProductIdForBarcode: request failed for barcode $barcode: ${it.message}")
        }.getOrNull()

        if (body.isNullOrEmpty()) {
            Log.d("FatSecretRepository", "getProductIdForBarcode: empty response for barcode $barcode")
            return null
        }

        Log.d("FatSecretRepository", "find_id_for_barcode response for $barcode: $body")

        val root = runCatching { JSONObject(body) }
            .onFailure {
                Log.d("FatSecretRepository", "getProductIdForBarcode: failed to parse JSON for barcode $barcode")
            }
            .getOrNull()
            ?: return null

        root.optString("food_id", null)?.takeIf { it.isNotBlank() }?.let { return it }
        root.optJSONObject("food")?.optString("food_id")?.takeIf { it.isNotBlank() }?.let { return it }

        root.optJSONObject("foods")?.optJSONArray("food")?.let { arr ->
            if (arr.length() > 0) {
                arr.optJSONObject(0)?.optString("food_id")?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }

        root.optJSONObject("foods")?.optJSONObject("food")?.optString("food_id")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        findFoodIdInNode(root)?.let { return it }

        Log.d("FatSecretRepository", "no food_id found in find_id_for_barcode response for $barcode")
        return null
    }

    private fun findFoodIdInNode(node: Any?): String? {
        return when (node) {
            is JSONObject -> {
                node.optString("food_id", null)?.takeIf { it.isNotBlank() }
                    ?: findFoodIdInNode(node.opt("food"))
                    ?: findFoodIdInNode(node.opt("foods"))
                    ?: run {
                        val keys = node.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            findFoodIdInNode(node.opt(key))?.let { return it }
                        }
                        null
                    }
            }

            is JSONArray -> {
                for (index in 0 until node.length()) {
                    findFoodIdInNode(node.opt(index))?.let { return it }
                }
                null
            }

            else -> null
        }
    }
}
