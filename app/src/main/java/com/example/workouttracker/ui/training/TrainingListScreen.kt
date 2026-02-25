package com.example.workouttracker.ui.training

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@Composable
fun TrainingListScreen(
    sessions: List<TrainingSession>,
    onStartWorkout: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenProgress: () -> Unit,
    onRepeat: (Long) -> Unit,
    onExport: (Long) -> Unit
) {
    val expandedState = remember { mutableStateMapOf<Long, Boolean>() }

    LazyColumn(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStartWorkout, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text(" Начать тренировку")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ElevatedAssistChip(onClick = onOpenCatalog, label = { Text("Каталог упражнений") }, leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAddCheck, contentDescription = null)
                        })
                        ElevatedAssistChip(onClick = onOpenTemplates, label = { Text("Шаблоны") })
                        ElevatedAssistChip(onClick = onOpenProgress, label = { Text("Статистика") })
                    }
                }
            }
        }

        items(sessions, key = { it.sessionId }) { session ->
            val expanded = expandedState[session.sessionId] ?: false
            Card {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(session.date)
                        Text("Объём: ${session.totalVolume.toInt()}")
                        IconButton(onClick = { expandedState[session.sessionId] = !expanded }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.rotate(if (expanded) 90f else 0f))
                        }
                    }
                    if (expanded) {
                        session.exercises.forEach { exercise ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                if (exercise.photoUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(exercise.photoUri),
                                        contentDescription = exercise.name,
                                        modifier = Modifier.size(52.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exercise.name)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        exercise.muscles.take(3).forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                                    }
                                    Text(exercise.sets.joinToString(", ") { "${it.order}) ${it.weight}kg × ${it.reps}" })
                                }
                                exercise.pr?.let {
                                    Text("PR: ${it.bestVolumeSet.toInt()} / ${"%.1f".format(it.bestE1rm)}")
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onRepeat(session.sessionId) }) { Text("Повторить") }
                            TextButton(onClick = { onExport(session.sessionId) }) { Text("Экспорт") }
                        }
                    }
                }
            }
        }
    }
}
