package com.example.workouttracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.ui.articles.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID



class ArticleViewModel(
    private val trainingViewModel: TrainingViewModel,
    application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("article_prefs", android.content.Context.MODE_PRIVATE)

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles

    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance

    init {
        loadArticles()
        observeTrainingSessions()
    }

    private fun loadArticles() {
        val savedPurchases = prefs.all
            .filterKeys { it.startsWith("purchased_") }
            .mapKeys { UUID.fromString(it.key.removePrefix("purchased_")) }

        _articles.value = listOf(
            Article(
                title = "Как накачать бицепс",
                date = "15 мар 2025",
                content = "Бицепс — это мышца, которую хотят все. Но как её накачать?\n\n" +
                        "1. Делай подъёмы штанги стоя — 4×10\n" +
                        "2. Молотковый гриф — 3×12\n" +
                        "3. Концентрированные подъёмы — 3×15\n\n" +
                        "Главное — техника и прогрессия веса!",
                cost = 100
            ),
            Article(
                title = "Диета для набора массы",
                date = "10 мар 2025",
                content = "Чтобы расти — нужно есть больше, чем тратишь.\n\n" +
                        "• Калории: +500 к норме\n" +
                        "• Белок: 2г на кг веса\n" +
                        "• Углеводы: рис, овсянка, картофель\n" +
                        "• Жиры: орехи, авокадо, масло\n\n" +
                        "Ешь 5–6 раз в день!",
                cost = 150
            ),
            Article(
                title = "Тренировка на ноги",
                date = "05 мар 2025",
                content = "Ноги — 50% тела. Не пропускай!\n\n" +
                        "1. Приседания — 4×8–12\n" +
                        "2. Румынская тяга — 4×10\n" +
                        "3. Жим ногами — 3×15\n" +
                        "4. Выпады — 3×12 на ногу\n\n" +
                        "Отдых между подходами — 2–3 минуты.",
                cost = 200
            )
        ).map { article ->
            article.copy(purchased = savedPurchases[article.id] == true)
        }
    }

    private fun observeTrainingSessions() {
        viewModelScope.launch {
            trainingViewModel.sessions.collectLatest { sessions ->
                val totalReps = sessions.sumOf { session ->
                    session.exercises.sumOf { ex -> ex.sets * ex.reps }
                }
                _balance.value = totalReps
            }
        }
    }

    fun spendPoints(points: Int): Boolean {
        return if (_balance.value >= points) {
            _balance.value -= points
            true
        } else {
            false
        }
    }

    fun buyArticle(articleId: UUID): Boolean {
        val article = _articles.value.find { it.id == articleId } ?: return false
        if (article.purchased) return false

        if (!spendPoints(article.cost)) return false  // ← Снимаем баллы

        _articles.value = _articles.value.map {
            if (it.id == articleId) it.copy(purchased = true) else it
        }

        prefs.edit()
            .putBoolean("purchased_$articleId", true)
            .apply()

        return true
    }
}