package com.example.workouttracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class Article(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val cost: Int,
    val purchased: Boolean = false
)

class ArticleViewModel(
    private val trainingViewModel: TrainingViewModel
) : AndroidViewModel(trainingViewModel.getApplication<Application>()) {

    private val _articles = MutableStateFlow(
        listOf(
            Article(title = "Как накачать бицепс", cost = 100),
            Article(title = "Диета для набора массы", cost = 150),
            Article(title = "Тренировка на ноги", cost = 200)
        )
    )
    val articles: StateFlow<List<Article>> = _articles

    private val _balance = MutableStateFlow(0)
    val balance: StateFlow<Int> = _balance

    private val _spent = MutableStateFlow(0)
    val spent: StateFlow<Int> = _spent

    // Доступ к SharedPreferences
    private val prefs = getApplication<Application>().getSharedPreferences("article_prefs", android.content.Context.MODE_PRIVATE)

    init {
        // Загружаем покупки
        _articles.value = _articles.value.map { article ->
            article.copy(purchased = prefs.getBoolean("purchased_${article.id}", article.purchased))
        }
        _spent.value = prefs.getInt("spent", 0)

        // Слушаем тренировки
        viewModelScope.launch {
            trainingViewModel.sessions.collect { sessions ->
                val totalReps = sessions.sumOf { session ->
                    session.exercises.sumOf { ex -> ex.sets * ex.reps }
                }
                _balance.value = totalReps
            }
        }
    }

    fun buyArticle(articleId: UUID): Boolean {
        val article = _articles.value.find { it.id == articleId } ?: return false
        if (_balance.value < article.cost) return false

        _spent.value += article.cost
        _articles.value = _articles.value.map {
            if (it.id == articleId) it.copy(purchased = true) else it
        }

        // Сохраняем
        prefs.edit()
            .putInt("spent", _spent.value)
            .putBoolean("purchased_$articleId", true)
            .apply()

        return true
    }
}