package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    currentProfile: NutritionProfile?,
    onSave: (NutritionProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var sex by remember(currentProfile) { mutableStateOf(currentProfile?.sex ?: Sex.MALE) }
    var ageText by remember(currentProfile) { mutableStateOf(currentProfile?.age?.toString().orEmpty()) }
    var heightText by remember(currentProfile) { mutableStateOf(currentProfile?.heightCm?.toString().orEmpty()) }
    var weightText by remember(currentProfile) { mutableStateOf(currentProfile?.weightKg?.toString().orEmpty()) }
    var goal by remember(currentProfile) { mutableStateOf(currentProfile?.goal ?: Goal.MAINTAIN_WEIGHT) }

    var dietCalories by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.calories?.toString().orEmpty()) }
    var dietProtein by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.protein?.toString().orEmpty()) }
    var dietFats by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.fats?.toString().orEmpty()) }
    var dietCarbs by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.carbs?.toString().orEmpty()) }
    var excludeOrLimit by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.excludeOrLimit.orEmpty()) }
    var increase by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.increase.orEmpty()) }
    var additionalRecommendations by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.additionalRecommendations.orEmpty()) }

    var favoriteText by remember(currentProfile) { mutableStateOf(currentProfile?.favoriteIngredients?.joinToString(", ").orEmpty()) }
    var dislikedText by remember(currentProfile) { mutableStateOf(currentProfile?.dislikedIngredients?.joinToString(", ").orEmpty()) }
    var allergyText by remember(currentProfile) { mutableStateOf(currentProfile?.allergies?.joinToString(", ").orEmpty()) }

    var ageError by remember { mutableStateOf<String?>(null) }
    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var dietError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val age = ageText.toIntOrNull()
                val height = heightText.toIntOrNull()
                val weight = weightText.toFloatOrNullComma()

                ageError = when {
                    age == null -> "Введите возраст"
                    age !in 5..100 -> "Возраст должен быть 5-100"
                    else -> null
                }
                heightError = when {
                    height == null -> "Введите рост"
                    height !in 100..250 -> "Рост должен быть 100-250"
                    else -> null
                }
                weightError = when {
                    weight == null -> "Введите вес"
                    weight <= 20f || weight >= 300f -> "Вес должен быть 20-300"
                    else -> null
                }

                val dietSettings = if (goal == Goal.DIET) {
                    val calories = dietCalories.toIntOrNull()
                    val protein = dietProtein.toIntOrNull()
                    val fats = dietFats.toIntOrNull()
                    val carbs = dietCarbs.toIntOrNull()
                    if (calories == null || protein == null || fats == null || carbs == null) {
                        dietError = "Для режима Диета заполните КБЖУ"
                        null
                    } else {
                        dietError = null
                        DietSettings(
                            calories = calories,
                            protein = protein,
                            fats = fats,
                            carbs = carbs,
                            excludeOrLimit = excludeOrLimit.trim(),
                            increase = increase.trim(),
                            additionalRecommendations = additionalRecommendations.trim()
                        )
                    }
                } else {
                    dietError = null
                    null
                }

                val hasErrors = listOf(ageError, heightError, weightError, dietError).any { it != null }
                if (!hasErrors && age != null && height != null && weight != null) {
                    onSave(
                        NutritionProfile(
                            sex = sex,
                            age = age,
                            heightCm = height,
                            weightKg = weight,
                            goal = goal,
                            dietSettings = dietSettings,
                            favoriteIngredients = favoriteText.toListFromCsv(),
                            dislikedIngredients = dislikedText.toListFromCsv(),
                            allergies = allergyText.toListFromCsv()
                        )
                    )
                    onDismiss()
                }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = { Text("Профиль питания", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Text("Пол", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SexChip("Мужчина", sex == Sex.MALE, { sex = Sex.MALE }, Icons.Default.Male)
                    SexChip("Женщина", sex == Sex.FEMALE, { sex = Sex.FEMALE }, Icons.Default.Female)
                }

                NumberField(ageText, { ageText = it; ageError = null }, "Возраст (лет)", ageError)
                NumberField(heightText, { heightText = it; heightError = null }, "Рост (см)", heightError)
                DecimalField(weightText, { weightText = it; weightError = null }, "Вес (кг)", weightError)

                Text("Цель", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalChip("Похудение", goal == Goal.LOSE_WEIGHT) { goal = Goal.LOSE_WEIGHT }
                    GoalChip("Поддержание", goal == Goal.MAINTAIN_WEIGHT) { goal = Goal.MAINTAIN_WEIGHT }
                    GoalChip("Набор", goal == Goal.GAIN_WEIGHT) { goal = Goal.GAIN_WEIGHT }
                    GoalChip("Диета", goal == Goal.DIET, Icons.Default.LocalFireDepartment) { goal = Goal.DIET }
                }

                if (goal == Goal.DIET) {
                    Text("Параметры диеты", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        NumberField(dietCalories, { dietCalories = it.filter(Char::isDigit).take(4) }, "Ккал", null, Modifier.weight(1f))
                        NumberField(dietProtein, { dietProtein = it.filter(Char::isDigit).take(3) }, "Б", null, Modifier.weight(1f))
                        NumberField(dietFats, { dietFats = it.filter(Char::isDigit).take(3) }, "Ж", null, Modifier.weight(1f))
                        NumberField(dietCarbs, { dietCarbs = it.filter(Char::isDigit).take(3) }, "У", null, Modifier.weight(1f))
                    }
                    OutlinedTextField(value = excludeOrLimit, onValueChange = { excludeOrLimit = it }, label = { Text("Исключить / ограничить") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(value = increase, onValueChange = { increase = it }, label = { Text("Увеличить") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(value = additionalRecommendations, onValueChange = { additionalRecommendations = it }, label = { Text("Дополнительные рекомендации") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    dietError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }

                OutlinedTextField(value = favoriteText, onValueChange = { favoriteText = it }, label = { Text("Любимые продукты (через запятую)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = dislikedText, onValueChange = { dislikedText = it }, label = { Text("Нелюбимые продукты (через запятую)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = allergyText, onValueChange = { allergyText = it }, label = { Text("Аллергии (через запятую)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        }
    )
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, error: String?, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit)) },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        isError = error != null,
        supportingText = { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun DecimalField(value: String, onChange: (String) -> Unit, label: String, error: String?, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
        isError = error != null,
        supportingText = { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun SexChip(label: String, selected: Boolean, onClick: () -> Unit, leadingIcon: ImageVector) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun GoalChip(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, modifier = Modifier.padding(end = 2.dp)) } },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
        )
    )
}

private fun String.toFloatOrNullComma(): Float? = this.replace(',', '.').toFloatOrNull()
private fun String.toListFromCsv(): List<String> = this.split(',').map { it.trim() }.filter { it.isNotEmpty() }
