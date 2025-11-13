package com.example.workouttracker.ui.achievements

import java.util.UUID

data class Achievement(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val description: String,
    val target: Int,        // целевое значение
    val current: Int,       // текущий прогресс (обрезанный по target в VM)
    val stars: Int = 1,     // 1..3 — “редкость/сложность”
    val category: String = "Общее" // группировка (для фильтров/сортировки)
) {
    val progress: Float get() = (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val isCompleted: Boolean get() = current >= target
}
