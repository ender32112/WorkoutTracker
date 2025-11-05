package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentNorm: Map<String, Int>,
    onSave: (Map<String, Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var calories by remember { mutableStateOf(currentNorm["calories"]?.toString() ?: "2500") }
    var protein by remember { mutableStateOf(currentNorm["protein"]?.toString() ?: "120") }
    var carbs by remember { mutableStateOf(currentNorm["carbs"]?.toString() ?: "300") }
    var fats by remember { mutableStateOf(currentNorm["fats"]?.toString() ?: "80") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Нормы КБЖУ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { char -> char.isDigit() } },
                    label = { Text("Калории") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { char -> char.isDigit() } },
                        label = { Text("Белки") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { char -> char.isDigit() } },
                        label = { Text("Углеводы") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = it.filter { char -> char.isDigit() } },
                        label = { Text("Жиры") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(mapOf(
                        "calories" to (calories.toIntOrNull() ?: 2500),
                        "protein" to (protein.toIntOrNull() ?: 120),
                        "carbs" to (carbs.toIntOrNull() ?: 300),
                        "fats" to (fats.toIntOrNull() ?: 80)
                    ))
                },
                enabled = calories.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}