package com.example.workouttracker.feature.training.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.core.auth.AuthSessionStore
import com.example.workouttracker.data.exercise.Exercise
import com.example.workouttracker.data.exercise.ExerciseFilters
import com.example.workouttracker.data.exercise.ExerciseRepository
import com.example.workouttracker.data.exercise.ExerciseSourceFilter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseViewModel @Inject constructor(
    authSessionStore: AuthSessionStore,
    private val repository: ExerciseRepository
) : ViewModel() {
    private val userId = authSessionStore.currentUserIdOrGuest()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedMuscle = MutableStateFlow<String?>(null)
    val selectedMuscle: StateFlow<String?> = _selectedMuscle

    private val _selectedBodyPart = MutableStateFlow<String?>(null)
    val selectedBodyPart: StateFlow<String?> = _selectedBodyPart

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly

    private val _sourceFilter = MutableStateFlow(ExerciseSourceFilter.ALL)
    val sourceFilter: StateFlow<ExerciseSourceFilter> = _sourceFilter

    private val _selectedExerciseId = MutableStateFlow<String?>(null)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private val filters = combine(
        _searchQuery,
        _selectedMuscle,
        _selectedBodyPart,
        _favoritesOnly,
        _sourceFilter
    ) { query, muscle, bodyPart, favoritesOnly, source ->
        ExerciseFilters(
            query = query,
            muscle = muscle,
            bodyPart = bodyPart,
            favoritesOnly = favoritesOnly,
            source = source
        )
    }

    val exercises: StateFlow<List<Exercise>> = filters
        .flatMapLatest { repository.observeExercises(userId, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredExercises: StateFlow<List<Exercise>> = exercises

    val selectedExercise: StateFlow<Exercise?> = combine(_selectedExerciseId, exercises) { selectedId, items ->
        items.firstOrNull { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val muscles: StateFlow<List<String>> = exercises
        .map { list ->
            list.flatMap { it.targetMuscles + it.secondaryMuscles }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bodyParts: StateFlow<List<String>> = exercises
        .map { list ->
            list.map { it.bodyPart.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .sortedBy { it.lowercase() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val resultCount: StateFlow<Int> = exercises
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        loadExercises()
    }

    fun loadExercises() {
        viewModelScope.launch {
            runCatching { repository.ensureLoaded() }
                .onFailure { _messages.tryEmit("Не удалось загрузить библиотеку упражнений") }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query.trimStart()
    }

    fun filterByMuscle(muscle: String?) {
        _selectedMuscle.value = muscle?.takeUnless { it.equals(_selectedMuscle.value, ignoreCase = true) }
    }

    fun filterByBodyPart(bodyPart: String?) {
        _selectedBodyPart.value = bodyPart?.takeUnless { it.equals(_selectedBodyPart.value, ignoreCase = true) }
    }

    fun toggleFavoritesOnly() {
        _favoritesOnly.value = !_favoritesOnly.value
    }

    fun setSourceFilter(filter: ExerciseSourceFilter) {
        _sourceFilter.value = filter
    }

    fun clearFilters() {
        _selectedMuscle.value = null
        _selectedBodyPart.value = null
        _favoritesOnly.value = false
        _sourceFilter.value = ExerciseSourceFilter.ALL
    }

    fun selectExercise(id: String?) {
        _selectedExerciseId.value = id
    }

    fun toggleFavorite(exercise: Exercise) {
        viewModelScope.launch {
            repository.setFavorite(userId, exercise.id, !exercise.isFavorite)
        }
    }
}

