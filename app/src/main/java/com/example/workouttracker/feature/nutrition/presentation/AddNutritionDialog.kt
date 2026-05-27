package com.example.workouttracker.feature.nutrition.presentation

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

private val russianLocale: Locale = Locale.forLanguageTag("ru-RU")
private const val defaultPortionWeight = "250"

private data class IngredientDraft(
    val id: UUID = UUID.randomUUID(),
    val name: String = "",
    val weightInDish: String = "100",
    val caloriesPer100g: String = "",
    val proteinPer100g: String = "",
    val fatsPer100g: String = "",
    val carbsPer100g: String = ""
)

@Composable
fun AddNutritionDialog(
    viewModel: NutritionViewModel,
    entry: NutritionEntry? = null,
    onConfirm: (NutritionEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val lookupProduct by viewModel.lookupProduct.collectAsState()
    val lookupError by viewModel.lookupError.collectAsState()
    val lookupBarcode by viewModel.lookupBarcode.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isEditing = entry != null

    var dishName by rememberSaveable(entry?.id) { mutableStateOf(entry?.dish?.name.orEmpty()) }
    var dateIso by rememberSaveable(entry?.id) { mutableStateOf(entry?.date ?: todayIso()) }
    var portionWeightText by rememberSaveable(entry?.id) {
        mutableStateOf(entry?.portionWeight?.toString() ?: defaultPortionWeight)
    }
    var mealType by rememberSaveable(entry?.id) { mutableStateOf(entry?.mealType ?: MealType.OTHER) }

    val ingredients = remember(entry?.id) {
        mutableStateListOf<DishIngredient>().apply {
            addAll(entry?.dish?.ingredients.orEmpty())
        }
    }

    var showIngredientEditor by remember { mutableStateOf(false) }
    var ingredientDraft by remember { mutableStateOf(IngredientDraft()) }
    var editingIngredientId by remember { mutableStateOf<UUID?>(null) }
    var showScanner by remember { mutableStateOf(false) }
    var showManualBarcodeDialog by remember { mutableStateOf(false) }
    var scannedBarcodeForManual by remember { mutableStateOf("") }
    var cameraMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var validationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var lookupWeightText by rememberSaveable { mutableStateOf("100") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showScanner = true
            cameraMessage = null
        } else {
            cameraMessage = "Разрешите доступ к камере, чтобы сканировать штрихкоды."
        }
    }

    val portionWeight = portionWeightText.toIntOrNull()?.coerceAtLeast(1) ?: 0
    val dish = remember(dishName, ingredients.toList()) {
        Dish(name = dishName.trim(), ingredients = ingredients.toList())
    }
    val draftEntry = remember(dateIso, mealType, dish, portionWeight) {
        NutritionEntry(
            id = entry?.id ?: UUID.randomUUID(),
            date = dateIso,
            mealType = mealType,
            dish = dish,
            portionWeight = portionWeight
        )
    }
    val saveLabel = if (isEditing) "Сохранить изменения" else "Сохранить приём пищи"
    val canSave = dishName.isNotBlank() && portionWeight > 0

    LaunchedEffect(showScanner) {
        if (showScanner) {
            cameraMessage = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                AddNutritionHeader(
                    title = if (isEditing) "Редактирование приёма пищи" else "Добавление приёма пищи",
                    subtitle = "Соберите блюдо и сразу проверьте итог по КБЖУ.",
                    onBack = onDismiss
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        FormSection(
                            title = "Основное",
                            subtitle = "Название, дата и размер порции."
                        ) {
                            OutlinedTextField(
                                value = dishName,
                                onValueChange = {
                                    dishName = it
                                    validationMessage = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.RestaurantMenu, contentDescription = null)
                                },
                                label = { Text("Название блюда") },
                                singleLine = true
                            )

                            DateField(
                                value = formatDateForDisplay(dateIso),
                                onClick = {
                                    val calendar = parseIsoDate(dateIso)
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            dateIso = formatIsoDate(year, month + 1, day)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            )

                            OutlinedTextField(
                                value = portionWeightText,
                                onValueChange = {
                                    portionWeightText = it.filter(Char::isDigit).take(4)
                                    validationMessage = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Размер порции, г") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            MealTypeSelector(
                                selected = mealType,
                                onSelected = { mealType = it }
                            )
                        }
                    }

                    item {
                        FormSection(
                            title = "Добавление ингредиентов",
                            subtitle = "Сначала отсканируйте продукт или внесите ингредиент вручную."
                        ) {
                            ActionCard(
                                icon = Icons.Default.QrCodeScanner,
                                title = "Сканировать штрихкод",
                                subtitle = "Найти продукт в сохранённой базе",
                                onClick = {
                                    val permissionGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (permissionGranted) {
                                        showScanner = true
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                emphasized = true
                            )

                            ActionCard(
                                icon = Icons.Default.Add,
                                title = "Добавить ингредиент вручную",
                                subtitle = "Указать название и КБЖУ самостоятельно",
                                onClick = {
                                    editingIngredientId = null
                                    ingredientDraft = IngredientDraft()
                                    showIngredientEditor = true
                                }
                            )

                            BannerCard(
                                text = cameraMessage
                                    ?: "Если продукт найден по штрихкоду, вы сможете добавить его в состав блюда одним нажатием."
                            )
                        }
                    }

                    item {
                        FormSection(
                            title = "Состав блюда",
                            subtitle = if (ingredients.isEmpty()) {
                                "Пока список пуст."
                            } else {
                                "Редактируйте ингредиенты и их вес прямо в составе блюда."
                            }
                        ) {
                            if (ingredients.isEmpty()) {
                                EmptyIngredientsState(
                                    onAdd = {
                                        editingIngredientId = null
                                        ingredientDraft = IngredientDraft()
                                        showIngredientEditor = true
                                    }
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ingredients.forEach { dishIngredient ->
                                        IngredientCard(
                                            item = dishIngredient,
                                            onEdit = {
                                                editingIngredientId = dishIngredient.id
                                                ingredientDraft = dishIngredient.toDraft()
                                                showIngredientEditor = true
                                            },
                                            onDelete = {
                                                ingredients.removeAll { it.id == dishIngredient.id }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        FormSection(
                            title = "Итоги",
                            subtitle = "Быстрая проверка КБЖУ для блюда и выбранной порции."
                        ) {
                            MacroSummaryCard(
                                title = "На 100 г блюда",
                                totalWeight = dish.totalWeight,
                                calories = dish.caloriesPer100g,
                                protein = dish.proteinPer100g,
                                fats = dish.fatsPer100g,
                                carbs = dish.carbsPer100g
                            )

                            MacroSummaryCard(
                                title = "Для выбранной порции",
                                totalWeight = portionWeight,
                                calories = draftEntry.caloriesFloat,
                                protein = draftEntry.proteinFloat,
                                fats = draftEntry.fatsFloat,
                                carbs = draftEntry.carbsFloat
                            )
                        }
                    }
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    validationMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            when {
                                dishName.isBlank() -> validationMessage = "Введите название блюда."
                                portionWeight <= 0 -> validationMessage = "Укажите размер порции больше нуля."
                                else -> {
                                    validationMessage = null
                                    onConfirm(draftEntry)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        enabled = canSave
                    ) {
                        Text(
                            text = saveLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Отмена")
                    }
                }
            }
        }
    }

    if (showIngredientEditor) {
        IngredientEditorDialog(
            title = if (editingIngredientId == null) "Новый ингредиент" else "Редактирование ингредиента",
            draft = ingredientDraft,
            onDraftChange = { ingredientDraft = it },
            onDismiss = { showIngredientEditor = false },
            onConfirm = {
                val ingredient = it.toDishIngredient()
                if (editingIngredientId == null) {
                    ingredients.add(ingredient)
                    if (dishName.isBlank()) {
                        dishName = ingredient.ingredient.name
                    }
                } else {
                    val index = ingredients.indexOfFirst { item -> item.id == editingIngredientId }
                    if (index != -1) {
                        ingredients[index] = ingredient.copy(id = editingIngredientId!!)
                    }
                }
                showIngredientEditor = false
            }
        )
    }

    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BarcodeScannerScreen(
                onDetected = { code ->
                    showScanner = false
                    viewModel.lookupBarcode(code)
                },
                onError = { message ->
                    showScanner = false
                    cameraMessage = message
                },
                onClose = { showScanner = false }
            )
        }
    }

    lookupProduct?.let { product ->
        AlertDialog(
            onDismissRequest = {
                lookupWeightText = "100"
                viewModel.clearLookupProduct()
            },
            title = { Text("Найденный продукт") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Штрихкод: ${product.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "На 100 г: ${formatMacro(product.calories100 ?: 0f)} ккал, " +
                            "Б ${formatMacro(product.protein100 ?: 0f)} г, " +
                            "Ж ${formatMacro(product.fats100 ?: 0f)} г, " +
                            "У ${formatMacro(product.carbs100 ?: 0f)} г",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = lookupWeightText,
                        onValueChange = { lookupWeightText = it.filter(Char::isDigit).take(4) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Вес в блюде, г") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val weight = lookupWeightText.toIntOrNull()?.coerceAtLeast(1) ?: 100
                        val ingredient = DishIngredient(
                            ingredient = Ingredient(
                                name = product.name,
                                caloriesPer100g = product.calories100 ?: 0f,
                                proteinPer100g = product.protein100 ?: 0f,
                                fatsPer100g = product.fats100 ?: 0f,
                                carbsPer100g = product.carbs100 ?: 0f
                            ),
                            weightInDish = weight
                        )
                        ingredients.add(ingredient)
                        if (dishName.isBlank()) {
                            dishName = product.name
                        }
                        lookupWeightText = "100"
                        viewModel.clearLookupProduct()
                    }
                ) {
                    Text("Добавить в состав")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        lookupWeightText = "100"
                        viewModel.clearLookupProduct()
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    lookupError?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearLookupProduct() },
            title = { Text("Продукт не найден") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!lookupBarcode.isNullOrBlank()) {
                        Text(
                            text = "Штрихкод: $lookupBarcode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(message)
                    Text(
                        text = "Вы можете заполнить данные вручную и сохранить продукт в локальную базу.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scannedBarcodeForManual = lookupBarcode.orEmpty()
                        showManualBarcodeDialog = true
                        viewModel.clearLookupProduct()
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

    if (showManualBarcodeDialog) {
        ManualBarcodeProductDialog(
            barcode = scannedBarcodeForManual,
            onDismiss = { showManualBarcodeDialog = false },
            onConfirm = { name, calories, protein, fats, carbs ->
                viewModel.saveManualBarcodeProduct(
                    barcode = scannedBarcodeForManual,
                    name = name,
                    calories100 = calories,
                    protein100 = protein,
                    fats100 = fats,
                    carbs100 = carbs
                )
                showManualBarcodeDialog = false
            }
        )
    }
}

@Composable
private fun AddNutritionHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
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
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun DateField(
    value: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Column {
                Text(
                    text = "Дата приёма пищи",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealTypeSelector(
    selected: MealType,
    onSelected: (MealType) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MealType.values().forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = {
                    Text(
                        text = type.displayName(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    emphasized: Boolean = false
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (emphasized) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                }
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    Icon(icon, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BannerCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyIngredientsState(onAdd: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Пока нет ингредиентов",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Добавьте первый ингредиент вручную или начните со сканирования штрихкода.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(onClick = onAdd) {
                Text("Добавить ингредиент")
            }
        }
    }
}

@Composable
private fun IngredientCard(
    item: DishIngredient,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.ingredient.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Вес в блюде: ${item.weightInDish} г",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать ингредиент")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить ингредиент")
                }
            }
            Text(
                text = "На 100 г: ${formatMacro(item.ingredient.caloriesPer100g)} ккал, " +
                    "Б ${formatMacro(item.ingredient.proteinPer100g)} г, " +
                    "Ж ${formatMacro(item.ingredient.fatsPer100g)} г, " +
                    "У ${formatMacro(item.ingredient.carbsPer100g)} г",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MacroSummaryCard(
    title: String,
    totalWeight: Int,
    calories: Float,
    protein: Float,
    fats: Float,
    carbs: Float
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill("Вес", "$totalWeight г")
                MetricPill("Калории", "${formatMacro(calories)} ккал")
                MetricPill("Белки", "${formatMacro(protein)} г")
                MetricPill("Жиры", "${formatMacro(fats)} г")
                MetricPill("Углеводы", "${formatMacro(carbs)} г")
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun IngredientEditorDialog(
    title: String,
    draft: IngredientDraft,
    onDraftChange: (IngredientDraft) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (IngredientDraft) -> Unit
) {
    val isValid = draft.name.isNotBlank() && (draft.weightInDish.toIntOrNull() ?: 0) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onDraftChange(draft.copy(name = it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название ингредиента") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.weightInDish,
                    onValueChange = {
                        onDraftChange(draft.copy(weightInDish = it.filter(Char::isDigit).take(4)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Вес в блюде, г") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Text(
                    text = "КБЖУ на 100 г",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                MacroFields(
                    calories = draft.caloriesPer100g,
                    protein = draft.proteinPer100g,
                    fats = draft.fatsPer100g,
                    carbs = draft.carbsPer100g,
                    onCaloriesChange = { onDraftChange(draft.copy(caloriesPer100g = sanitizeDecimalInput(it))) },
                    onProteinChange = { onDraftChange(draft.copy(proteinPer100g = sanitizeDecimalInput(it))) },
                    onFatsChange = { onDraftChange(draft.copy(fatsPer100g = sanitizeDecimalInput(it))) },
                    onCarbsChange = { onDraftChange(draft.copy(carbsPer100g = sanitizeDecimalInput(it))) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }, enabled = isValid) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun MacroFields(
    calories: String,
    protein: String,
    fats: String,
    carbs: String,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onFatsChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = calories,
            onValueChange = onCaloriesChange,
            modifier = Modifier.weight(1f),
            label = { Text("Ккал") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        OutlinedTextField(
            value = protein,
            onValueChange = onProteinChange,
            modifier = Modifier.weight(1f),
            label = { Text("Белки") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = fats,
            onValueChange = onFatsChange,
            modifier = Modifier.weight(1f),
            label = { Text("Жиры") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        OutlinedTextField(
            value = carbs,
            onValueChange = onCarbsChange,
            modifier = Modifier.weight(1f),
            label = { Text("Углеводы") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
    }
}

@Composable
private fun ManualBarcodeProductDialog(
    barcode: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, calories: Float, protein: Float, fats: Float, carbs: Float) -> Unit
) {
    var name by rememberSaveable(barcode) { mutableStateOf("") }
    var calories by rememberSaveable(barcode) { mutableStateOf("") }
    var protein by rememberSaveable(barcode) { mutableStateOf("") }
    var fats by rememberSaveable(barcode) { mutableStateOf("") }
    var carbs by rememberSaveable(barcode) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый продукт по штрихкоду") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Штрихкод: $barcode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название продукта") },
                    singleLine = true
                )
                Text(
                    text = "КБЖУ на 100 г",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                MacroFields(
                    calories = calories,
                    protein = protein,
                    fats = fats,
                    carbs = carbs,
                    onCaloriesChange = { calories = sanitizeDecimalInput(it) },
                    onProteinChange = { protein = sanitizeDecimalInput(it) },
                    onFatsChange = { fats = sanitizeDecimalInput(it) },
                    onCarbsChange = { carbs = sanitizeDecimalInput(it) }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        name.trim(),
                        calories.toFloatOrNull() ?: 0f,
                        protein.toFloatOrNull() ?: 0f,
                        fats.toFloatOrNull() ?: 0f,
                        carbs.toFloatOrNull() ?: 0f
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun DishIngredient.toDraft(): IngredientDraft = IngredientDraft(
    id = id,
    name = ingredient.name,
    weightInDish = weightInDish.toString(),
    caloriesPer100g = ingredient.caloriesPer100g.prettyDecimal(),
    proteinPer100g = ingredient.proteinPer100g.prettyDecimal(),
    fatsPer100g = ingredient.fatsPer100g.prettyDecimal(),
    carbsPer100g = ingredient.carbsPer100g.prettyDecimal()
)

private fun IngredientDraft.toDishIngredient(): DishIngredient = DishIngredient(
    id = id,
    ingredient = Ingredient(
        name = name.trim(),
        caloriesPer100g = caloriesPer100g.toFloatOrNull() ?: 0f,
        proteinPer100g = proteinPer100g.toFloatOrNull() ?: 0f,
        fatsPer100g = fatsPer100g.toFloatOrNull() ?: 0f,
        carbsPer100g = carbsPer100g.toFloatOrNull() ?: 0f
    ),
    weightInDish = weightInDish.toIntOrNull()?.coerceAtLeast(1) ?: 1
)

fun formatDateForDisplay(dateIso: String): String {
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val output = SimpleDateFormat("dd MMM yyyy", russianLocale)
    return runCatching { output.format(input.parse(dateIso) ?: Date()) }.getOrElse { dateIso }
}

private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun parseIsoDate(dateIso: String): Calendar {
    val calendar = Calendar.getInstance()
    runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso)?.let { parsed ->
            calendar.time = parsed
        }
    }
    return calendar
}

private fun formatIsoDate(year: Int, month: Int, day: Int): String =
    String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

private fun sanitizeDecimalInput(input: String): String {
    val cleaned = input.filter { it.isDigit() || it == '.' || it == ',' }
    val separatorIndex = cleaned.indexOfFirst { it == '.' || it == ',' }
    if (separatorIndex == -1) return cleaned
    val integerPart = cleaned.substring(0, separatorIndex)
    val decimalPart = cleaned.substring(separatorIndex + 1).filter(Char::isDigit)
    return "$integerPart.${decimalPart.take(2)}".trimEnd('.')
}

private fun Float.prettyDecimal(): String {
    val rounded = toInt()
    return if (rounded.toFloat() == this) rounded.toString() else formatMacro(this)
}

private fun formatMacro(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else String.format(russianLocale, "%.1f", value)
