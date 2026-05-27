package com.example.workouttracker.feature.training.presentation

enum class ExerciseSourceUi {
    GLOBAL,
    CUSTOM
}

enum class ExerciseSourceFilterUi {
    ALL,
    GLOBAL,
    CUSTOM
}

data class ExerciseEntry(
    val exerciseId: String,
    val name: String,
    val nameEn: String? = null,
    val muscles: List<String> = emptyList(),
    val bodyPart: String? = null,
    val equipment: String? = null,
    val sets: List<ExerciseSetSummary>,
    val mediaUri: String? = null,
    val localMediaUri: String? = null,
    val pr: ExercisePrUi? = null
) {
    val totalVolume: Double
        get() = sets.sumOf { it.weight.toDouble() * it.reps.toDouble() }
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
    val id: String,
    val name: String,
    val nameEn: String,
    val aliases: String,
    val muscles: List<String>,
    val bodyPart: String,
    val equipment: String,
    val favorite: Boolean,
    val mediaUri: String?,
    val localMediaUri: String?,
    val isCustom: Boolean,
    val source: ExerciseSourceUi,
    val lastUsedAt: Long?
)

data class ExerciseSetInput(
    val weight: String = "",
    val reps: String = ""
)

data class WorkoutExerciseInput(
    val instanceId: String,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseNameEn: String? = null,
    val bodyPart: String? = null,
    val muscles: List<String> = emptyList(),
    val equipment: String? = null,
    val mediaUri: String? = null,
    val localMediaUri: String? = null,
    val sets: List<ExerciseSetInput> = listOf(ExerciseSetInput())
)

data class ActiveWorkoutUiState(
    val startedAt: Long,
    val exercises: List<WorkoutExerciseInput> = emptyList(),
    val restTimerSecondsLeft: Int = 0,
    val timerRunning: Boolean = false
)

data class ExercisePrUi(
    val exerciseId: String,
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
    val exerciseId: String,
    val name: String,
    val nameEn: String? = null,
    val muscles: List<String>,
    val bodyPart: String? = null,
    val equipment: String? = null,
    val mediaUri: String? = null,
    val localMediaUri: String? = null,
    val isMissing: Boolean = false,
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

internal fun removeExerciseFromWorkout(
    exercises: List<WorkoutExerciseInput>,
    instanceId: String
): List<WorkoutExerciseInput> = exercises.filterNot { it.instanceId == instanceId }

internal fun removeSetFromWorkout(
    exercises: List<WorkoutExerciseInput>,
    instanceId: String,
    setIndex: Int
): List<WorkoutExerciseInput> {
    return exercises.mapNotNull { exercise ->
        if (exercise.instanceId != instanceId) {
            exercise
        } else {
            val updatedSets = exercise.sets.filterIndexed { index, _ -> index != setIndex }
            if (updatedSets.isEmpty()) null else exercise.copy(sets = updatedSets)
        }
    }
}

