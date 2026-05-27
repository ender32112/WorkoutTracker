package com.example.workouttracker.feature.training.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.core.auth.AuthSessionStore
import com.example.workouttracker.data.exercise.CustomExerciseDraft
import com.example.workouttracker.data.exercise.Exercise
import com.example.workouttracker.data.exercise.ExerciseRepository
import com.example.workouttracker.data.exercise.ExerciseSource
import com.example.workouttracker.data.local.ActiveWorkoutStateEntity
import com.example.workouttracker.data.local.PerformedExerciseDraft
import com.example.workouttracker.data.local.PerformedSessionWithExercises
import com.example.workouttracker.data.local.PerformedSetDraft
import com.example.workouttracker.data.local.UserEntity
import com.example.workouttracker.data.local.WorkoutTemplateEntity
import com.example.workouttracker.data.local.WorkoutTemplateExerciseEntity
import com.example.workouttracker.data.local.WorkoutTemplateWithExercises
import com.example.workouttracker.data.local.WorkoutTrackerDao
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TrainingViewModel @Inject constructor(
    authSessionStore: AuthSessionStore,
    private val dao: WorkoutTrackerDao,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val userId = authSessionStore.currentUserIdOrGuest()

    private val _activeWorkout = MutableStateFlow<ActiveWorkoutUiState?>(null)
    val activeWorkout: StateFlow<ActiveWorkoutUiState?> = _activeWorkout

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _restTimerFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val restTimerFinished: SharedFlow<Unit> = _restTimerFinished.asSharedFlow()

    private val gson = Gson()
    private var timerJob: Job? = null

    val allExercises: StateFlow<List<ExerciseCatalogItem>> = exerciseRepository.observeExercises(userId)
        .map { list -> list.map(::exerciseToCatalogItem) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<ExerciseCatalogItem>> = exerciseRepository.observeFavorites(userId)
        .map { list -> list.map(::exerciseToCatalogItem) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentExercises: StateFlow<List<ExerciseCatalogItem>> = exerciseRepository.observeRecent(userId)
        .map { list -> list.map(::exerciseToCatalogItem) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val quickAddExercises: StateFlow<List<ExerciseCatalogItem>> = combine(
        favorites,
        recentExercises,
        allExercises
    ) { favoriteItems, recentItems, allItems ->
        (favoriteItems + recentItems + allItems)
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<ExerciseCatalogItem> { it.favorite }
                    .thenByDescending { it.lastUsedAt ?: 0L }
                    .thenByDescending { it.isCustom }
                    .thenBy { it.name.lowercase() }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exercisePr: StateFlow<List<ExercisePrUi>> = dao.observeExercisePr(userId)
        .map { rows -> rows.map { ExercisePrUi(it.exerciseId, it.exerciseName, it.bestVolumeSet, it.bestE1rm) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val exercisePrMap: StateFlow<Map<String, ExercisePrUi>> = exercisePr
        .map { list -> list.associateBy { it.exerciseId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val sessions: StateFlow<List<TrainingSession>> = combine(
        dao.observePerformedSessionsWithExercises(userId),
        exerciseRepository.observeExercises(userId),
        exercisePrMap
    ) { list, catalog, prMap ->
        val catalogById = catalog.associateBy { it.id }
        list.map { mapPerformedSessionToUi(it, catalogById, prMap) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: StateFlow<List<WorkoutTemplateUi>> = combine(
        dao.observeWorkoutTemplatesWithExercises(userId),
        exerciseRepository.observeExercises(userId)
    ) { list, catalog ->
        val catalogById = catalog.associateBy { it.id }
        list.map { templateToUi(it, catalogById) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyVolume: StateFlow<List<WeeklyVolumeUi>> = dao.observeWeeklyVolume(userId)
        .map { rows -> rows.map { WeeklyVolumeUi(it.weekKey, it.volume) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (dao.getUserById(userId) == null) {
                dao.upsertUser(UserEntity(id = userId, name = "Local user", email = "$userId@local"))
            }
            exerciseRepository.ensureLoaded()
            exerciseRepository.migrateLegacyCatalog(userId, dao.getExercisesOnce(userId))
            restoreActiveWorkout()
        }
    }

    fun addOrUpdateExercise(
        id: Long? = null,
        name: String,
        muscles: List<String>,
        equipment: String?,
        aliases: String,
        favorite: Boolean,
        photoUri: String?
    ) {
        viewModelScope.launch {
            val safeName = name.trim()
            if (safeName.isBlank()) {
                notify("Введите название упражнения")
                return@launch
            }
            val customId = id?.let { legacyCustomId(it) } ?: newCustomExerciseId()
            exerciseRepository.upsertCustomExercise(
                CustomExerciseDraft(
                    id = customId,
                    userId = userId,
                    nameRu = safeName,
                    nameEn = aliases.trim().ifBlank { safeName },
                    bodyPart = inferBodyPartFromMuscles(muscles),
                    targetMuscles = muscles,
                    secondaryMuscles = emptyList(),
                    equipment = equipment?.trim().orEmpty(),
                    instructions = emptyList(),
                    localMediaUri = photoUri
                )
            )
            if (favorite) {
                exerciseRepository.setFavorite(userId, customId, true)
            }
            notify("Упражнение сохранено")
        }
    }

    fun updateExercisePhoto(exerciseId: Long, photoUri: String?) {
        viewModelScope.launch {
            val id = legacyCustomId(exerciseId)
            val current = exerciseRepository.getExerciseDetailsOnce(userId, id) ?: return@launch
            exerciseRepository.upsertCustomExercise(
                CustomExerciseDraft(
                    id = current.id,
                    userId = userId,
                    nameRu = current.nameRu,
                    nameEn = current.nameEn,
                    bodyPart = current.bodyPart,
                    targetMuscles = current.targetMuscles,
                    secondaryMuscles = current.secondaryMuscles,
                    equipment = current.equipment,
                    instructions = current.instructions,
                    localMediaUri = photoUri
                )
            )
            notify("Медиа упражнения обновлено")
        }
    }

    fun deleteExercise(id: Long) {
        viewModelScope.launch {
            exerciseRepository.deleteCustomExercise(userId, legacyCustomId(id))
        }
    }

    fun toggleFavorite(id: Long, value: Boolean) {
        viewModelScope.launch {
            exerciseRepository.setFavorite(userId, legacyCustomId(id), value)
        }
    }

    fun createTemplate(title: String) {
        viewModelScope.launch {
            val safe = title.trim()
            if (safe.isBlank()) return@launch
            dao.upsertWorkoutTemplate(WorkoutTemplateEntity(userId = userId, title = safe))
        }
    }

    fun addExerciseToTemplate(
        templateId: Long,
        exerciseId: String,
        defaultSets: Int,
        defaultReps: Int,
        order: Int
    ) {
        viewModelScope.launch {
            dao.upsertWorkoutTemplateExercise(
                WorkoutTemplateExerciseEntity(
                    templateId = templateId,
                    catalogExerciseId = exerciseId,
                    orderInTemplate = order,
                    defaultSets = defaultSets,
                    defaultReps = defaultReps
                )
            )
        }
    }

    fun addExerciseToTemplate(
        templateId: Long,
        exercise: ExerciseCatalogItem,
        defaultSets: Int,
        defaultReps: Int,
        order: Int
    ) {
        addExerciseToTemplate(templateId, exercise.id, defaultSets, defaultReps, order)
    }

    fun addLibraryExerciseToTemplate(
        templateId: Long,
        exercise: Exercise,
        defaultSets: Int,
        defaultReps: Int,
        order: Int
    ) {
        addExerciseToTemplate(templateId, exercise.id, defaultSets, defaultReps, order)
    }

    fun removeExerciseFromTemplate(entryId: Long) {
        viewModelScope.launch {
            dao.deleteWorkoutTemplateExercise(userId, entryId)
        }
    }

    fun startWorkoutFromTemplate(templateId: Long) {
        viewModelScope.launch {
            val template = dao.getWorkoutTemplateWithExercises(userId, templateId) ?: return@launch
            val catalogById = exerciseRepository.snapshotExercises(userId)
            _activeWorkout.value = ActiveWorkoutUiState(
                startedAt = System.currentTimeMillis(),
                exercises = template.exercises
                    .sortedBy { it.orderInTemplate }
                    .map { entry ->
                        val exercise = catalogById[entry.catalogExerciseId]
                        WorkoutExerciseInput(
                            instanceId = newInstanceId(),
                            exerciseId = entry.catalogExerciseId,
                            exerciseName = exercise?.nameRu ?: "Упражнение недоступно",
                            exerciseNameEn = exercise?.nameEn,
                            bodyPart = exercise?.bodyPart,
                            muscles = exercise?.targetMuscles.orEmpty(),
                            equipment = exercise?.equipment,
                            mediaUri = exercise?.gifUrl,
                            localMediaUri = exercise?.localMediaUri,
                            sets = List(entry.defaultSets.coerceAtLeast(1)) {
                                ExerciseSetInput(
                                    weight = entry.defaultWeight?.toString().orEmpty(),
                                    reps = entry.defaultReps.toString()
                                )
                            }
                        )
                    }
            )
            persistActiveWorkout()
        }
    }

    fun repeatSession(sessionId: Long) {
        val session = sessions.value.firstOrNull { it.sessionId == sessionId } ?: return
        _activeWorkout.value = ActiveWorkoutUiState(
            startedAt = System.currentTimeMillis(),
            exercises = session.exercises.map { ex ->
                WorkoutExerciseInput(
                    instanceId = newInstanceId(),
                    exerciseId = ex.exerciseId,
                    exerciseName = ex.name,
                    exerciseNameEn = ex.nameEn,
                    bodyPart = ex.bodyPart,
                    muscles = ex.muscles,
                    equipment = ex.equipment,
                    mediaUri = ex.mediaUri,
                    localMediaUri = ex.localMediaUri,
                    sets = ex.sets.map { set ->
                        ExerciseSetInput(weight = set.weight.toString(), reps = set.reps.toString())
                    }
                )
            }
        )
        persistActiveWorkout()
    }

    fun observeSessionDetail(sessionId: Long) = combine(
        dao.observePerformedSessionDetail(userId, sessionId),
        exerciseRepository.observeExercises(userId),
        exercisePrMap
    ) { session, catalog, prMap ->
        val catalogById = catalog.associateBy { it.id }
        session?.let { mapPerformedSessionToUi(it, catalogById, prMap) }
    }

    fun startWorkout() {
        _activeWorkout.value = ActiveWorkoutUiState(startedAt = System.currentTimeMillis())
        persistActiveWorkout()
        notify("Тренировка начата")
    }

    fun discardWorkout() {
        if (_activeWorkout.value == null) return
        timerJob?.cancel()
        _activeWorkout.value = null
        viewModelScope.launch {
            dao.clearActiveWorkoutState(userId)
        }
        notify("Тренировка отменена")
    }

    fun addExerciseToActiveWorkout(exercise: ExerciseCatalogItem) {
        appendExerciseToActiveWorkout(exerciseToDomain(exercise))
        viewModelScope.launch { exerciseRepository.markUsed(userId, exercise.id) }
        notify("Добавлено: ${exercise.name}")
    }

    fun addExerciseFromLibrary(exercise: Exercise) {
        appendExerciseToActiveWorkout(exercise)
        viewModelScope.launch { exerciseRepository.markUsed(userId, exercise.id) }
        notify("Добавлено из библиотеки: ${exercise.nameRu}")
    }

    fun removeExercise(instanceId: String) {
        val current = _activeWorkout.value ?: return
        _activeWorkout.value = current.copy(
            exercises = removeExerciseFromWorkout(current.exercises, instanceId)
        )
        persistActiveWorkout()
    }

    private fun appendExerciseToActiveWorkout(exercise: Exercise) {
        val current = _activeWorkout.value ?: ActiveWorkoutUiState(startedAt = System.currentTimeMillis())
        _activeWorkout.value = current.copy(
            exercises = current.exercises + WorkoutExerciseInput(
                instanceId = newInstanceId(),
                exerciseId = exercise.id,
                exerciseName = exercise.nameRu,
                exerciseNameEn = exercise.nameEn,
                bodyPart = exercise.bodyPart,
                muscles = (exercise.targetMuscles + exercise.secondaryMuscles).distinct(),
                equipment = exercise.equipment,
                mediaUri = exercise.gifUrl,
                localMediaUri = exercise.localMediaUri
            )
        )
        persistActiveWorkout()
    }

    fun addSet(instanceId: String) {
        mutateExercise(instanceId) { exercise ->
            exercise.copy(sets = exercise.sets + ExerciseSetInput())
        }
    }

    fun removeSet(instanceId: String, setIndex: Int) {
        val current = _activeWorkout.value ?: return
        val updatedExercises = removeSetFromWorkout(current.exercises, instanceId, setIndex)
        _activeWorkout.value = current.copy(exercises = updatedExercises)
        persistActiveWorkout()
    }

    fun updateSet(instanceId: String, setIndex: Int, weight: String? = null, reps: String? = null) {
        mutateExercise(instanceId) { exercise ->
            exercise.copy(
                sets = exercise.sets.mapIndexed { index, set ->
                    if (index != setIndex) set else set.copy(
                        weight = weight ?: set.weight,
                        reps = reps ?: set.reps
                    )
                }
            )
        }
    }

    fun startRestTimer(seconds: Int = 60) {
        timerJob?.cancel()
        val current = _activeWorkout.value ?: return
        _activeWorkout.value = current.copy(restTimerSecondsLeft = seconds, timerRunning = true)
        persistActiveWorkout()
        timerJob = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(1_000)
                left -= 1
                _activeWorkout.value = _activeWorkout.value?.copy(
                    restTimerSecondsLeft = left,
                    timerRunning = left > 0
                )
                persistActiveWorkout()
            }
            _restTimerFinished.tryEmit(Unit)
            notify("Отдых завершён")
        }
    }

    fun skipRestTimer() {
        timerJob?.cancel()
        _activeWorkout.value = _activeWorkout.value?.copy(restTimerSecondsLeft = 0, timerRunning = false)
        persistActiveWorkout()
    }

    fun restartRestTimer(seconds: Int = 60) = startRestTimer(seconds)

    fun finishWorkout() {
        val active = _activeWorkout.value ?: return
        viewModelScope.launch {
            val validationError = validateExercises(active.exercises)
            if (validationError != null) {
                notify(validationError)
                return@launch
            }

            val performed = active.exercises.map { exercise ->
                PerformedExerciseDraft(
                    exerciseId = exercise.exerciseId,
                    exerciseName = exercise.exerciseName,
                    sets = exercise.sets.map { set ->
                        PerformedSetDraft(weight = set.weight.toFloat(), reps = set.reps.toInt())
                    }
                )
            }

            runCatching {
                dao.persistWorkoutPerformed(
                    userId = userId,
                    startedAt = active.startedAt,
                    finishedAt = System.currentTimeMillis(),
                    exercises = performed
                )
                active.exercises.forEach { exercise ->
                    exerciseRepository.markUsed(userId, exercise.exerciseId)
                }
            }.onSuccess {
                timerJob?.cancel()
                _activeWorkout.value = null
                notify("Тренировка сохранена")
            }.onFailure {
                notify("Не удалось сохранить тренировку")
            }
        }
    }

    fun deleteCompletedSession(sessionId: Long) {
        viewModelScope.launch {
            dao.deleteWorkoutSession(userId, sessionId)
            notify("Тренировка удалена")
        }
    }

    fun saveEditedCompletedSession(sessionId: Long, exercises: List<WorkoutExerciseInput>) {
        viewModelScope.launch {
            val validationError = validateExercises(exercises)
            if (validationError != null) {
                notify(validationError)
                return@launch
            }

            val performed = exercises.map { exercise ->
                PerformedExerciseDraft(
                    exerciseId = exercise.exerciseId,
                    exerciseName = exercise.exerciseName,
                    sets = exercise.sets.map { set ->
                        PerformedSetDraft(
                            weight = set.weight.toFloat(),
                            reps = set.reps.toInt()
                        )
                    }
                )
            }

            runCatching {
                dao.replacePerformedSessionExercises(userId, sessionId, performed)
            }.onSuccess {
                notify("Изменения тренировки сохранены")
            }.onFailure {
                notify("Не удалось сохранить изменения тренировки")
            }
        }
    }

    private fun mutateExercise(instanceId: String, mapper: (WorkoutExerciseInput) -> WorkoutExerciseInput) {
        val current = _activeWorkout.value ?: return
        _activeWorkout.value = current.copy(
            exercises = current.exercises.map { exercise ->
                if (exercise.instanceId == instanceId) mapper(exercise) else exercise
            }
        )
        persistActiveWorkout()
    }

    private fun persistActiveWorkout() {
        val active = _activeWorkout.value ?: return
        viewModelScope.launch {
            dao.upsertActiveWorkoutState(
                ActiveWorkoutStateEntity(
                    userId = userId,
                    startedAt = active.startedAt,
                    updatedAt = System.currentTimeMillis(),
                    payloadJson = gson.toJson(active)
                )
            )
        }
    }

    private suspend fun restoreActiveWorkout() {
        val state = dao.getActiveWorkoutState(userId) ?: return
        try {
            val active = gson.fromJson(state.payloadJson, ActiveWorkoutUiState::class.java)
            val startedAt = active?.startedAt ?: state.startedAt
            val restored = active?.copy(startedAt = startedAt)
            if (restored == null) {
                _activeWorkout.value = null
                return
            }
            if (restored.timerRunning && restored.restTimerSecondsLeft > 0) {
                val elapsedSeconds = ((System.currentTimeMillis() - state.updatedAt) / 1_000L)
                    .toInt()
                    .coerceAtLeast(0)
                val remaining = (restored.restTimerSecondsLeft - elapsedSeconds).coerceAtLeast(0)
                _activeWorkout.value = restored.copy(
                    restTimerSecondsLeft = remaining,
                    timerRunning = remaining > 0
                )
                if (remaining > 0) startRestTimer(remaining)
            } else {
                _activeWorkout.value = restored
            }
        } catch (_: Exception) {
            _activeWorkout.value = null
        }
    }

    private fun notify(message: String) {
        _messages.tryEmit(message)
    }

    private fun validateExercises(exercises: List<WorkoutExerciseInput>): String? {
        if (exercises.isEmpty()) return "Добавьте хотя бы одно упражнение"
        exercises.forEach { exercise ->
            if (exercise.sets.isEmpty()) {
                return "У упражнения ${exercise.exerciseName} нет подходов"
            }
            exercise.sets.forEachIndexed { index, set ->
                val reps = set.reps.toIntOrNull()
                val weight = set.weight.toFloatOrNull()
                if (reps == null || reps <= 0) {
                    return "Проверьте повторы в ${exercise.exerciseName}, подход ${index + 1}"
                }
                if (weight == null || weight < 0f) {
                    return "Проверьте вес в ${exercise.exerciseName}, подход ${index + 1}"
                }
            }
        }
        return null
    }

    private fun templateToUi(
        templateWithExercises: WorkoutTemplateWithExercises,
        catalogById: Map<String, Exercise>
    ): WorkoutTemplateUi {
        return WorkoutTemplateUi(
            id = templateWithExercises.template.id,
            title = templateWithExercises.template.title,
            exercises = templateWithExercises.exercises
                .sortedBy { it.orderInTemplate }
                .map { entry ->
                    val exercise = catalogById[entry.catalogExerciseId]
                    WorkoutTemplateExerciseUi(
                        id = entry.id,
                        exerciseId = entry.catalogExerciseId,
                        name = exercise?.nameRu ?: "Упражнение недоступно",
                        nameEn = exercise?.nameEn,
                        muscles = exercise?.targetMuscles.orEmpty(),
                        bodyPart = exercise?.bodyPart,
                        equipment = exercise?.equipment,
                        mediaUri = exercise?.gifUrl,
                        localMediaUri = exercise?.localMediaUri,
                        isMissing = exercise == null,
                        orderInTemplate = entry.orderInTemplate,
                        defaultSets = entry.defaultSets,
                        defaultReps = entry.defaultReps,
                        defaultWeight = entry.defaultWeight
                    )
                }
        )
    }

    private fun exerciseToCatalogItem(exercise: Exercise) = ExerciseCatalogItem(
        id = exercise.id,
        name = exercise.nameRu,
        nameEn = exercise.nameEn,
        aliases = exercise.nameEn,
        muscles = (exercise.targetMuscles + exercise.secondaryMuscles).distinct(),
        bodyPart = exercise.bodyPart,
        equipment = exercise.equipment,
        favorite = exercise.isFavorite,
        mediaUri = exercise.gifUrl.ifBlank { null },
        localMediaUri = exercise.localMediaUri,
        isCustom = exercise.isCustom,
        source = if (exercise.source == ExerciseSource.CUSTOM) ExerciseSourceUi.CUSTOM else ExerciseSourceUi.GLOBAL,
        lastUsedAt = exercise.lastUsedAt
    )

    private fun exerciseToDomain(exercise: ExerciseCatalogItem) = Exercise(
        id = exercise.id,
        nameEn = exercise.nameEn,
        nameRu = exercise.name,
        gifUrl = exercise.mediaUri.orEmpty(),
        localMediaUri = exercise.localMediaUri,
        bodyPart = exercise.bodyPart,
        targetMuscles = exercise.muscles,
        secondaryMuscles = emptyList(),
        equipment = exercise.equipment,
        instructions = emptyList(),
        isFavorite = exercise.favorite,
        isCustom = exercise.isCustom,
        source = if (exercise.source == ExerciseSourceUi.CUSTOM) ExerciseSource.CUSTOM else ExerciseSource.GLOBAL,
        lastUsedAt = exercise.lastUsedAt
    )

    private fun legacyCustomId(id: Long): String = "custom:$userId:$id"

    private fun newCustomExerciseId(): String = "custom:$userId:${UUID.randomUUID()}"

    private fun newInstanceId(): String = UUID.randomUUID().toString()

    companion object {
        fun mapPerformedSessionToUi(
            session: PerformedSessionWithExercises,
            catalogById: Map<String, Exercise>,
            prMap: Map<String, ExercisePrUi>
        ): TrainingSession {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val exercises = session.exercises.map { performedExercise ->
                val catalogExercise = catalogById[performedExercise.exerciseEntity.catalogExerciseId]
                ExerciseEntry(
                    exerciseId = performedExercise.exerciseEntity.catalogExerciseId,
                    name = catalogExercise?.nameRu ?: performedExercise.exerciseEntity.exerciseNameSnapshot,
                    nameEn = catalogExercise?.nameEn,
                    muscles = (catalogExercise?.targetMuscles.orEmpty() + catalogExercise?.secondaryMuscles.orEmpty()).distinct(),
                    bodyPart = catalogExercise?.bodyPart,
                    equipment = catalogExercise?.equipment,
                    sets = performedExercise.sets
                        .sortedBy { it.setOrder }
                        .map { ExerciseSetSummary(order = it.setOrder + 1, weight = it.weight, reps = it.reps) },
                    mediaUri = catalogExercise?.gifUrl,
                    localMediaUri = catalogExercise?.localMediaUri,
                    pr = prMap[performedExercise.exerciseEntity.catalogExerciseId]
                )
            }
            return TrainingSession(
                sessionId = session.session.id,
                startedAt = session.session.startedAt,
                finishedAt = session.session.finishedAt,
                date = format.format(Date(session.session.startedAt)),
                exercises = exercises
            )
        }

        private fun inferBodyPartFromMuscles(muscles: List<String>): String {
            val raw = muscles.joinToString(" ").lowercase()
            return when {
                raw.contains("груд") -> "Грудь"
                raw.contains("спин") -> "Спина"
                raw.contains("ягод") || raw.contains("квад") || raw.contains("бед") -> "Ноги"
                raw.contains("плеч") || raw.contains("дельт") -> "Плечи"
                raw.contains("бицепс") || raw.contains("трицепс") -> "Руки"
                raw.contains("пресс") || raw.contains("кор") -> "Кор"
                else -> "Другое"
            }
        }
    }
}
