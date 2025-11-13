package com.example.workouttracker.ui.training

import java.util.*

data class ExerciseEntry(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val sets: Int,
    val reps: Int,
    val weight: Float,
    val photoUri: String? = null         // ← ДОБАВИЛИ: фото конкретного упражнения в сессии
)

data class TrainingSession(
    val id: UUID = UUID.randomUUID(),
    val date: String,
    val exercises: List<ExerciseEntry>
) {
    val totalVolume: Int
        get() = exercises.sumOf { it.sets * it.reps * it.weight.toInt() }
}

/** Каталог упражнений (для выбора и фоток тренажёров/примеров) */
data class ExerciseCatalogItem(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val photoUri: String? = null // content://... или file://
)
