package com.example.workouttracker.ui.training

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.ui.components.SectionHeader
import com.example.workouttracker.viewmodel.TrainingViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(trainingViewModel: TrainingViewModel = viewModel()) {
    val quick by trainingViewModel.quickAddExercises.collectAsState()
    val favorites by trainingViewModel.favorites.collectAsState()
    val recent by trainingViewModel.recentExercises.collectAsState()
    val active by trainingViewModel.activeWorkout.collectAsState()
    val prs by trainingViewModel.exercisePr.collectAsState()
    val weekly by trainingViewModel.weeklyVolume.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { SectionHeader(title = "Тренировки") },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
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
                0 -> CatalogTab(
                    quick = quick,
                    favorites = favorites,
                    recent = recent,
                    onSearch = trainingViewModel::setSearchQuery,
                    onMuscleFilter = trainingViewModel::setMuscleFilter,
                    onFavoriteToggle = { id, value -> trainingViewModel.toggleFavorite(id, value) },
                    onDelete = trainingViewModel::deleteExercise
                )

                1 -> WorkoutTab(
                    quickAdd = quick,
                    active = active,
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

                else -> ProgressTab(prs, weekly)
            }
        }
    }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, muscles, equipment, aliases, favorite, photo ->
                trainingViewModel.addOrUpdateExercise(
                    name = name,
                    muscles = muscles,
                    equipment = equipment,
                    aliases = aliases,
                    favorite = favorite,
                    photoUri = photo
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CatalogTab(
    quick: List<ExerciseCatalogItem>,
    favorites: List<ExerciseCatalogItem>,
    recent: List<ExerciseCatalogItem>,
    onSearch: (String) -> Unit,
    onMuscleFilter: (String?) -> Unit,
    onFavoriteToggle: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OutlinedTextField(value = search, onValueChange = { search = it; onSearch(it) }, label = { Text("Поиск / alias") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = muscle, onValueChange = { muscle = it; onMuscleFilter(it.ifBlank { null }) }, label = { Text("Фильтр по мышце") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text("Избранные: ${favorites.size}, Последние: ${recent.size}")
        }
        items(quick, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name)
                        Text("Мышцы: ${item.muscles.joinToString()}")
                    }
                    IconButton(onClick = { onFavoriteToggle(item.id, !item.favorite) }) {
                        Icon(if (item.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null)
                    }
                    if (!item.isBase) {
                        IconButton(onClick = { onDelete(item.id) }) { Icon(Icons.Default.Delete, contentDescription = null) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutTab(
    quickAdd: List<ExerciseCatalogItem>,
    active: ActiveWorkoutUiState?,
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
            Text("Быстро добавить:")
            quickAdd.take(6).forEach { ex -> TextButton(onClick = { onAddExercise(ex) }) { Text(ex.name) } }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Отдых: ${active.restTimerSecondsLeft} сек")
            Row {
                TextButton(onClick = { onRest(90) }) { Text("Таймер 90с") }
                TextButton(onClick = onSkipRest) { Text("Пропустить") }
                TextButton(onClick = { onRestartRest(90) }) { Text("Рестарт") }
            }
        }

        items(active.exercises, key = { it.exerciseId }) { exercise ->
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(exercise.exerciseName)
                    exercise.sets.forEachIndexed { idx, set ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = set.weight, onValueChange = { onSetUpdate(exercise.exerciseId, idx, it, null) }, label = { Text("Вес") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = set.reps, onValueChange = { onSetUpdate(exercise.exerciseId, idx, null, it) }, label = { Text("Повт") }, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onRemoveSet(exercise.exerciseId, idx) }) { Icon(Icons.Default.Delete, null) }
                        }
                    }
                    TextButton(onClick = { onAddSet(exercise.exerciseId) }) { Text("+ Сет") }
                }
            }
        }

        item {
            Text("Добавить упражнение")
            quickAdd.take(10).forEach { ex -> TextButton(onClick = { onAddExercise(ex) }) { Text(ex.name) } }
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) { Text("Завершить тренировку") }
        }
    }
}

@Composable
private fun ProgressTab(prs: List<ExercisePrUi>, weekly: List<WeeklyVolumeUi>) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("PR по упражнениям") }
        items(prs) { pr -> Text("${pr.exerciseName}: volume ${pr.bestVolumeSet.toInt()} / e1RM ${"%.1f".format(pr.bestE1rm)}") }
        item { Spacer(Modifier.height(8.dp)); Text("Недельный объём") }
        items(weekly) { w -> Text("${w.weekKey}: ${w.volume.toInt()}") }
    }
}

@Composable
private fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onSave: (String, List<String>, String?, String, Boolean, String?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var muscles by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    var aliases by remember { mutableStateOf("") }
    var favorite by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<String?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        photoUri = uri?.let { persistImageToInternal(context, it) }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое упражнение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") })
                OutlinedTextField(muscles, { muscles = it }, label = { Text("Мышцы через запятую") })
                OutlinedTextField(equipment, { equipment = it }, label = { Text("Инвентарь") })
                OutlinedTextField(aliases, { aliases = it }, label = { Text("Alias") })
                TextButton(onClick = { favorite = !favorite }) { Text(if (favorite) "Убрать из избранного" else "В избранное") }
                TextButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Добавить фото") }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, muscles.split(",").map { it.trim() }.filter { it.isNotBlank() }, equipment.ifBlank { null }, aliases, favorite, photoUri) }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun persistImageToInternal(context: android.content.Context, source: Uri): String? = try {
    val dir = File(context.filesDir, "exercise_photos").apply { mkdirs() }
    val file = File(dir, "ex_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(source)?.use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
    }
    file.absolutePath
} catch (_: Exception) {
    null
}

