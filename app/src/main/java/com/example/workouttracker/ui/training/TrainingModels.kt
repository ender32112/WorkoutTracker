package com.example.workouttracker.ui.training

import java.util.UUID

data class ExerciseEntry(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val sets: Int,
    val reps: Int,
    val weight: Float,
    val photoUri: String? = null
)

data class TrainingSession(
    val id: UUID = UUID.randomUUID(),
    val date: String,
    val exercises: List<ExerciseEntry>
) {
    val totalVolume: Int
        get() = exercises.sumOf { it.sets * it.reps * it.weight.toInt() }
}

data class ExerciseCatalogItem(
    val id: Long,
    val name: String,
    val aliases: String,
    val muscles: List<String>,
    val equipment: String?,
    val favorite: Boolean,
    val photoUri: String?,
    val isBase: Boolean,
    val lastUsedAt: Long?
)

data class ExerciseSetInput(
    val weight: String = "",
    val reps: String = ""
)

data class WorkoutExerciseInput(
    val exerciseId: Long,
    val exerciseName: String,
    val sets: List<ExerciseSetInput> = listOf(ExerciseSetInput())
)

data class ActiveWorkoutUiState(
    val startedAt: Long,
    val exercises: List<WorkoutExerciseInput> = emptyList(),
    val restTimerSecondsLeft: Int = 0,
    val timerRunning: Boolean = false
)

data class ExercisePrUi(
    val exerciseName: String,
    val bestVolumeSet: Double,
    val bestE1rm: Double
)

data class WeeklyVolumeUi(
    val weekKey: String,
    val volume: Double
)
