package com.example.workouttracker.ui.training

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.workouttracker.ui.components.SectionHeader
import com.example.workouttracker.viewmodel.TrainingViewModel

private enum class TrainingHomeView { LIST, CATALOG, TEMPLATES, PROGRESS, ACTIVE }

@Composable
fun ImprovedTrainingScreen(trainingViewModel: TrainingViewModel = viewModel()) {
    val quick by trainingViewModel.quickAddExercises.collectAsState()
    val active by trainingViewModel.activeWorkout.collectAsState()
    val prs by trainingViewModel.exercisePr.collectAsState()
    val weekly by trainingViewModel.weeklyVolume.collectAsState()
    val sessions by trainingViewModel.sessions.collectAsState()
    val templates by trainingViewModel.templates.collectAsState()
    val prMap by trainingViewModel.exercisePrMap.collectAsState()

    var screen by remember { mutableStateOf(TrainingHomeView.LIST) }

    Scaffold(
        topBar = {
            SectionHeader(
                title = "Тренировки",
                actions = {
                    if (screen != TrainingHomeView.LIST) {
                        IconButton(onClick = { screen = TrainingHomeView.LIST }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    }
                    IconButton(onClick = { screen = TrainingHomeView.CATALOG }) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = "Каталог")
                    }
                    IconButton(onClick = { screen = TrainingHomeView.TEMPLATES }) {
                        Icon(Icons.Default.ViewCarousel, contentDescription = "Шаблоны")
                    }
                    IconButton(onClick = { screen = TrainingHomeView.PROGRESS }) {
                        Icon(Icons.Default.AutoGraph, contentDescription = "Аналитика")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (screen) {
                TrainingHomeView.LIST -> TrainingListScreen(
                    sessions = sessions,
                    onStartWorkout = {
                        trainingViewModel.startWorkout()
                        screen = TrainingHomeView.ACTIVE
                    },
                    onOpenCatalog = { screen = TrainingHomeView.CATALOG },
                    onOpenTemplates = { screen = TrainingHomeView.TEMPLATES },
                    onOpenProgress = { screen = TrainingHomeView.PROGRESS },
                    onRepeat = {
                        trainingViewModel.repeatSession(it)
                        screen = TrainingHomeView.ACTIVE
                    },
                    onExport = {}
                )

                TrainingHomeView.CATALOG -> CatalogScreen(
                    items = quick,
                    onQuickAdd = trainingViewModel::addExerciseToActiveWorkout,
                    onStartWorkout = {
                        trainingViewModel.startWorkout()
                        screen = TrainingHomeView.ACTIVE
                    }
                )

                TrainingHomeView.TEMPLATES -> TrainingTemplatesScreen(
                    templates = templates,
                    exercises = quick,
                    onCreateTemplate = trainingViewModel::createTemplate,
                    onAddExerciseToTemplate = trainingViewModel::addExerciseToTemplate,
                    onRemoveExerciseFromTemplate = trainingViewModel::removeExerciseFromTemplate,
                    onStartFromTemplate = {
                        trainingViewModel.startWorkoutFromTemplate(it)
                        screen = TrainingHomeView.ACTIVE
                    }
                )

                TrainingHomeView.PROGRESS -> ProgressOverview(prs, weekly, sessions)
                TrainingHomeView.ACTIVE -> WorkoutActiveView(
                    active = active,
                    quickAdd = quick,
                    prMap = prMap,
                    onStart = trainingViewModel::startWorkout,
                    onAddExercise = trainingViewModel::addExerciseToActiveWorkout,
                    onSetUpdate = trainingViewModel::updateSet,
                    onAddSet = trainingViewModel::addSet,
                    onRemoveSet = trainingViewModel::removeSet,
                    onRest = trainingViewModel::startRestTimer,
                    onSkipRest = trainingViewModel::skipRestTimer,
                    onRestartRest = trainingViewModel::restartRestTimer,
                    onFinish = {
                        trainingViewModel.finishWorkout()
                        screen = TrainingHomeView.LIST
                    }
                )
            }
        }
    }
}

@Composable
private fun CatalogScreen(
    items: List<ExerciseCatalogItem>,
    onQuickAdd: (ExerciseCatalogItem) -> Unit,
    onStartWorkout: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f))) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Каталог упражнений", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${items.size} упражнений")
                    Spacer(Modifier.height(6.dp))
                }
                items(items, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item.photoUri?.let {
                                Image(
                                    painter = rememberAsyncImagePainter(it),
                                    contentDescription = item.name,
                                    modifier = Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.SemiBold)
                                Text(item.muscles.joinToString())
                            }
                            TextButton(onClick = { onQuickAdd(item) }) { Text("Добавить") }
                        }
                    }
                }
                item {
                    Button(onClick = onStartWorkout, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text(" Начать тренировку")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutActiveView(
    active: ActiveWorkoutUiState?,
    quickAdd: List<ExerciseCatalogItem>,
    prMap: Map<String, ExercisePrUi>,
    onStart: () -> Unit,
    onAddExercise: (ExerciseCatalogItem) -> Unit,
    onSetUpdate: (Long, Int, String?, String?) -> Unit,
    onAddSet: (Long) -> Unit,
    onRemoveSet: (Long, Int) -> Unit,
    onRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onRestartRest: (Int) -> Unit,
    onFinish: () -> Unit
) {
    if (active == null) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Новая тренировка")
            Button(onClick = onStart) { Text("Старт тренировки") }
            quickAdd.take(8).forEach { ex -> TextButton(onClick = { onAddExercise(ex) }) { Text(ex.name) } }
        }
        return
    }

    val maxTimerSeconds = 300f
    val timerPresets = listOf(60, 180, 300)
    var selectedPreset by remember { mutableStateOf(60) }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Таймер отдыха", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${active.restTimerSecondsLeft} сек")
                    LinearProgressIndicator(
                        progress = { active.restTimerSecondsLeft.coerceIn(0, 300) / maxTimerSeconds },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        timerPresets.forEach { sec ->
                            FilterChip(
                                selected = selectedPreset == sec,
                                onClick = { selectedPreset = sec },
                                label = { Text(if (sec < 120) "1 мин" else "${sec / 60} мин") }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onRest(selectedPreset) }) { Text("Запустить") }
                        TextButton(onClick = onSkipRest) { Text("Пропустить") }
                        TextButton(onClick = { onRestartRest(selectedPreset) }) { Text("Рестарт") }
                    }
                }
            }
        }

        items(active.exercises, key = { it.exerciseId }) { exercise ->
            val catalogExercise = quickAdd.firstOrNull { it.id == exercise.exerciseId }
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        catalogExercise?.photoUri?.let {
                            Image(
                                painter = rememberAsyncImagePainter(it),
                                contentDescription = exercise.exerciseName,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(exercise.exerciseName, fontWeight = FontWeight.SemiBold)
                            Text(catalogExercise?.muscles?.joinToString().orEmpty())
                            prMap[exercise.exerciseName]?.let { Text("PR: ${it.bestVolumeSet.toInt()} / ${"%.1f".format(it.bestE1rm)}") }
                        }
                    }
                    exercise.sets.forEachIndexed { idx, set ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(set.weight, { onSetUpdate(exercise.exerciseId, idx, it, null) }, label = { Text("Вес") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(set.reps, { onSetUpdate(exercise.exerciseId, idx, null, it) }, label = { Text("Повт") }, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onRemoveSet(exercise.exerciseId, idx) }) { Icon(Icons.Default.Delete, null) }
                        }
                    }
                    TextButton(onClick = { onAddSet(exercise.exerciseId) }) { Text("+ Сет") }
                }
            }
        }

        item {
            Text("Быстро добавить")
            quickAdd.take(8).forEach { ex -> TextButton(onClick = { onAddExercise(ex) }) { Text(ex.name) } }
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Завершить тренировку") }
        }
    }
}

private data class ExerciseAnalyticsUi(
    val name: String,
    val sessionsCount: Int,
    val totalVolume: Double,
    val bestSetVolume: Double,
    val firstVolume: Double,
    val lastVolume: Double
) {
    val deltaPercent: Double
        get() = if (firstVolume <= 0.0) 0.0 else ((lastVolume - firstVolume) / firstVolume) * 100
}

@Composable
private fun ProgressOverview(prs: List<ExercisePrUi>, weekly: List<WeeklyVolumeUi>, sessions: List<TrainingSession>) {
    val exerciseAnalytics = remember(sessions) {
        sessions
            .flatMap { session -> session.exercises.map { ex -> ex.name to ex.totalVolume } }
            .groupBy({ it.first }, { it.second })
            .map { (name, volumes) ->
                ExerciseAnalyticsUi(
                    name = name,
                    sessionsCount = volumes.size,
                    totalVolume = volumes.sum(),
                    bestSetVolume = volumes.maxOrNull() ?: 0.0,
                    firstVolume = volumes.firstOrNull() ?: 0.0,
                    lastVolume = volumes.lastOrNull() ?: 0.0
                )
            }
            .sortedByDescending { it.lastVolume }
    }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Лучшие PR")
                    prs.sortedByDescending { it.bestVolumeSet }.take(10).forEach { pr ->
                        Text("${pr.exerciseName}: volume ${pr.bestVolumeSet.toInt()} / e1RM ${"%.1f".format(pr.bestE1rm)}")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Недельный объём")
                    Spacer(Modifier.height(6.dp))
                    weekly.forEach { w -> Text("${w.weekKey}: ${w.volume.toInt()}") }
                }
            }
        }
        items(exerciseAnalytics, key = { it.name }) { analytics ->
            val positive = analytics.deltaPercent >= 0
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(analytics.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Сессий: ${analytics.sessionsCount} • Суммарный объём: ${analytics.totalVolume.toInt()}")
                    Text("Лучший объём за сессию: ${analytics.bestSetVolume.toInt()}")
                    Text(
                        text = "Прогресс: ${if (positive) "+" else ""}${"%.1f".format(analytics.deltaPercent)}%",
                        color = if (positive) Color(0xFF1F8F57) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
