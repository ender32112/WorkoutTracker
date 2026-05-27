package com.example.workouttracker.feature.training.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workouttracker.ui.designsystem.AppTopBar
import kotlinx.coroutines.flow.collectLatest

private enum class TrainingHomeView { DASHBOARD, SESSION, LIBRARY, TEMPLATES, PROGRESS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedTrainingScreen(
    trainingViewModel: TrainingViewModel = hiltViewModel(),
    exerciseViewModel: ExerciseViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val active by trainingViewModel.activeWorkout.collectAsState()
    val sessions by trainingViewModel.sessions.collectAsState()
    val templates by trainingViewModel.templates.collectAsState()
    val quickAdd by trainingViewModel.quickAddExercises.collectAsState()
    val favorites by trainingViewModel.favorites.collectAsState()
    val recent by trainingViewModel.recentExercises.collectAsState()
    val prs by trainingViewModel.exercisePr.collectAsState()
    val weekly by trainingViewModel.weeklyVolume.collectAsState()

    val exercises by exerciseViewModel.filteredExercises.collectAsState()
    val searchQuery by exerciseViewModel.searchQuery.collectAsState()
    val selectedMuscle by exerciseViewModel.selectedMuscle.collectAsState()
    val selectedBodyPart by exerciseViewModel.selectedBodyPart.collectAsState()
    val favoritesOnly by exerciseViewModel.favoritesOnly.collectAsState()
    val sourceFilter by exerciseViewModel.sourceFilter.collectAsState()
    val muscles by exerciseViewModel.muscles.collectAsState()
    val bodyParts by exerciseViewModel.bodyParts.collectAsState()
    val selectedExercise by exerciseViewModel.selectedExercise.collectAsState()
    val resultCount by exerciseViewModel.resultCount.collectAsState()

    var screen by rememberSaveable { mutableStateOf(TrainingHomeView.DASHBOARD) }
    var screenBackStack by rememberSaveable { mutableStateOf(listOf<String>()) }
    var selectedSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val saveableStateHolder = rememberSaveableStateHolder()

    fun navigateTo(target: TrainingHomeView, pushCurrent: Boolean = true) {
        if (target == screen) return
        if (pushCurrent) screenBackStack = (screenBackStack + screen.name).takeLast(12)
        screen = target
    }

    fun navigateBack(): Boolean {
        val last = screenBackStack.lastOrNull() ?: return false
        screenBackStack = screenBackStack.dropLast(1)
        screen = TrainingHomeView.valueOf(last)
        return true
    }

    fun resetToDashboard() {
        screenBackStack = emptyList()
        screen = TrainingHomeView.DASHBOARD
    }

    fun handleBackNavigation() {
        when {
            selectedExercise != null -> exerciseViewModel.selectExercise(null)
            selectedSessionId != null -> selectedSessionId = null
            !navigateBack() && screen != TrainingHomeView.DASHBOARD -> resetToDashboard()
        }
    }

    LaunchedEffect(trainingViewModel) {
        trainingViewModel.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(trainingViewModel, context, haptics) {
        trainingViewModel.restTimerFinished.collectLatest {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            val canVibrate = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.VIBRATE
            ) == PackageManager.PERMISSION_GRANTED
            if (!canVibrate) return@collectLatest

            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Vibrator::class.java)
                }
                vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(300L, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(300L)
                    }
                }
            }
        }
    }
    LaunchedEffect(exerciseViewModel) {
        exerciseViewModel.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(active?.startedAt) {
        if (active != null && screen == TrainingHomeView.DASHBOARD) {
            navigateTo(TrainingHomeView.SESSION, pushCurrent = false)
        }
    }

    BackHandler(
        enabled = selectedExercise != null || selectedSessionId != null || screenBackStack.isNotEmpty() || screen != TrainingHomeView.DASHBOARD,
        onBack = ::handleBackNavigation
    )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                            )
                        )
                    )
            ) {
                AppTopBar(
                    title = "Тренировки",
                    subtitle = "Дашборд, сессия, библиотека, шаблоны и прогресс",
                    actions = {
                        if (selectedExercise != null || selectedSessionId != null || screenBackStack.isNotEmpty() || screen != TrainingHomeView.DASHBOARD) {
                            IconButton(onClick = ::handleBackNavigation) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                        }
                    }
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        listOf(
                            TrainingHomeView.DASHBOARD to "Дашборд",
                            TrainingHomeView.SESSION to "Сессия",
                            TrainingHomeView.LIBRARY to "Библиотека",
                            TrainingHomeView.TEMPLATES to "Шаблоны",
                            TrainingHomeView.PROGRESS to "Прогресс"
                        ),
                        key = { it.first.name }
                    ) { (tab, label) ->
                        FilterChip(
                            selected = screen == tab,
                            onClick = { navigateTo(tab) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                label = "training_home"
            ) { current ->
                saveableStateHolder.SaveableStateProvider(current.name) {
                    when (current) {
                        TrainingHomeView.DASHBOARD -> TrainingDashboardSection(
                            sessions = sessions,
                            active = active,
                            weekly = weekly,
                            templatesCount = templates.size,
                            onStart = {
                                trainingViewModel.startWorkout()
                                navigateTo(TrainingHomeView.SESSION)
                            },
                            onOpenSession = { navigateTo(TrainingHomeView.SESSION) },
                            onOpenLibrary = { navigateTo(TrainingHomeView.LIBRARY) },
                            onRepeat = {
                                trainingViewModel.repeatSession(it)
                                navigateTo(TrainingHomeView.SESSION)
                            },
                            onOpenDetail = { selectedSessionId = it }
                        )

                        TrainingHomeView.SESSION -> TrainingSessionSection(
                            active = active,
                            favorites = favorites,
                            recent = recent,
                            quickAdd = quickAdd,
                            onStart = trainingViewModel::startWorkout,
                            onAddExercise = trainingViewModel::addExerciseToActiveWorkout,
                            onSetUpdate = trainingViewModel::updateSet,
                            onAddSet = trainingViewModel::addSet,
                            onRemoveSet = trainingViewModel::removeSet,
                            onRemoveExercise = trainingViewModel::removeExercise,
                            onRest = trainingViewModel::startRestTimer,
                            onSkipRest = trainingViewModel::skipRestTimer,
                            onRestartRest = trainingViewModel::restartRestTimer,
                            onFinish = {
                                trainingViewModel.finishWorkout()
                                resetToDashboard()
                            },
                            onDiscard = {
                                trainingViewModel.discardWorkout()
                                resetToDashboard()
                            },
                            onBrowseLibrary = { navigateTo(TrainingHomeView.LIBRARY) }
                        )

                        TrainingHomeView.LIBRARY -> ExerciseListScreen(
                            exercises = exercises,
                            searchQuery = searchQuery,
                            selectedMuscle = selectedMuscle,
                            selectedBodyPart = selectedBodyPart,
                            favoritesOnly = favoritesOnly,
                            sourceFilter = sourceFilter,
                            resultCount = resultCount,
                            muscles = muscles,
                            bodyParts = bodyParts,
                            onSearchChange = exerciseViewModel::search,
                            onSelectMuscle = exerciseViewModel::filterByMuscle,
                            onSelectBodyPart = exerciseViewModel::filterByBodyPart,
                            onToggleFavoritesOnly = exerciseViewModel::toggleFavoritesOnly,
                            onSourceFilterChange = exerciseViewModel::setSourceFilter,
                            onClearFilters = exerciseViewModel::clearFilters,
                            onExerciseClick = { exerciseViewModel.selectExercise(it.id) },
                            onToggleFavorite = exerciseViewModel::toggleFavorite,
                            onAddToWorkout = {
                                trainingViewModel.addExerciseFromLibrary(it)
                                navigateTo(TrainingHomeView.SESSION)
                            }
                        )

                        TrainingHomeView.TEMPLATES -> TrainingTemplatesScreen(
                            templates = templates,
                            exercises = quickAdd,
                            onCreateTemplate = trainingViewModel::createTemplate,
                            onAddExerciseToTemplate = trainingViewModel::addExerciseToTemplate,
                            onRemoveExerciseFromTemplate = trainingViewModel::removeExerciseFromTemplate,
                            onStartFromTemplate = {
                                trainingViewModel.startWorkoutFromTemplate(it)
                                navigateTo(TrainingHomeView.SESSION)
                            }
                        )

                        TrainingHomeView.PROGRESS -> TrainingProgressSection(
                            prs = prs,
                            weekly = weekly,
                            sessions = sessions
                        )
                    }
                }
            }
        }
    }

    selectedExercise?.let { exercise ->
        ModalBottomSheet(onDismissRequest = { exerciseViewModel.selectExercise(null) }) {
            ExerciseDetailScreen(
                exercise = exercise,
                onClose = { exerciseViewModel.selectExercise(null) },
                onAddToWorkout = {
                    trainingViewModel.addExerciseFromLibrary(exercise)
                    exerciseViewModel.selectExercise(null)
                    navigateTo(TrainingHomeView.SESSION)
                },
                onToggleFavorite = { exerciseViewModel.toggleFavorite(exercise) }
            )
        }
    }

    selectedSessionId?.let { sessionId ->
        val session = sessions.firstOrNull { it.sessionId == sessionId }
        if (session != null) {
            CompletedSessionSheet(
                session = session,
                onDismiss = { selectedSessionId = null },
                onRepeat = {
                    trainingViewModel.repeatSession(session.sessionId)
                    selectedSessionId = null
                    navigateTo(TrainingHomeView.SESSION)
                },
                onDelete = {
                    trainingViewModel.deleteCompletedSession(session.sessionId)
                    selectedSessionId = null
                },
                onSave = { exercises ->
                    trainingViewModel.saveEditedCompletedSession(session.sessionId, exercises)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletedSessionSheet(
    session: TrainingSession,
    onDismiss: () -> Unit,
    onRepeat: () -> Unit,
    onDelete: () -> Unit,
    onSave: (List<WorkoutExerciseInput>) -> Unit
) {
    var isEditing by remember(session.sessionId) { mutableStateOf(false) }
    val editableExercises = remember(session.sessionId) {
        mutableStateListOf<WorkoutExerciseInput>().apply {
            addAll(session.exercises.map { exercise ->
                WorkoutExerciseInput(
                    instanceId = exercise.exerciseId + "-" + session.sessionId + "-" + exercise.name,
                    exerciseId = exercise.exerciseId,
                    exerciseName = exercise.name,
                    exerciseNameEn = exercise.nameEn,
                    bodyPart = exercise.bodyPart,
                    muscles = exercise.muscles,
                    equipment = exercise.equipment,
                    mediaUri = exercise.mediaUri,
                    localMediaUri = exercise.localMediaUri,
                    sets = exercise.sets.map { set ->
                        ExerciseSetInput(weight = trimDecimal(set.weight), reps = set.reps.toString())
                    }
                )
            })
        }
    }
    val totalSets = session.exercises.sumOf { it.sets.size }
    val durationMinutes = ((session.finishedAt - session.startedAt).coerceAtLeast(0L) / 60_000L).toInt()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            formatSessionHeadline(session.startedAt),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(onClick = {}, label = { Text("${session.exercises.size} упражнений") })
                            AssistChip(onClick = {}, label = { Text("$totalSets подходов") })
                            AssistChip(onClick = {}, label = { Text(formatDuration(durationMinutes)) })
                            AssistChip(onClick = {}, label = { Text(formatSheetVolume(session.totalVolume)) })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onRepeat, modifier = Modifier.weight(1f)) {
                                Text("Повторить")
                            }
                            FilledTonalButton(
                                onClick = { isEditing = !isEditing },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Text(if (isEditing) "Просмотр" else "Редактировать")
                            }
                        }
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Text("Удалить тренировку")
                        }
                    }
                }
            }

            if (!isEditing) {
                items(session.exercises, key = { "${session.sessionId}-${it.exerciseId}-${it.name}" }) { exercise ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${exercise.sets.size} подходов • ${formatSheetVolume(exercise.totalVolume)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            exercise.sets.forEach { set ->
                                Text(
                                    "Подход ${set.order}: ${trimDecimal(set.weight)} кг × ${set.reps}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(editableExercises, key = { it.instanceId }) { exercise ->
                    EditableCompletedExerciseCard(
                        exercise = exercise,
                        onUpdateSet = { setIndex, weight, reps ->
                            val exerciseIndex = editableExercises.indexOfFirst { it.instanceId == exercise.instanceId }
                            if (exerciseIndex == -1) return@EditableCompletedExerciseCard
                            val updatedSets = editableExercises[exerciseIndex].sets.mapIndexed { index, set ->
                                if (index != setIndex) {
                                    set
                                } else {
                                    set.copy(weight = weight ?: set.weight, reps = reps ?: set.reps)
                                }
                            }
                            editableExercises[exerciseIndex] = editableExercises[exerciseIndex].copy(sets = updatedSets)
                        },
                        onAddSet = {
                            val exerciseIndex = editableExercises.indexOfFirst { it.instanceId == exercise.instanceId }
                            if (exerciseIndex == -1) return@EditableCompletedExerciseCard
                            editableExercises[exerciseIndex] = editableExercises[exerciseIndex].copy(
                                sets = editableExercises[exerciseIndex].sets + ExerciseSetInput()
                            )
                        },
                        onRemoveSet = { setIndex ->
                            val exerciseIndex = editableExercises.indexOfFirst { it.instanceId == exercise.instanceId }
                            if (exerciseIndex == -1) return@EditableCompletedExerciseCard
                            val updatedSets = editableExercises[exerciseIndex].sets.filterIndexed { index, _ -> index != setIndex }
                            if (updatedSets.isEmpty()) {
                                editableExercises.removeAt(exerciseIndex)
                            } else {
                                editableExercises[exerciseIndex] = editableExercises[exerciseIndex].copy(sets = updatedSets)
                            }
                        },
                        onRemoveExercise = {
                            val exerciseIndex = editableExercises.indexOfFirst { it.instanceId == exercise.instanceId }
                            if (exerciseIndex != -1) editableExercises.removeAt(exerciseIndex)
                        }
                    )
                }

                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSave(editableExercises.toList()) },
                            enabled = editableExercises.isNotEmpty()
                        ) {
                            Text("Сохранить изменения")
                        }
                        TextButton(onClick = { isEditing = false }) {
                            Text("Отмена")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableCompletedExerciseCard(
    exercise: WorkoutExerciseInput,
    onUpdateSet: (Int, String?, String?) -> Unit,
    onAddSet: () -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRemoveExercise: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.exerciseName, fontWeight = FontWeight.Bold)
                    Text(
                        "${exercise.sets.size} подходов",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onRemoveExercise) {
                    Text("Удалить")
                }
            }
            exercise.sets.forEachIndexed { index, set ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Подход ${index + 1}", fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { onRemoveSet(index) }) {
                                Text("Удалить")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = set.weight,
                                onValueChange = { value ->
                                    onUpdateSet(
                                        index,
                                        value.replace(',', '.')
                                            .filter { it.isDigit() || it == '.' }
                                            .take(6),
                                        null
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Вес, кг") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = set.reps,
                                onValueChange = { value ->
                                    onUpdateSet(
                                        index,
                                        null,
                                        value.filter(Char::isDigit).take(3)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Повторы") },
                                singleLine = true
                            )
                        }
                    }
                }
            }
            TextButton(onClick = onAddSet) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Добавить подход")
            }
        }
    }
}

private fun trimDecimal(value: Float): String {
    val intValue = value.toInt()
    return if (value == intValue.toFloat()) intValue.toString() else value.toString()
}

private fun formatSessionHeadline(timestamp: Long): String =
    java.text.SimpleDateFormat("d MMMM • HH:mm", java.util.Locale("ru")).format(java.util.Date(timestamp))

private fun formatDuration(minutes: Int): String =
    if (minutes <= 0) "без времени" else "$minutes мин"

private fun formatSheetVolume(volume: Double): String =
    "%,d кг".format(java.util.Locale.US, volume.toInt()).replace(',', ' ')
