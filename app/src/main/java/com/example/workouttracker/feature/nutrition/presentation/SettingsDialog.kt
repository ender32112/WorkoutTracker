package com.example.workouttracker.feature.nutrition.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun SettingsDialog(
    currentNorm: Map<String, Int>,
    recommendedNorm: Norm?,
    onSave: (Map<String, Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultNorm = Norm(
        calories = 2500,
        protein = 120,
        fats = 80,
        carbs = 300
    )
    val currentNormValues = Norm(
        calories = currentNorm["calories"] ?: 2500,
        protein = currentNorm["protein"] ?: 120,
        fats = currentNorm["fats"] ?: 80,
        carbs = currentNorm["carbs"] ?: 300
    )
    val effectiveRecommendedNorm = recommendedNorm ?: defaultNorm

    var calories by remember { mutableStateOf(currentNormValues.calories.toString()) }
    var protein by remember { mutableStateOf(currentNormValues.protein.toString()) }
    var carbs by remember { mutableStateOf(currentNormValues.carbs.toString()) }
    var fats by remember { mutableStateOf(currentNormValues.fats.toString()) }

    var calErr by remember { mutableStateOf<String?>(null) }
    var pErr by remember { mutableStateOf<String?>(null) }
    var cErr by remember { mutableStateOf<String?>(null) }
    var fErr by remember { mutableStateOf<String?>(null) }

    var warningMessage by remember { mutableStateOf<String?>(null) }
    var pendingSaveNorm by remember { mutableStateOf<Map<String, Int>?>(null) }

    val calMin = 800
    val calMax = 6000
    val pMin = 20
    val pMax = 400
    val cMin = 50
    val cMax = 800
    val fMin = 20
    val fMax = 300
    val deviationWarningPercent = 20f
    val macroCaloriesWarningPercent = 15f

    fun Int.inRange(min: Int, max: Int) = this in min..max
    fun clearTransientState() {
        calErr = null
        pErr = null
        cErr = null
        fErr = null
        warningMessage = null
        pendingSaveNorm = null
    }

    val canSubmit = calories.isNotBlank() && protein.isNotBlank() && carbs.isNotBlank() && fats.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Нормы КБЖУ") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NormHeroCard(
                    currentNorm = currentNormValues,
                    recommendedNorm = recommendedNorm
                )

                SectionBlock(
                    title = "Ваши нормы",
                    subtitle = "Пользовательская норма приоритетна, но лучше держаться рядом с рекомендацией"
                ) {
                    MacroInput(
                        value = calories,
                        onValueChange = {
                            calories = it.filter(Char::isDigit).take(4)
                            clearTransientState()
                        },
                        label = "Калории (ккал)",
                        error = calErr,
                        placeholder = effectiveRecommendedNorm.calories.toString(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MacroInput(
                            value = protein,
                            onValueChange = {
                                protein = it.filter(Char::isDigit).take(3)
                                clearTransientState()
                            },
                            label = "Белки (г)",
                            error = pErr,
                            placeholder = effectiveRecommendedNorm.protein.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        MacroInput(
                            value = fats,
                            onValueChange = {
                                fats = it.filter(Char::isDigit).take(3)
                                clearTransientState()
                            },
                            label = "Жиры (г)",
                            error = fErr,
                            placeholder = effectiveRecommendedNorm.fats.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    MacroInput(
                        value = carbs,
                        onValueChange = {
                            carbs = it.filter(Char::isDigit).take(3)
                            clearTransientState()
                        },
                        label = "Углеводы (г)",
                        error = cErr,
                        placeholder = effectiveRecommendedNorm.carbs.toString(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (recommendedNorm != null) {
                        TextButton(
                            onClick = {
                                calories = recommendedNorm.calories.toString()
                                protein = recommendedNorm.protein.toString()
                                fats = recommendedNorm.fats.toString()
                                carbs = recommendedNorm.carbs.toString()
                                clearTransientState()
                            }
                        ) {
                            Text("Подставить рекомендацию")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    var ok = true
                    val cal = calories.toIntOrNull() ?: run { calErr = "Число"; ok = false; 0 }
                    val pr = protein.toIntOrNull() ?: run { pErr = "Число"; ok = false; 0 }
                    val cr = carbs.toIntOrNull() ?: run { cErr = "Число"; ok = false; 0 }
                    val ft = fats.toIntOrNull() ?: run { fErr = "Число"; ok = false; 0 }

                    if (ok) {
                        if (!cal.inRange(calMin, calMax)) { calErr = "$calMin-$calMax"; ok = false }
                        if (!pr.inRange(pMin, pMax)) { pErr = "$pMin-$pMax"; ok = false }
                        if (!cr.inRange(cMin, cMax)) { cErr = "$cMin-$cMax"; ok = false }
                        if (!ft.inRange(fMin, fMax)) { fErr = "$fMin-$fMax"; ok = false }
                    }
                    if (!ok) return@TextButton

                    val candidate = mapOf(
                        "calories" to cal,
                        "protein" to pr,
                        "carbs" to cr,
                        "fats" to ft
                    )

                    val impliedCalories = pr * 4 + cr * 4 + ft * 9
                    val macroCaloriesDiff = abs(cal - impliedCalories) * 100f / cal.coerceAtLeast(1)
                    val warnings = buildList {
                        if (macroCaloriesDiff > macroCaloriesWarningPercent) {
                            add(
                                "Калории не совпадают с КБЖУ: по макросам выходит около $impliedCalories ккал."
                            )
                        }

                        recommendedNorm?.let { recommended ->
                            val calDiff = abs(cal - recommended.calories) * 100f / recommended.calories.coerceAtLeast(1)
                            val proteinDiff = abs(pr - recommended.protein) * 100f / recommended.protein.coerceAtLeast(1)
                            val fatsDiff = abs(ft - recommended.fats) * 100f / recommended.fats.coerceAtLeast(1)
                            val carbsDiff = abs(cr - recommended.carbs) * 100f / recommended.carbs.coerceAtLeast(1)

                            if (calDiff > deviationWarningPercent) add("Калории отличаются от рекомендации на ${calDiff.toInt()}%")
                            if (proteinDiff > deviationWarningPercent) add("Белки отличаются от рекомендации на ${proteinDiff.toInt()}%")
                            if (fatsDiff > deviationWarningPercent) add("Жиры отличаются от рекомендации на ${fatsDiff.toInt()}%")
                            if (carbsDiff > deviationWarningPercent) add("Углеводы отличаются от рекомендации на ${carbsDiff.toInt()}%")
                        }
                    }

                    if (warnings.isNotEmpty()) {
                        warningMessage =
                            "Проверьте новую норму перед сохранением:\n" +
                                warnings.joinToString(separator = "\n") +
                                "\n\nПодтвердите сохранение."
                        pendingSaveNorm = candidate
                        return@TextButton
                    }

                    onSave(candidate)
                    clearTransientState()
                },
                enabled = canSubmit
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

    if (warningMessage != null && pendingSaveNorm != null) {
        AlertDialog(
            onDismissRequest = {
                warningMessage = null
                pendingSaveNorm = null
            },
            title = { Text("Подтверждение изменений") },
            text = { Text(warningMessage!!) },
            confirmButton = {
                TextButton(onClick = {
                    onSave(pendingSaveNorm!!)
                    warningMessage = null
                    pendingSaveNorm = null
                }) {
                    Text("Сохранить всё равно")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    warningMessage = null
                    pendingSaveNorm = null
                }) {
                    Text("Вернуться")
                }
            }
        )
    }
}

@Composable
private fun NormHeroCard(
    currentNorm: Norm,
    recommendedNorm: Norm?
) {
    val gradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.84f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .background(gradient, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Сверка норм КБЖУ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Текущая пользовательская норма", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NormStatPill("Ккал", currentNorm.calories.toString(), Modifier.weight(1f))
                NormStatPill("Б", currentNorm.protein.toString(), Modifier.weight(1f))
                NormStatPill("Ж", currentNorm.fats.toString(), Modifier.weight(1f))
                NormStatPill("У", currentNorm.carbs.toString(), Modifier.weight(1f))
            }
            if (recommendedNorm != null) {
                Text("Рекомендуемая норма", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    NormStatPill("Ккал", recommendedNorm.calories.toString(), Modifier.weight(1f))
                    NormStatPill("Б", recommendedNorm.protein.toString(), Modifier.weight(1f))
                    NormStatPill("Ж", recommendedNorm.fats.toString(), Modifier.weight(1f))
                    NormStatPill("У", recommendedNorm.carbs.toString(), Modifier.weight(1f))
                }
            } else {
                Text(
                    text = "Рекомендация появится после заполнения профиля питания.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NormStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
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
private fun SectionBlock(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val sectionGradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .background(sectionGradient, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun MacroInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    placeholder: String?,
    modifier: Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = {
            if (!placeholder.isNullOrBlank()) {
                Text(placeholder)
            }
        },
        isError = error != null,
        supportingText = {
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

