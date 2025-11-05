package com.example.workouttracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import java.util.UUID
import com.example.workouttracker.ui.achievements.Achievement

class AchievementViewModel(
    private val trainingViewModel: TrainingViewModel,
    private val articleViewModel: ArticleViewModel,
    application: Application
) : AndroidViewModel(application) {

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements

    init {
        combine(
            trainingViewModel.sessions,
            articleViewModel.articles,
            articleViewModel.balance
        ) { sessions, articles, balance ->
            val totalWorkouts = sessions.size
            val totalReps = sessions.sumOf { it.exercises.sumOf { ex -> ex.sets * ex.reps } }
            val purchasedArticles = articles.count { it.purchased }

            listOf(
                Achievement(
                    id = UUID.randomUUID(),
                    title = "Новичок",
                    description = "Проведи 1 тренировку",
                    target = 1,
                    current = totalWorkouts.coerceAtMost(1),
                    stars = 1
                ),
                Achievement(
                    id = UUID.randomUUID(),
                    title = "Воин",
                    description = "Проведи 5 тренировок",
                    target = 5,
                    current = totalWorkouts.coerceAtMost(5),
                    stars = 2
                ),
                Achievement(
                    id = UUID.randomUUID(),
                    title = "Мастер",
                    description = "Купи 2 статьи",
                    target = 2,
                    current = purchasedArticles.coerceAtMost(2),
                    stars = 3
                ),
                Achievement(
                    id = UUID.randomUUID(),
                    title = "Силач",
                    description = "Сделай 1000 повторений",
                    target = 1000,
                    current = totalReps.coerceAtMost(1000),
                    stars = 3
                )
            )
        }.distinctUntilChanged().onEach { _achievements.value = it }.launchIn(viewModelScope)
    }
}