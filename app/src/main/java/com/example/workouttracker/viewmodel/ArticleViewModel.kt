package com.example.workouttracker.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.ui.articles.Article
import com.example.workouttracker.ui.articles.stableIdFromSlug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.max

/**
 * Баланс = «заработано» (из тренировок) – «потрачено» (prefs).
 * Так мы НЕ теряем покупки при пересчёте тренировок.
 */
class ArticleViewModel(
    private val trainingViewModel: TrainingViewModel,
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("article_prefs", Context.MODE_PRIVATE)

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles

    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance

    private val K_SPENT = "points_spent"

    init {
        loadArticles()
        observeTrainingSessions()
    }

    private fun loadArticles() {
        // Статичный каталог со стабильными слагами
        val catalog = listOf(
            Article(
                id = stableIdFromSlug("biceps"),
                slug = "biceps",
                title = "Как накачать бицепс",
                date = "15 мар 2025",
                content = """
                    Бицепс — это мышца, которую хотят все. Но как её накачать?

                    1. Подъёмы штанги стоя — 4×10
                    2. Молотковый гриф — 3×12
                    3. Концентрированные подъёмы — 3×15

                    Главное — техника и прогрессия веса!
                """.trimIndent(),
                cost = 100
            ),
            Article(
                id = stableIdFromSlug("mass-diet"),
                slug = "mass-diet",
                title = "Диета для набора массы",
                date = "10 мар 2025",
                content = """
                    Чтобы расти — нужно есть больше, чем тратишь.

                    • Калории: +500 к норме
                    • Белок: 2 г/кг веса
                    • Углеводы: рис, овсянка, картофель
                    • Жиры: орехи, авокадо, масло

                    Ешь 5–6 раз в день!
                """.trimIndent(),
                cost = 150
            ),
            Article(
                id = stableIdFromSlug("legs-training"),
                slug = "legs-training",
                title = "Тренировка на ноги",
                date = "05 мар 2025",
                content = """
                    Ноги — 50% тела. Не пропускай!

                    1. Приседания — 4×8–12
                    2. Румынская тяга — 4×10
                    3. Жим ногами — 3×15
                    4. Выпады — 3×12 на ногу

                    Отдых между подходами — 2–3 минуты.
                """.trimIndent(),
                cost = 200
            )
        )

        // Подтягиваем покупки из prefs по стабильным UUID
        val list = catalog.map { art ->
            val purchased = prefs.getBoolean("purchased_${art.id}", false)
            art.copy(purchased = purchased)
        }
        _articles.value = list
    }

    /** Подписываемся на тренировки и считаем «заработано» очков */
    private fun observeTrainingSessions() {
        viewModelScope.launch {
            trainingViewModel.sessions.collectLatest { sessions ->
                // === Новый подсчёт: очки за ОТРАБОТАННЫЙ ОБЪЁМ ===
                // 1 очко за каждые 50 кг суммарного тоннажа за ВСЕ тренировки.
                // (Можете менять делитель — 50/100/200 — под нужный темп прогресса.)
                val totalVolume = sessions.sumOf { it.totalVolume } // totalVolume уже = Σ(sets*reps*weight)
                val earned = (totalVolume / 50).coerceAtLeast(0)     // <- вот тут меняете "50" при желании

                val spent = prefs.getInt(K_SPENT, 0)
                _balance.value = (earned - spent).coerceAtLeast(0)
            }
        }
    }


    /** Пытаемся списать очки (пишем в prefs только «потрачено»). */
    private fun trySpend(points: Int): Boolean {
        val current = _balance.value
        if (current < points) return false
        val spent = prefs.getInt(K_SPENT, 0) + points
        prefs.edit().putInt(K_SPENT, spent).apply()
        _balance.value = current - points
        return true
    }

    fun buyArticle(articleId: UUID): Boolean {
        val current = _articles.value
        val idx = current.indexOfFirst { it.id == articleId }
        if (idx == -1) return false
        val art = current[idx]
        if (art.purchased) return false

        if (!trySpend(art.cost)) return false

        val updated = art.copy(purchased = true)
        _articles.value = current.toMutableList().apply { set(idx, updated) }

        prefs.edit().putBoolean("purchased_$articleId", true).apply()
        return true
    }
}
