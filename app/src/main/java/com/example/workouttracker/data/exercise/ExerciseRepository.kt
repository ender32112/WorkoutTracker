package com.example.workouttracker.data.exercise

import com.example.workouttracker.data.local.ExerciseEntity as LegacyExerciseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class Exercise(
    val id: String,
    val nameEn: String,
    val nameRu: String,
    val gifUrl: String,
    val localMediaUri: String?,
    val bodyPart: String,
    val targetMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val instructions: List<String>,
    val isFavorite: Boolean,
    val isCustom: Boolean,
    val source: ExerciseSource,
    val lastUsedAt: Long?
)

data class ExerciseFilters(
    val query: String = "",
    val muscle: String? = null,
    val bodyPart: String? = null,
    val favoritesOnly: Boolean = false,
    val source: ExerciseSourceFilter = ExerciseSourceFilter.ALL
)

data class CustomExerciseDraft(
    val id: String,
    val userId: String,
    val nameRu: String,
    val nameEn: String,
    val bodyPart: String,
    val targetMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: String,
    val instructions: List<String>,
    val localMediaUri: String? = null
)

enum class ExerciseSourceFilter {
    ALL,
    GLOBAL,
    CUSTOM
}

class ExerciseRepository(
    private val dao: ExerciseCatalogDao,
    private val seedLoader: ExerciseSeedLoader
) {
    suspend fun ensureLoaded() = seedLoader.ensureLoaded()

    fun observeExercises(userId: String, filters: ExerciseFilters = ExerciseFilters()): Flow<List<Exercise>> =
        combine(
            dao.observeFilteredExercises(
                userId = userId,
                query = filters.query.trim(),
                muscle = filters.muscle.orEmpty(),
                bodyPart = filters.bodyPart.orEmpty(),
                favoritesOnly = filters.favoritesOnly,
                sourceFilter = when (filters.source) {
                    ExerciseSourceFilter.ALL -> ""
                    ExerciseSourceFilter.GLOBAL -> ExerciseSource.GLOBAL.name
                    ExerciseSourceFilter.CUSTOM -> ExerciseSource.CUSTOM.name
                }
            ),
            dao.observeUserMeta(userId)
        ) { exercises, meta ->
            mapWithMeta(exercises, meta)
        }

    fun observeFavorites(userId: String): Flow<List<Exercise>> =
        observeExercises(userId, ExerciseFilters(favoritesOnly = true))

    fun observeRecent(userId: String, limit: Int = 8): Flow<List<Exercise>> =
        combine(
            dao.observeRecentExercises(userId, limit),
            dao.observeUserMeta(userId)
        ) { exercises, meta ->
            mapWithMeta(exercises, meta).sortedByDescending { it.lastUsedAt ?: 0L }
        }

    fun observeTemplatePicker(userId: String, filters: ExerciseFilters = ExerciseFilters()): Flow<List<Exercise>> =
        observeExercises(userId, filters)

    fun getExerciseDetails(userId: String, id: String): Flow<Exercise?> =
        combine(dao.getExerciseById(userId, id), dao.observeUserMeta(userId)) { exercise, meta ->
            exercise?.toDomain(meta.firstOrNull { it.exerciseId == exercise.id })
        }

    suspend fun getExerciseDetailsOnce(userId: String, id: String): Exercise? {
        val exercise = dao.getExerciseByIdOnce(userId, id) ?: return null
        val meta = dao.getUserMeta(userId, id)
        return exercise.toDomain(meta)
    }

    suspend fun setFavorite(userId: String, exerciseId: String, favorite: Boolean) {
        val existing = dao.getUserMeta(userId, exerciseId)
        dao.upsertUserMeta(
            ExerciseUserMetaEntity(
                userId = userId,
                exerciseId = exerciseId,
                isFavorite = favorite,
                lastUsedAt = existing?.lastUsedAt,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun markUsed(userId: String, exerciseId: String, usedAt: Long = System.currentTimeMillis()) {
        val existing = dao.getUserMeta(userId, exerciseId)
        dao.upsertUserMeta(
            ExerciseUserMetaEntity(
                userId = userId,
                exerciseId = exerciseId,
                isFavorite = existing?.isFavorite ?: false,
                lastUsedAt = usedAt,
                updatedAt = usedAt
            )
        )
    }

    suspend fun upsertCustomExercise(draft: CustomExerciseDraft) {
        dao.upsertExercise(
            ExerciseEntity(
                id = draft.id,
                userId = draft.userId,
                nameEn = draft.nameEn.ifBlank { draft.nameRu },
                nameRu = draft.nameRu,
                gifUrl = "",
                localMediaUri = draft.localMediaUri,
                bodyPart = draft.bodyPart,
                targetMuscles = draft.targetMuscles.distinct(),
                secondaryMuscles = draft.secondaryMuscles.distinct(),
                equipment = draft.equipment,
                instructions = draft.instructions.filter { it.isNotBlank() },
                source = ExerciseSource.CUSTOM,
                isCustom = true
            )
        )
    }

    suspend fun deleteCustomExercise(userId: String, id: String) {
        dao.deleteCustomExercise(userId, id)
    }

    suspend fun migrateLegacyCatalog(userId: String, legacyExercises: List<LegacyExerciseEntity>) {
        if (legacyExercises.isEmpty()) return
        val currentCustom = dao.getCustomExercisesOnce(userId).associateBy { it.id }
        legacyExercises.forEach { legacy ->
            val mappedId = legacy.toUnifiedId(userId)
            if (mappedId !in currentCustom) {
                dao.upsertExercise(
                    ExerciseEntity(
                        id = mappedId,
                        userId = if (legacy.sourceExerciseId != null) GLOBAL_USER_ID else userId,
                        nameEn = legacy.aliases?.takeIf { it.isNotBlank() } ?: legacy.name,
                        nameRu = legacy.name,
                        gifUrl = legacy.photoUri.takeUnless { it.isNullOrBlank() || it.startsWith("/") }.orEmpty(),
                        localMediaUri = legacy.photoUri.takeIf { !it.isNullOrBlank() && it.startsWith("/") },
                        bodyPart = inferBodyPart(legacy.muscles),
                        targetMuscles = parseLegacyMuscles(legacy.muscles),
                        secondaryMuscles = emptyList(),
                        equipment = legacy.equipment.orEmpty(),
                        instructions = emptyList(),
                        source = if (legacy.sourceExerciseId != null) ExerciseSource.GLOBAL else ExerciseSource.CUSTOM,
                        isCustom = legacy.sourceExerciseId == null
                    )
                )
            }
            if (legacy.isFavorite || legacy.lastUsedAt != null) {
                val existing = dao.getUserMeta(userId, mappedId)
                dao.upsertUserMeta(
                    ExerciseUserMetaEntity(
                        userId = userId,
                        exerciseId = mappedId,
                        isFavorite = legacy.isFavorite || (existing?.isFavorite ?: false),
                        lastUsedAt = maxOf(legacy.lastUsedAt ?: 0L, existing?.lastUsedAt ?: 0L).takeIf { it > 0L },
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun hasGlobalExercises(): Boolean = dao.countGlobalExercises() > 0

    suspend fun snapshotExercises(userId: String): Map<String, Exercise> =
        observeExercises(userId).first().associateBy { it.id }

    companion object {
        const val GLOBAL_USER_ID = "__global__"
    }
}

private fun mapWithMeta(
    exercises: List<ExerciseEntity>,
    meta: List<ExerciseUserMetaEntity>
): List<Exercise> {
    val metaByExerciseId = meta.associateBy { it.exerciseId }
    return exercises
        .map { it.toDomain(metaByExerciseId[it.id]) }
        .sortedWith(
            compareByDescending<Exercise> { it.isFavorite }
                .thenByDescending { it.lastUsedAt ?: 0L }
                .thenByDescending { it.isCustom }
                .thenBy { it.nameRu.lowercase() }
        )
}

private fun ExerciseEntity.toDomain(meta: ExerciseUserMetaEntity?) = Exercise(
    id = id,
    nameEn = nameEn,
    nameRu = nameRu,
    gifUrl = gifUrl,
    localMediaUri = localMediaUri,
    bodyPart = bodyPart,
    targetMuscles = targetMuscles,
    secondaryMuscles = secondaryMuscles,
    equipment = equipment,
    instructions = instructions,
    isFavorite = meta?.isFavorite == true,
    isCustom = isCustom,
    source = source,
    lastUsedAt = meta?.lastUsedAt
)

private fun LegacyExerciseEntity.toUnifiedId(userId: String): String =
    sourceExerciseId ?: "custom:$userId:$id"

private fun parseLegacyMuscles(raw: String): List<String> =
    raw.split(",").map { it.trim() }.filter { it.isNotBlank() }

private fun inferBodyPart(rawMuscles: String): String {
    val value = rawMuscles.lowercase()
    return when {
        value.contains("груд") -> "Грудь"
        value.contains("спин") -> "Спина"
        value.contains("ягод") -> "Ягодицы"
        value.contains("квад") || value.contains("бед") || value.contains("икр") -> "Ноги"
        value.contains("плеч") || value.contains("дельт") -> "Плечи"
        value.contains("бицепс") || value.contains("трицепс") -> "Руки"
        value.contains("пресс") || value.contains("кор") -> "Кор"
        else -> "Другое"
    }
}
