package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.viewmodel.NutritionViewModel
import com.example.workouttracker.ui.components.SectionHeader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<NutritionEntry?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    val grouped = entries.groupBy { it.date }.toSortedMap(compareByDescending { it })
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayTotal = grouped[today]?.fold(
        NutritionEntry(date = today, name = "", calories = 0, protein = 0, carbs = 0, fats = 0, weight = 0)
    ) { acc, e ->
        acc.copy(
            calories = acc.calories + e.calories,
            protein = acc.protein + e.protein,
            carbs = acc.carbs + e.carbs,
            fats = acc.fats + e.fats,
            weight = acc.weight + e.weight
        )
    } ?: NutritionEntry(date = today, name = "", calories = 0, protein = 0, carbs = 0, fats = 0, weight = 0)

    Scaffold(
        topBar = {
            // ЕДИНАЯ ШАПКА
            SectionHeader(
                title = "Питание",
                titleStyle = MaterialTheme.typography.headlineSmall,
                // height = 48.dp,
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { TodayCard(todayTotal, viewModel.dailyNorm) }

            grouped.forEach { (date, list) ->
                stickyHeader {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatDateForDisplay(date),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                items(list, key = { it.id }) { entry ->
                    NutritionEntryCard(
                        entry = entry,
                        onEdit = { editEntry = entry },
                        onDelete = { viewModel.removeEntry(entry.id) }
                    )
                }
            }

            if (grouped.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет записей", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddNutritionDialog(
            onConfirm = { newEntry ->
                viewModel.addEntry(newEntry)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editEntry?.let { entry ->
        AddNutritionDialog(
            entry = entry,
            onConfirm = { updated ->
                viewModel.updateEntry(updated)
                editEntry = null
            },
            onDismiss = { editEntry = null }
        )
    }

    if (showSettings) {
        SettingsDialog(
            currentNorm = viewModel.dailyNorm,
            onSave = { norm ->
                viewModel.updateNorm(norm)
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )
    }
}

// ——— Вспомогательные UI ———

@Composable
fun TodayCard(todayTotal: NutritionEntry, norm: Map<String, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Сегодня", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            MacroProgress("Калории", todayTotal.calories, norm["calories"]!!)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                MacroText("Б", todayTotal.protein, norm["protein"]!!)
                MacroText("Ж", todayTotal.fats, norm["fats"]!!)
                MacroText("У", todayTotal.carbs, norm["carbs"]!!)
            }
        }
    }
}

@Composable
fun MacroProgress(label: String, value: Int, norm: Int) {
    val progress = (value.coerceAtMost(norm).toFloat() / norm).coerceIn(0f, 1f)
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("$value / $norm", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun MacroText(label: String, value: Int, norm: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text("$value г", style = MaterialTheme.typography.bodyMedium)
        Text("/$norm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NutritionEntryCard(entry: NutritionEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text("${entry.calories} ккал • ${entry.weight} г", style = MaterialTheme.typography.bodyMedium)
                Text("Б:${entry.protein}  Ж:${entry.fats}  У:${entry.carbs}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Редактировать") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Удалить") }
            }
        }
    }
}
