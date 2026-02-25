package com.example.workouttracker.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
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
    suspend fun upsertExercise(entry: ExerciseEntity): Long

    @Query("DELETE FROM exercise_catalog WHERE id = :exerciseId AND userId = :userId AND isBase = 0")
    suspend fun deleteCustomExercise(userId: String, exerciseId: Long)

    @Query("SELECT * FROM exercise_catalog WHERE userId = :userId ORDER BY isBase DESC, name ASC")
    fun observeExercises(userId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercise_catalog WHERE userId = :userId AND isBase = 1")
    suspend fun countBaseExercises(userId: String): Int

    @Query(
        """
        SELECT * FROM exercise_catalog
        WHERE userId = :userId
          AND (:muscle IS NULL OR lower(muscles) LIKE '%' || lower(:muscle) || '%')
          AND (
            :query IS NULL OR :query = '' OR
            lower(name) LIKE '%' || lower(:query) || '%' OR
            lower(COALESCE(aliases,'')) LIKE '%' || lower(:query) || '%'
          )
        ORDER BY isFavorite DESC, COALESCE(lastUsedAt,0) DESC, name ASC
        """
    )
    fun observeSearchExercises(userId: String, query: String?, muscle: String?): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_catalog WHERE userId = :userId AND isFavorite = 1 ORDER BY name")
    fun observeFavorites(userId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_catalog WHERE userId = :userId AND lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observeRecentExercises(userId: String, limit: Int = 8): Flow<List<ExerciseEntity>>

    @Query("UPDATE exercise_catalog SET isFavorite = :favorite WHERE id = :exerciseId AND userId = :userId")
    suspend fun updateFavorite(userId: String, exerciseId: Long, favorite: Boolean)

    @Query("UPDATE exercise_catalog SET photoUri = :photoUri WHERE id = :exerciseId AND userId = :userId")
    suspend fun updateExercisePhoto(userId: String, exerciseId: Long, photoUri: String?)

    @Query("UPDATE exercise_catalog SET lastUsedAt = :usedAt WHERE id = :exerciseId AND userId = :userId")
    suspend fun markExerciseUsed(userId: String, exerciseId: Long, usedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutTemplate(entry: WorkoutTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutTemplateExercise(entry: WorkoutTemplateExerciseEntity)

    @Query("DELETE FROM workout_template_exercise WHERE id = :entryId")
    suspend fun deleteWorkoutTemplateExercise(entryId: Long)

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE userId = :userId ORDER BY title ASC")
    fun observeWorkoutTemplatesWithExercises(userId: String): Flow<List<WorkoutTemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    suspend fun getWorkoutTemplateWithExercises(templateId: Long): WorkoutTemplateWithExercises?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(entry: WorkoutSessionPerformedEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutPerformedExercise(entry: WorkoutPerformedExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSet(entry: WorkoutSetPerformedEntity)

    @Query("SELECT * FROM workout_session_performed WHERE userId = :userId ORDER BY startedAt DESC")
    fun observePerformedSessions(userId: String): Flow<List<WorkoutSessionPerformedEntity>>

    @Transaction
    @Query("SELECT * FROM workout_session_performed WHERE userId = :userId ORDER BY startedAt DESC")
    fun observePerformedSessionsWithExercises(userId: String): Flow<List<PerformedSessionWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_session_performed WHERE id = :sessionId")
    fun observePerformedSessionDetail(sessionId: Long): Flow<PerformedSessionWithExercises?>

    @Query(
        """
        SELECT wspe.exerciseNameSnapshot AS exerciseName,
               MAX(wsp.weight * wsp.reps) AS bestVolumeSet,
               MAX(wsp.weight * (1 + wsp.reps / 30.0)) AS bestE1rm
        FROM workout_set_performed wsp
        INNER JOIN workout_performed_exercise wspe ON wspe.id = wsp.performedExerciseId
        INNER JOIN workout_session_performed wspf ON wspf.id = wspe.sessionId
        WHERE wspf.userId = :userId
        GROUP BY wspe.exerciseNameSnapshot
        ORDER BY exerciseName
        """
    )
    fun observeExercisePr(userId: String): Flow<List<ExercisePrRow>>

    @Query(
        """
        SELECT strftime('%Y-%W', datetime(wspf.startedAt / 1000, 'unixepoch')) AS weekKey,
               SUM(wsp.weight * wsp.reps) AS volume
        FROM workout_set_performed wsp
        INNER JOIN workout_performed_exercise wspe ON wspe.id = wsp.performedExerciseId
        INNER JOIN workout_session_performed wspf ON wspf.id = wspe.sessionId
        WHERE wspf.userId = :userId
        GROUP BY weekKey
        ORDER BY weekKey DESC
        LIMIT 12
        """
    )
    fun observeWeeklyVolume(userId: String): Flow<List<WeeklyVolumeRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActiveWorkoutState(state: ActiveWorkoutStateEntity)

    @Query("SELECT * FROM active_workout_state WHERE userId = :userId")
    suspend fun getActiveWorkoutState(userId: String): ActiveWorkoutStateEntity?

    @Query("DELETE FROM active_workout_state WHERE userId = :userId")
    suspend fun clearActiveWorkoutState(userId: String)

    @Query("SELECT * FROM weight_entries WHERE userId = :userId ORDER BY loggedAt DESC")
    fun observeWeight(userId: String): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM step_entries WHERE userId = :userId ORDER BY dateIso DESC")
    fun observeStepEntries(userId: String): Flow<List<StepEntryEntity>>

    @Transaction
    suspend fun persistWorkoutPerformed(
        userId: String,
        startedAt: Long,
        finishedAt: Long,
        exercises: List<PerformedExerciseDraft>
    ) {
        val sessionId = insertWorkoutSession(
            WorkoutSessionPerformedEntity(
                userId = userId,
                startedAt = startedAt,
                finishedAt = finishedAt
            )
        )
        exercises.forEach { ex ->
            val perfExerciseId = insertWorkoutPerformedExercise(
                WorkoutPerformedExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = ex.exerciseId,
                    exerciseNameSnapshot = ex.exerciseName
                )
            )
            ex.sets.forEachIndexed { index, set ->
                insertWorkoutSet(
                    WorkoutSetPerformedEntity(
                        performedExerciseId = perfExerciseId,
                        setOrder = index,
                        weight = set.weight,
                        reps = set.reps
                    )
                )
            }
            markExerciseUsed(userId, ex.exerciseId, finishedAt)
        }
        clearActiveWorkoutState(userId)
    }
}

data class WorkoutPerformedExerciseWithSets(
    @Embedded val exerciseEntity: WorkoutPerformedExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "performedExerciseId")
    val sets: List<WorkoutSetPerformedEntity>
)

data class PerformedSessionWithExercises(
    @Embedded val session: WorkoutSessionPerformedEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
        entity = WorkoutPerformedExerciseEntity::class
    )
    val exercises: List<WorkoutPerformedExerciseWithSets>
)

data class TemplateExerciseWithDetails(
    @Embedded val templateExercise: WorkoutTemplateExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity
)

data class WorkoutTemplateWithExercises(
    @Embedded val template: WorkoutTemplateEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId",
        entity = WorkoutTemplateExerciseEntity::class
    )
    val exercises: List<TemplateExerciseWithDetails>
)

data class ExercisePrRow(
    val exerciseName: String,
    val bestVolumeSet: Double,
    val bestE1rm: Double
)

data class WeeklyVolumeRow(
    val weekKey: String,
    val volume: Double
)

data class PerformedSetDraft(
    val weight: Float,
    val reps: Int
)

data class PerformedExerciseDraft(
    val exerciseId: Long,
    val exerciseName: String,
    val sets: List<PerformedSetDraft>
)
