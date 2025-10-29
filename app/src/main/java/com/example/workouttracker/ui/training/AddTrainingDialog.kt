package com.example.workouttracker.ui.training

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrainingDialog(
    onSave: (TrainingSession) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val allExercises = listOf(
        "Жим штанги лежа", "Присед Смит", "Тяга верхнего блока",
        "Становая тяга", "Румынская тяга", "Бёрпи", "Подтягивания", "Выпады"
    )
    var entries by remember { mutableStateOf(listOf<ExerciseEntry>()) }

    var expanded by remember { mutableStateOf(false) }
    var selectedName by remember { mutableStateOf(allExercises.first()) }
    var sets by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая тренировка", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Дата
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Дата: $date")
                }

                // Упражнение
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {},
                        label = { Text("Упражнение") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        allExercises.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedName = name
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Поля
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it.filter { it.isDigit() } },
                    label = { Text("Подходы") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it.filter { it.isDigit() } },
                    label = { Text("Повторы") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { it.isDigit() || it == '.' } },
                    label = { Text("Вес (кг)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Добавить
                Button(
                    onClick = {
                        val entry = ExerciseEntry(
                            name = selectedName,
                            sets = sets.toIntOrNull() ?: 1,
                            reps = reps.toIntOrNull() ?: 1,
                            weight = weight.toFloatOrNull() ?: 0f
                        )
                        entries = entries + entry
                        sets = ""; reps = ""; weight = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Добавить упражнение")
                }

                // Список
                if (entries.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Упражнения:", style = MaterialTheme.typography.titleMedium)
                            entries.forEach {
                                Text("• ${it.name}: ${it.sets}×${it.reps} @ ${it.weight}кг")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(TrainingSession(date = date, exercises = entries))
                onDismiss()
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )

    if (showDatePicker) {
        val c = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                date = String.format("%04d-%02d-%02d", y, m + 1, d)
                showDatePicker = false
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}