package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNutritionDialog(
    onConfirm: (NutritionEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая запись", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Дата (гггг-мм-дд)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { char -> char.isDigit() } },
                    label = { Text("Калории") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it.filter { char -> char.isDigit() } },
                    label = { Text("Белки (г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it.filter { char -> char.isDigit() } },
                    label = { Text("Углеводы (г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fats,
                    onValueChange = { fats = it.filter { char -> char.isDigit() } },
                    label = { Text("Жиры (г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val entry = NutritionEntry(
                        date = date,
                        calories = calories.toIntOrNull() ?: 0,
                        protein = protein.toIntOrNull() ?: 0,
                        carbs = carbs.toIntOrNull() ?: 0,
                        fats = fats.toIntOrNull() ?: 0
                    )
                    onConfirm(entry)
                },
                enabled = calories.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}