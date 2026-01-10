package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.viewmodel.NutritionViewModel
import com.example.workouttracker.ui.components.SectionHeader
import com.example.workouttracker.ui.nutrition_analytic.NutritionAnalyticsFullScreen
import com.example.workouttracker.ui.nutrition.FridgeDialog
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
    val recommendedNorm by viewModel.recommendedNorm.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val planMessage by viewModel.planMessage.collectAsState()
    val fridgePrompt by viewModel.fridgeExtraPrompt.collectAsState()
    val todayAnalytics by viewModel.todayAnalytics.collectAsState()
    val weeklyAnalytics by viewModel.weeklyAnalytics.collectAsState()
    val foodRatings by viewModel.foodRatings.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editEntry by remember { mutableStateOf<NutritionEntry?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var replaceMealType by remember { mutableStateOf<MealType?>(null) }
    var replaceComment by remember { mutableStateOf("") }
    var showFridgeChoiceDialog by remember { mutableStateOf(false) }
    var showRegenerateWarning by remember { mutableStateOf(false) }
    var showFridgeDialog by remember { mutableStateOf(false) }
    var showAnalyticsScreen by remember { mutableStateOf(false) }

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()                // ← добавили
    suspend fun snack(msg: String) { snackbarHost.showSnackbar(msg) }

    val grouped = entries.groupBy { it.date }.toSortedMap(compareByDescending { it })
    val mealTypeOrder = listOf(
        MealType.BREAKFAST,
        MealType.LUNCH,
        MealType.DINNER,
        MealType.SNACK,
        MealType.OTHER
    )
    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today = formatter.format(Date())
    val yesterday = remember(today) {
        Calendar.getInstance().apply {
            time = Date()
            add(Calendar.DAY_OF_YEAR, -1)
        }.let { formatter.format(it.time) }
    }
    val todayEntries = grouped[today].orEmpty()
    val todaySummary = viewModel.getDailySummary(today)
    val todayTotal = NutritionEntry(
        date = todaySummary.date,
        name = if (todayEntries.isEmpty()) "" else "Итого",
        calories = todaySummary.calories,
        protein = todaySummary.protein,
        carbs = todaySummary.carbs,
        fats = todaySummary.fats,
        weight = todayEntries.sumOf { it.weight }
    )

    val canReuseYesterdayPlan = mealPlan == null && viewModel.hasCachedPlanForDate(yesterday)

    LaunchedEffect(planMessage) {
        planMessage?.let {
            snack(it)
            viewModel.consumePlanMessage()
        }
    }

    LaunchedEffect(showAnalyticsScreen) {
        if (showAnalyticsScreen) {
            viewModel.computeTodayAnalytics()
            viewModel.computeWeeklyAnalytics()
        }
    }

    Scaffold(
        topBar = {
            SectionHeader(
                title = "Питание",
                titleStyle = MaterialTheme.typography.headlineSmall,
                actions = {
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Filled.Person, contentDescription = "Профиль питания")
                    }
                    IconButton(onClick = { showAnalyticsScreen = true }) {
                        Icon(Icons.Filled.QueryStats, contentDescription = "Аналитика питания")
                    }
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
                    onGenerateClick = {
                        val planExists = mealPlan != null || viewModel.hasCachedPlanForDate(today)
                        if (planExists) {
                            showRegenerateWarning = true
                        } else {
                            showFridgeChoiceDialog = true
                        }
                    },
                    onDismissError = { viewModel.clearPlanError() },
                    onReplaceMeal = { type ->
                        replaceMealType = type
                        replaceComment = ""
                        showReplaceDialog = true
                    },
                    canReuseYesterdayPlan = canReuseYesterdayPlan,
                    onReuseYesterdayClick = { viewModel.reuseYesterdayPlan() }
                )
            }

            grouped.forEach { (date, _) ->
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

                val byMealType = viewModel.getEntriesByDate(date)

                mealTypeOrder.forEach { type ->
                    val entriesForType = byMealType[type].orEmpty()
                    if (entriesForType.isNotEmpty()) {
                        item { MealTypeHeader(type) }

                        items(entriesForType, key = { it.id }) { entry ->
                            NutritionEntryCard(
                                entry = entry,
                                onEdit = { editEntry = entry },
                                onDelete = {
                                    viewModel.removeEntry(entry.id)
                                    scope.launch { snack("Удалено: ${entry.name}") }   // ← заменили LaunchedEffect
                                }
                            )
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
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

    if (showAnalyticsScreen) {
        Dialog(
            onDismissRequest = { showAnalyticsScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BackHandler { showAnalyticsScreen = false }
                NutritionAnalyticsFullScreen(
                    todayTotal = todayTotal,
                    norm = viewModel.dailyNorm,
                    todayAnalytics = todayAnalytics,
                    weeklyAnalytics = weeklyAnalytics,
                    foodRatings = foodRatings,
                    onRefreshToday = { viewModel.computeTodayAnalytics() },
                    onRefreshWeekly = { viewModel.computeWeeklyAnalytics() },
                    onClose = { showAnalyticsScreen = false }
                )
            }
    }

    if (showRegenerateWarning) {
        AlertDialog(
            onDismissRequest = { showRegenerateWarning = false },
            title = { Text("Обновить план") },
            text = { Text("Текущий план будет удалён. Продолжить генерацию заново?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRegenerateWarning = false
                        viewModel.resetTodayPlan()
                        showFridgeChoiceDialog = true
                    },
                    enabled = !isPlanLoading
                ) {
                    Text("Продолжить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateWarning = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showFridgeChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showFridgeChoiceDialog = false },
            title = { Text("Сформировать план") },
            text = { Text("Хотите использовать продукты из холодильника?") },
            confirmButton = {
                TextButton(onClick = {
                    showFridgeChoiceDialog = false
                    showFridgeDialog = true
                }) {
                    Text("Да")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFridgeChoiceDialog = false
                    viewModel.generateTodayPlan()
                }) {
                    Text("Нет")
                }
            }
        )
    }

    if (showFridgeDialog) {
        FridgeDialog(
            onConfirm = { fridge, allowExtra ->
                showFridgeDialog = false
                viewModel.generatePlanFromFridge(fridge, allowExtra)
            },
            onDismiss = { showFridgeDialog = false }
        )
    }

    fridgePrompt?.let {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Недостаточно продуктов") },
            text = {
                Text("Эти продукты не покрывают вашу дневную норму. Разрешить использование дополнительных продуктов?")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.allowExtraProductsForFridgePlan() }) { Text("Да") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.continueWithoutExtraProducts() }) { Text("Нет") }
            }
        )
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
            recommendedNorm = recommendedNorm,
            onSave = { norm ->
                viewModel.updateNorm(norm)
                showSettings = false
                scope.launch { snack("Нормы сохранены") }               // ← заменили LaunchedEffect
            },
            onDismiss = { showSettings = false }
        )
    }

    if (showReplaceDialog && replaceMealType != null) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("Замена приёма пищи") },
            text = {
                OutlinedTextField(
                    value = replaceComment,
                    onValueChange = { replaceComment = it },
                    label = { Text("Комментарий (необязательно)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.replaceMeal(
                            replaceMealType!!,
                            replaceComment.trim().ifBlank { null }
                        )
                        showReplaceDialog = false
                    },
                    enabled = !isPlanLoading
                ) {
                    Text("Заменить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showProfileDialog) {
        ProfileDialog(
            currentProfile = profile,
            onSave = { newProfile ->
                viewModel.updateProfile(newProfile)
            },
            onSyncFromMainProfile = {
                viewModel.syncFromMainProfile()
            },
            onDismiss = { showProfileDialog = false }
        )
    }
}


/* ===== Вспомогательные UI ===== */

@Composable
fun MealTypeHeader(type: MealType) {
    Text(
        text = type.displayName(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

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
    onDismissError: () -> Unit,
    onReplaceMeal: (MealType) -> Unit,
    canReuseYesterdayPlan: Boolean,
    onReuseYesterdayClick: () -> Unit
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
                                text = meal.type.displayName(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(Modifier.height(4.dp))
                            meal.items.forEach { item ->
                                Text(
                                    text = "${item.name} — ${item.grams} г, ${item.calories} ккал (Б:${item.protein} Ж:${item.fat} У:${item.carbs})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { onReplaceMeal(meal.type) },
                                    enabled = !isLoading
                                ) {
                                    Text("Заменить")
                                }
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
                if (canReuseYesterdayPlan) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onReuseYesterdayClick, enabled = !isLoading) {
                        Text("Использовать вчерашний план")
                    }
                }
            }
        }
    }
}
