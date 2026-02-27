package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.viewmodel.NutritionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FridgeManagerDialog(
    viewModel: NutritionViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val items by viewModel.fridgeItems.collectAsState()
    val lookupProduct by viewModel.lookupProduct.collectAsState()
    val lookupError by viewModel.lookupError.collectAsState()
    var showManualAdd by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<FridgeItemUiModel?>(null) }
    var showScanner by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Холодильник") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(onClick = { showScanner = true }, modifier = Modifier.weight(1f).height(52.dp)) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Сканировать", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(onClick = { showManualAdd = true }, modifier = Modifier.weight(1f).height(52.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Добавить", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.id }) { item ->
                        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f))))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { editingItem = item }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                                    }
                                    IconButton(onClick = { viewModel.removeFridgeItem(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                                    }
                                }
                                Text("КБЖУ / 100г: ${item.calories100.toInt()} / ${item.protein100.toInt()} / ${item.fats100.toInt()} / ${item.carbs100.toInt()}")
                                Text("Осталось: ${item.amount} ${if (item.unitType == QuantityUnit.GRAMS) "г" else "шт"}")
                                Text("Добавлено: ${formatFridgeDate(item.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
        dismissButton = {}
    )

    if (showManualAdd) {
        ManualFridgeAddDialog(
            onAdd = { name, c, p, f, carb, amount, unit ->
                viewModel.addManualProductToFridge(name, c, p, f, carb, amount, unit)
                showManualAdd = false
            },
            onDismiss = { showManualAdd = false }
        )
    }

    editingItem?.let { item ->
        ManualFridgeAddDialog(
            initialName = item.name,
            initialCalories = item.calories100,
            initialProtein = item.protein100,
            initialFats = item.fats100,
            initialCarbs = item.carbs100,
            initialAmount = item.amount,
            initialUnit = item.unitType,
            dialogTitle = "Редактирование продукта",
            confirmLabel = "Сохранить",
            onAdd = { name, c, p, f, carb, amount, unit ->
                viewModel.updateFridgeItem(item.id, name, c, p, f, carb, amount, unit)
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }

    if (showScanner) {
        Dialog(onDismissRequest = { showScanner = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BarcodeScannerScreen(
                onDetected = {
                    showScanner = false
                    viewModel.lookupBarcode(it)
                },
                onError = { showScanner = false },
                onClose = { showScanner = false }
            )
        }
    }

    lookupProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { viewModel.clearLookupProduct() },
            title = { Text("Добавить в холодильник") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.name)
                    Text("КБЖУ / 100г: ${product.calories100 ?: 0f} / ${product.protein100 ?: 0f} / ${product.fats100 ?: 0f} / ${product.carbs100 ?: 0f}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addScannedProductToFridge(product, 1, QuantityUnit.PIECES)
                    viewModel.clearLookupProduct()
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { viewModel.clearLookupProduct() }) { Text("Отмена") } }
        )
    }

    lookupError?.let {
        AlertDialog(
            onDismissRequest = { viewModel.clearLookupProduct() },
            title = { Text("Ошибка") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { viewModel.clearLookupProduct() }) { Text("Ок") } }
        )
    }
}

@Composable
private fun ManualFridgeAddDialog(
    initialName: String = "",
    initialCalories: Float? = null,
    initialProtein: Float? = null,
    initialFats: Float? = null,
    initialCarbs: Float? = null,
    initialAmount: Int = 100,
    initialUnit: QuantityUnit = QuantityUnit.GRAMS,
    dialogTitle: String = "Новый продукт",
    confirmLabel: String = "Добавить",
    onAdd: (String, Float, Float, Float, Float, Int, QuantityUnit) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var calories by remember { mutableStateOf(initialCalories?.prettyNumber().orEmpty()) }
    var protein by remember { mutableStateOf(initialProtein?.prettyNumber().orEmpty()) }
    var fats by remember { mutableStateOf(initialFats?.prettyNumber().orEmpty()) }
    var carbs by remember { mutableStateOf(initialCarbs?.prettyNumber().orEmpty()) }
    var amount by remember { mutableStateOf(initialAmount.toString()) }
    var unit by remember { mutableStateOf(initialUnit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название продукта") }, modifier = Modifier.fillMaxWidth())
                Text("КБЖУ на 100 г", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = calories, onValueChange = { calories = sanitizeDecimalInput(it) }, label = { Text("Ккал") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = protein, onValueChange = { protein = sanitizeDecimalInput(it) }, label = { Text("Б") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = fats, onValueChange = { fats = sanitizeDecimalInput(it) }, label = { Text("Ж") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = carbs, onValueChange = { carbs = sanitizeDecimalInput(it) }, label = { Text("У") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit) }, label = { Text("Сколько осталось") }, modifier = Modifier.fillMaxWidth())
                Text("Единица измерения", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = unit == QuantityUnit.GRAMS,
                        onClick = { unit = QuantityUnit.GRAMS },
                        label = { Text("Граммы") }
                    )
                    FilterChip(
                        selected = unit == QuantityUnit.PIECES,
                        onClick = { unit = QuantityUnit.PIECES },
                        label = { Text("Штуки") }
                    )
                }
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("Выбрано: ${if (unit == QuantityUnit.GRAMS) "граммы" else "штуки"}") },
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAdd(
                    name.trim(),
                    calories.replace(',', '.').toFloatOrNull() ?: 0f,
                    protein.replace(',', '.').toFloatOrNull() ?: 0f,
                    fats.replace(',', '.').toFloatOrNull() ?: 0f,
                    carbs.replace(',', '.').toFloatOrNull() ?: 0f,
                    amount.toIntOrNull() ?: 1,
                    unit
                )
            }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun Float.prettyNumber(): String {
    val asInt = toInt()
    return if (asInt.toFloat() == this) asInt.toString() else toString()
}

private fun sanitizeDecimalInput(input: String): String {
    val cleaned = input.filter { it.isDigit() || it == ',' || it == '.' }
    val separatorIndex = cleaned.indexOfFirst { it == ',' || it == '.' }
    if (separatorIndex == -1) return cleaned
    val integerPart = cleaned.substring(0, separatorIndex)
    val decimalPart = cleaned.substring(separatorIndex + 1).filter(Char::isDigit)
    return "$integerPart.${decimalPart.take(2)}".trimEnd('.')
}

private fun formatFridgeDate(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
