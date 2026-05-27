package com.example.workouttracker.viewmodel

import com.example.workouttracker.data.local.ActiveWorkoutStateEntity
import com.example.workouttracker.feature.training.presentation.ActiveWorkoutUiState
import com.example.workouttracker.feature.training.presentation.ExerciseSetInput
import com.example.workouttracker.feature.training.presentation.WorkoutExerciseInput
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ActiveWorkoutSerializationTest {
    private val gson = Gson()

    @Test
    fun `active workout gson serialization restores key fields`() {
        val original = ActiveWorkoutUiState(
            startedAt = 1710000000000,
            exercises = listOf(
                WorkoutExerciseInput(
                    instanceId = "instance-1",
                    exerciseId = "squat",
                    exerciseName = "Squat",
                    sets = listOf(ExerciseSetInput(weight = "100", reps = "5"), ExerciseSetInput(weight = "105", reps = "3"))
                ),
                WorkoutExerciseInput(
                    instanceId = "instance-2",
                    exerciseId = "bench-press",
                    exerciseName = "Bench Press",
                    sets = listOf(ExerciseSetInput(weight = "80", reps = "8"))
                )
            ),
            restTimerSecondsLeft = 47,
            timerRunning = true
        )

        val entity = ActiveWorkoutStateEntity(
            userId = "test_user",
            startedAt = original.startedAt,
            updatedAt = System.currentTimeMillis(),
            payloadJson = gson.toJson(original)
        )

        val restored = gson.fromJson(entity.payloadJson, ActiveWorkoutUiState::class.java)

        assertNotNull(restored)
        assertEquals(original.startedAt, restored.startedAt)
        assertEquals(original.restTimerSecondsLeft, restored.restTimerSecondsLeft)
        assertEquals(original.timerRunning, restored.timerRunning)
        assertEquals(original.exercises.size, restored.exercises.size)
        assertEquals(original.exercises[0].exerciseName, restored.exercises[0].exerciseName)
        assertEquals(original.exercises[0].sets[0].weight, restored.exercises[0].sets[0].weight)
    }
}
