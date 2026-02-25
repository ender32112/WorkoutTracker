package com.example.workouttracker.ui.training

data class ExerciseEntry(
    val exerciseId: Long,
    val name: String,
    val muscles: List<String> = emptyList(),
    val sets: List<ExerciseSetSummary>,
    val photoUri: String? = null,
    val pr: ExercisePrUi? = null
) {
    val totalVolume: Double
        get() = sets.sumOf { it.weight * it.reps }
}

data class ExerciseSetSummary(
    val order: Int,
    val weight: Float,
    val reps: Int
)

data class TrainingSession(
    val sessionId: Long,
    val startedAt: Long,
    val finishedAt: Long,
    val date: String,
    val exercises: List<ExerciseEntry>
) {
    val totalVolume: Double
        get() = exercises.sumOf { it.totalVolume }
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

data class WorkoutTemplateExerciseUi(
    val id: Long,
    val exerciseId: Long,
    val name: String,
    val muscles: List<String>,
    val photoUri: String?,
    val orderInTemplate: Int,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultWeight: Float?
)

data class WorkoutTemplateUi(
    val id: Long,
    val title: String,
    val exercises: List<WorkoutTemplateExerciseUi>
)
