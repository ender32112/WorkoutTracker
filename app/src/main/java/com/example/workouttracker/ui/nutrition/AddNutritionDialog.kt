package com.example.workouttracker.ui.nutrition

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

data class DishIngredientUi(
    val id: UUID = UUID.randomUUID(),
    var name: String = "",
    var caloriesPer100g: String = "",
    var proteinPer100g: String = "",
    var fatsPer100g: String = "",
    var carbsPer100g: String = "",
    var weightInDish: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNutritionDialog(
    entry: NutritionEntry? = null,
    onConfirm: (NutritionEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var date by remember(entry?.date) {
        mutableStateOf(entry?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date()))
    }
    var dishName by remember(entry?.dish?.name) { mutableStateOf(entry?.dish?.name ?: "") }
    var mealType by remember(entry?.mealType) { mutableStateOf(entry?.mealType ?: MealType.OTHER) }
    var portionWeightStr by remember(entry?.portionWeight) { mutableStateOf(entry?.portionWeight?.toString() ?: "") }

    val ingredients = remember { mutableStateListOf<DishIngredientUi>() }

    fun formatOneDecimal(value: Float): String = String.format(Locale.getDefault(), "%.1f", value)

    LaunchedEffect(entry?.id) {
        ingredients.clear()
        if (entry != null) {
            entry.dish.ingredients.forEach { dishIngredient ->
                ingredients.add(
                    DishIngredientUi(
                        id = dishIngredient.id,
                        name = dishIngredient.ingredient.name,
                        caloriesPer100g = formatOneDecimal(dishIngredient.ingredient.caloriesPer100g),
                        proteinPer100g = formatOneDecimal(dishIngredient.ingredient.proteinPer100g),
                        fatsPer100g = formatOneDecimal(dishIngredient.ingredient.fatsPer100g),
                        carbsPer100g = formatOneDecimal(dishIngredient.ingredient.carbsPer100g),
                        weightInDish = dishIngredient.weightInDish.toString()
                    )
                )
            }
        }
        if (ingredients.isEmpty()) {
            ingredients.add(DishIngredientUi())
        }
    }

    // Ошибки
    var dishNameError by remember { mutableStateOf<String?>(null) }
    var ingredientsError by remember { mutableStateOf<String?>(null) }
    var portionWeightError by remember { mutableStateOf<String?>(null) }
    var hardLimitError by remember { mutableStateOf<String?>(null) }

    // Жёсткие рамки
    val WEIGHT_MIN = 1
    val WEIGHT_MAX = 2000
    val PER100_MIN = 0f
    val PER100_MAX = 100f
    val CALORIES_PER100_MAX = 1000f
    val KCAL_MAX = 6000

    val parsedPortionWeight = portionWeightStr.toIntOrNull() ?: 0

    val totalWeightDish = ingredients.sumOf { it.weightInDish.toIntOrNull()?.takeIf { w -> w > 0 } ?: 0 }

    fun limitToOneDecimalInput(raw: String): String {
        val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' }
        val separatorIndex = filtered.indexOfFirst { it == '.' || it == ',' }
        val integerPart = if (separatorIndex == -1) filtered else filtered.substring(0, separatorIndex)
        val decimalPart = if (separatorIndex == -1) "" else filtered.substring(separatorIndex + 1).filter { it.isDigit() }.take(1)
        val separator = if (separatorIndex == -1 || (integerPart.isEmpty() && decimalPart.isEmpty())) "" else filtered[separatorIndex].toString()
        val safeInteger = (integerPart.takeIf { it.isNotEmpty() } ?: if (separator.isNotEmpty()) "0" else "").take(4)

        return buildString {
            append(safeInteger)
            if (separator.isNotEmpty()) {
                append(separator)
                append(decimalPart)
            }
        }
    }

    fun parseMacro(value: String): Float {
        val parsed = value.replace(',', '.').toFloatOrNull()?.coerceAtLeast(0f) ?: 0f
        return ((parsed * 10f).roundToInt()) / 10f
    }

    fun weightedMacro(selector: (DishIngredientUi) -> Float): Float {
        if (totalWeightDish == 0) return 0f
        val numerator = ingredients.sumOf { ingredient ->
            val weight = ingredient.weightInDish.toIntOrNull()?.takeIf { it > 0 } ?: 0
            val macro = selector(ingredient)
            (macro * weight).toDouble()
        }
        val per100g = (numerator / totalWeightDish).toFloat()
        return ((per100g * 10f).roundToInt()) / 10f
    }

    val dishCaloriesPer100g = weightedMacro { parseMacro(it.caloriesPer100g) }
    val dishProteinPer100g = weightedMacro { parseMacro(it.proteinPer100g) }
    val dishFatsPer100g = weightedMacro { parseMacro(it.fatsPer100g) }
    val dishCarbsPer100g = weightedMacro { parseMacro(it.carbsPer100g) }

    val portionCalories = (dishCaloriesPer100g * parsedPortionWeight / 100f).roundToInt()
    val portionProtein = (dishProteinPer100g * parsedPortionWeight / 100f).roundToInt()
    val portionFats = (dishFatsPer100g * parsedPortionWeight / 100f).roundToInt()
    val portionCarbs = (dishCarbsPer100g * parsedPortionWeight / 100f).roundToInt()

    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        val parts = date.split("-").map { it.toInt() }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                date = String.format("%04d-%02d-%02d", y, m + 1, d)
                showDatePicker = false
            },
            parts[0], parts[1] - 1, parts[2]
        ).apply { setOnDismissListener { showDatePicker = false } }.show()
    }

    val canSave = dishName.isNotBlank() && ingredients.isNotEmpty() && parsedPortionWeight in WEIGHT_MIN..WEIGHT_MAX &&
            totalWeightDish > 0 && portionCalories <= KCAL_MAX

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (entry == null) "Добавить приём" else "Редактировать приём",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    OutlinedTextField(
                        value = dishName,
                        onValueChange = {
                            dishName = it.take(40)
                            dishNameError = null
                        },
                        label = { Text("Название блюда") },
                        supportingText = { dishNameError?.let { msg -> Text(msg, color = MaterialTheme.colorScheme.error) } },
                        isError = dishNameError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = formatDateForDisplay(date),
                        onValueChange = {},
                        label = { Text("Дата") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = "Выбрать дату")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "Тип приёма пищи",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                    )
                    Spacer(Modifier.height(8.dp))

                    val mealTypeLabels = mapOf(
                        MealType.BREAKFAST to "Завтрак",
                        MealType.LUNCH to "Обед",
                        MealType.DINNER to "Ужин",
                        MealType.SNACK to "Перекус",
                        MealType.OTHER to "Другое"
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        mealTypeLabels.forEach { (type, label) ->
                            FilterChip(
                                selected = mealType == type,
                                onClick = { mealType = type },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Ингредиенты блюда",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                itemsIndexed(ingredients, key = { _, item -> item.id }) { index, ingredient ->
                    Surface(
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Ингредиент ${index + 1}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                            )
                            OutlinedTextField(
                                value = ingredient.name,
                                onValueChange = {
                                    ingredients[index] = ingredient.copy(name = it.take(40))
                                    ingredientsError = null
                                },
                                label = { Text("Название") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = ingredient.caloriesPer100g,
                                    onValueChange = { value ->
                                        ingredients[index] = ingredient.copy(
                                            caloriesPer100g = limitToOneDecimalInput(value)
                                        )
                                        ingredientsError = null
                                    },
                                    label = { Text("Калории /100 г") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = ingredient.proteinPer100g,
                                    onValueChange = { value ->
                                        ingredients[index] = ingredient.copy(
                                            proteinPer100g = limitToOneDecimalInput(value)
                                        )
                                        ingredientsError = null
                                    },
                                    label = { Text("Белки /100 г") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = ingredient.fatsPer100g,
                                    onValueChange = { value ->
                                        ingredients[index] = ingredient.copy(
                                            fatsPer100g = limitToOneDecimalInput(value)
                                        )
                                        ingredientsError = null
                                    },
                                    label = { Text("Жиры /100 г") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = ingredient.carbsPer100g,
                                    onValueChange = { value ->
                                        ingredients[index] = ingredient.copy(
                                            carbsPer100g = limitToOneDecimalInput(value)
                                        )
                                        ingredientsError = null
                                    },
                                    label = { Text("Углеводы /100 г") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = ingredient.weightInDish,
                                onValueChange = { value ->
                                    ingredients[index] = ingredient.copy(
                                        weightInDish = value.filter { it.isDigit() }.take(5)
                                    )
                                    ingredientsError = null
                                },
                                label = { Text("Вес в блюде, г") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = { ingredients.remove(ingredient) },
                                    enabled = ingredients.size > 1
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                                    Spacer(Modifier.width(6.dp))
                                    Text("Удалить")
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        ingredients.add(DishIngredientUi())
                        ingredientsError = null
                    }) {
                        Text("Добавить ингредиент")
                    }
                    if (ingredientsError != null) {
                        Text(
                            text = ingredientsError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Блюдо (на 100 г)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (totalWeightDish > 0) {
                                    Text(
                                        text = "$totalWeightDish г всего",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(6.dp))

                            if (totalWeightDish == 0) {
                                Text(
                                    "Добавьте вес ингредиентов, чтобы рассчитать блюдо",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "Калории: ${formatOneDecimal(dishCaloriesPer100g)} ккал",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Б: ${formatOneDecimal(dishProteinPer100g)} г   Ж: ${formatOneDecimal(dishFatsPer100g)} г   У: ${formatOneDecimal(dishCarbsPer100g)} г",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = portionWeightStr,
                        onValueChange = { value ->
                            portionWeightStr = value.filter { it.isDigit() }.take(4)
                            portionWeightError = null
                            hardLimitError = null
                        },
                        label = { Text("Сколько грамм блюда вы съели?") },
                        supportingText = {
                            when {
                                hardLimitError != null -> Text(hardLimitError!!, color = MaterialTheme.colorScheme.error)
                                portionWeightError != null -> Text(portionWeightError!!, color = MaterialTheme.colorScheme.error)
                                parsedPortionWeight > 0 -> Text(
                                    "Порция: ${portionCalories} ккал, $portionProtein г белков, $portionFats г жиров, $portionCarbs г углеводов",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        isError = portionWeightError != null || hardLimitError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    dishNameError = null
                    ingredientsError = null
                    portionWeightError = null
                    hardLimitError = null

                    if (dishName.isBlank()) {
                        dishNameError = "Укажите название блюда"
                        return@TextButton
                    }

                    if (ingredients.isEmpty()) {
                        ingredientsError = "Добавьте хотя бы один ингредиент"
                        return@TextButton
                    }

                    ingredients.forEach { ingredient ->
                        if (ingredient.name.isBlank()) {
                            ingredientsError = "Укажите названия всех ингредиентов"
                            return@TextButton
                        }

                        val weightInDish = ingredient.weightInDish.toIntOrNull()
                        if (weightInDish == null || weightInDish <= 0) {
                            ingredientsError = "Вес ингредиента должен быть больше 0"
                            return@TextButton
                        }

                        fun checkRange(value: String, label: String, min: Float, max: Float): Boolean {
                            val parsed = value.replace(',', '.').toFloatOrNull() ?: 0f
                            if (value.isBlank()) return true
                            if (parsed !in min..max) {
                                ingredientsError = "$label /100 г: $min..$max"
                                return false
                            }
                            return true
                        }

                        if (!checkRange(ingredient.proteinPer100g, "Белки", PER100_MIN, PER100_MAX) ||
                            !checkRange(ingredient.fatsPer100g, "Жиры", PER100_MIN, PER100_MAX) ||
                            !checkRange(ingredient.carbsPer100g, "Углеводы", PER100_MIN, PER100_MAX) ||
                            !checkRange(ingredient.caloriesPer100g, "Калории", PER100_MIN, CALORIES_PER100_MAX)
                        ) {
                            return@TextButton
                        }
                    }

                    if (totalWeightDish <= 0) {
                        ingredientsError = "Общий вес блюда должен быть больше 0"
                        return@TextButton
                    }

                    if (parsedPortionWeight !in WEIGHT_MIN..WEIGHT_MAX) {
                        portionWeightError = "Диапазон $WEIGHT_MIN–$WEIGHT_MAX г"
                        return@TextButton
                    }

                    if (portionCalories > KCAL_MAX) {
                        hardLimitError = "Слишком много калорий (> $KCAL_MAX ккал)"
                        return@TextButton
                    }

                    val dishIngredients = ingredients.map { ingredient ->
                        val ingredientWeight = ingredient.weightInDish.toIntOrNull() ?: 0
                        DishIngredient(
                            id = ingredient.id,
                            ingredient = Ingredient(
                                id = ingredient.id,
                                name = ingredient.name.trim(),
                                caloriesPer100g = parseMacro(ingredient.caloriesPer100g),
                                proteinPer100g = parseMacro(ingredient.proteinPer100g),
                                fatsPer100g = parseMacro(ingredient.fatsPer100g),
                                carbsPer100g = parseMacro(ingredient.carbsPer100g)
                            ),
                            weightInDish = ingredientWeight
                        )
                    }

                    val dish = Dish(
                        id = entry?.dish?.id ?: UUID.randomUUID(),
                        name = dishName.trim(),
                        ingredients = dishIngredients
                    )

                    val newEntry = NutritionEntry(
                        id = entry?.id ?: UUID.randomUUID(),
                        date = date,
                        mealType = mealType,
                        dish = dish,
                        portionWeight = parsedPortionWeight
                    )

                    onConfirm(newEntry)
                },
                enabled = canSave
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

fun formatDateForDisplay(date: String): String =
    date.replace(Regex("(\\d{4})-(\\d{2})-(\\d{2})"), "$3.$2.$1")
