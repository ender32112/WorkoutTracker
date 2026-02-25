package com.example.workouttracker.ui.training

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "История тренировок",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            items(sessions, key = { it.sessionId }) { session ->
                val expanded = expandedState[session.sessionId] ?: false
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(session.date, fontWeight = FontWeight.Bold)
                                Text("Объём: ${session.totalVolume.toInt()}")
                            }
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
                                        Text(exercise.name, fontWeight = FontWeight.Medium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            exercise.muscles.take(3).forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                                        }
                                        Text(exercise.sets.joinToString(" • ") { "${it.order}) ${it.weight}кг × ${it.reps}" })
                                    }
                                    exercise.pr?.let {
                                        Text("PR ${it.bestVolumeSet.toInt()}")
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
            item { androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(86.dp)) }
        }

        Button(
            onClick = onStartWorkout,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Text(" Начать тренировку")
        }
    }
}
