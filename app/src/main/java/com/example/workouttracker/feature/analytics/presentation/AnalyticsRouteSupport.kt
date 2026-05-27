package com.example.workouttracker.feature.analytics.presentation

import com.example.workouttracker.feature.nutrition.presentation.NutritionEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class StepsHistoryRange(val days: Long, val label: String) {
    WEEK(7, "Неделя"),
    MONTH(30, "Месяц")
}

internal fun formatAnalyticsPrettyDate(timestamp: Long): String =
    SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(timestamp))

internal fun formatAnalyticsTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

internal fun buildTodayNutritionTotal(
    entries: List<NutritionEntry>,
    todayIso: String
): NutritionEntry {
    val todayEntries = entries.filter { it.date == todayIso }
    if (todayEntries.isEmpty()) {
        return NutritionEntry(
            date = todayIso,
            name = "",
            calories = 0,
            protein = 0,
            carbs = 0,
            fats = 0,
            weight = 0
        )
    }

    return NutritionEntry(
        date = todayIso,
        name = "Итого",
        calories = todayEntries.sumOf { it.calories },
        protein = todayEntries.sumOf { it.protein },
        carbs = todayEntries.sumOf { it.carbs },
        fats = todayEntries.sumOf { it.fats },
        weight = todayEntries.sumOf { it.weight }
    )
}

internal fun validateAnalyticsShortDate(text: String): String? {
    val regex = Regex("""^\d{2}\.\d{2}$""")
    if (!regex.matches(text)) return "Дата в формате ДД.ММ"

    val day = text.substring(0, 2).toIntOrNull() ?: return "Некорректный день"
    val month = text.substring(3, 5).toIntOrNull() ?: return "Некорректный месяц"
    if (month !in 1..12) return "Месяц должен быть от 01 до 12"

    val maxDay = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        else -> 29
    }
    return if (day !in 1..maxDay) "День должен быть от 01 до $maxDay" else null
}

internal fun validateAnalyticsWeight(text: String): String? {
    if (text.isBlank()) return "Введите вес"
    val value = text.replace(',', '.').toFloatOrNull() ?: return "Неверный формат веса"
    return if (value !in 30f..300f) "Диапазон 30-300 кг" else null
}
