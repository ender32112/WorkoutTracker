package com.example.workouttracker.data.exercise

import android.content.Context
import com.example.workouttracker.data.local.WorkoutTrackerDatabase
import com.example.workouttracker.data.settings.AppSettingsDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseSeedLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: WorkoutTrackerDatabase,
    private val settingsDataStore: AppSettingsDataStore
) {
    private val catalogVersion = 5
    private val gson = Gson()
    private val mutex = Mutex()

    suspend fun ensureLoaded() {
        mutex.withLock {
            val alreadyLoaded = settingsDataStore.exercisesLoadedFlow.first()
            val currentVersion = settingsDataStore.exercisesCatalogVersionFlow.first()
            val dao = database.exerciseCatalogDao()
            val globalCount = dao.countGlobalExercises()
            val needsReload = currentVersion < catalogVersion || globalCount < 100

            if ((alreadyLoaded || globalCount > 0) && !needsReload) {
                settingsDataStore.setExercisesLoaded(true)
                settingsDataStore.setExercisesCatalogVersion(catalogVersion)
                return
            }

            val (baseCatalog, localizationMap) = withContext(Dispatchers.IO) {
                val exercisesJson = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
                val exercisesType = object : TypeToken<List<ExerciseAssetDto>>() {}.type
                val catalog = gson.fromJson<List<ExerciseAssetDto>>(exercisesJson, exercisesType).orEmpty()

                val localizationJson = context.assets.open("exercise_localization_ru.json").bufferedReader().use { it.readText() }
                val localizationType = object : TypeToken<Map<String, ExerciseLocalizationDto>>() {}.type
                val localization = gson.fromJson<Map<String, ExerciseLocalizationDto>>(localizationJson, localizationType).orEmpty()

                catalog to localization
            }

            if (baseCatalog.isEmpty()) return

            if (needsReload && globalCount > 0) {
                dao.deleteGlobalExercises()
            }

            dao.insertExercises(baseCatalog.map { dto ->
                dto.toEntity(localizationMap[dto.id])
            })

            settingsDataStore.setExercisesLoaded(true)
            settingsDataStore.setExercisesCatalogVersion(catalogVersion)
        }
    }
}

private data class ExerciseAssetDto(
    val id: String,
    val name: String,
    val nameRu: String? = null,
    val gifUrl: String,
    val targetMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val bodyPart: String? = null,
    val bodyParts: List<String> = emptyList(),
    val equipment: String? = null,
    val equipments: List<String> = emptyList(),
    val instructions: List<String> = emptyList()
) {
    fun toEntity(localization: ExerciseLocalizationDto?): ExerciseEntity {
        val resolvedBodyPart = normalizeBodyPart(
            localization?.bodyPart ?: bodyPart ?: bodyParts.firstOrNull().orEmpty()
        )
        val resolvedEquipment = normalizeEquipment(
            localization?.equipment ?: equipment ?: equipments.firstOrNull().orEmpty()
        )
        val resolvedNameRu = normalizeRussianName(
            localization?.nameRu?.takeIf { it.isNotBlank() }
                ?: nameRu?.takeIf { it.isNotBlank() }
                ?: name
        )
        val resolvedInstructions = (localization?.instructionsRu?.takeIf { it.isNotEmpty() } ?: instructions)
            .map(::normalizeInstruction)
            .filter { it.isNotBlank() }

        return ExerciseEntity(
            id = id,
            userId = ExerciseRepository.GLOBAL_USER_ID,
            nameEn = name,
            nameRu = resolvedNameRu,
            gifUrl = gifUrl,
            localMediaUri = null,
            bodyPart = resolvedBodyPart,
            targetMuscles = (localization?.targetMuscles ?: targetMuscles).map(::normalizeMuscle).distinct(),
            secondaryMuscles = (localization?.secondaryMuscles ?: secondaryMuscles).map(::normalizeMuscle).distinct(),
            equipment = resolvedEquipment,
            instructions = resolvedInstructions
        )
    }
}

private data class ExerciseLocalizationDto(
    val nameRu: String? = null,
    val bodyPart: String? = null,
    val equipment: String? = null,
    val targetMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructionsRu: List<String> = emptyList()
)

private fun normalizeRussianName(value: String): String =
    russifyFitnessTerms(value)
        .replace(Regex("\\s+"), " ")
        .trim()

private fun normalizeBodyPart(value: String): String = when (value.trim().lowercase()) {
    "legs", "ноги" -> "Ноги"
    "back", "спина" -> "Спина"
    "arms", "руки" -> "Руки"
    "chest", "грудь" -> "Грудь"
    "shoulders", "плечи" -> "Плечи"
    "waist", "core", "abs", "кор" -> "Кор"
    else -> value.trim().ifBlank { "Другое" }
}

private fun normalizeEquipment(value: String): String = when (value.trim().lowercase()) {
    "body only", "bodyweight", "собственный вес" -> "Собственный вес"
    "machine", "тренажер", "тренажёр" -> "Тренажёр"
    "barbell", "штанга" -> "Штанга"
    "dumbbell", "гантели", "гантель" -> "Гантели"
    "kettlebells", "гиря", "гири" -> "Гиря"
    "bands", "резинки", "резинка" -> "Резинки"
    else -> value.trim().ifBlank { "Другое" }
}

private fun normalizeMuscle(value: String): String = when (value.trim().lowercase()) {
    "abdominals", "пресс" -> "Пресс"
    "hamstrings", "задняя поверхность бедра" -> "Задняя поверхность бедра"
    "quadriceps", "квадрицепсы" -> "Квадрицепсы"
    "glutes", "ягодицы" -> "Ягодицы"
    "lats", "широчайшие" -> "Широчайшие"
    "middle back", "средняя часть спины" -> "Средняя часть спины"
    "lower back", "нижняя часть спины" -> "Нижняя часть спины"
    "biceps", "бицепсы" -> "Бицепсы"
    "triceps", "трицепсы" -> "Трицепсы"
    "shoulders", "плечи" -> "Плечи"
    "chest", "грудь" -> "Грудь"
    "calves", "икры" -> "Икры"
    else -> value.trim()
}

private fun normalizeInstruction(value: String): String =
    russifyFitnessTerms(value)
        .replace('\u00A0', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

private fun russifyFitnessTerms(value: String): String =
    value
        .replace("Atlas Stone", "камень Атласа")
        .replace("Preacher Curl", "сгибание на скамье Скотта")
        .replace("Skull Crusher", "французский жим")
        .replace("Skullcrusher", "французский жим")
        .replace("Power Clean", "силовой подъём на грудь")
        .replace("See-Saw Press", "попеременный жим")
        .replace("Sled Drag", "тяга саней")
        .replace("IT Band", "илиотибиальный тракт")
        .replace("Bosu Ball", "полусфера босу")
        .replace("J-hooks", "крючки стойки")
        .replace("J-hook", "крючок стойки")
        .replace("E-Z", "изи")
        .replace("EZ-Bar", "изи-гриф")
        .replace("EZ", "изи")
        .replace("SMR", "миофасциальный релиз")
