package com.example.workouttracker.ui.training

import java.util.*

data class ExerciseEntry(
    val name: String,
    val sets: Int,
    val reps: Int
)

data class TrainingSession(
    val id: UUID = UUID.randomUUID(),
    val date: String,
    val exercises: List<ExerciseEntry>
)
