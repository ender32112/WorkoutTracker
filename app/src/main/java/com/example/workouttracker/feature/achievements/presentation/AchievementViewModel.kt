package com.example.workouttracker.feature.achievements.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.core.auth.AuthSessionStore
import com.example.workouttracker.data.local.ArticleRepository
import com.example.workouttracker.data.local.ArticlePurchaseEntity
import com.example.workouttracker.data.local.PerformedSessionWithExercises
import com.example.workouttracker.data.local.WorkoutTrackerDao
import com.example.workouttracker.feature.achievements.presentation.Achievement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@HiltViewModel
class AchievementViewModel @Inject constructor(
    authSessionStore: AuthSessionStore,
    private val dao: WorkoutTrackerDao,
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val userId = authSessionStore.currentUserIdOrGuest()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements
    val uiState: StateFlow<AchievementsUiState> =
        _achievements
            .map { AchievementsUiState(achievements = it) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                AchievementsUiState()
            )

    init {
        combine(
            dao.observePerformedSessionsWithExercises(userId),
            articleRepository.observePurchases(userId)
        ) { sessions: List<PerformedSessionWithExercises>, purchases: List<ArticlePurchaseEntity> ->
            val totalWorkouts = sessions.size
            val totalReps = sessions.sumOf { session ->
                session.exercises.sumOf { ex -> ex.sets.sumOf { it.reps } }
            }
            val totalVolume = sessions.sumOf { session ->
                session.exercises.sumOf { ex ->
                    ex.sets.sumOf { (it.weight * it.reps).toDouble() }
                }
            }.roundToInt()
            val purchasedArticles = purchases.size
            val workoutDates = sessions.map { formatWorkoutDay(it.session.startedAt) }
            val (currentStreak, bestStreak) = computeStreak(workoutDates)

            fun a(
                title: String,
                description: String,
                target: Int,
                current: Int,
                stars: Int,
                cat: String
            ) = Achievement(
                title = title,
                description = description,
                target = target,
                current = current.coerceAtMost(target),
                stars = stars,
                category = cat
            )

            listOf(
                a("Первый шаг", "Проведи 1 тренировку", 1, totalWorkouts, 1, "Прогресс"),
                a("В рабочем режиме", "Проведи 5 тренировок", 5, totalWorkouts, 1, "Прогресс"),
                a("Движение — жизнь", "Проведи 15 тренировок", 15, totalWorkouts, 2, "Прогресс"),
                a("На дистанции", "Проведи 30 тренировок", 30, totalWorkouts, 3, "Прогресс"),
                a("Тысяча повторений", "Сделай 1 000 повторений суммарно", 1_000, totalReps, 2, "Объём"),
                a("Десятки тысяч", "Сделай 10 000 повторений", 10_000, totalReps, 3, "Объём"),
                a("Гора железа", "Подними суммарно 10 000 кг", 10_000, totalVolume, 2, "Объём"),
                a("Железный человек", "Подними суммарно 100 000 кг", 100_000, totalVolume, 3, "Объём"),
                a("Ученик", "Купи 1 статью", 1, purchasedArticles, 1, "Знания"),
                a("Исследователь", "Купи 5 статей", 5, purchasedArticles, 2, "Знания"),
                a("Серия пошла", "Тренируйся без пропусков 3 дня подряд", 3, currentStreak, 2, "Серии"),
                a("Неостановим", "Серия 7 дней", 7, currentStreak, 3, "Серии"),
                a("Лучшая серия", "Достигни серии 14 дней (лучший результат)", 14, bestStreak, 3, "Рекорды")
            )
        }
            .distinctUntilChanged()
            .onEach { _achievements.value = it }
            .launchIn(viewModelScope)
    }

    private fun computeStreak(dates: List<String>): Pair<Int, Int> {
        if (dates.isEmpty()) return 0 to 0
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val uniqueDays = dates
            .mapNotNull { runCatching { fmt.parse(it) }.getOrNull() }
            .map { dayStart(it) }
            .distinct()
            .sorted()

        var best = 1
        var current = 1
        for (i in 1 until uniqueDays.size) {
            val prev = uniqueDays[i - 1]
            val cur = uniqueDays[i]
            val diff = (cur.time - prev.time) / (24 * 60 * 60 * 1000L)
            if (diff == 1L) {
                current += 1
                if (current > best) best = current
            } else if (diff > 1L) {
                current = 1
            }
        }

        val today = dayStart(Date())
        var tail = 0
        var prev = today
        for (i in uniqueDays.size - 1 downTo 0) {
            val d = uniqueDays[i]
            val diff = (prev.time - d.time) / (24 * 60 * 60 * 1000L)
            if (diff == 0L || diff == 1L) {
                tail += 1
                prev = d
                if (diff == 1L) continue
            } else break
        }
        return tail.coerceAtLeast(1) to best
    }

    private fun dayStart(date: Date): Date {
        val cal = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }

    private fun formatWorkoutDay(startedAt: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return fmt.format(Date(startedAt))
    }
}

