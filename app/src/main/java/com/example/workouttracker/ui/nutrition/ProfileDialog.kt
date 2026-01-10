package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    currentProfile: NutritionProfile?,
    onSave: (NutritionProfile) -> Unit,
    onSyncFromMainProfile: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var sex by remember(currentProfile) { mutableStateOf(currentProfile?.sex ?: Sex.MALE) }
    var ageText by remember(currentProfile) { mutableStateOf(currentProfile?.age?.toString().orEmpty()) }
    var heightText by remember(currentProfile) { mutableStateOf(currentProfile?.heightCm?.toString().orEmpty()) }
    var weightText by remember(currentProfile) { mutableStateOf(currentProfile?.weightKg?.toString().orEmpty()) }
    var goal by remember(currentProfile) { mutableStateOf(currentProfile?.goal ?: Goal.MAINTAIN_WEIGHT) }
    var activityLevel by remember(currentProfile) {
        mutableStateOf(currentProfile?.activityLevel ?: ActivityLevel.MODERATE)
    }
    var clinicalDiet by remember(currentProfile) {
        mutableStateOf(currentProfile?.clinicalDiet ?: ClinicalDiet.NONE)
    }
    var sodiumText by remember(currentProfile) {
        mutableStateOf(currentProfile?.dietConstraints?.maxSodiumMgPerDay?.toString().orEmpty())
    }
    var potassiumText by remember(currentProfile) {
        mutableStateOf(currentProfile?.dietConstraints?.maxPotassiumMgPerDay?.toString().orEmpty())
    }
    var phosphorusText by remember(currentProfile) {
        mutableStateOf(currentProfile?.dietConstraints?.maxPhosphorusMgPerDay?.toString().orEmpty())
    }
    var proteinText by remember(currentProfile) {
        mutableStateOf(currentProfile?.dietConstraints?.proteinGrPerDay?.toString().orEmpty())
    }
    var strictDiet by remember(currentProfile) {
        mutableStateOf(currentProfile?.dietConstraints?.strict ?: true)
    }
    var favoriteText by remember(currentProfile) {
        mutableStateOf(currentProfile?.favoriteIngredients?.joinToString(", ").orEmpty())
    }
    var dislikedText by remember(currentProfile) {
        mutableStateOf(currentProfile?.dislikedIngredients?.joinToString(", ").orEmpty())
    }
    var allergyText by remember(currentProfile) {
        mutableStateOf(currentProfile?.allergies?.joinToString(", ").orEmpty())
    }

    var ageError by remember { mutableStateOf<String?>(null) }
    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var mainFieldsEditable by remember(currentProfile) { mutableStateOf(currentProfile == null) }
    var showSyncWarning by remember { mutableStateOf(false) }

    if (showSyncWarning) {
        AlertDialog(
            onDismissRequest = { showSyncWarning = false },
            title = { Text("Синхронизация профиля") },
            text = { Text("Эти данные отличаются от основного профиля. Обновить основной профиль?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSyncFromMainProfile()
                        mainFieldsEditable = false
                        showSyncWarning = false
                    }
                ) {
                    Text("Обновить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mainFieldsEditable = true
                        showSyncWarning = false
                    }
                ) {
                    Text("Оставить локально")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val age = ageText.toIntOrNull()
                    val height = heightText.toIntOrNull()
                    val weight = weightText.toFloatOrNullComma()
                    val sodium = sodiumText.toIntOrNull()
                    val potassium = potassiumText.toIntOrNull()
                    val phosphorus = phosphorusText.toIntOrNull()
                    val protein = proteinText.toIntOrNull()

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

                    val hasErrors = listOf(ageError, heightError, weightError).any { it != null }
                    if (!hasErrors && age != null && height != null && weight != null) {
                        val constraints = if (
                            listOf(sodium, potassium, phosphorus, protein).any { it != null } || !strictDiet
                        ) {
                            DietConstraints(
                                maxSodiumMgPerDay = sodium,
                                maxPotassiumMgPerDay = potassium,
                                maxPhosphorusMgPerDay = phosphorus,
                                proteinGrPerDay = protein,
                                strict = strictDiet
                            )
                        } else {
                            null
                        }
                        val profile = NutritionProfile(
                            sex = sex,
                            age = age,
                            heightCm = height,
                            weightKg = weight,
                            goal = goal,
                            activityLevel = activityLevel,
                            clinicalDiet = clinicalDiet,
                            dietConstraints = constraints,
                            favoriteIngredients = favoriteText.toListFromCsv(),
                            dislikedIngredients = dislikedText.toListFromCsv(),
                            allergies = allergyText.toListFromCsv()
                        )
                        onSave(profile)
                        onDismiss()
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        title = { Text("Профиль питания", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Пол", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SexChip(
                        label = "Мужчина",
                        selected = sex == Sex.MALE,
                        onClick = {
                            if (mainFieldsEditable) {
                                sex = Sex.MALE
                            } else {
                                showSyncWarning = true
                            }
                        },
                        leadingIcon = Icons.Default.Male
                    )
                    SexChip(
                        label = "Женщина",
                        selected = sex == Sex.FEMALE,
                        onClick = {
                            if (mainFieldsEditable) {
                                sex = Sex.FEMALE
                            } else {
                                showSyncWarning = true
                            }
                        },
                        leadingIcon = Icons.Default.Female
                    )
                }

                OutlinedTextField(
                    value = ageText,
                    onValueChange = { if (mainFieldsEditable) { ageText = it; ageError = null } },
                    label = { Text("Возраст (лет)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = ageError != null,
                    supportingText = { ageError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    readOnly = !mainFieldsEditable,
                    trailingIcon = {
                        if (!mainFieldsEditable) {
                            IconButton(onClick = { showSyncWarning = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { if (mainFieldsEditable) { heightText = it; heightError = null } },
                    label = { Text("Рост (см)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = heightError != null,
                    supportingText = { heightError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    readOnly = !mainFieldsEditable,
                    trailingIcon = {
                        if (!mainFieldsEditable) {
                            IconButton(onClick = { showSyncWarning = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { if (mainFieldsEditable) { weightText = it; weightError = null } },
                    label = { Text("Вес (кг)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = weightError != null,
                    supportingText = { weightError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    readOnly = !mainFieldsEditable,
                    trailingIcon = {
                        if (!mainFieldsEditable) {
                            IconButton(onClick = { showSyncWarning = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Цель", style = MaterialTheme.typography.titleSmall)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoalChip(
                        label = "Похудение",
                        selected = goal == Goal.LOSE_WEIGHT,
                        onClick = {
                            if (mainFieldsEditable) {
                                goal = Goal.LOSE_WEIGHT
                            } else {
                                showSyncWarning = true
                            }
                        }
                    )
                    GoalChip(
                        label = "Поддержание",
                        selected = goal == Goal.MAINTAIN_WEIGHT,
                        onClick = {
                            if (mainFieldsEditable) {
                                goal = Goal.MAINTAIN_WEIGHT
                            } else {
                                showSyncWarning = true
                            }
                        }
                    )
                    GoalChip(
                        label = "Набор",
                        selected = goal == Goal.GAIN_WEIGHT,
                        onClick = {
                            if (mainFieldsEditable) {
                                goal = Goal.GAIN_WEIGHT
                            } else {
                                showSyncWarning = true
                            }
                        }
                    )
                }

                Text("Уровень активности", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActivityChip(
                        label = "Сидячий",
                        selected = activityLevel == ActivityLevel.SEDENTARY,
                        onClick = { activityLevel = ActivityLevel.SEDENTARY }
                    )
                    ActivityChip(
                        label = "Лёгкий",
                        selected = activityLevel == ActivityLevel.LIGHT,
                        onClick = { activityLevel = ActivityLevel.LIGHT }
                    )
                    ActivityChip(
                        label = "Умеренный",
                        selected = activityLevel == ActivityLevel.MODERATE,
                        onClick = { activityLevel = ActivityLevel.MODERATE }
                    )
                    ActivityChip(
                        label = "Активный",
                        selected = activityLevel == ActivityLevel.ACTIVE,
                        onClick = { activityLevel = ActivityLevel.ACTIVE }
                    )
                    ActivityChip(
                        label = "Очень активный",
                        selected = activityLevel == ActivityLevel.VERY_ACTIVE,
                        onClick = { activityLevel = ActivityLevel.VERY_ACTIVE }
                    )
                }

                Text("Клиническая диета", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClinicalDietChip(
                        label = "Нет",
                        selected = clinicalDiet == ClinicalDiet.NONE,
                        onClick = { clinicalDiet = ClinicalDiet.NONE }
                    )
                    ClinicalDietChip(
                        label = "Диабет",
                        selected = clinicalDiet == ClinicalDiet.DIABETES,
                        onClick = { clinicalDiet = ClinicalDiet.DIABETES }
                    )
                    ClinicalDietChip(
                        label = "Почечная",
                        selected = clinicalDiet == ClinicalDiet.RENAL,
                        onClick = { clinicalDiet = ClinicalDiet.RENAL }
                    )
                    ClinicalDietChip(
                        label = "Сердце",
                        selected = clinicalDiet == ClinicalDiet.CARDIAC,
                        onClick = { clinicalDiet = ClinicalDiet.CARDIAC }
                    )
                    ClinicalDietChip(
                        label = "ЖКТ",
                        selected = clinicalDiet == ClinicalDiet.GASTROINTESTINAL,
                        onClick = { clinicalDiet = ClinicalDiet.GASTROINTESTINAL }
                    )
                    ClinicalDietChip(
                        label = "Другая",
                        selected = clinicalDiet == ClinicalDiet.CUSTOM,
                        onClick = { clinicalDiet = ClinicalDiet.CUSTOM }
                    )
                }

                Text("Назначение врача (скелет)", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = sodiumText,
                    onValueChange = { sodiumText = it },
                    label = { Text("Соль, мг/сутки") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = potassiumText,
                    onValueChange = { potassiumText = it },
                    label = { Text("Калий, мг/сутки") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phosphorusText,
                    onValueChange = { phosphorusText = it },
                    label = { Text("Фосфор, мг/сутки") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proteinText,
                    onValueChange = { proteinText = it },
                    label = { Text("Белок, г/сутки") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = strictDiet,
                        onCheckedChange = { strictDiet = it }
                    )
                    Text("Строго соблюдать")
                }

                OutlinedTextField(
                    value = favoriteText,
                    onValueChange = { favoriteText = it },
                    label = { Text("Любимые продукты (через запятую)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = dislikedText,
                    onValueChange = { dislikedText = it },
                    label = { Text("Нелюбимые продукты (через запятую)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = allergyText,
                    onValueChange = { allergyText = it },
                    label = { Text("Аллергии (через запятую)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }
    )
}

@Composable
private fun SexChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingIcon: ImageVector
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun GoalChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun ActivityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun ClinicalDietChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    )
}

private fun String.toFloatOrNullComma(): Float? {
    return this.replace(',', '.').toFloatOrNull()
}

private fun String.toListFromCsv(): List<String> {
    return this.split(',').map { it.trim() }.filter { it.isNotEmpty() }
}
