package com.example.workouttracker.feature.training.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingSessionLogicTest {

    @Test
    fun removeExerciseFromWorkout_removesTargetExercise() {
        val exercises = listOf(
            workoutExercise(instanceId = "a", sets = 2),
            workoutExercise(instanceId = "b", sets = 1)
        )

        val updated = removeExerciseFromWorkout(exercises, "a")

        assertEquals(1, updated.size)
        assertEquals("b", updated.single().instanceId)
    }

    @Test
    fun removeSetFromWorkout_removesOnlySet_whenExerciseStillHasOtherSets() {
        val exercises = listOf(workoutExercise(instanceId = "a", sets = 2))

        val updated = removeSetFromWorkout(exercises, "a", 0)

        assertEquals(1, updated.size)
        assertEquals(1, updated.single().sets.size)
    }

    @Test
    fun removeSetFromWorkout_removesExercise_whenLastSetDeleted() {
        val exercises = listOf(
            workoutExercise(instanceId = "a", sets = 1),
            workoutExercise(instanceId = "b", sets = 2)
        )

        val updated = removeSetFromWorkout(exercises, "a", 0)

        assertEquals(1, updated.size)
        assertEquals("b", updated.single().instanceId)
        assertTrue(updated.single().sets.isNotEmpty())
    }

    private fun workoutExercise(instanceId: String, sets: Int): WorkoutExerciseInput {
        return WorkoutExerciseInput(
            instanceId = instanceId,
            exerciseId = "exercise-$instanceId",
            exerciseName = "Exercise $instanceId",
            sets = List(sets) { ExerciseSetInput(weight = "10", reps = "8") }
        )
    }
}
