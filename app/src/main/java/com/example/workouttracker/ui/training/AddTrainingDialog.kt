package com.example.workouttracker.ui.training

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrainingDialog(
    session: TrainingSession? = null,
    onSave: (TrainingSession) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember(session) {
        mutableStateOf(session?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val allExercises = listOf(
        "Жим штанги лежа", "Присед", "Становая тяга", "Тяга верхнего блока",
        "Подтягивания", "Отжимания", "Жим гантелей", "Выпады"
    )

    // Копируем с id!
    var exercises by remember(session) {
        mutableStateOf(
            session?.exercises?.map { it.copy() }?.toMutableList() ?: mutableStateListOf<ExerciseEntry>()
        )
    }

    var showExercisePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var editingNewExerciseId by remember { mutableStateOf<UUID?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (session == null) "Новая тренировка" else "Редактировать",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = formatDateForDisplay(date),
                        onValueChange = {},
                        label = { Text("Дата") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Выбрать дату")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { showExercisePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить упражнение")
                    }
                }

                items(exercises, key = { it.id }) { exercise ->
                    val isEditing = editingNewExerciseId == exercise.id
                    var editName by remember(isEditing) { mutableStateOf(exercise.name) }
                    var editSets by remember(isEditing) { mutableStateOf(exercise.sets.toString()) }
                    var editReps by remember(isEditing) { mutableStateOf(exercise.reps.toString()) }
                    var editWeight by remember(isEditing) { mutableStateOf(exercise.weight.toString()) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            AnimatedVisibility(visible = !isEditing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${exercise.sets}×${exercise.reps} @ ${exercise.weight} кг",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = { editingNewExerciseId = exercise.id }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                                        }
                                        IconButton(onClick = { exercises.remove(exercise) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Удалить")
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = isEditing) {
                                Column {
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("Упражнение") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editSets,
                                            onValueChange = { editSets = it.filter { char -> char.isDigit() } },
                                            label = { Text("Подх.") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = editReps,
                                            onValueChange = { editReps = it.filter { char -> char.isDigit() } },
                                            label = { Text("Повт.") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = editWeight,
                                            onValueChange = { editWeight = it.filter { char -> char.isDigit() || char == '.' } },
                                            label = { Text("Вес") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { editingNewExerciseId = null }) {
                                            Text("Отмена")
                                        }
                                        TextButton(onClick = {
                                            val updated = exercise.copy(
                                                name = editName,
                                                sets = editSets.toIntOrNull() ?: 1,
                                                reps = editReps.toIntOrNull() ?: 1,
                                                weight = editWeight.toFloatOrNull() ?: 0f
                                            )
                                            val index = exercises.indexOf(exercise)
                                            if (index != -1) {
                                                exercises[index] = updated
                                            }
                                            editingNewExerciseId = null
                                        }) {
                                            Text("Сохранить")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newSession = TrainingSession(
                        id = session?.id ?: UUID.randomUUID(),
                        date = date,
                        exercises = exercises.toList()
                    )
                    onSave(newSession)
                },
                enabled = exercises.isNotEmpty()
            ) {
                Text("Готово")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )

    // DatePicker
    if (showDatePicker) {
        val parts = date.split("-").map { it.toInt() }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                date = String.format("%04d-%02d-%02d", y, m + 1, d)
                showDatePicker = false
            },
            parts[0], parts[1] - 1, parts[2]
        ).show()
    }

    // Добавление упражнения
    if (showExercisePicker) {
        var search by remember { mutableStateOf("") }
        val filtered = allExercises.filter { it.contains(search, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { showExercisePicker = false },
            title = { Text("Выберите упражнение") },
            text = {
                Column {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        label = { Text("Поиск") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filtered) { name ->
                            ListItem(
                                headlineContent = { Text(name) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newExercise = ExerciseEntry(name = name, sets = 0, reps = 0, weight = 0f)
                                        exercises.add(newExercise)
                                        showExercisePicker = false

                                        coroutineScope.launch {
                                            val index = exercises.indexOf(newExercise)
                                            listState.animateScrollToItem(index + 2)
                                            editingNewExerciseId = newExercise.id
                                        }
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExercisePicker = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

fun formatDateForDisplay(date: String): String {
    return date.replace(Regex("(\\d{4})-(\\d{2})-(\\d{2})"), "$3.$2.$1")
}