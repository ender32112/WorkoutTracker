package com.example.workouttracker.data.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseCatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercise(item: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id AND userId = :userId")
    suspend fun deleteCustomExercise(userId: String, id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserMeta(item: ExerciseUserMetaEntity)

    @Query(
        """
        SELECT * FROM exercises
        WHERE userId = :globalUserId OR userId = :userId
        ORDER BY isCustom DESC, nameRu COLLATE NOCASE ASC
        """
    )
    fun getAllExercises(
        userId: String,
        globalUserId: String = ExerciseRepository.GLOBAL_USER_ID
    ): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT * FROM exercises
        WHERE (userId = :globalUserId OR userId = :userId)
          AND (
            :query = '' OR
            lower(nameRu) LIKE '%' || lower(:query) || '%' OR
            lower(nameEn) LIKE '%' || lower(:query) || '%' OR
            lower(bodyPart) LIKE '%' || lower(:query) || '%' OR
            lower(equipment) LIKE '%' || lower(:query) || '%' OR
            lower(targetMuscles) LIKE '%' || lower(:query) || '%' OR
            lower(secondaryMuscles) LIKE '%' || lower(:query) || '%'
          )
          AND (
            :muscle = '' OR
            lower(targetMuscles) LIKE '%' || lower(:muscle) || '%' OR
            lower(secondaryMuscles) LIKE '%' || lower(:muscle) || '%'
          )
          AND (:bodyPart = '' OR lower(bodyPart) = lower(:bodyPart))
          AND (:sourceFilter = '' OR source = :sourceFilter)
          AND (
            :favoritesOnly = 0 OR EXISTS(
              SELECT 1
              FROM exercise_user_meta meta
              WHERE meta.userId = :userId
                AND meta.exerciseId = exercises.id
                AND meta.isFavorite = 1
            )
          )
        ORDER BY isCustom DESC, nameRu COLLATE NOCASE ASC
        """
    )
    fun observeFilteredExercises(
        userId: String,
        query: String,
        muscle: String,
        bodyPart: String,
        favoritesOnly: Boolean,
        sourceFilter: String,
        globalUserId: String = ExerciseRepository.GLOBAL_USER_ID
    ): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT * FROM exercises
        WHERE id = :id
          AND (userId = :globalUserId OR userId = :userId)
        LIMIT 1
        """
    )
    fun getExerciseById(
        userId: String,
        id: String,
        globalUserId: String = ExerciseRepository.GLOBAL_USER_ID
    ): Flow<ExerciseEntity?>

    @Query(
        """
        SELECT * FROM exercises
        WHERE id = :id
          AND (userId = :globalUserId OR userId = :userId)
        LIMIT 1
        """
    )
    suspend fun getExerciseByIdOnce(
        userId: String,
        id: String,
        globalUserId: String = ExerciseRepository.GLOBAL_USER_ID
    ): ExerciseEntity?

    @Query(
        """
        SELECT * FROM exercises
        WHERE userId = :userId
        ORDER BY createdAt DESC, nameRu COLLATE NOCASE ASC
        """
    )
    suspend fun getCustomExercisesOnce(userId: String): List<ExerciseEntity>

    @Query(
        """
        SELECT exerciseId
        FROM exercise_user_meta
        WHERE userId = :userId AND isFavorite = 1
        """
    )
    fun observeFavoriteIds(userId: String): Flow<List<String>>

    @Query("SELECT * FROM exercise_user_meta WHERE userId = :userId")
    fun observeUserMeta(userId: String): Flow<List<ExerciseUserMetaEntity>>

    @Query(
        """
        SELECT * FROM exercises
        WHERE id IN (
            SELECT exerciseId
            FROM exercise_user_meta
            WHERE userId = :userId
              AND lastUsedAt IS NOT NULL
            ORDER BY lastUsedAt DESC
            LIMIT :limit
        )
        ORDER BY isCustom DESC, nameRu COLLATE NOCASE ASC
        """
    )
    fun observeRecentExercises(userId: String, limit: Int): Flow<List<ExerciseEntity>>

    @Query(
        """
        SELECT * FROM exercise_user_meta
        WHERE userId = :userId
          AND exerciseId = :exerciseId
        LIMIT 1
        """
    )
    suspend fun getUserMeta(userId: String, exerciseId: String): ExerciseUserMetaEntity?

    @Query("SELECT COUNT(*) FROM exercises WHERE userId = :globalUserId")
    suspend fun countGlobalExercises(globalUserId: String = ExerciseRepository.GLOBAL_USER_ID): Int

    @Query("DELETE FROM exercises WHERE userId = :globalUserId")
    suspend fun deleteGlobalExercises(globalUserId: String = ExerciseRepository.GLOBAL_USER_ID)
}
