package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.ui.components.SectionHeader
import com.example.workouttracker.viewmodel.NutritionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanScreen(
    viewModel: NutritionViewModel = viewModel()
) {
    val mealPlan by viewModel.mealPlan.collectAsState()
    val isPlanLoading by viewModel.isPlanLoading.collectAsState()
    val planError by viewModel.planError.collectAsState()
    val planMessage by viewModel.planMessage.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val recommendedNorm by viewModel.recommendedNorm.collectAsState()
    val fridgePrompt by viewModel.fridgeExtraPrompt.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var replaceMealType by remember { mutableStateOf<MealType?>(null) }
    var replaceComment by remember { mutableStateOf("") }
    var showFridgeChoiceDialog by remember { mutableStateOf(false) }
    var showRegenerateWarning by remember { mutableStateOf(false) }
    var showFridgeDialog by remember { mutableStateOf(false) }
    var showFridgeManagerDialog by remember { mutableStateOf(false) }
    var showMealPlanActionsDialog by remember { mutableStateOf(false) }

    val snackbarHost = remember { SnackbarHostState() }
    suspend fun snack(msg: String) { snackbarHost.showSnackbar(msg) }

    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today = formatter.format(Date())
    val yesterday = remember(today) {
        Calendar.getInstance().apply {
            time = Date()
            add(Calendar.DAY_OF_YEAR, -1)
        }.let { formatter.format(it.time) }
    }
    val canReuseYesterdayPlan = mealPlan == null && viewModel.hasCachedPlanForDate(yesterday)

    LaunchedEffect(planMessage) {
        planMessage?.let {
            snack(it)
            viewModel.consumePlanMessage()
        }
    }

    Scaffold(
        topBar = {
            SectionHeader(
                title = "План питания",
                titleStyle = MaterialTheme.typography.headlineSmall,
                actions = {
                    androidx.compose.material3.IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Filled.Person, contentDescription = "Профиль питания")
                    }
                    androidx.compose.material3.IconButton(onClick = { showFridgeManagerDialog = true }) {
                        Icon(Icons.Filled.Kitchen, contentDescription = "Холодильник")
                    }
                    androidx.compose.material3.IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    MealPlanCard(
                        mealPlan = mealPlan,
                        isLoading = isPlanLoading,
                        error = planError,
                        onDismissError = { viewModel.clearPlanError() },
                        onReplaceMeal = { type ->
                            replaceMealType = type
                            replaceComment = ""
                            showReplaceDialog = true
                        }
                    )
                }
            }

            FloatingActionButton(
                onClick = { showMealPlanActionsDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Управление планом питания")
            }
        }
    }

    if (showMealPlanActionsDialog) {
        MealPlanActionsDialog(
            mealPlan = mealPlan,
            isLoading = isPlanLoading,
            canReuseYesterdayPlan = canReuseYesterdayPlan,
            onDismiss = { showMealPlanActionsDialog = false },
            onGenerateOrUpdate = {
                showMealPlanActionsDialog = false
                val planExists = mealPlan != null || viewModel.hasCachedPlanForDate(today)
                if (planExists) {
                    showRegenerateWarning = true
                } else {
                    showFridgeChoiceDialog = true
                }
            },
            onReuseYesterday = {
                showMealPlanActionsDialog = false
                viewModel.reuseYesterdayPlan()
            }
        )
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

    if (showReplaceDialog && replaceMealType != null) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("Замена приёма пищи") },
            text = {
                androidx.compose.material3.OutlinedTextField(
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
            onSave = { newProfile -> viewModel.updateProfile(newProfile) },
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showSettings) {
        SettingsDialog(
            currentNorm = viewModel.dailyNorm,
            recommendedNorm = recommendedNorm,
            onSave = { norm ->
                viewModel.updateNorm(norm)
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )
    }

    if (showFridgeManagerDialog) {
        FridgeManagerDialog(viewModel = viewModel, onDismiss = { showFridgeManagerDialog = false })
    }
}

@Composable
private fun MealPlanActionsDialog(
    mealPlan: MealPlan?,
    isLoading: Boolean,
    canReuseYesterdayPlan: Boolean,
    onDismiss: () -> Unit,
    onGenerateOrUpdate: () -> Unit,
    onReuseYesterday: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("План питания") },
        text = {
            Text(
                if (mealPlan == null) "Сгенерировать план на сегодня?"
                else "План уже есть. Хотите обновить его?"
            )
        },
        confirmButton = {
            TextButton(onClick = onGenerateOrUpdate, enabled = !isLoading) {
                Text(if (mealPlan == null) "Сгенерировать" else "Обновить")
            }
        },
        dismissButton = {
            if (canReuseYesterdayPlan) {
                TextButton(onClick = onReuseYesterday, enabled = !isLoading) {
                    Text("Взять вчерашний")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}
