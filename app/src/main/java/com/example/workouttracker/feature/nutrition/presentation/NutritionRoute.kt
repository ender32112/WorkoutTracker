package com.example.workouttracker.feature.nutrition.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workouttracker.feature.nutrition.presentation.NutritionViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NutritionRoute(
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val planMessage by viewModel.planMessage.collectAsState()
    val savedProducts by viewModel.savedProducts.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var dialogState by remember { mutableStateOf(NutritionDialogState()) }

    val formatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val today = formatter.format(Date())
    val yesterday = remember(today) {
        Calendar.getInstance().apply {
            time = Date()
            add(Calendar.DAY_OF_YEAR, -1)
        }.let { formatter.format(it.time) }
    }
    val canReuseYesterdayPlan = uiState.mealPlan == null && viewModel.hasCachedPlanForDate(yesterday)
    val mealTypeOrder = remember {
        listOf(
            MealType.BREAKFAST,
            MealType.LUNCH,
            MealType.DINNER,
            MealType.SNACK,
            MealType.OTHER
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is NutritionUiEffect.Snackbar -> snackbarHost.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(planMessage) {
        planMessage?.let {
            snackbarHost.showSnackbar(it)
            viewModel.consumePlanMessage()
        }
    }

    NutritionContent(
        groupedEntries = uiState.groupedEntries,
        mealTypeOrder = mealTypeOrder,
        todayTotal = uiState.todayTotal,
        dailyNorm = uiState.dailyNorm,
        onOpenProfile = { dialogState = dialogState.copy(showProfileDialog = true) },
        onOpenFridge = { dialogState = dialogState.copy(showFridgeManagerDialog = true) },
        onOpenSettings = { dialogState = dialogState.copy(showSettings = true) },
        onOpenSavedProducts = {
            viewModel.loadSavedProducts()
            dialogState = dialogState.copy(showSavedProducts = true)
        },
        onOpenMealPlan = { dialogState = dialogState.copy(showMealPlanSheet = true) },
        onOpenAddDialog = { dialogState = dialogState.copy(showAddDialog = true) },
        onEditEntry = { dialogState = dialogState.copy(editEntry = it) },
        onDeleteEntry = { entry ->
            viewModel.removeEntry(entry.id)
            viewModel.emitMessage("Удалено: ${entry.name}")
        },
        entriesByDate = viewModel::getEntriesByDate,
        snackbarHostState = snackbarHost
    )

    NutritionOverlays(
        viewModel = viewModel,
        uiState = uiState,
        canReuseYesterdayPlan = canReuseYesterdayPlan,
        dialogState = dialogState,
        onReplaceCommentChange = { dialogState = dialogState.copy(replaceComment = it) },
        onDismissAddDialog = { dialogState = dialogState.copy(showAddDialog = false) },
        onDismissEditDialog = { dialogState = dialogState.copy(editEntry = null) },
        onDismissSettings = { dialogState = dialogState.copy(showSettings = false) },
        onDismissProfileDialog = { dialogState = dialogState.copy(showProfileDialog = false) },
        onDismissFridgeManagerDialog = { dialogState = dialogState.copy(showFridgeManagerDialog = false) },
        onDismissReplaceDialog = { dialogState = dialogState.copy(showReplaceDialog = false) },
        onDismissFridgeChoiceDialog = { dialogState = dialogState.copy(showFridgeChoiceDialog = false) },
        onDismissRegenerateWarning = { dialogState = dialogState.copy(showRegenerateWarning = false) },
        onDismissFridgeDialog = { dialogState = dialogState.copy(showFridgeDialog = false) },
        onDismissMealPlanSheet = { dialogState = dialogState.copy(showMealPlanSheet = false) },
        onDismissSavedProducts = { dialogState = dialogState.copy(showSavedProducts = false) },
        onShowFridgeDialog = {
            dialogState = dialogState.copy(
                showFridgeChoiceDialog = false,
                showFridgeDialog = true
            )
        },
        onShowFridgeManagerDialog = {
            dialogState = dialogState.copy(
                showFridgeDialog = false,
                showFridgeManagerDialog = true
            )
        },
        onShowReplaceDialog = { type ->
            dialogState = dialogState.copy(
                replaceMealType = type,
                replaceComment = "",
                showReplaceDialog = true
            )
        },
        onHideReplaceDialog = { dialogState = dialogState.copy(showReplaceDialog = false) },
        onConfirmAddEntry = { newEntry ->
            viewModel.addEntry(newEntry)
            dialogState = dialogState.copy(showAddDialog = false)
            viewModel.emitMessage("Добавлено: ${newEntry.name}")
        },
        onConfirmUpdateEntry = { updated ->
            viewModel.updateEntry(updated)
            dialogState = dialogState.copy(editEntry = null)
            viewModel.emitMessage("Обновлено: ${updated.name}")
        },
        onSaveNorm = { norm ->
            viewModel.updateNorm(norm)
            dialogState = dialogState.copy(showSettings = false)
        },
        onGenerateOrUpdatePlan = {
            val planExists = uiState.mealPlan != null || viewModel.hasCachedPlanForDate(today)
            dialogState = if (planExists) {
                dialogState.copy(showRegenerateWarning = true)
            } else {
                dialogState.copy(showFridgeChoiceDialog = true)
            }
        },
        onReuseYesterdayPlan = { viewModel.reuseYesterdayPlan() },
        onConfirmFridgePlan = { fridge ->
            dialogState = dialogState.copy(showFridgeDialog = false)
            viewModel.generatePlanFromFridge(fridge, allowExtraProducts = false)
        },
        onGeneratePlanWithoutFridge = {
            dialogState = dialogState.copy(showFridgeChoiceDialog = false)
            viewModel.generateTodayPlan()
        },
        onResetPlanAndReopenChoice = {
            viewModel.resetTodayPlan()
            dialogState = dialogState.copy(
                showRegenerateWarning = false,
                showFridgeChoiceDialog = true
            )
        },
        onAllowExtraProducts = { viewModel.allowExtraProductsForFridgePlan() },
        onContinueWithoutExtraProducts = { viewModel.continueWithoutExtraProducts() },
        onReplaceMeal = { type, comment ->
            viewModel.replaceMeal(type, comment)
            dialogState = dialogState.copy(showReplaceDialog = false)
        },
        onSaveProfile = { newProfile ->
            viewModel.updateProfile(newProfile)
            dialogState = dialogState.copy(showProfileDialog = false)
        },
        savedProducts = savedProducts,
        onSavedProductsSearch = viewModel::loadSavedProducts,
        onSavedProductSelected = { product ->
            viewModel.selectSavedProduct(product)
            dialogState = dialogState.copy(
                showSavedProducts = false,
                showAddDialog = true
            )
        }
    )
}

