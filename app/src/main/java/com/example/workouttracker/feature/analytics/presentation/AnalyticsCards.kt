package com.example.workouttracker.feature.analytics.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workouttracker.feature.analytics.runtime.StepDataSource
import com.example.workouttracker.feature.nutrition.presentation.NutritionEntry
import com.example.workouttracker.feature.training.presentation.ExerciseEntry
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun StepsCardPretty(
    steps: Long,
    goal: Int,
    stepDataSource: StepDataSource,
    hasPermission: Boolean,
    onRequest: () -> Unit,
    onLongPressEdit: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val progress = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val accent = MaterialTheme.colorScheme.primary
    val sourceLabel = when (stepDataSource) {
        StepDataSource.DEVICE_SENSOR -> "Датчик устройства"
        StepDataSource.HEALTH_CONNECT -> "Health Connect"
    }
    val statusText = when {
        !hasPermission -> "Разрешение ещё не выдано"
        steps >= goal && goal > 0 -> "Цель на сегодня закрыта"
        goal > 0 -> "До цели осталось ${(goal - steps).coerceAtLeast(0)}"
        else -> "Цель не задана"
    }

    ElevatedCard(
        modifier = Modifier.combinedClickable(onClick = onHistoryClick, onLongClick = onLongPressEdit),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(999.dp), color = accent.copy(alpha = 0.12f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = accent)
                            Text("Шаги сегодня", color = accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        formatCount(steps),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill("Цель", formatCount(goal.toLong()))
                        MetricPill("Источник", sourceLabel)
                    }
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(78.dp)) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = statusText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            if (!hasPermission) {
                FilledTonalButton(onClick = onRequest) {
                    Text("Разрешить доступ к шагам")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onHistoryClick) {
                        Text("История")
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherCardPretty(
    city: String,
    weather: String,
    subtitle: String?
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Погода", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(city.ifBlank { "Город не выбран" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                weather.ifBlank { "Погодные данные появятся после обновления." },
                style = MaterialTheme.typography.bodyLarge
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun NutritionTodayCardPretty(
    total: NutritionEntry,
    norm: Map<String, Int>
) {
    val caloriesGoal = norm["calories"] ?: 0
    val proteinGoal = norm["protein"] ?: 0
    val fatsGoal = norm["fats"] ?: 0
    val carbsGoal = norm["carbs"] ?: 0

    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Сегодня по питанию", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("Краткая сводка по калориям и КБЖУ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RingMacro(
                    label = "Калории",
                    value = total.calories,
                    goal = caloriesGoal,
                    color = Color(0xFFFF8A65),
                    modifier = Modifier.weight(0.88f)
                )
                Column(
                    modifier = Modifier.weight(1.12f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MacroLine("Белки", total.protein, proteinGoal, Color(0xFF66BB6A))
                    MacroLine("Жиры", total.fats, fatsGoal, Color(0xFFFFB74D))
                    MacroLine("Углеводы", total.carbs, carbsGoal, Color(0xFF64B5F6))
                }
            }
        }
    }
}

@Composable
fun WeightInputCardPretty(
    input: String,
    error: String?,
    history: List<Pair<String, Float>>,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onEditClick: () -> Unit
) {
    val latest = history.lastOrNull()
    val previous = history.dropLast(1).lastOrNull()
    val delta = if (latest != null && previous != null) latest.second - previous.second else null

    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Вес и динамика", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        latest?.let { "Последний вес: ${formatWeight(it.second)} кг" }
                            ?: "Добавьте первый замер веса",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    delta?.let {
                        Text(
                            "Изменение к предыдущему: ${if (it >= 0f) "+" else ""}${formatWeight(it)} кг",
                            color = if (it >= 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                TextButton(onClick = onEditClick) {
                    Text("История")
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Вес, кг") },
                singleLine = true,
                isError = error != null,
                supportingText = {
                    Text(
                        error ?: "Новый вес сразу попадёт в историю и синхронизируется с профилем.",
                        color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            FilledTonalButton(onClick = onSave) {
                Text("Сохранить вес")
            }

            WeightChartWithAxes(history = history)
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MacroLine(
    label: String,
    value: Int,
    goal: Int,
    color: Color
) {
    val progress = if (goal > 0) (value.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(
                if (goal > 0) "$value / $goal" else value.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
            drawRoundRect(
                color = trackColor,
                cornerRadius = CornerRadius(100f, 100f)
            )
            drawRoundRect(
                color = color,
                size = Size(size.width * progress, size.height),
                cornerRadius = CornerRadius(100f, 100f)
            )
        }
    }
}

@Composable
private fun RingMacro(
    label: String,
    value: Int,
    goal: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (value.toFloat() / goal).coerceIn(0f, 1f) else 0f
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(112.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = color.copy(alpha = 0.18f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 16f, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(if (goal > 0) "/ $goal" else "без цели", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WeightChartWithAxes(
    history: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    val points = history.takeLast(8)
    val values = points.map { it.second }
    val maxValue = values.maxOrNull() ?: 0f
    val minValue = values.minOrNull() ?: 0f
    val range = max(1f, maxValue - minValue)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 190.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (points.isEmpty()) {
            Surface(shape = RoundedCornerShape(20.dp), color = surfaceVariant) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("График появится после нескольких замеров веса.", color = outline)
                }
            }
            return
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(184.dp)
        ) {
            val leftPad = 44.dp.toPx()
            val bottomPad = 30.dp.toPx()
            val topPad = 12.dp.toPx()
            val chartWidth = size.width - leftPad
            val chartHeight = size.height - bottomPad - topPad
            val gridColor = outline.copy(alpha = 0.18f)

            repeat(4) { index ->
                val y = topPad + chartHeight * index / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(size.width, y),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }

            val xStep = if (points.size > 1) chartWidth / (points.size - 1) else 0f
            val offsets = points.mapIndexed { index, point ->
                val normalized = (point.second - minValue) / range
                Offset(
                    x = leftPad + xStep * index,
                    y = topPad + chartHeight - normalized * chartHeight
                )
            }

            offsets.zipWithNext().forEach { (start, end) ->
                drawLine(color = primary, start = start, end = end, strokeWidth = 6f)
            }

            val latestIndex = offsets.lastIndex
            offsets.forEachIndexed { index, point ->
                val radius = if (index == latestIndex) 8f else 6f
                drawCircle(color = primary, radius = radius, center = point)
                drawCircle(color = surface, radius = radius / 2f, center = point)
            }

            drawContext.canvas.nativeCanvas.apply {
                val labelPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 11.sp.toPx()
                    color = android.graphics.Color.GRAY
                }
                listOf(maxValue, (maxValue + minValue) / 2f, minValue).forEachIndexed { index, label ->
                    val y = topPad + chartHeight * index / 2f + 4.dp.toPx()
                    drawText(String.format(Locale.US, "%.1f", label), 0f, y, labelPaint)
                }
                points.forEachIndexed { index, point ->
                    val label = point.first.takeLast(5)
                    drawText(label, leftPad + xStep * index - 16.dp.toPx(), size.height - 4.dp.toPx(), labelPaint)
                }
                points.lastOrNull()?.let { latest ->
                    drawText(
                        "Сейчас: ${String.format(Locale.US, "%.1f", latest.second)} кг",
                        leftPad,
                        topPad - 2.dp.toPx(),
                        labelPaint.apply { color = primary.toArgb() }
                    )
                }
            }
        }
    }
}

@Composable
fun MacroPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BestExercisesCardPretty(exercises: Collection<ExerciseEntry>) {
    val topExercises = remember(exercises) {
        exercises.sortedByDescending { it.totalVolume }.take(5)
    }

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Лучшие упражнения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (topExercises.isEmpty()) {
                Text(
                    "Тренировочные данные появятся здесь после накопления истории.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                topExercises.forEachIndexed { index, exercise ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${index + 1}. ${exercise.name}",
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${exercise.sets.size} подходов • ${exercise.muscles.take(2).joinToString()}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                formatVolume(exercise.totalVolume),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatDate(isoDate: String): String = try {
    val parts = isoDate.split("-")
    if (parts.size == 3) "${parts[2]}.${parts[1]}" else isoDate
} catch (_: Exception) {
    isoDate
}

private fun formatVolume(volume: Double): String =
    "%,d".format(Locale.US, volume.toInt()).replace(',', ' ')

private fun Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )

private fun formatCount(value: Long): String =
    "%,d".format(Locale.US, value).replace(',', ' ')

private fun formatWeight(value: Float): String {
    val rounded = if (abs(value % 1f) < 0.05f) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
    return rounded.replace('.', ',')
}
