package com.example.workouttracker.ui.training

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.viewmodel.TrainingViewModel
import com.example.workouttracker.ui.components.SectionHeader
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    trainingViewModel: TrainingViewModel = viewModel()
) {
    val sessions by trainingViewModel.sessions.collectAsState()
    val catalog by trainingViewModel.exerciseCatalog.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<TrainingSession?>(null) }
    var showCatalog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // ЕДИНАЯ ШАПКА
            SectionHeader(
                title = "Тренировки",
                titleStyle = MaterialTheme.typography.headlineSmall,
                // subtitle = "История и план",               // при желании
                // height = 48.dp,                            // локальная настройка (иначе — глобальная)
                actions = {
                    FilledTonalButton(
                        onClick = { showCatalog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Упражнения", maxLines = 1, softWrap = false)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("Нет тренировок", style = MaterialTheme.typography.bodyLarge) }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sessions.reversed()) { session ->
                        TrainingCard(
                            session = session,
                            onEdit = { editingSession = session },
                            onDelete = { trainingViewModel.removeSession(session.id.toString()) }
                        )
                    }
                }
            }

            if (showAddDialog || editingSession != null) {
                AddTrainingDialog(
                    session = editingSession,
                    availableExercises = catalog,
                    onSave = { newSession ->
                        if (editingSession != null) trainingViewModel.updateSession(newSession)
                        else trainingViewModel.addSession(newSession)
                        showAddDialog = false
                        editingSession = null
                    },
                    onDismiss = {
                        showAddDialog = false
                        editingSession = null
                    }
                )
            }

            if (showCatalog) {
                ExerciseCatalogDialog(
                    items = catalog,
                    onAdd = { name, photoUri -> trainingViewModel.addCatalogItem(name, photoUri) },
                    onUpdate = { trainingViewModel.updateCatalogItem(it) },
                    onDelete = { trainingViewModel.removeCatalogItem(it) },
                    onDismiss = { showCatalog = false }
                )
            }
        }
    }
}

/* ---------- ВСПОМОГАТЕЛЬНОЕ: сохранение фото в app-internal и декодер ---------- */

private fun persistImageToInternal(context: android.content.Context, source: Uri): String? {
    return try {
        val dir = File(context.filesDir, "exercise_photos").apply { mkdirs() }
        val ext = when (context.contentResolver.getType(source)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val file = File(dir, "${UUID.randomUUID()}.$ext")
        context.contentResolver.openInputStream(source)?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        file.toURI().toString()
    } catch (_: Throwable) {
        null
    }
}

private fun loadBitmapFlexible(context: android.content.Context, uriString: String?): Bitmap? {
    if (uriString.isNullOrBlank()) return null
    return try {
        val uri = Uri.parse(uriString)
        when (uri.scheme) {
            "file" -> BitmapFactory.decodeFile(uri.path)
            else -> context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (_: Throwable) { null }
}

/* ----------------------- Диалог каталога упражнений ----------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseCatalogDialog(
    items: List<ExerciseCatalogItem>,
    onAdd: (String, String?) -> Unit,
    onUpdate: (ExerciseCatalogItem) -> Unit,
    onDelete: (UUID) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var newName by remember { mutableStateOf("") }
    var newPhotoUri by remember { mutableStateOf<String?>(null) }

    val pickNewPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        newPhotoUri = uri?.let { persistImageToInternal(context, it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Список упражнений") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp, max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Название нового упражнения") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                pickNewPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Фото", maxLines = 1, softWrap = false)
                        }
                        val bmpNew = remember(newPhotoUri) { loadBitmapFlexible(context, newPhotoUri) }
                        if (bmpNew != null) {
                            Image(bitmap = bmpNew.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                val name = newName.trim()
                                if (name.isNotEmpty()) {
                                    onAdd(name, newPhotoUri)
                                    newName = ""
                                    newPhotoUri = null
                                }
                            },
                            modifier = Modifier.height(40.dp)
                        ) { Text("Добавить", maxLines = 1, softWrap = false) }
                    }
                }

                Divider()

                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        var edit by remember(item.id) { mutableStateOf(false) }
                        var name by remember(item.id) { mutableStateOf(item.name) }
                        var photoUri by remember(item.id) { mutableStateOf(item.photoUri) }

                        val pickEditPhoto = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.PickVisualMedia()
                        ) { uri ->
                            photoUri = uri?.let { persistImageToInternal(context, it) } ?: photoUri
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (!edit) item.name else "Редактирование",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Row {
                                        IconButton(onClick = { edit = !edit }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                                        }
                                        IconButton(onClick = { onDelete(item.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                if (!edit) {
                                    val bmp = remember(photoUri) { loadBitmapFlexible(context, photoUri) }
                                    if (bmp != null) {
                                        Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(64.dp))
                                    } else {
                                        Text("Фото не добавлено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("Название") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                pickEditPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            },
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Фото", maxLines = 1, softWrap = false)
                                        }
                                        val bmp = remember(photoUri) { loadBitmapFlexible(context, photoUri) }
                                        if (bmp != null) {
                                            Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(48.dp))
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { edit = false },
                                            modifier = Modifier.height(40.dp)
                                        ) { Text("Отмена", maxLines = 1, softWrap = false) }
                                        TextButton(
                                            onClick = {
                                                val newName = name.trim().ifBlank { item.name }
                                                onUpdate(ExerciseCatalogItem(id = item.id, name = newName, photoUri = photoUri))
                                                edit = false
                                            },
                                            modifier = Modifier.height(40.dp)
                                        ) { Text("Сохранить", maxLines = 1, softWrap = false) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
        dismissButton = {}
    )
}

/* ----------------------- Карточка тренировки ----------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingCard(
    session: TrainingSession,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = formatDateForDisplay(session.date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${session.exercises.size} упр. • ${formatVolume(session.totalVolume)} кг",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Редактировать") }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            AnimatedVisibility(expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    session.exercises.forEach { ex ->
                        Text(
                            text = "• ${ex.name}: ${ex.sets}×${ex.reps} @ ${ex.weight} кг",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatVolume(v: Number): String =
    String.format(Locale.getDefault(), "%.1f", v.toDouble())
