package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()                // ← добавили
    suspend fun snack(msg: String) { snackbarHost.showSnackbar(msg) }

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
            SectionHeader(
                title = "Питание",
                titleStyle = MaterialTheme.typography.headlineSmall,
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
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
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
                    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = formatDateForDisplay(date),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                items(list, key = { it.id }) { entry ->
                    NutritionEntryCard(
                        entry = entry,
                        onEdit = { editEntry = entry },
                        onDelete = {
                            viewModel.removeEntry(entry.id)
                            scope.launch { snack("Удалено: ${entry.name}") }   // ← заменили LaunchedEffect
                        }
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
                scope.launch { snack("Добавлено: ${newEntry.name}") }   // ← заменили LaunchedEffect
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
                scope.launch { snack("Обновлено: ${updated.name}") }    // ← заменили LaunchedEffect
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
                scope.launch { snack("Нормы сохранены") }               // ← заменили LaunchedEffect
            },
            onDismiss = { showSettings = false }
        )
    }
}


/* ===== Вспомогательные UI ===== */

@Composable
fun TodayCard(todayTotal: NutritionEntry, norm: Map<String, Int>) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                "Сегодня",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(10.dp))
            MacroProgress("Калории", todayTotal.calories, norm["calories"] ?: 2500)
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MacroChip(
                    label = "Б",
                    value = todayTotal.protein,
                    norm = norm["protein"] ?: 120,
                    color = Color(0xFF66BB6A),
                    modifier = Modifier.weight(1f)
                )
                MacroChip(
                    label = "Ж",
                    value = todayTotal.fats,
                    norm = norm["fats"] ?: 80,
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.weight(1f)
                )
                MacroChip(
                    label = "У",
                    value = todayTotal.carbs,
                    norm = norm["carbs"] ?: 300,
                    color = Color(0xFF64B5F6),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MacroProgress(label: String, value: Int, norm: Int) {
    val p = (value.toFloat() / norm.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(p)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$value / $norm", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun MacroChip(
    label: String,
    value: Int,
    norm: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp) // компактнее, чтобы уместилось
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(4.dp))

            // ВОТ ЗДЕСЬ: компактная строка, норма мельче — вся строка помещается
            val text = buildAnnotatedString {
                append("$label:$value/")
                withStyle(
                    SpanStyle(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    append(norm.toString())
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                maxLines = 1,
                overflow = TextOverflow.Clip, // не обрезаем середину на маленьких экранах
                softWrap = false
            )
        }
    }
}


@Composable
fun NutritionEntryCard(entry: NutritionEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
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
