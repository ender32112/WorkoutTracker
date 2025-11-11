package com.example.workouttracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.ui.achievements.Achievement
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

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
            // агрегаты
            val totalWorkouts = sessions.size
            val totalReps = sessions.sumOf { it.exercises.sumOf { ex -> ex.sets * ex.reps } }
            val totalVolume = sessions.sumOf { it.totalVolume }
            val purchasedArticles = articles.count { it.purchased }
            val (currentStreak, bestStreak) = computeStreak(sessions.map { it.date })

            // удобный конструктор (обрезаем current до target, чтобы прогресс не “переливался”)
            fun a(
                title: String, description: String,
                target: Int, current: Int, stars: Int, cat: String
            ) = Achievement(
                title = title,
                description = description,
                target = target,
                current = current.coerceAtMost(target),
                stars = stars,
                category = cat
            )

            listOf(
                // Стартовые и прогресс
                a("Первый шаг", "Проведи 1 тренировку", 1, totalWorkouts, 1, "Прогресс"),
                a("В рабочем режиме", "Проведи 5 тренировок", 5, totalWorkouts, 1, "Прогресс"),
                a("Движение — жизнь", "Проведи 15 тренировок", 15, totalWorkouts, 2, "Прогресс"),
                a("На дистанции", "Проведи 30 тренировок", 30, totalWorkouts, 3, "Прогресс"),

                // Объём и повторы
                a("Тысяча повторений", "Сделай 1 000 повторений суммарно", 1_000, totalReps, 2, "Объём"),
                a("Десятки тысяч", "Сделай 10 000 повторений", 10_000, totalReps, 3, "Объём"),
                a("Гора железа", "Подними суммарно 10 000 кг", 10_000, totalVolume, 2, "Объём"),
                a("Железный человек", "Подними суммарно 100 000 кг", 100_000, totalVolume, 3, "Объём"),

                // Обучение/контент (привязка к статьям)
                a("Ученик", "Купи 1 статью", 1, purchasedArticles, 1, "Знания"),
                a("Исследователь", "Купи 5 статей", 5, purchasedArticles, 2, "Знания"),

                // Серии
                a("Серия пошла", "Тренируйся без пропусков 3 дня подряд", 3, currentStreak, 2, "Серии"),
                a("Неостановим", "Серия 7 дней", 7, currentStreak, 3, "Серии"),
                a("Лучшая серия", "Достигни серии 14 дней (лучший результат)", 14, bestStreak, 3, "Рекорды")
            )
        }
            .distinctUntilChanged()
            .onEach { _achievements.value = it }
            .launchIn(viewModelScope)
    }

    /** Возвращает (текущая серия подряд, лучшая серия). На вход — даты в формате yyyy-MM-dd */
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

        // вычисляем текущую серию до СЕГОДНЯ
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
}
