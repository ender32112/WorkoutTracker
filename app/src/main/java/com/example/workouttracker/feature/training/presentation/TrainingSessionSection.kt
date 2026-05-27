package com.example.workouttracker.feature.training.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.max

private data class RestPreset(val seconds: Int, val label: String)

@Composable
fun TrainingSessionSection(
    active: ActiveWorkoutUiState?,
    favorites: List<ExerciseCatalogItem>,
    recent: List<ExerciseCatalogItem>,
    quickAdd: List<ExerciseCatalogItem>,
    onStart: () -> Unit,
    onAddExercise: (ExerciseCatalogItem) -> Unit,
    onSetUpdate: (String, Int, String?, String?) -> Unit,
    onAddSet: (String) -> Unit,
    onRemoveSet: (String, Int) -> Unit,
    onRemoveExercise: (String) -> Unit,
    onRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onRestartRest: (Int) -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onBrowseLibrary: () -> Unit
) {
    var addQuery by rememberSaveable { mutableStateOf("") }
    var preset by rememberSaveable { mutableStateOf(90) }
    val restPresets = remember {
        listOf(
            RestPreset(45, "45 сек"),
            RestPreset(60, "1 мин"),
            RestPreset(90, "1:30"),
            RestPreset(120, "2 мин"),
            RestPreset(180, "3 мин")
        )
    }

    if (active == null) {
        EmptyTrainingSessionState(
            favorites = favorites,
            recent = recent,
            onStart = onStart,
            onAddExercise = onAddExercise,
            onBrowseLibrary = onBrowseLibrary
        )
        return
    }

    val suggestions = quickAdd.filter {
        addQuery.isBlank() ||
            it.name.contains(addQuery, ignoreCase = true) ||
            it.nameEn.contains(addQuery, ignoreCase = true) ||
            it.muscles.any { muscle -> muscle.contains(addQuery, ignoreCase = true) }
    }
    val exerciseCount = active.exercises.size
    val setCount = active.exercises.sumOf { it.sets.size }
    val totalVolume = active.exercises.sumOf { exercise ->
        exercise.sets.sumOf { set ->
            (set.weight.toDoubleOrNull() ?: 0.0) * (set.reps.toIntOrNull() ?: 0)
        }
    }
    val estimatedDurationMinutes = max(1, setCount * 2)
    val timerTotalSeconds = if (active.timerRunning) max(preset, active.restTimerSecondsLeft) else preset
    val timerProgress = if (timerTotalSeconds > 0) {
        1f - (active.restTimerSecondsLeft.toFloat() / timerTotalSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val quickFavorites = favorites.take(4)
    val quickRecent = recent.take(4)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SessionHeroCard(
                active = active,
                exerciseCount = exerciseCount,
                setCount = setCount,
                totalVolume = totalVolume,
                estimatedDurationMinutes = estimatedDurationMinutes,
                onBrowseLibrary = onBrowseLibrary,
                onDiscard = onDiscard
            )
        }

        item {
            RestTimerCard(
                active = active,
                preset = preset,
                presets = restPresets,
                timerProgress = timerProgress,
                onSelectPreset = { preset = it },
                onRest = onRest,
                onRestartRest = onRestartRest,
                onSkipRest = onSkipRest
            )
        }

        items(active.exercises, key = { it.instanceId }) { exercise ->
            ActiveExerciseCard(
                exercise = exercise,
                onSetUpdate = onSetUpdate,
                onAddSet = onAddSet,
                onRemoveSet = onRemoveSet,
                onRemoveExercise = onRemoveExercise
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Быстрое добавление упражнений",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Добавляйте знакомые упражнения без лишнего скролла, а для полного списка переходите в библиотеку.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = addQuery,
                        onValueChange = { addQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Поиск по названию, мышцам или английскому имени") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    if (addQuery.isBlank() && quickFavorites.isNotEmpty()) {
                        QuickAddSection(
                            title = "Избранное",
                            subtitle = "Самые частые упражнения для быстрого старта",
                            items = quickFavorites,
                            onItemClick = onAddExercise
                        )
                    }

                    if (addQuery.isBlank() && quickRecent.isNotEmpty()) {
                        QuickAddSection(
                            title = "Недавние",
                            subtitle = "То, что вы использовали в последних тренировках",
                            items = quickRecent,
                            onItemClick = onAddExercise
                        )
                    }

                    QuickAddSection(
                        title = if (addQuery.isBlank()) "Подходящие упражнения" else "Результаты поиска",
                        subtitle = if (addQuery.isBlank()) "Подборка из локальной библиотеки" else "Лучшие совпадения по текущему запросу",
                        items = suggestions.take(8),
                        onItemClick = onAddExercise
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(onClick = onBrowseLibrary) {
                            Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null)
                            Text("Открыть библиотеку")
                        }
                        if (active.exercises.isEmpty()) {
                            TextButton(onClick = onDiscard) {
                                Text("Отменить тренировку")
                            }
                        } else {
                            Button(onClick = onFinish) {
                                Text("Завершить тренировку")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionHeroCard(
    active: ActiveWorkoutUiState,
    exerciseCount: Int,
    setCount: Int,
    totalVolume: Double,
    estimatedDurationMinutes: Int,
    onBrowseLibrary: () -> Unit,
    onDiscard: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedAssistChip(
                onClick = {},
                label = { Text("Рабочий режим") },
                leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null) }
            )
            Text(
                if (exerciseCount == 0) "Тренировка уже начата, но пока пустая"
                else "Сессия в работе и готова к следующему подходу",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (exerciseCount == 0) {
                    "Если запуск был случайным, тренировку можно отменить. Если всё в порядке, откройте библиотеку и добавьте упражнения."
                } else {
                    "Подходы, веса и таймер отдыха сохраняются автоматически. Можно спокойно продолжать сессию и возвращаться к ней позже."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SessionMetricChip("Упражнения", exerciseCount.toString())
                SessionMetricChip("Подходы", setCount.toString())
                SessionMetricChip("Объём", formatVolume(totalVolume))
                SessionMetricChip("Темп", "~$estimatedDurationMinutes мин")
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onBrowseLibrary) {
                    Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null)
                    Text(if (exerciseCount == 0) "Добавить из библиотеки" else "Открыть библиотеку")
                }
                if (exerciseCount == 0) {
                    TextButton(onClick = onDiscard) {
                        Text("Отменить")
                    }
                }
            }
        }
    }
}

@Composable
private fun RestTimerCard(
    active: ActiveWorkoutUiState,
    preset: Int,
    presets: List<RestPreset>,
    timerProgress: Float,
    onSelectPreset: (Int) -> Unit,
    onRest: (Int) -> Unit,
    onRestartRest: (Int) -> Unit,
    onSkipRest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Таймер отдыха",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (active.timerRunning) "Обратный отсчёт до следующего подхода."
                        else "Выберите пресет и запустите отдых между подходами.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatRestCountdown(active.restTimerSecondsLeft),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        if (active.timerRunning) "Осталось" else "Готово",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LinearProgressIndicator(
                progress = { timerProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 10.dp),
                trackColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { restPreset ->
                    FilterChip(
                        selected = preset == restPreset.seconds,
                        onClick = { onSelectPreset(restPreset.seconds) },
                        label = { Text(restPreset.label) }
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onRest(preset) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text(if (active.timerRunning) "Запустить заново" else "Запустить")
                }
                FilledTonalButton(onClick = { onRestartRest(preset) }) {
                    Icon(Icons.Default.Timer, contentDescription = null)
                    Text("Ещё раз")
                }
                TextButton(onClick = onSkipRest) {
                    Text("Пропустить")
                }
            }
        }
    }
}

@Composable
private fun ActiveExerciseCard(
    exercise: WorkoutExerciseInput,
    onSetUpdate: (String, Int, String?, String?) -> Unit,
    onAddSet: (String) -> Unit,
    onRemoveSet: (String, Int) -> Unit,
    onRemoveExercise: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        exercise.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        exercise.bodyPart?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                        exercise.equipment?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                        AssistChip(onClick = {}, label = { Text("${exercise.sets.size} подходов") })
                    }
                }
                IconButton(onClick = { onRemoveExercise(exercise.instanceId) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить упражнение")
                }
            }

            exercise.sets.forEachIndexed { index, set ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            Text("Подход ${index + 1}", fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { onRemoveSet(exercise.instanceId, index) }) {
                                Text("Удалить")
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = set.weight,
                                onValueChange = { value ->
                                    onSetUpdate(
                                        exercise.instanceId,
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
                                    onSetUpdate(
                                        exercise.instanceId,
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

            TextButton(onClick = { onAddSet(exercise.instanceId) }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Добавить подход")
            }
        }
    }
}

@Composable
private fun EmptyTrainingSessionState(
    favorites: List<ExerciseCatalogItem>,
    recent: List<ExerciseCatalogItem>,
    onStart: () -> Unit,
    onAddExercise: (ExerciseCatalogItem) -> Unit,
    onBrowseLibrary: () -> Unit
) {
    val quickStart = remember(favorites, recent) {
        (favorites + recent).distinctBy { it.id }.take(8)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ElevatedAssistChip(
                    onClick = {},
                    label = { Text("Рабочий режим") },
                    leadingIcon = { Icon(Icons.Default.Bolt, contentDescription = null) }
                )
                Text(
                    "Сессия ещё не начата",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Запустите новую тренировку, откройте библиотеку или сразу добавьте знакомые упражнения для быстрого старта.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onStart) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("Начать тренировку")
                    }
                    FilledTonalButton(onClick = onBrowseLibrary) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null)
                        Text("Из библиотеки")
                    }
                }
            }
        }

        if (quickStart.isNotEmpty()) {
            QuickAddSection(
                title = "Быстрый старт",
                subtitle = "Сразу добавьте знакомые упражнения в новую сессию",
                items = quickStart,
                onItemClick = onAddExercise
            )
        }
    }
}

@Composable
private fun SessionMetricChip(label: String, value: String) {
    AssistChip(onClick = {}, label = { Text("$label: $value") })
}

@Composable
private fun QuickAddSection(
    title: String,
    subtitle: String,
    items: List<ExerciseCatalogItem>,
    onItemClick: (ExerciseCatalogItem) -> Unit
) {
    if (items.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { exercise ->
                FilledTonalButton(
                    onClick = { onItemClick(exercise) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = exercise.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val meta = buildList {
                            if (exercise.bodyPart.isNotBlank()) add(exercise.bodyPart)
                            if (exercise.muscles.isNotEmpty()) add(exercise.muscles.take(2).joinToString(", "))
                        }.joinToString(" • ")
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatRestCountdown(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val minutes = safe / 60
    val remainder = safe % 60
    return "%02d:%02d".format(minutes, remainder)
}

private fun formatVolume(volume: Double): String =
    "%,d кг".format(volume.toInt()).replace(',', ' ')
