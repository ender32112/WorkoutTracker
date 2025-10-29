package com.example.workouttracker.ui.training

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrainingDialog(
    onSave: (TrainingSession) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember { mutableStateOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) }
    var showDatePicker by remember { mutableStateOf(false) }

    val allExercises = listOf("Жим штанги лежа", "Присед Смит", "Тяга верхнего блока", "Становая тяга", "Румынская тяга", "Бёрпи", "Подтягивания", "Выпады")
    var entries by remember { mutableStateOf(listOf<ExerciseEntry>()) }


    var selectedName by remember { mutableStateOf(allExercises.first()) }
    var sets by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить тренировку") },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {

                Button(onClick = { showDatePicker = true }) {
                    Text("Дата тренировки: $date")
                }
                Spacer(Modifier.height(8.dp))


                Text("Новое упражнение", style = MaterialTheme.typography.titleSmall)

                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = {}
                ) {
                    TextField(
                        value = selectedName,
                        onValueChange = {},
                        label = { Text("Упражнение") },
                        readOnly = true,
                        trailingIcon = { /* стрелка */ }
                    )

                }

                allExercises.forEach { name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedName == name,
                            onClick = { selectedName = name }
                        )
                        Text(name)
                    }
                }
                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Подходы") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Повторы в подходе") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    val entry = ExerciseEntry(
                        name = selectedName,
                        sets = sets.toIntOrNull() ?: 0,
                        reps = reps.toIntOrNull() ?: 0
                    )
                    entries = entries + entry
                    sets = ""
                    reps = ""
                }) {
                    Text("Добавить упражнение")
                }
                Spacer(Modifier.height(12.dp))


                if (entries.isNotEmpty()) {
                    Text("Упражнения в сессии:", style = MaterialTheme.typography.titleSmall)
                    entries.forEach {
                        Text("• ${it.name}: ${it.sets}×${it.reps}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(TrainingSession(date = date, exercises = entries))
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
            LocalContext.current,
            { _, y, m, d ->
                date = String.format("%04d-%02d-%02d", y, m + 1, d)
                showDatePicker = false
            },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
