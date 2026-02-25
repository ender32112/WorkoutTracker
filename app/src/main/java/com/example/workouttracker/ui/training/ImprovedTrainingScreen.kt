package com.example.workouttracker.ui.training

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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

    Scaffold(topBar = { SectionHeader(title = "Тренировки") }) { padding ->
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

                TrainingHomeView.CATALOG -> CatalogScreen(quick, trainingViewModel::addExerciseToActiveWorkout) { screen = TrainingHomeView.LIST }
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

                TrainingHomeView.PROGRESS -> ProgressOverview(prs, weekly) { screen = TrainingHomeView.LIST }
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
                    },
                    onBack = { screen = TrainingHomeView.LIST }
                )
            }
        }
    }
}

@Composable
private fun CatalogScreen(items: List<ExerciseCatalogItem>, onQuickAdd: (ExerciseCatalogItem) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { TextButton(onClick = onBack) { Text("← Назад") } }
        items(items, key = { it.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.name)
                    TextButton(onClick = { onQuickAdd(item) }) { Text("+ в тренировку") }
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
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    if (active == null) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) { Text("← Назад") }
            Button(onClick = onStart) { Text("Старт тренировки") }
            quickAdd.take(8).forEach { ex -> TextButton(onClick = { onAddExercise(ex) }) { Text(ex.name) } }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            TextButton(onClick = onBack) { Text("← К списку") }
            Card {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rest Timer: ${active.restTimerSecondsLeft} сек")
                    LinearProgressIndicator(
                        progress = { (active.restTimerSecondsLeft.coerceIn(0, 90) / 90f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onRest(90) }) { Text("90с") }
                        TextButton(onClick = onSkipRest) { Text("Пропустить") }
                        TextButton(onClick = { onRestartRest(90) }) { Text("Рестарт") }
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
                                modifier = Modifier.size(48.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(exercise.exerciseName)
                            Text(catalogExercise?.muscles?.joinToString().orEmpty())
                            prMap[exercise.exerciseName]?.let { Text("PR: ${it.bestVolumeSet.toInt()} / ${"%.1f".format(it.bestE1rm)}") }
                        }
                    }
                    exercise.sets.forEachIndexed { idx, set ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(set.weight, { onSetUpdate(exercise.exerciseId, idx, it, null) }, label = { Text("Вес") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(set.reps, { onSetUpdate(exercise.exerciseId, idx, null, it) }, label = { Text("Повт") }, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onRemoveSet(exercise.exerciseId, idx) }) { androidx.compose.material3.Icon(Icons.Default.Delete, null) }
                        }
                    }
                    TextButton(onClick = { onAddSet(exercise.exerciseId) }) { Text("+ Сет") }
                }
            }
        }

        item {
            Text("Быстро добавить")
            quickAdd.take(10).forEach { ex -> TextButton(onClick = { onAddExercise(ex) }) { Text(ex.name) } }
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Завершить тренировку") }
        }
    }
}

@Composable
private fun ProgressOverview(prs: List<ExercisePrUi>, weekly: List<WeeklyVolumeUi>, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { TextButton(onClick = onBack) { Text("← Назад") } }
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
                    Text("Placeholder chart")
                    Spacer(Modifier.height(8.dp))
                    weekly.forEach { w -> Text("${w.weekKey}: ${w.volume.toInt()}") }
                }
            }
        }
    }
}
