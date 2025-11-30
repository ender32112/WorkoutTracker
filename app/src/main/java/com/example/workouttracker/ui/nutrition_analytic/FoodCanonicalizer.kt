package com.example.workouttracker.ui.nutrition_analytic

import android.content.Context
import android.content.SharedPreferences
import com.example.workouttracker.llm.NutritionAiRepository
import java.util.Locale

class FoodCanonicalizer(
    context: Context,
    private val aiRepository: NutritionAiRepository
) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("food_canonical_cache", Context.MODE_PRIVATE)

    private val localDictionary: Map<String, String> = mapOf(
        "куриная грудка" to "курица",
        "филе курицы" to "курица",
        "куриное филе" to "курица",
        "курица" to "курица",
        "спагетти" to "макароны",
        "паста" to "макароны",
        "макароны" to "макароны",
        "йогурт натуральный" to "йогурт",
        "греческий йогурт" to "йогурт",
        "йогурт" to "йогурт",
        "творог обезжиренный" to "творог",
        "творог 5%" to "творог",
        "творог" to "творог"
    )

    private fun normalizeLocal(name: String): String {
        val lower = name.lowercase(Locale.getDefault()).trim()
        if (lower.isBlank()) return ""
        val cleaned = lower
            .replace("[\\p{Punct}]+".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
        return localDictionary[cleaned] ?: cleaned
    }

    private fun readFromCache(key: String): String? = prefs.getString(key, null)

    private fun saveToCache(key: String, canonical: String) {
        prefs.edit().putString(key, canonical).apply()
    }

    suspend fun canonicalize(name: String): String {
        val raw = name.trim()
        if (raw.isEmpty()) return ""

        val localNormalized = normalizeLocal(raw)
        if (localDictionary.containsKey(localNormalized)) {
            return localDictionary.getValue(localNormalized)
        }

        readFromCache(localNormalized)?.let { return it }

        return runCatching {
            val aiName = aiRepository.canonicalizeFoodName(localNormalized)
            val result = aiName.trim().ifBlank { localNormalized }
            saveToCache(localNormalized, result)
            result
        }.getOrElse {
            localNormalized
        }
    }
}
