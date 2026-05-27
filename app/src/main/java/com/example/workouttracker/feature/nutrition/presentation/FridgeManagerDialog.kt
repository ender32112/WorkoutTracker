package com.example.workouttracker.feature.nutrition.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.workouttracker.feature.nutrition.presentation.NutritionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FridgeManagerDialog(
    viewModel: NutritionViewModel = hiltViewModel(),
    onDismiss: () -> Unit
) {
    val items by viewModel.fridgeItems.collectAsState()
    val lookupProduct by viewModel.lookupProduct.collectAsState()
    val lookupError by viewModel.lookupError.collectAsState()
    val lookupBarcode by viewModel.lookupBarcode.collectAsState()

    var showManualAdd by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<FridgeItemUiModel?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var showManualScannedAdd by remember { mutableStateOf(false) }
    var scannedBarcodeForManual by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(FridgeSortOption.RECENT) }

    val visibleItems = remember(items, searchQuery, sortOption) {
        val query = searchQuery.trim().lowercase(Locale.getDefault())
        val filtered = if (query.isBlank()) {
            items
        } else {
            items.filter { it.name.lowercase(Locale.getDefault()).contains(query) }
        }

        when (sortOption) {
            FridgeSortOption.RECENT -> filtered.sortedByDescending { it.updatedAt }
            FridgeSortOption.NAME -> filtered.sortedBy { it.name.lowercase(Locale.getDefault()) }
            FridgeSortOption.LOW_STOCK -> filtered.sortedBy {
                if (it.unitType == QuantityUnit.GRAMS) it.amount else it.amount * 100
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Холодильник") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                item {
                    FridgeHeroCard(items = items)
                }

                item {
                    FridgeActionButtons(
                        onScannerClick = { showScanner = true },
                        onManualClick = { showManualAdd = true }
                    )
                }

                if (items.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it.take(40) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Поиск по продуктам") },
                            placeholder = { Text("Например: курица, молоко, рис") }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FridgeSortOption.values().forEach { option ->
                                FilterChip(
                                    selected = sortOption == option,
                                    onClick = { sortOption = option },
                                    label = { Text(option.label) }
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                        }
                    }
                }

                when {
                    items.isEmpty() -> item { EmptyFridgeState() }
                    visibleItems.isEmpty() -> item { EmptyFridgeSearchState() }
                    else -> {
                        items(visibleItems, key = { it.id }) { item ->
                            FridgeItemCard(
                                item = item,
                                onEdit = { editingItem = item },
                                onDelete = { viewModel.removeFridgeItem(item.id) },
                                onQuickDeduct = { amount ->
                                    viewModel.deductFridgeItem(item, amount)
                                }
                            )
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

    if (showManualScannedAdd) {
        ManualFridgeAddDialog(
            dialogTitle = "Штрихкод: $scannedBarcodeForManual",
            onAdd = { name, c, p, f, carb, amount, unit ->
                viewModel.saveManualBarcodeProduct(scannedBarcodeForManual, name, c, p, f, carb)
                viewModel.addManualProductToFridge(name, c, p, f, carb, amount, unit)
                showManualScannedAdd = false
            },
            onDismiss = { showManualScannedAdd = false }
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
        val productKey = product.barcode.ifBlank { product.name }
        var addAmount by remember(productKey) { mutableStateOf("1") }
        var addUnit by remember(productKey) { mutableStateOf(QuantityUnit.PIECES) }

        AlertDialog(
            onDismissRequest = { viewModel.clearLookupProduct() },
            title = { Text("Добавить в холодильник") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "На 100 г: ${formatMacro(product.calories100 ?: 0f)} ккал, " +
                            "Б ${formatMacro(product.protein100 ?: 0f)} г, " +
                            "Ж ${formatMacro(product.fats100 ?: 0f)} г, " +
                            "У ${formatMacro(product.carbs100 ?: 0f)} г"
                    )

                    OutlinedTextField(
                        value = addAmount,
                        onValueChange = { addAmount = it.filter(Char::isDigit).take(5) },
                        label = { Text("Количество") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = addUnit == QuantityUnit.PIECES,
                            onClick = { addUnit = QuantityUnit.PIECES },
                            label = { Text("Штуки") }
                        )
                        FilterChip(
                            selected = addUnit == QuantityUnit.GRAMS,
                            onClick = { addUnit = QuantityUnit.GRAMS },
                            label = { Text("Граммы") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = addAmount.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    viewModel.addScannedProductToFridge(product, amount, addUnit)
                    viewModel.clearLookupProduct()
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { viewModel.clearLookupProduct() }) { Text("Отмена") } }
        )
    }

    lookupError?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearLookupProduct() },
            title = { Text("Продукт не найден") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(message)
                    if (!lookupBarcode.isNullOrBlank()) {
                        Text("Штрихкод: $lookupBarcode")
                    }
                    Text("Можно заполнить КБЖУ вручную и сохранить продукт в локальную базу.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scannedBarcodeForManual = lookupBarcode.orEmpty()
                        viewModel.clearLookupProduct()
                        showManualScannedAdd = true
                    },
                    enabled = !lookupBarcode.isNullOrBlank()
                ) {
                    Text("Заполнить вручную")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearLookupProduct() }) {
                    Text("Отмена")
                }
            }
        )
    }
}

private enum class FridgeSortOption(val label: String) {
    RECENT("Свежие"),
    NAME("По имени"),
    LOW_STOCK("Мало осталось")
}

@Composable
private fun FridgeHeroCard(items: List<FridgeItemUiModel>) {
    val gradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.86f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f)
        )
    )

    val totalPositions = items.size
    val gramsItems = items.count { it.unitType == QuantityUnit.GRAMS }
    val piecesItems = items.count { it.unitType == QuantityUnit.PIECES }
    val lowStockItems = items.count { item ->
        if (item.unitType == QuantityUnit.GRAMS) item.amount <= 250 else item.amount <= 2
    }
    val totalCaloriesEstimate = items.sumOf { item ->
        val multiplier = if (item.unitType == QuantityUnit.GRAMS) item.amount / 100f else item.amount.toFloat()
        (item.calories100 * multiplier).toDouble()
    }.roundToInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .background(gradient, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Сводка запасов", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Быстрый обзор доступных продуктов перед составлением плана.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FridgeMetricPill(label = "Позиции", value = totalPositions.toString(), modifier = Modifier.weight(1f))
                FridgeMetricPill(label = "Граммы", value = gramsItems.toString(), modifier = Modifier.weight(1f))
                FridgeMetricPill(label = "Штуки", value = piecesItems.toString(), modifier = Modifier.weight(1f))
            }
            if (lowStockItems > 0) {
                Text(
                    "Мало осталось в $lowStockItems позициях. Пополните запасы для более точного плана.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Оценка энергетической емкости: ~$totalCaloriesEstimate ккал",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FridgeActionButtons(
    onScannerClick: () -> Unit,
    onManualClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useColumn = maxWidth < 360.dp

        if (useColumn) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FridgeActionButton(
                    icon = Icons.Default.QrCodeScanner,
                    title = "Сканер",
                    subtitle = "По штрихкоду",
                    onClick = onScannerClick,
                    modifier = Modifier.fillMaxWidth()
                )
                FridgeActionButton(
                    icon = Icons.Default.Add,
                    title = "Добавить",
                    subtitle = "Вручную",
                    onClick = onManualClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FridgeActionButton(
                    icon = Icons.Default.QrCodeScanner,
                    title = "Сканер",
                    subtitle = "По штрихкоду",
                    onClick = onScannerClick,
                    modifier = Modifier.weight(1f)
                )
                FridgeActionButton(
                    icon = Icons.Default.Add,
                    title = "Добавить",
                    subtitle = "Вручную",
                    onClick = onManualClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FridgeMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FridgeActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 72.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.56f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyFridgeState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "Холодильник пока пуст. Добавьте продукты вручную или через сканер.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyFridgeSearchState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "По вашему запросу ничего не найдено. Попробуйте другой текст или очистите поиск.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FridgeItemCard(
    item: FridgeItemUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuickDeduct: (Int) -> Unit
) {
    val primaryDeduct = if (item.unitType == QuantityUnit.GRAMS) 100 else 1
    val secondaryDeduct = if (item.unitType == QuantityUnit.GRAMS) 250 else 5
    val unitLabel = if (item.unitType == QuantityUnit.GRAMS) "г" else "шт"

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
            Text(
                "На 100 г: ${formatMacro(item.calories100)} ккал, Б ${formatMacro(item.protein100)} г, " +
                    "Ж ${formatMacro(item.fats100)} г, У ${formatMacro(item.carbs100)} г",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Осталось: ${item.amount} $unitLabel",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { onQuickDeduct(primaryDeduct.coerceAtMost(item.amount)) },
                    label = { Text("-$primaryDeduct $unitLabel") },
                    enabled = item.amount > 0
                )
                AssistChip(
                    onClick = { onQuickDeduct(secondaryDeduct.coerceAtMost(item.amount)) },
                    label = { Text("-$secondaryDeduct $unitLabel") },
                    enabled = item.amount > 0
                )
            }
            Text(
                "Обновлено: ${formatFridgeDate(item.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название продукта") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("КБЖУ на 100 г", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = sanitizeDecimalInput(it) },
                        label = { Text("Ккал") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = sanitizeDecimalInput(it) },
                        label = { Text("Б") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = fats,
                        onValueChange = { fats = sanitizeDecimalInput(it) },
                        label = { Text("Ж") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = sanitizeDecimalInput(it) },
                        label = { Text("У") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).take(5) },
                    label = { Text("Сколько осталось") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

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

                Text(
                    text = "Выбрано: ${if (unit == QuantityUnit.GRAMS) "граммы" else "штуки"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(
                        name.trim(),
                        calories.replace(',', '.').toFloatOrNull() ?: 0f,
                        protein.replace(',', '.').toFloatOrNull() ?: 0f,
                        fats.replace(',', '.').toFloatOrNull() ?: 0f,
                        carbs.replace(',', '.').toFloatOrNull() ?: 0f,
                        amount.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        unit
                    )
                },
                enabled = name.isNotBlank()
            ) { Text(confirmLabel) }
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

private fun formatMacro(value: Float): String = "%.1f".format(value)

