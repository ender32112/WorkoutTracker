package com.example.workouttracker.ui.nutrition

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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
fun AddNutritionDialog(
    entry: NutritionEntry? = null,
    onConfirm: (NutritionEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var date by remember(entry?.date) {
        mutableStateOf(entry?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var name by remember(entry?.name) { mutableStateOf(entry?.name ?: "") }
    var calories by remember(entry?.calories) { mutableStateOf(entry?.calories?.toString() ?: "") }
    var protein by remember(entry?.protein) { mutableStateOf(entry?.protein?.toString() ?: "") }
    var carbs by remember(entry?.carbs) { mutableStateOf(entry?.carbs?.toString() ?: "") }
    var fats by remember(entry?.fats) { mutableStateOf(entry?.fats?.toString() ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Добавить приём" else "Редактировать", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название блюда") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formatDateForDisplay(date),
                    onValueChange = {},
                    label = { Text("Дата") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.CalendarMonth, "Выбрать")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { it.isDigit() } },
                    label = { Text("Калории") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it.filter { it.isDigit() } },
                        label = { Text("Белки") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { it.isDigit() } },
                        label = { Text("Углеводы") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = it.filter { it.isDigit() } },
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
                    val newEntry = (entry ?: NutritionEntry(
                        date = date, name = "", calories = 0, protein = 0, carbs = 0, fats = 0
                    )).copy(
                        date = date,
                        name = name,
                        calories = calories.toIntOrNull() ?: 0,
                        protein = protein.toIntOrNull() ?: 0,
                        carbs = carbs.toIntOrNull() ?: 0,
                        fats = fats.toIntOrNull() ?: 0
                    )
                    onConfirm(newEntry)
                },
                enabled = name.isNotBlank() && calories.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )

    if (showDatePicker) {
        val parts = date.split("-").map { it.toInt() }
        DatePickerDialog(
            context,
            { _, y, m, d -> date = String.format("%04d-%02d-%02d", y, m + 1, d) },
            parts[0], parts[1] - 1, parts[2]
        ).show()
    }
}

fun formatDateForDisplay(date: String): String =
    date.replace(Regex("(\\d{4})-(\\d{2})-(\\d{2})"), "$3.$2.$1")