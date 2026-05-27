package com.example.workouttracker.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTrackerDao {
    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE lower(email) = lower(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightEntry(entry: WeightEntryEntity)

    @Query("DELETE FROM weight_entries WHERE userId = :userId")
    suspend fun deleteWeightEntries(userId: String)

    @Query("DELETE FROM weight_entries WHERE userId = :userId AND loggedAt >= :startMillis AND loggedAt < :endMillis")
    suspend fun deleteWeightEntriesInRange(userId: String, startMillis: Long, endMillis: Long)

    @Query("SELECT * FROM weight_entries WHERE userId = :userId ORDER BY loggedAt DESC, id DESC")
    suspend fun getWeightEntriesOnce(userId: String): List<WeightEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStepEntry(entry: StepEntryEntity)

    @Query("DELETE FROM step_entries WHERE userId = :userId")
    suspend fun deleteStepEntries(userId: String)

    @Query("DELETE FROM step_entries WHERE userId = :userId AND dateIso = :dateIso")
    suspend fun deleteStepEntryByDate(userId: String, dateIso: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(entry: ExerciseEntity): Long

    @Query("DELETE FROM exercise_catalog WHERE id = :exerciseId AND userId = :userId AND isBase = 0")
    suspend fun deleteCustomExercise(userId: String, exerciseId: Long)

    @Query("SELECT * FROM exercise_catalog WHERE userId = :userId ORDER BY isBase DESC, name ASC")
    fun observeExercises(userId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_catalog WHERE userId = :userId ORDER BY isBase DESC, name ASC")
    suspend fun getExercisesOnce(userId: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercise_catalog WHERE userId = :userId AND sourceExerciseId = :sourceExerciseId LIMIT 1")
    suspend fun getExerciseBySourceId(userId: String, sourceExerciseId: String): ExerciseEntity?

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

    @Query(
        """
        DELETE FROM workout_template_exercise
        WHERE id = :entryId
          AND templateId IN (SELECT id FROM workout_templates WHERE userId = :userId)
        """
    )
    suspend fun deleteWorkoutTemplateExercise(userId: String, entryId: Long)

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE userId = :userId ORDER BY title ASC")
    fun observeWorkoutTemplatesWithExercises(userId: String): Flow<List<WorkoutTemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :templateId AND userId = :userId")
    suspend fun getWorkoutTemplateWithExercises(userId: String, templateId: Long): WorkoutTemplateWithExercises?

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
    @Query("SELECT * FROM workout_session_performed WHERE id = :sessionId AND userId = :userId")
    fun observePerformedSessionDetail(userId: String, sessionId: Long): Flow<PerformedSessionWithExercises?>

    @Query(
        """
        DELETE FROM workout_session_performed
        WHERE id = :sessionId
          AND userId = :userId
        """
    )
    suspend fun deleteWorkoutSession(userId: String, sessionId: Long)

    @Query(
        """
        DELETE FROM workout_performed_exercise
        WHERE sessionId = :sessionId
          AND sessionId IN (
            SELECT id FROM workout_session_performed
            WHERE userId = :userId
          )
        """
    )
    suspend fun deletePerformedExercisesForSession(userId: String, sessionId: Long)

    @Query(
        """
        SELECT wspe.catalogExerciseId AS exerciseId,
               MAX(wspe.exerciseNameSnapshot) AS exerciseName,
               MAX(wsp.weight * wsp.reps) AS bestVolumeSet,
               MAX(wsp.weight * (1 + wsp.reps / 30.0)) AS bestE1rm
        FROM workout_set_performed wsp
        INNER JOIN workout_performed_exercise wspe ON wspe.id = wsp.performedExerciseId
        INNER JOIN workout_session_performed wspf ON wspf.id = wspe.sessionId
        WHERE wspf.userId = :userId
        GROUP BY wspe.catalogExerciseId
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

    @Query("SELECT * FROM weight_entries WHERE userId = :userId ORDER BY loggedAt DESC, id DESC")
    fun observeWeight(userId: String): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries WHERE userId = :userId ORDER BY loggedAt DESC, id DESC LIMIT 1")
    suspend fun getLatestWeightEntry(userId: String): WeightEntryEntity?

    @Query("SELECT * FROM step_entries WHERE userId = :userId ORDER BY dateIso DESC")
    fun observeStepEntries(userId: String): Flow<List<StepEntryEntity>>

    @Query("SELECT * FROM nutrition_entries WHERE userId = :userId ORDER BY dateIso DESC, createdAt DESC")
    fun observeNutritionEntries(userId: String): Flow<List<NutritionEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNutritionEntry(entry: NutritionEntryEntity)

    @Query("SELECT * FROM nutrition_entries WHERE userId = :userId ORDER BY dateIso DESC, createdAt DESC")
    suspend fun getNutritionEntriesOnce(userId: String): List<NutritionEntryEntity>

    @Query("DELETE FROM nutrition_entries WHERE id = :entryId AND userId = :userId")
    suspend fun deleteNutritionEntry(userId: String, entryId: String)

    @Query("SELECT * FROM fridge_items WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeFridgeItems(userId: String): Flow<List<FridgeItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFridgeItem(item: FridgeItemEntity): Long

    @Update
    suspend fun updateFridgeItem(item: FridgeItemEntity)

    @Query("DELETE FROM fridge_items WHERE id = :itemId AND userId = :userId")
    suspend fun deleteFridgeItem(userId: String, itemId: Long)

    @Query("SELECT * FROM product_cache WHERE barcode = :barcode LIMIT 1")
    suspend fun getCachedProductByBarcode(barcode: String): ProductCacheEntity?

    @Query("SELECT * FROM product_cache ORDER BY cachedAt DESC")
    suspend fun getCachedProducts(): List<ProductCacheEntity>

    @Query(
        """
        SELECT * FROM product_cache
        WHERE lower(name) LIKE '%' || lower(:query) || '%'
           OR barcode LIKE '%' || :query || '%'
        ORDER BY cachedAt DESC
        """
    )
    suspend fun searchCachedProducts(query: String): List<ProductCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedProduct(item: ProductCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNutritionProfile(entry: NutritionProfileEntity)

    @Query("SELECT * FROM nutrition_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getNutritionProfile(userId: String): NutritionProfileEntity?

    @Query("SELECT * FROM nutrition_profiles WHERE userId = :userId LIMIT 1")
    fun observeNutritionProfile(userId: String): Flow<NutritionProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMealPlan(entry: MealPlanEntity)

    @Query("SELECT * FROM meal_plans WHERE userId = :userId AND dateIso = :dateIso LIMIT 1")
    suspend fun getMealPlan(userId: String, dateIso: String): MealPlanEntity?

    @Query("DELETE FROM meal_plans WHERE userId = :userId AND dateIso = :dateIso")
    suspend fun deleteMealPlan(userId: String, dateIso: String)

    @Query("SELECT * FROM meal_plans WHERE userId = :userId ORDER BY dateIso DESC")
    suspend fun getMealPlans(userId: String): List<MealPlanEntity>

    @Query("SELECT * FROM meal_plans WHERE userId = :userId AND dateIso = :dateIso LIMIT 1")
    fun observeMealPlan(userId: String, dateIso: String): Flow<MealPlanEntity?>

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
                    catalogExerciseId = ex.exerciseId,
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
        }
        clearActiveWorkoutState(userId)
    }

    @Transaction
    suspend fun replacePerformedSessionExercises(
        userId: String,
        sessionId: Long,
        exercises: List<PerformedExerciseDraft>
    ) {
        deletePerformedExercisesForSession(userId, sessionId)
        exercises.forEach { ex ->
            val perfExerciseId = insertWorkoutPerformedExercise(
                WorkoutPerformedExerciseEntity(
                    sessionId = sessionId,
                    catalogExerciseId = ex.exerciseId,
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
        }
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

data class WorkoutTemplateWithExercises(
    @Embedded val template: WorkoutTemplateEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId",
        entity = WorkoutTemplateExerciseEntity::class
    )
    val exercises: List<WorkoutTemplateExerciseEntity>
)

data class ExercisePrRow(
    val exerciseId: String,
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
    val exerciseId: String,
    val exerciseName: String,
    val sets: List<PerformedSetDraft>
)
