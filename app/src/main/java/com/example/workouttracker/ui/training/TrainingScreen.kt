package com.example.workouttracker.ui.training

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.viewmodel.TrainingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    trainingViewModel: TrainingViewModel
) {

    val sessions by trainingViewModel.sessions.collectAsState()


    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить тренировку")
            }
        }
    ) { innerPadding ->

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Пока нет тренировок")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(sessions) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    "Дата: ${session.date}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                session.exercises.forEach { ex ->
                                    Text(
                                        "• ${ex.name}: ${ex.sets}×${ex.reps}",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            IconButton(onClick = {
                                trainingViewModel.removeSession(session.id.toString())
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить тренировку"
                                )
                            }
                        }
                    }
                }
            }
        }


        if (showDialog) {
            AddTrainingDialog(
                onSave = {
                    trainingViewModel.addSession(it)
                    showDialog = false
                },
                onDismiss = {
                    showDialog = false
                }
            )
        }
    }
}
