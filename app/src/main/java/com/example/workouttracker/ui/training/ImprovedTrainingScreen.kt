package com.example.workouttracker.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.ui.components.SectionHeader
import com.example.workouttracker.viewmodel.TrainingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovedTrainingScreen(trainingViewModel: TrainingViewModel = viewModel()) {
    val quick by trainingViewModel.quickAddExercises.collectAsState()
    val active by trainingViewModel.activeWorkout.collectAsState()
    val prs by trainingViewModel.exercisePr.collectAsState()
    val weekly by trainingViewModel.weeklyVolume.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SectionHeader(title = "Тренировки") },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showBottomSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                listOf("Каталог", "Тренировка", "Прогресс").forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                }
            }

            when (selectedTab) {
                0 -> CatalogGrid(
                    items = quick,
                    onFavoriteToggle = { id, value -> trainingViewModel.toggleFavorite(id, value) },
                    onDelete = trainingViewModel::deleteExercise,
                    onQuickAdd = trainingViewModel::addExerciseToActiveWorkout
                )
                1 -> WorkoutActiveView(
                    active = active,
                    quickAdd = quick,
                    onStart = trainingViewModel::startWorkout,
                    onAddExercise = trainingViewModel::addExerciseToActiveWorkout,
                    onSetUpdate = trainingViewModel::updateSet,
                    onAddSet = trainingViewModel::addSet,
                    onRemoveSet = trainingViewModel::removeSet,
                    onRest = trainingViewModel::startRestTimer,
                    onSkipRest = trainingViewModel::skipRestTimer,
                    onRestartRest = trainingViewModel::restartRestTimer,
                    onFinish = trainingViewModel::finishWorkout
                )
                else -> ProgressOverview(prs, weekly)
            }
        }
    }

    if (showBottomSheet) {
        AddTrainingBottomSheet(
            availableExercises = quick,
            onSave = { session ->
                session.exercises.forEach { exercise ->
                    trainingViewModel.addOrUpdateExercise(
                        name = exercise.name,
                        muscles = emptyList(),
                        equipment = null,
                        aliases = "",
                        favorite = false,
                        photoUri = exercise.photoUri
                    )
                }
                showBottomSheet = false
            },
            onDismiss = { showBottomSheet = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CatalogGrid(
    items: List<ExerciseCatalogItem>,
    onFavoriteToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onQuickAdd: (ExerciseCatalogItem) -> Unit
) {
    LazyVerticalGrid(columns = GridCells.Adaptive(170.dp), modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items, key = { it.id }) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.name)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        item.muscles.take(3).forEach { muscle ->
                            AssistChip(onClick = {}, label = { Text(muscle) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onQuickAdd(item) }) { Text("+ в тренировку") }
                        IconButton(onClick = { onFavoriteToggle(item.id, !item.favorite) }) {
                            Icon(if (item.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                        }
                        if (!item.isBase) {
                            IconButton(onClick = { onDelete(item.id) }) { Icon(Icons.Default.Delete, null) }
                        }
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
            Button(onClick = onStart) { Text("Старт тренировки") }
            quickAdd.take(8).forEach { ex -> TextButton(onClick = { onAddExercise(ex) }) { Text(ex.name) } }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
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
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(exercise.exerciseName)
                    exercise.sets.forEachIndexed { idx, set ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(set.weight, { onSetUpdate(exercise.exerciseId, idx, it, null) }, label = { Text("Вес") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(set.reps, { onSetUpdate(exercise.exerciseId, idx, null, it) }, label = { Text("Повт") }, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemoveSet(exercise.exerciseId, idx) }) { Icon(Icons.Default.Delete, null) }
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
private fun ProgressOverview(prs: List<ExercisePrUi>, weekly: List<WeeklyVolumeUi>) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Графики прогресса")
                    Text("Плейсхолдер для графиков (будет подключен на следующем этапе)")
                }
            }
        }
        item { Text("PR") }
        items(prs) { pr -> Text("${pr.exerciseName}: volume ${pr.bestVolumeSet.toInt()} / e1RM ${"%.1f".format(pr.bestE1rm)}") }
        item { Spacer(Modifier.height(8.dp)); Text("Недельный объём") }
        items(weekly) { w -> Text("${w.weekKey}: ${w.volume}") }
    }
}
