package com.example.workouttracker.ui.nutrition_analytic

import android.content.Context
import android.content.SharedPreferences
import com.example.workouttracker.llm.NutritionAiRepository
import java.text.Normalizer
import java.util.Locale

class FoodCanonicalizer(
    context: Context,
    private val aiRepository: NutritionAiRepository
) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("food_canonical_cache", Context.MODE_PRIVATE)

    /**
     * Версия правил. При изменении алгоритма ключи в кеше сменятся, чтобы пересчитать старые записи.
     */
    private val cacheVersion = 2

    /**
     * Расширенный словарь синонимов и форм. Значение — каноническое имя.
     */
    private val localDictionary: Map<String, String> = mapOf(
        // Птица и мясо
        "куриная грудка" to "курица",
        "филе курицы" to "курица",
        "куриное филе" to "курица",
        "курица" to "курица",
        "куриные бедра" to "курица",
        "индейка" to "индейка",
        "филе индейки" to "индейка",
        "говядина" to "говядина",
        "фарш говяжий" to "говядина",

        // Гарниры и крупы
        "спагетти" to "макароны",
        "паста" to "макароны",
        "макароны" to "макароны",
        "рис" to "рис",
        "рис басмати" to "рис",
        "гречка" to "гречка",

        // Молочка
        "йогурт натуральный" to "йогурт",
        "греческий йогурт" to "йогурт",
        "йогурт" to "йогурт",
        "творог обезжиренный" to "творог",
        "творог 5%" to "творог",
        "творог" to "творог",
        "кефир" to "кефир",

        // Фрукты и овощи
        "яблоки" to "яблоко",
        "яблоко" to "яблоко",
        "бананы" to "банан",
        "банан" to "банан",
        "огурцы" to "огурец",
        "огурец" to "огурец",
        "помидоры" to "помидор",
        "помидор" to "помидор",

        // Разное
        "яйца" to "яйцо",
        "яйцо" to "яйцо",
        "овсянка" to "овсянка",
        "овсяные хлопья" to "овсянка"
    )

    private val knownCanonicalNames: Set<String> = localDictionary.values.toSet()

    private val unitRegex = "(?i)(кг|г|гр|грамм|мг|л|мл|шт|pieces|pcs)".toRegex()
    private val quantityRegex = "(\\d+[.,]?\\d*)\\s*${unitRegex.pattern}".toRegex()

    private data class PreprocessResult(
        val cleaned: String,
        val descriptors: List<String>
    )

    private fun preprocess(raw: String): PreprocessResult {
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase(Locale.getDefault())
            .trim()

        val descriptors = mutableListOf<String>()

        val withoutQuantities = quantityRegex.replace(normalized) { matchResult ->
            descriptors.add(matchResult.value)
            ""
        }

        val cleaned = withoutQuantities
            .replace(unitRegex, " ")
            .replace("[\\p{Punct}]+".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

        return PreprocessResult(cleaned, descriptors)
    }

    private fun heuristicLemma(word: String): String {
        val lowered = word.lowercase(Locale.getDefault())
        val exceptions = mapOf(
            "дети" to "ребенок",
            "люди" to "человек"
        )
        exceptions[lowered]?.let { return it }

        val endings = listOf("ов", "ев", "ей", "ами", "ями", "ями", "ями", "ями", "ами", "ями")
        endings.firstOrNull { lowered.endsWith(it) }?.let { suffix ->
            return lowered.removeSuffix(suffix)
        }

        return when {
            lowered.endsWith("ы") || lowered.endsWith("и") -> lowered.dropLast(1)
            lowered.endsWith("а") || lowered.endsWith("я") -> lowered.dropLast(1)
            else -> lowered
        }
    }

    private fun applyDictionary(name: String): String? {
        localDictionary[name]?.let { return it }
        val lemma = heuristicLemma(name)
        return localDictionary[lemma]
    }

    private fun readFromCache(key: String): String? = prefs.getString(cacheKey(key), null)

    private fun saveToCache(key: String, canonical: String) {
        prefs.edit().putString(cacheKey(key), canonical).apply()
    }

    private fun cacheKey(raw: String): String = "v$cacheVersion|$raw"

    private fun jaccardSimilarity(aTokens: Set<String>, bTokens: Set<String>): Double {
        if (aTokens.isEmpty() || bTokens.isEmpty()) return 0.0
        val intersection = aTokens.intersect(bTokens).size.toDouble()
        val union = aTokens.union(bTokens).size.toDouble()
        return intersection / union
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val costs = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            var lastValue = i
            costs[0] = i + 1
            for (j in b.indices) {
                val newValue = if (a[i] == b[j]) lastValue else 1 + minOf(lastValue, costs[j], costs[j + 1])
                lastValue = costs[j + 1]
                costs[j + 1] = newValue
            }
        }
        return costs[b.length]
    }

    private fun fuzzyMatch(name: String): String? {
        val tokens = name.split(" ").filter { it.isNotBlank() }.toSet()
        val candidates = knownCanonicalNames

        var best: Pair<String, Double>? = null
        candidates.forEach { candidate ->
            val distance = levenshteinDistance(name, candidate)
            val distanceScore = 1.0 - (distance.toDouble() / maxOf(name.length, candidate.length))
            val candidateTokens = candidate.split(" ").filter { it.isNotBlank() }.toSet()
            val tokenScore = jaccardSimilarity(tokens, candidateTokens)
            val score = (distanceScore + tokenScore) / 2.0
            if (score > (best?.second ?: 0.0)) {
                best = candidate to score
            }
        }

        val threshold = 0.7
        return best?.takeIf { it.second >= threshold }?.first
    }

    private fun semanticMatch(name: String): String? {
        val tokens = name.split(" ").filter { it.isNotBlank() }.sorted()
        val signature = tokens.joinToString(" ")
        val candidates = knownCanonicalNames
        return candidates.firstOrNull { candidate ->
            val candidateSignature = candidate.split(" ").filter { it.isNotBlank() }.sorted().joinToString(" ")
            candidateSignature == signature
        }
    }

    suspend fun canonicalize(name: String): String {
        val raw = name.trim()
        if (raw.isEmpty()) return ""

        val preprocess = preprocess(raw)
        val base = preprocess.cleaned
        if (base.isEmpty()) return ""

        applyDictionary(base)?.let { return it }

        readFromCache(base)?.let { return it }

        val semanticMatch = semanticMatch(base)
        if (semanticMatch != null) {
            saveToCache(base, semanticMatch)
            return semanticMatch
        }

        val fuzzy = fuzzyMatch(base)
        if (fuzzy != null) {
            saveToCache(base, fuzzy)
            return fuzzy
        }

        return runCatching {
            val aiName = aiRepository.canonicalizeFoodName(base)
            val result = aiName.trim().ifBlank { base }
            saveToCache(base, result)
            result
        }.getOrElse {
            base
        }
    }
}
