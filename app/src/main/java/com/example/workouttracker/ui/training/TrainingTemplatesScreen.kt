package com.example.workouttracker.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp

@Composable
fun TrainingTemplatesScreen(
    templates: List<WorkoutTemplateUi>,
    exercises: List<ExerciseCatalogItem>,
    onCreateTemplate: (String) -> Unit,
    onAddExerciseToTemplate: (Long, Long, Int, Int, Int) -> Unit,
    onRemoveExerciseFromTemplate: (Long) -> Unit,
    onStartFromTemplate: (Long) -> Unit
) {
    var templateTitle by remember { mutableStateOf("") }
    var selectedTemplateId by remember { mutableLongStateOf(0L) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(10.dp)) {
                OutlinedTextField(templateTitle, { templateTitle = it }, label = { Text("Новый шаблон") }, modifier = Modifier.weight(1f))
                Button(onClick = {
                    onCreateTemplate(templateTitle)
                    templateTitle = ""
                }) { Text("Создать") }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates, key = { it.id }) { template ->
                val availableExercises = exercises
                    .filterNot { catalog -> template.exercises.any { it.exerciseId == catalog.id } }
                    .take(15)

                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(template.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Упражнений: ${template.exercises.size}")

                        Text(template.exercises.take(6).joinToString(prefix = "Состав: ") { it.name }, style = MaterialTheme.typography.bodySmall)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                selectedTemplateId = if (selectedTemplateId == template.id) 0L else template.id
                            }) {
                                Text(if (selectedTemplateId == template.id) "Свернуть" else "Редактировать")
                            }
                            Button(onClick = { onStartFromTemplate(template.id) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Text(" Запустить")
                            }
                        }

                        if (selectedTemplateId == template.id) {
                            Text("Добавить упражнения", fontWeight = FontWeight.SemiBold)
                            availableExercises.forEach { exercise ->
                                val defaults = smartDefaultsByMuscle(exercise.muscles)
                                TextButton(onClick = {
                                    val nextOrder = (template.exercises.maxOfOrNull { it.orderInTemplate } ?: 0) + 1
                                    onAddExerciseToTemplate(template.id, exercise.id, defaults.first, defaults.second, nextOrder)
                                }) {
                                    Text("+ ${exercise.name} (${defaults.first}x${defaults.second})")
                                }
                            }
                            template.exercises.sortedBy { it.orderInTemplate }.forEach { item ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("#${item.orderInTemplate}: ${item.name} ${item.defaultSets}x${item.defaultReps}")
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
}

private fun smartDefaultsByMuscle(muscles: List<String>): Pair<Int, Int> {
    val normalized = muscles.joinToString(" ").lowercase()
    return when {
        normalized.contains("пресс") || normalized.contains("икр") -> 4 to 15
        normalized.contains("ягод") || normalized.contains("квад") || normalized.contains("бицепс бедра") -> 4 to 10
        else -> 3 to 12
    }
}
