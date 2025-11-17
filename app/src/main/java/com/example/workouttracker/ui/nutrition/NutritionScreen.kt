package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val mealPlan by viewModel.mealPlan.collectAsState()
    val isPlanLoading by viewModel.isPlanLoading.collectAsState()
    val planError by viewModel.planError.collectAsState()

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
            item {
                MealPlanCard(
                    mealPlan = mealPlan,
                    isLoading = isPlanLoading,
                    error = planError,
                    onGenerateClick = { viewModel.generatePlanForToday() },
                    onDismissError = { viewModel.clearPlanError() }
                )
            }

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


@Composable
fun MealPlanCard(
    mealPlan: MealPlan?,
    isLoading: Boolean,
    error: String?,
    onGenerateClick: () -> Unit,
    onDismissError: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "План питания",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Button(
                    onClick = onGenerateClick,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Генерация...")
                    } else {
                        Text(if (mealPlan == null) "Сгенерировать" else "Обновить")
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissError) {
                            Text("Ок", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            if (mealPlan != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Цель: ${mealPlan.targetCalories} ккал • Б:${mealPlan.targetProtein} Ж:${mealPlan.targetFat} У:${mealPlan.targetCarbs}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))

                mealPlan.meals.forEach { meal ->
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        tonalElevation = 1.dp,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(
                                text = when (meal.type) {
                                    MealType.BREAKFAST -> "Завтрак"
                                    MealType.LUNCH -> "Обед"
                                    MealType.DINNER -> "Ужин"
                                    MealType.SNACK -> "Перекус"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(Modifier.height(4.dp))
                            meal.items.forEach { item ->
                                Text(
                                    text = "${item.name} — ${item.grams} г, ${item.calories} ккал (Б:${item.protein} Ж:${item.fat} У:${item.carbs})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            } else if (!isLoading && error == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "План на сегодня ещё не создан.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

