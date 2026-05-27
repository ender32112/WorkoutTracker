package com.example.workouttracker.feature.nutrition.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class GoalUi(
    val goal: Goal,
    val title: String,
    val hint: String,
    val icon: ImageVector,
    val gradient: List<Color>
)

@Composable
fun ProfileDialog(
    currentProfile: NutritionProfile?,
    prefillData: ProfilePrefillData = ProfilePrefillData(),
    onSave: (NutritionProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val resolvedSex = prefillData.sex ?: currentProfile?.sex
    val resolvedAge = prefillData.age ?: currentProfile?.age
    val resolvedHeight = prefillData.heightCm ?: currentProfile?.heightCm
    val resolvedWeight = prefillData.weightKg ?: currentProfile?.weightKg

    val hasResolvedData = resolvedSex != null && resolvedAge != null && resolvedHeight != null && resolvedWeight != null

    var goal by remember(currentProfile) { mutableStateOf(currentProfile?.goal ?: Goal.MAINTAIN_WEIGHT) }
    var generalError by remember { mutableStateOf<String?>(null) }

    var dietCalories by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.calories?.toString().orEmpty()) }
    var dietProtein by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.protein?.toString().orEmpty()) }
    var dietFats by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.fats?.toString().orEmpty()) }
    var dietCarbs by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.carbs?.toString().orEmpty()) }
    var excludeOrLimit by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.excludeOrLimit.orEmpty()) }
    var increase by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.increase.orEmpty()) }
    var additionalRecommendations by remember(currentProfile) { mutableStateOf(currentProfile?.dietSettings?.additionalRecommendations.orEmpty()) }
    var dietError by remember { mutableStateOf<String?>(null) }

    var favoriteText by remember(currentProfile) { mutableStateOf(currentProfile?.favoriteIngredients?.joinToString(", ").orEmpty()) }
    var dislikedText by remember(currentProfile) { mutableStateOf(currentProfile?.dislikedIngredients?.joinToString(", ").orEmpty()) }
    var allergyText by remember(currentProfile) { mutableStateOf(currentProfile?.allergies?.joinToString(", ").orEmpty()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val goalItems = remember {
        listOf(
            GoalUi(
                goal = Goal.LOSE_WEIGHT,
                title = "Похудение",
                hint = "Подходит, если цель - плавно снижать вес без резких ограничений.",
                icon = Icons.Default.LocalFireDepartment,
                gradient = listOf(Color(0xFFFFD180), Color(0xFFFF8A65))
            ),
            GoalUi(
                goal = Goal.MAINTAIN_WEIGHT,
                title = "Поддержание",
                hint = "Оптимально, когда нужно удерживать текущий вес и режим питания.",
                icon = Icons.Default.MonitorWeight,
                gradient = listOf(Color(0xFFB3E5FC), Color(0xFFB2DFDB))
            ),
            GoalUi(
                goal = Goal.GAIN_WEIGHT,
                title = "Набор",
                hint = "Используйте при цели набора веса и тренировках с нагрузкой.",
                icon = Icons.Default.FitnessCenter,
                gradient = listOf(Color(0xFFC5E1A5), Color(0xFF80DEEA))
            ),
            GoalUi(
                goal = Goal.DIET,
                title = "Медицинская диета",
                hint = "Этот режим - для лечебного питания и соблюдения рекомендаций врача.",
                icon = Icons.Default.MedicalServices,
                gradient = listOf(Color(0xFFFFCCBC), Color(0xFFFFAB91))
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Профиль питания",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(bottom = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroPersonalizationCard(
                        goal = goal,
                        sex = resolvedSex,
                        age = resolvedAge,
                        heightCm = resolvedHeight,
                        weightKg = resolvedWeight,
                        hasResolvedData = hasResolvedData
                    )

                    if (!hasResolvedData) {
                        InlineWarning("Не удалось подтянуть данные из основного профиля. Сначала заполните возраст, рост и вес в общем профиле.")
                    }

                    SectionBlock(
                        title = "Цель питания",
                        subtitle = "Выберите режим. Подсказка по цели открывается через «?»."
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            goalItems.forEach { item ->
                                GoalSelectorCard(
                                    item = item,
                                    selected = goal == item.goal,
                                    onSelect = {
                                        goal = item.goal
                                        generalError = null
                                        if (goal != Goal.DIET) dietError = null
                                    },
                                    onShowHint = {
                                        scope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(
                                                message = item.hint,
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (goal == Goal.DIET) {
                        SectionBlock(
                            title = "Медицинская диета",
                            subtitle = "Заполните параметры вручную согласно назначению врача"
                        ) {

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                MacroNumberField(
                                    value = dietCalories,
                                    onChange = { dietCalories = it.take(4); dietError = null },
                                    label = "Ккал",
                                    modifier = Modifier.weight(1f)
                                )
                                MacroNumberField(
                                    value = dietProtein,
                                    onChange = { dietProtein = it.take(3); dietError = null },
                                    label = "Белки, г",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                MacroNumberField(
                                    value = dietFats,
                                    onChange = { dietFats = it.take(3); dietError = null },
                                    label = "Жиры, г",
                                    modifier = Modifier.weight(1f)
                                )
                                MacroNumberField(
                                    value = dietCarbs,
                                    onChange = { dietCarbs = it.take(3); dietError = null },
                                    label = "Углеводы, г",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            MultiLineField(
                                value = excludeOrLimit,
                                onChange = { excludeOrLimit = it },
                                label = "Что исключить / ограничить",
                                placeholder = "Например: соль, сахар, глютен"
                            )
                            MultiLineField(
                                value = increase,
                                onChange = { increase = it },
                                label = "Что увеличить",
                                placeholder = "Например: овощи, вода, клетчатка"
                            )
                            MultiLineField(
                                value = additionalRecommendations,
                                onChange = { additionalRecommendations = it },
                                label = "Комментарий врача",
                                placeholder = "Например: дробное питание 5 раз в день"
                            )

                            dietError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    SectionBlock(
                        title = "Предпочтения и ограничения",
                        subtitle = "Данные используются для генерации и замены блюд"
                    ) {
                        MultiLineField(
                            value = favoriteText,
                            onChange = { favoriteText = it },
                            label = "Любимые продукты",
                            placeholder = "Например: индейка, рис, творог"
                        )
                        MultiLineField(
                            value = dislikedText,
                            onChange = { dislikedText = it },
                            label = "Нелюбимые продукты",
                            placeholder = "Например: печень, фасоль"
                        )
                        MultiLineField(
                            value = allergyText,
                            onChange = { allergyText = it },
                            label = "Аллергии / мед. ограничения",
                            placeholder = "Например: орехи, лактоза"
                        )
                        Text(
                            text = "Вводите значения через запятую.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    generalError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val sex = resolvedSex
                val age = resolvedAge
                val height = resolvedHeight
                val weight = resolvedWeight

                if (sex == null || age == null || height == null || weight == null) {
                    generalError = "Нет данных из основного профиля (пол, возраст, рост, вес)."
                    return@Button
                }

                val dietSettings = if (goal == Goal.DIET) {
                    val calories = dietCalories.toIntOrNull()
                    val protein = dietProtein.toIntOrNull()
                    val fats = dietFats.toIntOrNull()
                    val carbs = dietCarbs.toIntOrNull()

                    when {
                        calories == null || protein == null || fats == null || carbs == null -> {
                            dietError = "Для медицинской диеты заполните КБЖУ."
                            null
                        }
                        calories !in 800..5000 -> {
                            dietError = "Калории должны быть в диапазоне 800-5000."
                            null
                        }
                        protein !in 20..400 || fats !in 15..250 || carbs !in 15..700 -> {
                            dietError = "Проверьте допустимые диапазоны Б/Ж/У."
                            null
                        }
                        else -> {
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
                    }
                } else {
                    dietError = null
                    null
                }

                if (goal == Goal.DIET && dietSettings == null) return@Button

                generalError = null
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
            }) {
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
private fun HeroPersonalizationCard(
    goal: Goal,
    sex: Sex?,
    age: Int?,
    heightCm: Int?,
    weightKg: Float?,
    hasResolvedData: Boolean
) {
    val gradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        )
    )

    val bmi = if (heightCm != null && weightKg != null && heightCm > 0) {
        val m = heightCm / 100f
        weightKg / (m * m)
    } else null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .background(gradient, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Персонализация плана",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (hasResolvedData) "Данные подтянуты из профиля" else "Нужны данные основного профиля",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Цель: ${goal.toReadableLabel()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (hasResolvedData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Параметры синхронизированы", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricPill("Пол", sex?.toReadableLabel() ?: "—")
                MetricPill("Возраст", age?.toString() ?: "—")
                MetricPill("Рост", heightCm?.let { "$it см" } ?: "—")
                MetricPill("Вес", weightKg?.formatWeightUi() ?: "—")
                MetricPill("BMI", bmi?.let { "%.1f".format(it) } ?: "—")
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Box(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionBlock(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.28f)
                    .heightIn(min = 3.dp, max = 3.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(999.dp))
            )
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun GoalSelectorCard(
    item: GoalUi,
    selected: Boolean,
    onSelect: () -> Unit,
    onShowHint: () -> Unit
) {
    val brush = if (selected) {
        Brush.horizontalGradient(item.gradient)
    } else {
        Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface))
    }

    val titleColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(14.dp)
            )
            .background(brush, RoundedCornerShape(14.dp))
            .clickable(onClick = onSelect)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    IconButton(
                        onClick = onShowHint,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Пояснение", tint = titleColor)
                    }
                }
            }

            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun InlineWarning(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun MacroNumberField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit)) },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun MultiLineField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun Goal.toReadableLabel(): String = when (this) {
    Goal.LOSE_WEIGHT -> "Похудение"
    Goal.MAINTAIN_WEIGHT -> "Поддержание"
    Goal.GAIN_WEIGHT -> "Набор"
    Goal.DIET -> "Медицинская диета"
}

private fun Sex.toReadableLabel(): String = when (this) {
    Sex.MALE -> "Мужчина"
    Sex.FEMALE -> "Женщина"
}

private fun Float.formatWeightUi(): String {
    val rounded = (this * 10f).roundToInt() / 10f
    val text = if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
    return "$text кг"
}

private fun String.toListFromCsv(): List<String> =
    split(',').map { it.trim() }.filter { it.isNotEmpty() }

