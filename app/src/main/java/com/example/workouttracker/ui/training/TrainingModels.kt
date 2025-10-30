package com.example.workouttracker.ui.training

import java.util.*

data class ExerciseEntry(
    val id: UUID = UUID.randomUUID(),  // ← ВАЖНО!
    val name: String,
    val sets: Int,
    val reps: Int,
    val weight: Float
)

data class TrainingSession(
    val id: UUID = UUID.randomUUID(),
    val date: String,
    val exercises: List<ExerciseEntry>
) {
    val totalVolume: Int
        get() = exercises.sumOf { it.sets * it.reps * it.weight.toInt() }
}