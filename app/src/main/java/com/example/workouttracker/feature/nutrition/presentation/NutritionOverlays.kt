package com.example.workouttracker.feature.nutrition.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionOverlays(
    viewModel: NutritionViewModel,
    uiState: NutritionUiState,
    canReuseYesterdayPlan: Boolean,
    dialogState: NutritionDialogState,
    onReplaceCommentChange: (String) -> Unit,
    onDismissAddDialog: () -> Unit,
    onDismissEditDialog: () -> Unit,
    onDismissSettings: () -> Unit,
    onDismissProfileDialog: () -> Unit,
    onDismissFridgeManagerDialog: () -> Unit,
    onDismissReplaceDialog: () -> Unit,
    onDismissFridgeChoiceDialog: () -> Unit,
    onDismissRegenerateWarning: () -> Unit,
    onDismissFridgeDialog: () -> Unit,
    onDismissMealPlanSheet: () -> Unit,
    onDismissSavedProducts: () -> Unit,
    onShowFridgeDialog: () -> Unit,
    onShowFridgeManagerDialog: () -> Unit,
    onShowReplaceDialog: (MealType) -> Unit,
    onHideReplaceDialog: () -> Unit,
    onConfirmAddEntry: (NutritionEntry) -> Unit,
    onConfirmUpdateEntry: (NutritionEntry) -> Unit,
    onSaveNorm: (Map<String, Int>) -> Unit,
    onGenerateOrUpdatePlan: () -> Unit,
    onReuseYesterdayPlan: () -> Unit,
    onConfirmFridgePlan: (List<FridgeProduct>) -> Unit,
    onGeneratePlanWithoutFridge: () -> Unit,
    onResetPlanAndReopenChoice: () -> Unit,
    onAllowExtraProducts: () -> Unit,
    onContinueWithoutExtraProducts: () -> Unit,
    onReplaceMeal: (MealType, String?) -> Unit,
    onSaveProfile: (NutritionProfile) -> Unit,
    savedProducts: List<ProductLookupResult>,
    onSavedProductsSearch: (String) -> Unit,
    onSavedProductSelected: (ProductLookupResult) -> Unit
) {
    if (dialogState.showAddDialog) {
        AddNutritionDialog(
            viewModel = viewModel,
            onConfirm = onConfirmAddEntry,
            onDismiss = onDismissAddDialog
        )
    }

    dialogState.editEntry?.let { entry ->
        AddNutritionDialog(
            viewModel = viewModel,
            entry = entry,
            onConfirm = onConfirmUpdateEntry,
            onDismiss = onDismissEditDialog
        )
    }

    if (dialogState.showSettings) {
        SettingsDialog(
            currentNorm = uiState.dailyNorm,
            recommendedNorm = uiState.recommendedNorm,
            onSave = onSaveNorm,
            onDismiss = onDismissSettings
        )
    }

    if (dialogState.showMealPlanSheet) {
        ModalBottomSheet(onDismissRequest = onDismissMealPlanSheet) {
            MealPlanSheetContent(
                mealPlan = uiState.mealPlan,
                isLoading = uiState.isPlanLoading,
                error = uiState.planError,
                canReuseYesterdayPlan = canReuseYesterdayPlan,
                onDismissError = { viewModel.clearPlanError() },
                onGenerateOrUpdate = onGenerateOrUpdatePlan,
                onReuseYesterday = onReuseYesterdayPlan,
                onReplaceMeal = onShowReplaceDialog
            )
        }
    }

    if (dialogState.showSavedProducts) {
        SavedProductsDialog(
            products = savedProducts,
            onSearch = onSavedProductsSearch,
            onSelect = onSavedProductSelected,
            onDismiss = onDismissSavedProducts
        )
    }

    if (dialogState.showRegenerateWarning) {
        AlertDialog(
            onDismissRequest = onDismissRegenerateWarning,
            title = { Text("Обновить план") },
            text = {
                Text("Текущий план будет удалён. Продолжить и сгенерировать новый?")
            },
            confirmButton = {
                TextButton(
                    onClick = onResetPlanAndReopenChoice,
                    enabled = !uiState.isPlanLoading
                ) {
                    Text("Продолжить")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRegenerateWarning) {
                    Text("Отмена")
                }
            }
        )
    }

    if (dialogState.showFridgeChoiceDialog) {
        AlertDialog(
            onDismissRequest = onDismissFridgeChoiceDialog,
            title = { Text("Сформировать план") },
            text = { Text("Использовать продукты из холодильника при генерации плана?") },
            confirmButton = {
                TextButton(onClick = onShowFridgeDialog) {
                    Text("Да")
                }
            },
            dismissButton = {
                TextButton(onClick = onGeneratePlanWithoutFridge) {
                    Text("Нет")
                }
            }
        )
    }

    if (dialogState.showFridgeDialog) {
        FridgeDialog(
            fridgeItems = uiState.fridgeItems,
            onConfirm = onConfirmFridgePlan,
            onDismiss = onDismissFridgeDialog,
            onManageFridge = onShowFridgeManagerDialog
        )
    }

    uiState.fridgePrompt?.let {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Недостаточно продуктов") },
            text = {
                Text("Эти продукты не покрывают дневную норму. Разрешить использовать дополнительные продукты?")
            },
            confirmButton = {
                TextButton(onClick = onAllowExtraProducts) { Text("Да") }
            },
            dismissButton = {
                TextButton(onClick = onContinueWithoutExtraProducts) { Text("Нет") }
            }
        )
    }

    if (dialogState.showReplaceDialog && dialogState.replaceMealType != null) {
        AlertDialog(
            onDismissRequest = onHideReplaceDialog,
            title = { Text("Заменить приём пищи") },
            text = {
                OutlinedTextField(
                    value = dialogState.replaceComment,
                    onValueChange = onReplaceCommentChange,
                    label = { Text("Комментарий для замены") },
                    modifier = Modifier
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReplaceMeal(
                            dialogState.replaceMealType,
                            dialogState.replaceComment.trim().ifBlank { null }
                        )
                    },
                    enabled = !uiState.isPlanLoading
                ) {
                    Text("Заменить")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissReplaceDialog) {
                    Text("Отмена")
                }
            }
        )
    }

    if (dialogState.showProfileDialog) {
        ProfileDialog(
            currentProfile = uiState.profile,
            prefillData = uiState.profilePrefill,
            onSave = onSaveProfile,
            onDismiss = onDismissProfileDialog
        )
    }

    if (dialogState.showFridgeManagerDialog) {
        FridgeManagerDialog(
            viewModel = viewModel,
            onDismiss = onDismissFridgeManagerDialog
        )
    }
}
