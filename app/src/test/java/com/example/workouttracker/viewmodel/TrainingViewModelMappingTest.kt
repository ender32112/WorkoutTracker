package com.example.workouttracker.viewmodel

import com.example.workouttracker.data.local.ExerciseEntity
import com.example.workouttracker.data.local.PerformedSessionWithExercises
import com.example.workouttracker.data.local.WorkoutPerformedExerciseEntity
import com.example.workouttracker.data.local.WorkoutPerformedExerciseWithSets
import com.example.workouttracker.data.local.WorkoutSessionPerformedEntity
import com.example.workouttracker.data.local.WorkoutSetPerformedEntity
import com.example.workouttracker.ui.training.ExercisePrUi
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainingViewModelMappingTest {

    @Test
    fun mapPerformedSessionToUi_mapsExercisesAndSets() {
        val source = PerformedSessionWithExercises(
            session = WorkoutSessionPerformedEntity(id = 11, userId = "u", startedAt = 1000, finishedAt = 2000),
            exercises = listOf(
                WorkoutPerformedExerciseWithSets(
                    exerciseEntity = WorkoutPerformedExerciseEntity(id = 5, sessionId = 11, exerciseId = 7, exerciseNameSnapshot = "Bench"),
                    sets = listOf(
                        WorkoutSetPerformedEntity(performedExerciseId = 5, setOrder = 0, weight = 100f, reps = 5),
                        WorkoutSetPerformedEntity(performedExerciseId = 5, setOrder = 1, weight = 105f, reps = 4)
                    )
                )
            )
        )

        val result = TrainingViewModel.mapPerformedSessionToUi(
            source,
            catalogById = mapOf(7L to ExerciseEntity(id = 7, userId = "u", name = "Bench", muscles = "Грудь,Трицепс")),
            prMap = mapOf("Bench" to ExercisePrUi("Bench", 500.0, 120.0))
        )

        assertEquals(11L, result.sessionId)
        assertEquals(1, result.exercises.size)
        assertEquals(2, result.exercises.first().sets.size)
        assertEquals(920.0, result.totalVolume, 0.001)
    }
}
