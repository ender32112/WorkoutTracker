package com.example.workouttracker.ui.training

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddTrainingDialog(
    session: TrainingSession? = null,
    availableExercises: List<ExerciseCatalogItem> = emptyList(),
    onSave: (TrainingSession) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var date by remember(session) {
        mutableStateOf(session?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val exercises = remember(session) {
        mutableStateListOf<ExerciseEntry>().also { list ->
            session?.exercises?.forEach { list.add(it.copy()) }
        }
    }

    var showExercisePicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var editingExerciseId by remember { mutableStateOf<UUID?>(null) }

    // Вспомогательные санитайзеры
    fun sanitizeInt(s: String, maxDigits: Int = 3) =
        s.filter { it.isDigit() }.take(maxDigits)

    fun sanitizeFloatDecimal(s: String, maxLen: Int = 6): String {
        val normalized = s.replace(',', '.')
        val cleaned = buildString {
            var dot = false
            for (c in normalized) {
                if (c.isDigit()) append(c)
                else if (c == '.' && !dot) { append('.'); dot = true }
                if (length >= maxLen) break
            }
        }
        return cleaned
    }

    // Диалог выбора/создания упражнения
    if (showExercisePicker) {
        var search by remember { mutableStateOf("") }
        val names = availableExercises.map { it.name }.distinct().sorted()
        val filtered = names.filter { it.contains(search, ignoreCase = true) }

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

                    if (search.isNotBlank() && filtered.isEmpty()) {
                        TextButton(
                            onClick = {
                                val name = search.trim()
                                val fromCatalog = availableExercises.firstOrNull { it.name.equals(name, true) }
                                val newExercise = ExerciseEntry(
                                    name = name,
                                    sets = 3, reps = 10, weight = 0f,
                                    photoUri = fromCatalog?.photoUri
                                )
                                exercises.add(newExercise)
                                showExercisePicker = false
                                val last = (exercises.size - 1).coerceAtLeast(0)
                                coroutineScope.launch { listState.animateScrollToItem(last) }
                                editingExerciseId = newExercise.id
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Создать \"$search\"", maxLines = 1, softWrap = false)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filtered) { n ->
                            val catalogItem = availableExercises.firstOrNull { it.name == n }
                            ListItem(
                                headlineContent = { Text(n, maxLines = 1) },
                                supportingContent = {
                                    if (catalogItem?.photoUri != null) Text("Есть фото") else Text("Без фото")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newExercise = ExerciseEntry(
                                            name = n, sets = 3, reps = 10, weight = 0f,
                                            photoUri = catalogItem?.photoUri
                                        )
                                        exercises.add(newExercise)
                                        showExercisePicker = false
                                        val last = (exercises.size - 1).coerceAtLeast(0)
                                        coroutineScope.launch { listState.animateScrollToItem(last) }
                                        editingExerciseId = newExercise.id
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showExercisePicker = false }) { Text("Отмена") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (session == null) "Новая тренировка" else "Редактировать тренировку",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
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
                    FilledTonalButton(
                        onClick = { showExercisePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить упражнение", maxLines = 1, softWrap = false)
                    }
                }

                items(exercises, key = { it.id }) { exercise ->
                    val isEditing = editingExerciseId == exercise.id

                    var editName by remember(isEditing) { mutableStateOf(exercise.name) }
                    var editSets by remember(isEditing) { mutableStateOf(exercise.sets.toString()) }
                    var editReps by remember(isEditing) { mutableStateOf(exercise.reps.toString()) }
                    var editWeight by remember(isEditing) { mutableStateOf(exercise.weight.toString()) }
                    var editPhotoUri by remember(isEditing) { mutableStateOf(exercise.photoUri) }

                    var nameErr by remember(isEditing) { mutableStateOf<String?>(null) }
                    var setsErr by remember(isEditing) { mutableStateOf<String?>(null) }
                    var repsErr by remember(isEditing) { mutableStateOf<String?>(null) }
                    var weightErr by remember(isEditing) { mutableStateOf<String?>(null) }

                    // Лаунчер выбора фото (используем заранее захваченный context!)
                    val pickPhoto = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia()
                    ) { uri ->
                        editPhotoUri = uri?.let { persistImageToInternal(context, it) } ?: editPhotoUri
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {

                            AnimatedVisibility(visible = !isEditing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${exercise.sets}×${exercise.reps} @ ${exercise.weight} кг",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (exercise.photoUri != null) {
                                            val bmp = remember(exercise.photoUri) {
                                                loadBitmapFlexible(context, exercise.photoUri)
                                            }
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                            }
                                        }
                                        IconButton(onClick = { editingExerciseId = exercise.id }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                                        }
                                        IconButton(onClick = { exercises.remove(exercise) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Удалить")
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = isEditing) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = editName,
                                            onValueChange = { editName = it.take(40); nameErr = null },
                                            label = { Text("Упражнение") },
                                            isError = nameErr != null,
                                            supportingText = { if (nameErr != null) Text(nameErr!!) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                pickPhoto.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Фото")
                                        }
                                    }

                                    val bmp = remember(editPhotoUri) { loadBitmapFlexible(context, editPhotoUri) }
                                    AnimatedVisibility(visible = bmp != null) {
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(72.dp)
                                                    .clip(CircleShape)
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = editSets,
                                            onValueChange = { editSets = sanitizeInt(it, 2); setsErr = null },
                                            label = { Text("Подх. (1–10)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            isError = setsErr != null,
                                            supportingText = { if (setsErr != null) Text(setsErr!!) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = editReps,
                                            onValueChange = { editReps = sanitizeInt(it, 3); repsErr = null },
                                            label = { Text("Повт. (1–100)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            isError = repsErr != null,
                                            supportingText = { if (repsErr != null) Text(repsErr!!) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = editWeight,
                                            onValueChange = { editWeight = sanitizeFloatDecimal(it, 6); weightErr = null },
                                            label = { Text("Вес (0–1000)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            isError = weightErr != null,
                                            supportingText = { if (weightErr != null) Text(weightErr!!) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                        TextButton(onClick = { editingExerciseId = null }) { Text("Отмена") }
                                        TextButton(
                                            onClick = {
                                                val s = editSets.toIntOrNull() ?: 0
                                                val r = editReps.toIntOrNull() ?: 0
                                                val w = editWeight.toFloatOrNull() ?: -1f
                                                var ok = true
                                                if (editName.isBlank()) { nameErr = "Укажите название"; ok = false }
                                                if (s !in 1..10) { setsErr = "1–10"; ok = false }
                                                if (r !in 1..100) { repsErr = "1–100"; ok = false }
                                                if (w < 0f || w > 1000f) { weightErr = "0–1000 кг"; ok = false }
                                                if (!ok) return@TextButton

                                                val updated = exercise.copy(
                                                    name = editName.trim(),
                                                    sets = s, reps = r, weight = w,
                                                    photoUri = editPhotoUri
                                                )
                                                val index = exercises.indexOf(exercise)
                                                if (index != -1) exercises[index] = updated
                                                editingExerciseId = null
                                            }
                                        ) { Text("Сохранить") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val sessionOk = exercises.isNotEmpty() && exercises.all {
                it.name.isNotBlank() && it.sets in 1..10 && it.reps in 1..100 && it.weight in 0f..1000f
            }
            TextButton(
                onClick = {
                    val newSession = TrainingSession(
                        id = session?.id ?: UUID.randomUUID(),
                        date = date,
                        exercises = exercises.toList()
                    )
                    onSave(newSession)
                },
                enabled = sessionOk
            ) { Text("Готово") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )

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
}

fun formatDateForDisplay(date: String): String =
    date.replace(Regex("(\\d{4})-(\\d{2})-(\\d{2})"), "$3.$2.$1")
