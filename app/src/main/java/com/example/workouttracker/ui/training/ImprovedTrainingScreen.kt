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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewCarousel
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
import androidx.compose.ui.text.style.TextOverflow
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

                TrainingHomeView.CATALOG -> CatalogScreen(items = quick)

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
private fun CatalogScreen(items: List<ExerciseCatalogItem>) {
    var query by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }
    var selectedEquipment by remember { mutableStateOf<String?>(null) }

    val muscleFilters = remember(items) { items.flatMap { it.muscles }.distinct().sorted().take(10) }
    val equipmentFilters = remember(items) { items.mapNotNull { it.equipment }.distinct().sorted().take(8) }

    val filtered = remember(items, query, selectedMuscle, selectedEquipment) {
        val q = query.trim().lowercase()
        items.filter {
            val queryPass = q.isBlank() ||
                it.name.lowercase().contains(q) ||
                it.aliases.lowercase().contains(q) ||
                it.muscles.any { muscle -> muscle.lowercase().contains(q) }
            val musclePass = selectedMuscle == null || it.muscles.any { m -> m.equals(selectedMuscle, ignoreCase = true) }
            val equipmentPass = selectedEquipment == null || it.equipment.equals(selectedEquipment, ignoreCase = true)
            queryPass && musclePass && equipmentPass
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.22f))) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 10.dp,
            shadowElevation = 10.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Каталог упражнений", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Фото упражнения можно добавить при создании/редактировании записи тренировки.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        label = { Text("Поиск упражнения") },
                        singleLine = true
                    )
                    if (muscleFilters.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            muscleFilters.take(3).forEach { muscle ->
                                FilterChip(
                                    selected = selectedMuscle == muscle,
                                    onClick = { selectedMuscle = if (selectedMuscle == muscle) null else muscle },
                                    label = { Text(muscle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                    if (equipmentFilters.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            equipmentFilters.take(2).forEach { equipment ->
                                FilterChip(
                                    selected = selectedEquipment == equipment,
                                    onClick = { selectedEquipment = if (selectedEquipment == equipment) null else equipment },
                                    label = { Text(equipment, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                    Text("Найдено: ${filtered.size}", style = MaterialTheme.typography.bodySmall)
                }

                items(filtered, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (item.photoUri.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Image(
                                    painter = rememberAsyncImagePainter(item.photoUri),
                                    contentDescription = item.name,
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(item.muscles.joinToString(), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                item.equipment?.let {
                                    Text("Инвентарь: $it", style = MaterialTheme.typography.labelSmall)
                                }
                            }
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
    var addQuery by remember { mutableStateOf("") }

    if (active == null) {
        val filtered = quickAdd.filter {
            addQuery.isBlank() || it.name.contains(addQuery, ignoreCase = true) || it.aliases.contains(addQuery, ignoreCase = true)
        }
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Новая тренировка")
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Старт тренировки") }
            OutlinedTextField(addQuery, { addQuery = it }, label = { Text("Поиск упражнения") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            filtered.take(8).forEach { ex -> TextButton(onClick = { onAddExercise(ex) }) { Text(ex.name) } }
        }
        return
    }

    val maxTimerSeconds = 300f
    val timerPresets = listOf(60, 180, 300)
    var selectedPreset by remember { mutableStateOf(60) }
    val filteredQuick = remember(quickAdd, addQuery) {
        quickAdd.filter {
            addQuery.isBlank() || it.name.contains(addQuery, ignoreCase = true) || it.aliases.contains(addQuery, ignoreCase = true)
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
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
                                label = { Text(if (sec < 120) "1 мин" else "${sec / 60} мин", maxLines = 1) }
                            )
                        }
                    }
                    Button(onClick = { onRest(selectedPreset) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Запустить таймер", maxLines = 1)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onSkipRest, modifier = Modifier.weight(1f)) { Text("Пропустить", maxLines = 1) }
                        TextButton(onClick = { onRestartRest(selectedPreset) }, modifier = Modifier.weight(1f)) { Text("Перезапустить", maxLines = 1) }
                    }
                }
            }
        }

        items(active.exercises, key = { it.exerciseId }) { exercise ->
            val catalogExercise = quickAdd.firstOrNull { it.id == exercise.exerciseId }
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        if (catalogExercise?.photoUri.isNullOrBlank()) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(catalogExercise?.photoUri),
                                contentDescription = exercise.exerciseName,
                                modifier = Modifier.size(42.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(exercise.exerciseName, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(catalogExercise?.muscles?.joinToString().orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            prMap[exercise.exerciseName]?.let {
                                Text("PR: ${it.bestVolumeSet.toInt()} / ${"%.1f".format(it.bestE1rm)}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    exercise.sets.forEachIndexed { idx, set ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(set.weight, { onSetUpdate(exercise.exerciseId, idx, it, null) }, label = { Text("Вес") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(set.reps, { onSetUpdate(exercise.exerciseId, idx, null, it) }, label = { Text("Повторы") }, modifier = Modifier.weight(1f), singleLine = true)
                            TextButton(onClick = { onRemoveSet(exercise.exerciseId, idx) }) { Icon(Icons.Default.Delete, null) }
                        }
                    }
                    TextButton(onClick = { onAddSet(exercise.exerciseId) }) { Text("+ Добавить сет") }
                }
            }
        }

        item {
            Text("Быстрое добавление")
            OutlinedTextField(
                value = addQuery,
                onValueChange = { addQuery = it },
                label = { Text("Найти упражнение") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            filteredQuick.take(10).forEach { ex -> TextButton(onClick = { onAddExercise(ex) }) { Text(ex.name) } }
            Spacer(Modifier.height(6.dp))
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
            .sortedByDescending { it.totalVolume }
    }

    val totalSessions = sessions.size
    val totalVolume = sessions.sumOf { it.totalVolume }
    val avgSessionVolume = if (totalSessions == 0) 0.0 else totalVolume / totalSessions

    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Сводка", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Тренировок: $totalSessions")
                    Text("Общий объём: ${totalVolume.toInt()}")
                    Text("Средний объём: ${avgSessionVolume.toInt()}")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Лучшие PR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    prs.sortedByDescending { it.bestVolumeSet }.take(7).forEach { pr ->
                        Text("${pr.exerciseName}: объем ${pr.bestVolumeSet.toInt()} • e1RM ${"%.1f".format(pr.bestE1rm)}")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Недельный объём", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    weekly.forEach { w -> Text("${w.weekKey}: ${w.volume.toInt()}") }
                }
            }
        }
        items(exerciseAnalytics, key = { it.name }) { analytics ->
            val positive = analytics.deltaPercent >= 0
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(analytics.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Сессий: ${analytics.sessionsCount} • Суммарно: ${analytics.totalVolume.toInt()}")
                    Text("Лучший объём: ${analytics.bestSetVolume.toInt()}")
                    Text(
                        text = "Динамика: ${if (positive) "+" else ""}${"%.1f".format(analytics.deltaPercent)}%",
                        color = if (positive) Color(0xFF1F8F57) else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
