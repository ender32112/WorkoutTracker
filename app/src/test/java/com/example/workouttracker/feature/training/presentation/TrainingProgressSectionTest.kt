package com.example.workouttracker.feature.training.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingProgressSectionTest {

    @Test
    fun buildExerciseProgressStats_sortsSessionsOldToNewForTrend_andUsesBestSetVolume() {
        val newerSession = TrainingSession(
            sessionId = 2,
            startedAt = 2_000L,
            finishedAt = 2_100L,
            date = "2026-03-20",
            exercises = listOf(
                ExerciseEntry(
                    exerciseId = "bench",
                    name = "Bench Press",
                    sets = listOf(
                        ExerciseSetSummary(order = 0, weight = 100f, reps = 4),
                        ExerciseSetSummary(order = 1, weight = 30f, reps = 6)
                    )
                )
            )
        )
        val olderSession = TrainingSession(
            sessionId = 1,
            startedAt = 1_000L,
            finishedAt = 1_100L,
            date = "2026-03-18",
            exercises = listOf(
                ExerciseEntry(
                    exerciseId = "bench",
                    name = "Bench Press",
                    sets = listOf(
                        ExerciseSetSummary(order = 0, weight = 50f, reps = 4),
                        ExerciseSetSummary(order = 1, weight = 40f, reps = 5)
                    )
                )
            )
        )

        val stats = buildExerciseProgressStats(listOf(newerSession, olderSession))
        val bench = stats.single()

        assertEquals("Bench Press", bench.exerciseName)
        assertEquals(2, bench.sessionCount)
        assertEquals(980.0, bench.totalVolume, 0.001)
        assertEquals(400.0, bench.bestSetVolume, 0.001)
        assertTrue("Trend should reflect growth from older to newer session", bench.trendPercent > 0.0)
        assertEquals(45.0, bench.trendPercent, 0.001)
    }
}
