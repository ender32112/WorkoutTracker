package com.example.workouttracker.ui.training

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrainingBottomSheet(
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

    var name by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("10") }
    var weight by remember { mutableStateOf("0") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var nameErr by remember { mutableStateOf<String?>(null) }

    val exercises = remember(session) {
        mutableStateListOf<ExerciseEntry>().also { list -> session?.exercises?.forEach { list.add(it.copy()) } }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        photoUri = uri?.let { persistImageToInternal(context, it) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Новая тренировка")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Дата: ${formatDateForDisplay(date)}", modifier = Modifier.weight(1f))
                    TextButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, null)
                        Text("Изменить")
                    }
                }
            }
            item {
                OutlinedTextField(name, { name = it.take(40); nameErr = null }, label = { Text("Упражнение") }, isError = nameErr != null, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(sets, { sets = it.filter(Char::isDigit).take(2) }, label = { Text("Подходы") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(reps, { reps = it.filter(Char::isDigit).take(3) }, label = { Text("Повторы") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(weight, { weight = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' }.take(6) }, label = { Text("Вес") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Row {
                    TextButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Text("Фото")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        val safeName = name.trim()
                        val s = sets.toIntOrNull() ?: 0
                        val r = reps.toIntOrNull() ?: 0
                        val w = weight.toFloatOrNull() ?: -1f
                        if (safeName.isBlank()) {
                            nameErr = "Название обязательно"
                            return@Button
                        }
                        if (s !in 1..10 || r !in 1..100 || w !in 0f..1000f) return@Button
                        exercises.add(
                            ExerciseEntry(
                                exerciseId = 0L,
                                name = safeName,
                                sets = List(s) { idx ->
                                    ExerciseSetSummary(
                                        order = idx + 1,
                                        weight = w,
                                        reps = r
                                    )
                                },
                                photoUri = photoUri
                            )
                        )
                        name = ""
                        photoUri = null
                    }) { Text("Добавить") }
                }
            }

            itemsIndexed(exercises) { index, exercise ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val bitmap = remember(exercise.photoUri) { loadBitmapFlexible(context, exercise.photoUri) }
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.padding(end = 8.dp).height(40.dp).clip(CircleShape))
                    }
                    val firstSet = exercise.sets.firstOrNull()
                    val setCount = exercise.sets.size
                    Text(
                        "${exercise.name} • ${setCount}x${firstSet?.reps ?: 0} • ${firstSet?.weight ?: 0f}",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { exercises.removeAt(index) }) { Icon(Icons.Default.Delete, null) }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (exercises.isEmpty()) return@Button
                        val now = System.currentTimeMillis()
                        onSave(
                            TrainingSession(
                                sessionId = session?.sessionId ?: 0L,
                                startedAt = session?.startedAt ?: now,
                                finishedAt = session?.finishedAt ?: now,
                                date = date,
                                exercises = exercises.toList()
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Готово") }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showDatePicker) {
        val parts = date.split("-").map { it.toInt() }
        DatePickerDialog(context, { _, y, m, d ->
            date = String.format("%04d-%02d-%02d", y, m + 1, d)
            showDatePicker = false
        }, parts[0], parts[1] - 1, parts[2]).apply {
            setOnDismissListener { showDatePicker = false }
        }.show()
    }
}

@Composable
fun AddTrainingDialog(
    session: TrainingSession? = null,
    availableExercises: List<ExerciseCatalogItem> = emptyList(),
    onSave: (TrainingSession) -> Unit,
    onDismiss: () -> Unit
) {
    // Совместимость со старыми вызовами: теперь используем BottomSheet вместо AlertDialog.
    AddTrainingBottomSheet(session = session, availableExercises = availableExercises, onSave = onSave, onDismiss = onDismiss)
}

fun formatDateForDisplay(date: String): String =
    date.replace(Regex("(\\d{4})-(\\d{2})-(\\d{2})"), "$3.$2.$1")

fun persistImageToInternal(context: Context, source: Uri): String? = try {
    val dir = File(context.filesDir, "exercise_photos").apply { mkdirs() }
    val file = File(dir, "ex_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(source)?.use { input ->
        FileOutputStream(file).use { output -> input.copyTo(output) }
    }
    file.absolutePath
} catch (_: Exception) {
    null
}

fun loadBitmapFlexible(context: Context, pathOrUri: String?): Bitmap? {
    if (pathOrUri.isNullOrBlank()) return null
    return try {
        if (pathOrUri.startsWith("/")) {
            BitmapFactory.decodeFile(pathOrUri)
        } else {
            val uri = Uri.parse(pathOrUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            }
        }
    } catch (_: Exception) {
        null
    }
}
