package com.example.workouttracker.feature.training.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingTemplatesScreen(
    templates: List<WorkoutTemplateUi>,
    exercises: List<ExerciseCatalogItem>,
    onCreateTemplate: (String) -> Unit,
    onAddExerciseToTemplate: (Long, String, Int, Int, Int) -> Unit,
    onRemoveExerciseFromTemplate: (Long) -> Unit,
    onStartFromTemplate: (Long) -> Unit
) {
    var templateTitle by remember { mutableStateOf("") }
    var expandedTemplateId by remember { mutableLongStateOf(0L) }
    var pickerTemplateId by remember { mutableLongStateOf(0L) }
    var pickerQuery by remember { mutableStateOf("") }

    val pickerTemplate = templates.firstOrNull { it.id == pickerTemplateId }
    val availableExercises = remember(pickerTemplateId, pickerQuery, templates, exercises) {
        val template = templates.firstOrNull { it.id == pickerTemplateId }
        val usedIds = template?.exercises?.map { it.exerciseId }.orEmpty().toSet()
        exercises
            .filterNot { it.id in usedIds }
            .filter {
                pickerQuery.isBlank() ||
                    it.name.contains(pickerQuery, true) ||
                    it.nameEn.contains(pickerQuery, true) ||
                    it.muscles.any { muscle -> muscle.contains(pickerQuery, true) }
            }
            .take(40)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Шаблоны тренировок",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Сохраняйте рабочие связки упражнений и запускайте их как готовую тренировку без длинного ручного скролла.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = templateTitle,
                        onValueChange = { templateTitle = it },
                        label = { Text("Новый шаблон") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            onCreateTemplate(templateTitle.trim())
                            templateTitle = ""
                        },
                        enabled = templateTitle.isNotBlank()
                    ) {
                        Text("Создать")
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(templates, key = { it.id }) { template ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            template.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Упражнений: ${template.exercises.size}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val preview = template.exercises
                            .sortedBy { it.orderInTemplate }
                            .take(3)
                            .joinToString(" • ") { it.name }
                        Text(
                            preview.ifBlank { "Пока без упражнений" },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    expandedTemplateId = if (expandedTemplateId == template.id) 0L else template.id
                                }
                            ) {
                                Text(if (expandedTemplateId == template.id) "Свернуть" else "Управлять")
                            }
                            Button(onClick = { onStartFromTemplate(template.id) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Text("Запустить")
                            }
                        }

                        if (expandedTemplateId == template.id) {
                            TextButton(
                                onClick = {
                                    pickerTemplateId = template.id
                                    pickerQuery = ""
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Добавить упражнение")
                            }
                            template.exercises
                                .sortedBy { it.orderInTemplate }
                                .forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "${item.orderInTemplate}. ${item.name}",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "${item.defaultSets} × ${item.defaultReps}" +
                                                    (item.equipment?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            if (item.isMissing) {
                                                Text(
                                                    "Упражнение больше не найдено в каталоге. Лучше заменить его перед запуском.",
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                        IconButton(onClick = { onRemoveExerciseFromTemplate(item.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    if (pickerTemplate != null) {
        ModalBottomSheet(onDismissRequest = { pickerTemplateId = 0L }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Добавить упражнение в «${pickerTemplate.title}»",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = pickerQuery,
                    onValueChange = { pickerQuery = it },
                    label = { Text("Поиск упражнения") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableExercises, key = { it.id }) { exercise ->
                        Button(
                            onClick = {
                                val defaults = smartDefaultsByMuscle(exercise.muscles)
                                val nextOrder = (pickerTemplate.exercises.maxOfOrNull { it.orderInTemplate } ?: 0) + 1
                                onAddExerciseToTemplate(
                                    pickerTemplate.id,
                                    exercise.id,
                                    defaults.first,
                                    defaults.second,
                                    nextOrder
                                )
                                pickerTemplateId = 0L
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(exercise.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${defaultsLabel(exercise.muscles)} • ${exercise.muscles.take(2).joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun smartDefaultsByMuscle(muscles: List<String>): Pair<Int, Int> {
    val normalized = muscles.joinToString(" ").lowercase()
    return when {
        normalized.contains("пресс") || normalized.contains("икры") -> 4 to 15
        normalized.contains("ягод") || normalized.contains("квад") || normalized.contains("бед") -> 4 to 10
        else -> 3 to 12
    }
}

private fun defaultsLabel(muscles: List<String>): String {
    val (sets, reps) = smartDefaultsByMuscle(muscles)
    return "Старт: ${sets}x$reps"
}
