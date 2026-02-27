package com.example.workouttracker.ui.nutrition

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject

class FatSecretRepository(
    private val consumerKey: String,
    private val consumerSecret: String,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    private val tag = "FatSecretRepository"

    suspend fun lookupProduct(barcode: String): ProductLookupResult? = withContext(Dispatchers.IO) {
        if (consumerKey.isBlank() || consumerSecret.isBlank()) {
            Log.d(tag, "lookupProduct: consumerKey/consumerSecret is blank")
            return@withContext null
        }

        lookupWithOAuth1(barcode)
    }

    private suspend fun callFatSecretSafe(apiParams: Map<String, String>): String? = withContext(Dispatchers.IO) {
        val baseUrl = "https://platform.fatsecret.com/rest/server.api"
        Log.d(tag, "calling OAuth1FatSecret for method=${apiParams["method"]} params=$apiParams")
        return@withContext runCatching {
            OAuth1FatSecret.callFatSecret(httpClient, baseUrl, apiParams, consumerKey, consumerSecret)
        }.onFailure { e ->
            Log.d(tag, "callFatSecretSafe: exception for method=${apiParams["method"]}: ${e.message}")
        }.getOrNull()
    }

    private suspend fun lookupWithOAuth1(barcode: String): ProductLookupResult? {
        Log.d(tag, "lookupWithOAuth1: start for barcode=$barcode")
        val productId = getProductIdForBarcode(barcode)
        if (productId == null) {
            Log.d(tag, "lookupWithOAuth1: no productId for barcode $barcode")
            return null
        }

        val apiParams = mapOf(
            "method" to "food.get.v4",
            "food_id" to productId,
            "format" to "json"
        )

        Log.d(tag, "calling OAuth1FatSecret for food.get productId=$productId")
        val body = callFatSecretSafe(apiParams)

        if (body.isNullOrEmpty()) {
            Log.d(tag, "lookupWithOAuth1: empty food.get response for productId $productId")
            return null
        }

        Log.d(tag, "food.get response for productId $productId: $body")

        val root = runCatching { JSONObject(body) }
            .onFailure {
                Log.d(tag, "lookupWithOAuth1: failed to parse food.get JSON for productId $productId")
            }
            .getOrNull()
            ?: return null

        root.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }?.let {
            Log.d(tag, "lookupWithOAuth1: food.get error for productId $productId: $it")
            return null
        }

        val food = root.optJSONObject("food") ?: run {
            Log.d(tag, "lookupWithOAuth1: no food object in food.get response for productId $productId")
            return null
        }

        val servingsNode = food.optJSONObject("servings")
        val servingsArray = servingsNode?.optJSONArray("serving")
        val serving = if (servingsArray != null && servingsArray.length() > 0) {
            servingsArray.optJSONObject(0)
        } else {
            servingsNode?.optJSONObject("serving")
        } ?: run {
            Log.d(tag, "lookupWithOAuth1: no serving data in food.get response for productId $productId")
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
        val tryMethods = listOf(
            mapOf("method" to "food.find_id_for_barcode", "barcode" to barcode, "format" to "json"),
            mapOf("method" to "foods.find_id_for_barcode", "barcode" to barcode, "format" to "json"),
            mapOf("method" to "foods.search", "search_expression" to barcode, "format" to "json"),
            mapOf("method" to "food.search", "search_expression" to barcode, "format" to "json")
        )

        for (apiParams in tryMethods) {
            val method = apiParams["method"] ?: "unknown"
            val body = callFatSecretSafe(apiParams)
            if (body.isNullOrEmpty()) {
                Log.d(tag, "getProductIdForBarcode: empty response for method=$method barcode=$barcode")
                continue
            }

            Log.d(tag, "find_id_for_barcode response for $barcode (method=$method): $body")

            val root = runCatching { JSONObject(body) }
                .onFailure {
                    Log.d(tag, "getProductIdForBarcode: failed to parse JSON for method=$method barcode=$barcode")
                }
                .getOrNull()
                ?: continue

            root.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }?.let {
                Log.d(tag, "getProductIdForBarcode method=$method returned error: $it")
            }

            root.optString("food_id", null)?.takeIf { it.isNotBlank() }?.let { return it }
            root.optJSONObject("food")?.optString("food_id")?.takeIf { it.isNotBlank() }?.let { return it }

            root.optJSONObject("foods")?.optJSONArray("food")?.let { arr ->
                if (arr.length() > 0) {
                    arr.optJSONObject(0)?.optString("food_id")?.takeIf { it.isNotBlank() }?.let { return it }
                    arr.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }

            root.optJSONObject("foods")?.optJSONObject("food")?.optString("food_id")
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }

            scanForFoodId(root)?.let { return it }
        }

        Log.d(tag, "getProductIdForBarcode: no food_id found in any response for $barcode")
        return null
    }

    private fun scanForFoodId(root: JSONObject): String? {
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val node = root.opt(key)
            if (node is JSONObject) {
                node.optString("food_id", null)?.takeIf { it.isNotBlank() }?.let { return it }
                node.optJSONObject("food")?.optString("food_id")?.takeIf { it.isNotBlank() }?.let { return it }
                node.optJSONArray("food")?.let { arr ->
                    if (arr.length() > 0) {
                        arr.optJSONObject(0)?.optString("food_id")?.takeIf { it.isNotBlank() }?.let { return it }
                        arr.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }?.let { return it }
                    }
                }
            }
        }
        return null
    }
}
