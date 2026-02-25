package com.example.workouttracker.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.data.local.ActiveWorkoutStateEntity
import com.example.workouttracker.data.local.ExerciseEntity
import com.example.workouttracker.data.local.PerformedExerciseDraft
import com.example.workouttracker.data.local.PerformedSessionWithExercises
import com.example.workouttracker.data.local.PerformedSetDraft
import com.example.workouttracker.data.local.TemplateExerciseWithDetails
import com.example.workouttracker.data.local.UserEntity
import com.example.workouttracker.data.local.WorkoutTemplateEntity
import com.example.workouttracker.data.local.WorkoutTemplateExerciseEntity
import com.example.workouttracker.data.local.WorkoutTemplateWithExercises
import com.example.workouttracker.data.local.WorkoutTrackerDatabase
import com.example.workouttracker.ui.training.ActiveWorkoutUiState
import com.example.workouttracker.ui.training.ExerciseCatalogItem
import com.example.workouttracker.ui.training.ExerciseEntry
import com.example.workouttracker.ui.training.ExercisePrUi
import com.example.workouttracker.ui.training.ExerciseSetInput
import com.example.workouttracker.ui.training.ExerciseSetSummary
import com.example.workouttracker.ui.training.TrainingSession
import com.example.workouttracker.ui.training.WeeklyVolumeUi
import com.example.workouttracker.ui.training.WorkoutExerciseInput
import com.example.workouttracker.ui.training.WorkoutTemplateExerciseUi
import com.example.workouttracker.ui.training.WorkoutTemplateUi
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainingViewModel(application: Application) : AndroidViewModel(application) {
    private val authPrefs = application.getSharedPreferences(AuthViewModel.AUTH_PREFS_NAME, Context.MODE_PRIVATE)
    private val userId = authPrefs.getString(AuthViewModel.KEY_CURRENT_USER_ID, null) ?: "guest"

    private val dao = WorkoutTrackerDatabase.getInstance(application).dao()

    private val searchQuery = MutableStateFlow("")
    private val muscleFilter = MutableStateFlow<String?>(null)

    private val _activeWorkout = MutableStateFlow<ActiveWorkoutUiState?>(null)
    val activeWorkout: StateFlow<ActiveWorkoutUiState?> = _activeWorkout

    private var timerJob: Job? = null
    private val gson = Gson()

    val allExercises: StateFlow<List<ExerciseCatalogItem>> = dao.observeExercises(userId)
        .map { list -> list.map(::exerciseEntityToUi) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quickAddExercises: StateFlow<List<ExerciseCatalogItem>> = combine(
        searchQuery,
        muscleFilter,
        dao.observeExercises(userId)
    ) { query, muscle, all ->
        all.filter { ex ->
            val queryPass = query.isBlank() || ex.name.contains(query, true) || (ex.aliases ?: "").contains(query, true)
            val musclePass = muscle.isNullOrBlank() || ex.muscles.contains(muscle, true)
            queryPass && musclePass
        }.sortedWith(compareByDescending<ExerciseEntity> { it.isFavorite }.thenByDescending { it.lastUsedAt ?: 0L }.thenBy { it.name })
            .map(::exerciseEntityToUi)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<ExerciseCatalogItem>> = dao.observeFavorites(userId)
        .map { it.map(::exerciseEntityToUi) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentExercises: StateFlow<List<ExerciseCatalogItem>> = dao.observeRecentExercises(userId)
        .map { it.map(::exerciseEntityToUi) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercisePr: StateFlow<List<ExercisePrUi>> = dao.observeExercisePr(userId)
        .map { rows -> rows.map { ExercisePrUi(it.exerciseName, it.bestVolumeSet, it.bestE1rm) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercisePrMap: StateFlow<Map<String, ExercisePrUi>> = exercisePr
        .map { list -> list.associateBy { it.exerciseName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val sessions: StateFlow<List<TrainingSession>> = combine(
        dao.observePerformedSessionsWithExercises(userId),
        dao.observeExercises(userId),
        exercisePrMap
    ) { list, catalog, prMap ->
        val catalogById = catalog.associateBy { it.id }
        list.map { mapPerformedSessionToUi(it, catalogById, prMap) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<WorkoutTemplateUi>> = dao.observeWorkoutTemplatesWithExercises(userId)
        .map { list -> list.map(::templateToUi) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyVolume: StateFlow<List<WeeklyVolumeUi>> = dao.observeWeeklyVolume(userId)
        .map { rows -> rows.map { WeeklyVolumeUi(it.weekKey, it.volume) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            dao.upsertUser(UserEntity(id = userId, name = "Local user", email = "$userId@local"))
            seedBaseCatalogIfNeeded()
            restoreActiveWorkout()
        }
    }

    fun setSearchQuery(query: String) { searchQuery.value = query }
    fun setMuscleFilter(muscle: String?) { muscleFilter.value = muscle }

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
            val safe = name.trim()
            if (safe.isBlank()) return@launch
            dao.upsertExercise(
                ExerciseEntity(
                    id = id ?: 0,
                    userId = userId,
                    name = safe,
                    aliases = aliases.trim().ifBlank { null },
                    muscles = muscles.joinToString(","),
                    equipment = equipment?.trim()?.ifBlank { null },
                    isFavorite = favorite,
                    photoUri = photoUri,
                    isBase = false
                )
            )
        }
    }

    fun deleteExercise(id: Long) {
        viewModelScope.launch { dao.deleteCustomExercise(userId, id) }
    }

    fun toggleFavorite(id: Long, value: Boolean) {
        viewModelScope.launch { dao.updateFavorite(userId, id, value) }
    }

    fun createTemplate(title: String) {
        viewModelScope.launch {
            if (title.isBlank()) return@launch
            dao.upsertWorkoutTemplate(WorkoutTemplateEntity(userId = userId, title = title.trim()))
        }
    }

    fun addExerciseToTemplate(templateId: Long, exerciseId: Long, defaultSets: Int, defaultReps: Int, order: Int) {
        viewModelScope.launch {
            dao.upsertWorkoutTemplateExercise(
                WorkoutTemplateExerciseEntity(
                    templateId = templateId,
                    exerciseId = exerciseId,
                    orderInTemplate = order,
                    defaultSets = defaultSets,
                    defaultReps = defaultReps
                )
            )
        }
    }

    fun removeExerciseFromTemplate(entryId: Long) {
        viewModelScope.launch { dao.deleteWorkoutTemplateExercise(entryId) }
    }

    fun startWorkoutFromTemplate(templateId: Long) {
        viewModelScope.launch {
            val template = dao.getWorkoutTemplateWithExercises(templateId) ?: return@launch
            _activeWorkout.value = ActiveWorkoutUiState(
                startedAt = System.currentTimeMillis(),
                exercises = template.exercises
                    .sortedBy { it.templateExercise.orderInTemplate }
                    .map { detail ->
                        WorkoutExerciseInput(
                            exerciseId = detail.exercise.id,
                            exerciseName = detail.exercise.name,
                            sets = List(detail.templateExercise.defaultSets) {
                                ExerciseSetInput(
                                    weight = detail.templateExercise.defaultWeight?.toString().orEmpty(),
                                    reps = detail.templateExercise.defaultReps.toString()
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
                    exerciseId = ex.exerciseId,
                    exerciseName = ex.name,
                    sets = ex.sets.map { set -> ExerciseSetInput(weight = set.weight.toString(), reps = set.reps.toString()) }
                )
            }
        )
        persistActiveWorkout()
    }

    fun observeSessionDetail(sessionId: Long) = dao.observePerformedSessionDetail(sessionId)

    fun startWorkout() {
        _activeWorkout.value = ActiveWorkoutUiState(startedAt = System.currentTimeMillis())
        persistActiveWorkout()
    }

    fun addExerciseToActiveWorkout(exercise: ExerciseCatalogItem) {
        val current = _activeWorkout.value ?: ActiveWorkoutUiState(startedAt = System.currentTimeMillis())
        _activeWorkout.value = current.copy(
            exercises = current.exercises + WorkoutExerciseInput(
                exerciseId = exercise.id,
                exerciseName = exercise.name
            )
        )
        persistActiveWorkout()
    }

    fun addSet(exerciseId: Long) {
        mutateExercise(exerciseId) { it.copy(sets = it.sets + ExerciseSetInput()) }
    }

    fun removeSet(exerciseId: Long, setIndex: Int) {
        mutateExercise(exerciseId) {
            if (it.sets.size <= 1) it else it.copy(sets = it.sets.filterIndexed { i, _ -> i != setIndex })
        }
    }

    fun updateSet(exerciseId: Long, setIndex: Int, weight: String? = null, reps: String? = null) {
        mutateExercise(exerciseId) { ex ->
            ex.copy(sets = ex.sets.mapIndexed { idx, set ->
                if (idx != setIndex) set else set.copy(
                    weight = weight ?: set.weight,
                    reps = reps ?: set.reps
                )
            })
        }
    }

    fun startRestTimer(seconds: Int = 90) {
        timerJob?.cancel()
        val current = _activeWorkout.value ?: return
        _activeWorkout.value = current.copy(restTimerSecondsLeft = seconds, timerRunning = true)
        persistActiveWorkout()
        timerJob = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(1000)
                left -= 1
                _activeWorkout.value = _activeWorkout.value?.copy(restTimerSecondsLeft = left, timerRunning = left > 0)
                persistActiveWorkout()
            }
        }
    }

    fun skipRestTimer() {
        timerJob?.cancel()
        _activeWorkout.value = _activeWorkout.value?.copy(restTimerSecondsLeft = 0, timerRunning = false)
        persistActiveWorkout()
    }

    fun restartRestTimer(seconds: Int = 90) = startRestTimer(seconds)

    fun finishWorkout() {
        val active = _activeWorkout.value ?: return
        viewModelScope.launch {
            val performed = active.exercises.mapNotNull { ex ->
                val sets = ex.sets.mapNotNull {
                    val w = it.weight.toFloatOrNull()
                    val r = it.reps.toIntOrNull()
                    if (w == null || r == null) null else PerformedSetDraft(w, r)
                }
                if (sets.isEmpty()) null else PerformedExerciseDraft(ex.exerciseId, ex.exerciseName, sets)
            }
            if (performed.isNotEmpty()) {
                dao.persistWorkoutPerformed(
                    userId = userId,
                    startedAt = active.startedAt,
                    finishedAt = System.currentTimeMillis(),
                    exercises = performed
                )
            } else {
                dao.clearActiveWorkoutState(userId)
            }
            _activeWorkout.value = null
            timerJob?.cancel()
        }
    }

    private fun mutateExercise(exerciseId: Long, mapper: (WorkoutExerciseInput) -> WorkoutExerciseInput) {
        val current = _activeWorkout.value ?: return
        _activeWorkout.value = current.copy(
            exercises = current.exercises.map { if (it.exerciseId == exerciseId) mapper(it) else it }
        )
        persistActiveWorkout()
    }

    private fun persistActiveWorkout() {
        val active = _activeWorkout.value ?: return
        viewModelScope.launch {
            val payloadJson = gson.toJson(active)
            dao.upsertActiveWorkoutState(
                ActiveWorkoutStateEntity(
                    userId = userId,
                    startedAt = active.startedAt,
                    updatedAt = System.currentTimeMillis(),
                    payloadJson = payloadJson
                )
            )
        }
    }

    private suspend fun restoreActiveWorkout() {
        val state = dao.getActiveWorkoutState(userId) ?: return
        try {
            val active = gson.fromJson(state.payloadJson, ActiveWorkoutUiState::class.java)
            val startedAt = active?.startedAt ?: state.startedAt
            _activeWorkout.value = active?.copy(startedAt = startedAt)
        } catch (_: Exception) {
            _activeWorkout.value = null
        }
    }

    private suspend fun seedBaseCatalogIfNeeded() {
        if (dao.countBaseExercises(userId) > 0) return
        defaultBaseExercises.forEach { (name, muscles, aliases, equipment) ->
            dao.upsertExercise(
                ExerciseEntity(
                    userId = userId,
                    name = name,
                    aliases = aliases,
                    muscles = muscles,
                    equipment = equipment,
                    isBase = true
                )
            )
        }
    }

    private fun templateToUi(templateWithExercises: WorkoutTemplateWithExercises): WorkoutTemplateUi {
        return WorkoutTemplateUi(
            id = templateWithExercises.template.id,
            title = templateWithExercises.template.title,
            exercises = templateWithExercises.exercises
                .sortedBy { it.templateExercise.orderInTemplate }
                .map(::templateExerciseToUi)
        )
    }

    private fun templateExerciseToUi(detail: TemplateExerciseWithDetails) = WorkoutTemplateExerciseUi(
        id = detail.templateExercise.id,
        exerciseId = detail.exercise.id,
        name = detail.exercise.name,
        muscles = detail.exercise.muscles.split(",").map { it.trim() }.filter { it.isNotBlank() },
        photoUri = detail.exercise.photoUri,
        orderInTemplate = detail.templateExercise.orderInTemplate,
        defaultSets = detail.templateExercise.defaultSets,
        defaultReps = detail.templateExercise.defaultReps,
        defaultWeight = detail.templateExercise.defaultWeight
    )

    private fun exerciseEntityToUi(entity: ExerciseEntity) = ExerciseCatalogItem(
        id = entity.id,
        name = entity.name,
        aliases = entity.aliases ?: "",
        muscles = entity.muscles.split(",").map { it.trim() }.filter { it.isNotBlank() },
        equipment = entity.equipment,
        favorite = entity.isFavorite,
        photoUri = entity.photoUri,
        isBase = entity.isBase,
        lastUsedAt = entity.lastUsedAt
    )

    companion object {
        fun mapPerformedSessionToUi(
            session: PerformedSessionWithExercises,
            catalogById: Map<Long, ExerciseEntity>,
            prMap: Map<String, ExercisePrUi>
        ): TrainingSession {
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val exercises = session.exercises.map { performedExercise ->
                val catalogExercise = catalogById[performedExercise.exerciseEntity.exerciseId]
                val name = catalogExercise?.name ?: performedExercise.exerciseEntity.exerciseNameSnapshot
                ExerciseEntry(
                    exerciseId = performedExercise.exerciseEntity.exerciseId,
                    name = name,
                    muscles = catalogExercise?.muscles?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                    sets = performedExercise.sets.sortedBy { it.setOrder }
                        .map { ExerciseSetSummary(order = it.setOrder + 1, weight = it.weight, reps = it.reps) },
                    photoUri = catalogExercise?.photoUri,
                    pr = prMap[name]
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

        private val defaultBaseExercises = listOf(
            BaseExerciseSeed("Жим лёжа", "Грудь,Трицепс", "bench press", "Штанга"),
            BaseExerciseSeed("Приседания со штангой", "Квадрицепс,Ягодицы", "back squat", "Штанга"),
            BaseExerciseSeed("Становая тяга", "Спина,Ягодицы", "deadlift", "Штанга"),
            BaseExerciseSeed("Подтягивания", "Спина,Бицепс", "pull up", "Турник"),
            BaseExerciseSeed("Жим гантелей сидя", "Плечи,Трицепс", "dumbbell shoulder press", "Гантели"),
            BaseExerciseSeed("Тяга горизонтального блока", "Спина", "seated row", "Тренажёр"),
            BaseExerciseSeed("Выпады", "Квадрицепс,Ягодицы", "lunges", "Гантели"),
            BaseExerciseSeed("Планка", "Пресс", "plank", null)
        )

        private data class BaseExerciseSeed(
            val name: String,
            val muscles: String,
            val aliases: String?,
            val equipment: String?
        )
    }
}
