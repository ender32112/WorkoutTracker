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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(templateTitle, { templateTitle = it }, label = { Text("Новый шаблон") }, modifier = Modifier.weight(1f))
            Button(onClick = {
                onCreateTemplate(templateTitle)
                templateTitle = ""
            }) { Text("Создать") }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(templates, key = { it.id }) { template ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(template.title)
                        Text(template.exercises.joinToString { it.name })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { selectedTemplateId = template.id }) { Text("Редактировать") }
                            Button(onClick = { onStartFromTemplate(template.id) }) { Text("Start from template") }
                        }

                        if (selectedTemplateId == template.id) {
                            exercises.take(10).forEachIndexed { index, exercise ->
                                TextButton(onClick = {
                                    onAddExerciseToTemplate(template.id, exercise.id, 3, 10, template.exercises.size + index + 1)
                                }) { Text("+ ${exercise.name}") }
                            }
                            template.exercises.forEach { item ->
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
