package com.example.workouttracker.ui.achievements

import java.util.UUID

data class Achievement(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val description: String,
    val target: Int,        // Цель (например, 10 тренировок)
    val current: Int,       // Текущий прогресс
    val stars: Int = 1      // 1, 2, 3
) {
    val progress: Float get() = (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val isCompleted: Boolean get() = current >= target
}