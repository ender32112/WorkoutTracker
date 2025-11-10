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

    // Новые поля ввода: вес порции и БЖУ "на 100 г"
    var weightStr by remember(entry?.weight) { mutableStateOf(entry?.weight?.toString() ?: "") }
    var p100 by remember { mutableStateOf("") }
    var f100 by remember { mutableStateOf("") }
    var c100 by remember { mutableStateOf("") }

    // Если редактируем старую запись — подставим оценку per-100 из итога и веса (по желанию можно не делать)
    LaunchedEffect(entry?.id) {
        entry?.let {
            if (weightStr.isBlank()) weightStr = it.weight.toString()
            // Подставлять обратные per-100 не обязательно; оставляем пустыми, чтобы пользователь вёл свои значения
        }
    }

    // Парсинг
    val weight = weightStr.toIntOrNull() ?: 0
    val p100f = p100.replace(',', '.').toFloatOrNull() ?: 0f
    val f100f = f100.replace(',', '.').toFloatOrNull() ?: 0f
    val c100f = c100.replace(',', '.').toFloatOrNull() ?: 0f

    // Итоговые БЖУ и калории (по текущему вводу)
    val totalProtein = ((p100f * weight) / 100f).toInt()
    val totalFats   = ((f100f * weight) / 100f).toInt()
    val totalCarbs  = ((c100f * weight) / 100f).toInt()
    val totalKcal   = 4 * totalProtein + 9 * totalFats + 4 * totalCarbs

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
                            Icon(Icons.Filled.CalendarMonth, contentDescription = "Выбрать дату")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Вес порции в граммах
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { s -> weightStr = s.filter { it.isDigit() } },
                    label = { Text("Вес порции, г") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // БЖУ на 100 г
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = p100,
                        onValueChange = { s ->
                            p100 = s.filter { it.isDigit() || it == '.' || it == ',' }
                        },
                        label = { Text("Белки / 100 г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = f100,
                        onValueChange = { s ->
                            f100 = s.filter { it.isDigit() || it == '.' || it == ',' }
                        },
                        label = { Text("Жиры / 100 г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = c100,
                        onValueChange = { s ->
                            c100 = s.filter { it.isDigit() || it == '.' || it == ',' }
                        },
                        label = { Text("Углеводы / 100 г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Превью итоговых значений (автоподсчёт)
                ElevatedCard {
                    Column(Modifier.padding(12.dp)) {
                        Text("Итого за порцию", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("Калории: $totalKcal ккал", style = MaterialTheme.typography.bodyMedium)
                        Text("Б: $totalProtein г   Ж: $totalFats г   У: $totalCarbs г", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            // Разрешаем сохранить только если введены базовые поля
            val canSave = name.isNotBlank() && weight > 0 &&
                    (p100f > 0f || f100f > 0f || c100f > 0f)

            TextButton(
                onClick = {
                    val base = entry ?: NutritionEntry(
                        date = date, name = "", calories = 0, protein = 0, carbs = 0, fats = 0, weight = 0
                    )
                    val newEntry = base.copy(
                        date = date,
                        name = name.trim(),
                        calories = totalKcal,
                        protein = totalProtein,
                        carbs = totalCarbs,
                        fats = totalFats,
                        weight = weight
                    )
                    onConfirm(newEntry)
                },
                enabled = canSave
            ) { Text("Сохранить") }
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
