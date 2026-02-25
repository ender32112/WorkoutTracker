package com.example.workouttracker.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutTrackerDaoTest {
    private lateinit var db: WorkoutTrackerDatabase
    private lateinit var dao: WorkoutTrackerDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutTrackerDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.dao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observePerformedSessionsWithExercises_returnsNestedData() = runBlocking {
        dao.upsertUser(UserEntity("u1", "User", "u@u"))
        val exId = dao.upsertExercise(ExerciseEntity(userId = "u1", name = "Squat", muscles = "Legs"))
        dao.persistWorkoutPerformed(
            userId = "u1",
            startedAt = 1000,
            finishedAt = 2000,
            exercises = listOf(
                PerformedExerciseDraft(
                    exerciseId = exId,
                    exerciseName = "Squat",
                    sets = listOf(PerformedSetDraft(100f, 5), PerformedSetDraft(105f, 4))
                )
            )
        )

        val sessions = dao.observePerformedSessionsWithExercises("u1").first()

        assertEquals(1, sessions.size)
        assertEquals(1, sessions[0].exercises.size)
        assertEquals(2, sessions[0].exercises[0].sets.size)
    }
}
