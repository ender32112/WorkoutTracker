package com.example.workouttracker.ui.nutrition

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNutritionDialog(
    entry: NutritionEntry? = null,
    onConfirm: (NutritionEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var date by remember(entry?.date) {
        mutableStateOf(entry?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var name by remember(entry?.name) { mutableStateOf(entry?.name ?: "") }

    var weightStr by remember(entry?.weight) { mutableStateOf(entry?.weight?.toString() ?: "") }
    var p100 by remember { mutableStateOf("") }
    var f100 by remember { mutableStateOf("") }
    var c100 by remember { mutableStateOf("") }

    // ← при редактировании подставляем оценку per-100 из текущей записи (если поля ещё пустые)
    LaunchedEffect(entry?.id) {
        entry?.let { e ->
            if (weightStr.isBlank()) weightStr = e.weight.toString()
            if (e.weight > 0) {
                if (p100.isBlank()) p100 = ((e.protein * 100f) / e.weight).coerceIn(0f, 100f).let { String.format(Locale.US, "%.1f", it) }
                if (f100.isBlank()) f100 = ((e.fats * 100f) / e.weight).coerceIn(0f, 100f).let { String.format(Locale.US, "%.1f", it) }
                if (c100.isBlank()) c100 = ((e.carbs * 100f) / e.weight).coerceIn(0f, 100f).let { String.format(Locale.US, "%.1f", it) }
            }
        }
    }

    // Ошибки
    var nameError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var macroError by remember { mutableStateOf<String?>(null) }
    var hardLimitError by remember { mutableStateOf<String?>(null) }

    // Парсинг
    val weight = weightStr.toIntOrNull() ?: 0
    val p100f = p100.replace(',', '.').toFloatOrNull() ?: 0f
    val f100f = f100.replace(',', '.').toFloatOrNull() ?: 0f
    val c100f = c100.replace(',', '.').toFloatOrNull() ?: 0f

    // Итоги
    val totalProtein = ((p100f * weight) / 100f).toInt()
    val totalFats   = ((f100f * weight) / 100f).toInt()
    val totalCarbs  = ((c100f * weight) / 100f).toInt()
    val totalKcal   = 4 * totalProtein + 9 * totalFats + 4 * totalCarbs

    // Жёсткие рамки
    val WEIGHT_MIN = 1
    val WEIGHT_MAX = 2000
    val PER100_MIN = 0f
    val PER100_MAX = 100f
    val KCAL_MAX = 6000

    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        val parts = date.split("-").map { it.toInt() }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                date = String.format("%04d-%02d-%02d", y, m + 1, d)
                showDatePicker = false
            },
            parts[0], parts[1] - 1, parts[2]
        ).apply { setOnDismissListener { showDatePicker = false } }.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (entry == null) "Добавить приём" else "Редактировать приём",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(40) // ограничим длину
                        nameError = null
                    },
                    label = { Text("Название блюда") },
                    supportingText = { if (nameError != null) Text(nameError!!, color = MaterialTheme.colorScheme.error) },
                    isError = nameError != null,
                    singleLine = true,
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

                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { s ->
                        weightStr = s.filter { it.isDigit() }.take(4)
                        weightError = null
                        hardLimitError = null
                    },
                    label = { Text("Вес порции, г") },
                    supportingText = {
                        when {
                            hardLimitError != null -> Text(hardLimitError!!, color = MaterialTheme.colorScheme.error)
                            weightError != null -> Text(weightError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    isError = weightError != null || hardLimitError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = p100,
                        onValueChange = { s ->
                            p100 = s.filter { it.isDigit() || it == '.' || it == ',' }.take(6)
                            macroError = null
                        },
                        label = { Text("Белки / 100 г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = f100,
                        onValueChange = { s ->
                            f100 = s.filter { it.isDigit() || it == '.' || it == ',' }.take(6)
                            macroError = null
                        },
                        label = { Text("Жиры / 100 г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = c100,
                        onValueChange = { s ->
                            c100 = s.filter { it.isDigit() || it == '.' || it == ',' }.take(6)
                            macroError = null
                        },
                        label = { Text("Углеводы / 100 г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (macroError != null) {
                    Text(macroError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Итого за порцию", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(6.dp))
                        Text("Калории: $totalKcal ккал", style = MaterialTheme.typography.bodyMedium)
                        Text("Б: $totalProtein г   Ж: $totalFats г   У: $totalCarbs г", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            val canSave = name.isNotBlank() && weight in WEIGHT_MIN..WEIGHT_MAX &&
                    // хотя бы один макро > 0
                    (p100f > 0f || f100f > 0f || c100f > 0f) &&
                    // все введённые макро в допустимых границах
                    (p100f in PER100_MIN..PER100_MAX || p100.isBlank()) &&
                    (f100f in PER100_MIN..PER100_MAX || f100.isBlank()) &&
                    (c100f in PER100_MIN..PER100_MAX || c100.isBlank()) &&
                    // итоговые калории не зашкаливают
                    (totalKcal <= KCAL_MAX)

            TextButton(
                onClick = {
                    var ok = true
                    if (name.isBlank()) { nameError = "Укажите название"; ok = false }

                    if (weight !in WEIGHT_MIN..WEIGHT_MAX) {
                        weightError = "Диапазон $WEIGHT_MIN–$WEIGHT_MAX г"
                        ok = false
                    }

                    fun checkMacro(v: Float, label: String): Boolean {
                        if (v == 0f) return true // ноль допустим, если другие макросы заданы
                        if (v !in PER100_MIN..PER100_MAX) {
                            macroError = "$label /100 г: $PER100_MIN..$PER100_MAX"
                            return false
                        }
                        return true
                    }
                    val mOk = checkMacro(p100f, "Белки") && checkMacro(f100f, "Жиры") && checkMacro(c100f, "Углеводы")
                    if (!mOk) ok = false

                    if (p100f <= 0f && f100f <= 0f && c100f <= 0f) {
                        macroError = "Укажите хотя бы один макроэлемент /100 г"
                        ok = false
                    }

                    if (totalKcal > KCAL_MAX) {
                        hardLimitError = "Слишком много калорий (> $KCAL_MAX ккал)"
                        ok = false
                    } else {
                        hardLimitError = null
                    }

                    if (!ok) return@TextButton

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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}


fun formatDateForDisplay(date: String): String =
    date.replace(Regex("(\\d{4})-(\\d{2})-(\\d{2})"), "$3.$2.$1")
