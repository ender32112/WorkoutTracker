package com.example.workouttracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTrackerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntry(entry: WeightEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStepEntry(entry: StepEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(entry: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutTemplate(entry: WorkoutTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(entry: WorkoutSessionEntity)

    @Query("SELECT * FROM weight_entries WHERE userId = :userId ORDER BY loggedAt DESC")
    fun observeWeight(userId: String): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM step_entries WHERE userId = :userId ORDER BY dateIso DESC")
    fun observeStepEntries(userId: String): Flow<List<StepEntryEntity>>
}
