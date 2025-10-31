package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.R
import com.example.workouttracker.viewmodel.NutritionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Подсчёт итогов
    val total = entries.fold(
        NutritionEntry(
            date = "",
            calories = 0,
            protein = 0,
            carbs = 0,
            fats = 0
        )
    ) { acc, e ->
        NutritionEntry(
            date = "",
            calories = acc.calories + e.calories,
            protein = acc.protein + e.protein,
            carbs = acc.carbs + e.carbs,
            fats = acc.fats + e.fats
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Питание") }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(painterResource(R.drawable.ic_add), "Добавить") },
                text = { Text("Запись") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Карточка с нормой
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Сегодня", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("${total.calories} / ${viewModel.dailyNorm["calories"]} ккал")
                    LinearProgressIndicator(
                        progress = {
                            val norm = viewModel.dailyNorm["calories"]!!
                            total.calories.coerceAtMost(norm).toFloat() / norm
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Б: ${total.protein}г")
                        Text("Ж: ${total.fats}г")
                        Text("У: ${total.carbs}г")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Список записей
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет записей", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(entry.date, style = MaterialTheme.typography.titleMedium)
                                    Text("${entry.calories} ккал • Б:${entry.protein} Ж:${entry.fats} У:${entry.carbs}")
                                }
                                IconButton(onClick = { viewModel.removeEntry(entry.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Диалог
        if (showDialog) {
            AddNutritionDialog(
                onConfirm = {
                    viewModel.addEntry(it)
                    showDialog = false
                },
                onDismiss = { showDialog = false }
            )
        }
    }
}