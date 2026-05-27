package com.example.workouttracker.feature.analytics.presentation

import com.example.workouttracker.feature.analytics.runtime.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

private enum class StepDialogHistoryRangeUnused(val days: Long, val label: String) {
    WEEK(7, "Неделя"),
    MONTH(30, "Месяц")
}

@Composable
fun StepEditDialog(
    currentSteps: Long,
    onApply: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var input by rememberSaveable(currentSteps) { mutableStateOf(currentSteps.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun validate(): Long? {
        if (input.isBlank()) {
            error = "Введите количество шагов"
            return null
        }
        val value = input.filter { it.isDigit() }.toLongOrNull()
        if (value == null) {
            error = "Только целые числа"
            return null
        }
        if (value < 0L || value > 200_000L) {
            error = "Диапазон 0–200 000"
            return null
        }
        error = null
        return value
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null) },
        title = { Text("Редактировать шаги") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Долгое нажатие по счётчику открывает меню для разработчика.")
                OutlinedTextField(
                    value = input,
                    onValueChange = { newValue ->
                        input = newValue.filter { it.isDigit() }
                        error = null
                    },
                    label = { Text("Шаги за сегодня") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = validate()
                if (value != null) onApply(value)
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun StepHistoryAddDialog(
    onAdd: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var dateInput by rememberSaveable { mutableStateOf(todayIso()) }
    var stepsInput by rememberSaveable { mutableStateOf("") }
    var dateError by remember { mutableStateOf<String?>(null) }
    var stepsError by remember { mutableStateOf<String?>(null) }

    fun validateAndSubmit() {
        val normalizedDate = dateInput.trim()
        val dateErr = validateIsoDate(normalizedDate)
        val sanitizedSteps = stepsInput.filter { it.isDigit() }
        val stepsValue = sanitizedSteps.toLongOrNull()
        val stepsErr = when {
            stepsInput.isBlank() -> "Введите шаги"
            stepsValue == null -> "Только числа"
            stepsValue < 0 -> "Шаги не могут быть отрицательными"
            else -> null
        }

        dateError = dateErr
        stepsError = stepsErr

        if (dateErr == null && stepsErr == null && stepsValue != null) {
            onAdd(normalizedDate, stepsValue)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить запись") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Скрытая запись шагов по дате", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = {
                        dateInput = it
                        dateError = null
                    },
                    label = { Text("Дата (ГГГГ-ММ-ДД)") },
                    isError = dateError != null,
                    singleLine = true,
                    supportingText = { dateError?.let { err -> Text(err, color = MaterialTheme.colorScheme.error) } }
                )
                OutlinedTextField(
                    value = stepsInput,
                    onValueChange = {
                        stepsInput = it
                        stepsError = null
                    },
                    label = { Text("Шаги") },
                    singleLine = true,
                    isError = stepsError != null,
                    supportingText = { stepsError?.let { err -> Text(err, color = MaterialTheme.colorScheme.error) } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { validateAndSubmit() }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsHistoryBottomSheet(
    history: List<StepHistoryEntry>,
    todaySteps: Long,
    goal: Int,
    onAddEntry: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val combinedHistory = remember(history, todaySteps) {
        val map = history.associateBy { it.isoDate }.toMutableMap()
        val today = todayIso()
        map[today] = StepHistoryEntry(today, isoToPrettyDate(today), todaySteps)
        map.values.sortedBy { it.isoDate }
    }

    var selectedRangeName by rememberSaveable { mutableStateOf(StepsHistoryRange.MONTH.name) }
    var showAddDialog by remember { mutableStateOf(false) }

    val selectedRange = remember(selectedRangeName) {
        runCatching { StepsHistoryRange.valueOf(selectedRangeName) }
            .getOrDefault(StepsHistoryRange.MONTH)
    }

    val filteredHistory = remember(combinedHistory, selectedRange) {
        filterHistoryByRange(combinedHistory, selectedRange.days)
    }

    if (showAddDialog) {
        StepHistoryAddDialog(
            onAdd = { dateIso, steps ->
                onAddEntry(dateIso, steps)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "История шагов",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { showAddDialog = true })
            )
            Text(
                "Сравните ежедневные шаги и линию цели.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepsHistoryRange.values().forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { selectedRangeName = range.name },
                        label = { Text(range.label) }
                    )
                }
            }

            if (filteredHistory.isEmpty()) {
                Text("Пока нет данных о шагах.")
            } else {
                StepsHistoryChart(history = filteredHistory, goal = goal)

                val goalLineColor = MaterialTheme.colorScheme.tertiary
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(4.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Шаги", style = MaterialTheme.typography.labelMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.width(24.dp).height(4.dp)) {
                            drawLine(
                                color = goalLineColor,
                                start = Offset.Zero,
                                end = Offset(size.width, 0f),
                                strokeWidth = size.height,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Цель", style = MaterialTheme.typography.labelMedium)
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredHistory.sortedByDescending { it.isoDate }.forEach { entry ->
                        val reached = entry.steps >= goal
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(entry.prettyDate, style = MaterialTheme.typography.titleSmall)
                                Text("${entry.steps} шагов", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.weight(1f))
                            AssistChip(
                                onClick = {},
                                leadingIcon = {
                                    Icon(
                                        if (reached) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null
                                    )
                                },
                                label = { Text(if (reached) "Цель достигнута" else "Цель не достигнута") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepsHistoryChart(
    history: List<StepHistoryEntry>,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.tertiary
    val axisColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val density = LocalDensity.current
    val yPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }
    }.also {
        it.textSize = with(density) { 10.sp.toPx() }
        it.color = android.graphics.Color.GRAY
    }
    val xPaint = remember {
        android.graphics.Paint().apply { isAntiAlias = true }
    }.also {
        it.textSize = with(density) { 10.sp.toPx() }
        it.color = android.graphics.Color.GRAY
        it.textAlign = android.graphics.Paint.Align.CENTER
    }
    val values = history.map { it.steps.toFloat() }
    val displayMax = max(goal.toFloat(), values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val stepsY = 5
    val labelCount = minOf(5, history.size.coerceAtLeast(1))
    val labelIndices: List<Int> = when {
        history.isEmpty() -> emptyList()
        labelCount == 1 -> listOf(0)
        else -> (0 until labelCount)
            .map { idx -> ((history.size - 1).toFloat() * idx / (labelCount - 1)).roundToInt().coerceIn(0, history.lastIndex) }
            .distinct()
    }
    val goalLabelPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.GRAY
            textAlign = android.graphics.Paint.Align.LEFT
        }
    }.also {
        it.textSize = with(density) { 10.sp.toPx() }
    }

    Column(modifier = modifier.fillMaxWidth().height(240.dp)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val paddingPx = with(density) { 24.dp.toPx() }
            val paddingLeft = paddingPx
            val paddingRight = paddingPx.coerceAtMost(with(density) { 20.dp.toPx() })
            val paddingTop = paddingPx * 0.8f
            val paddingBottom = paddingPx * 1.4f

            Canvas(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                val width = size.width
                val height = size.height
                val graphWidth = (width - paddingLeft - paddingRight).coerceAtLeast(1f)
                val graphHeight = (height - paddingTop - paddingBottom).coerceAtLeast(1f)

                drawLine(axisColor, Offset(paddingLeft, paddingTop), Offset(paddingLeft, height - paddingBottom), 2f)
                drawLine(axisColor, Offset(paddingLeft, height - paddingBottom), Offset(width - paddingRight, height - paddingBottom), 2f)

                for (i in 0..stepsY) {
                    val fy = i / stepsY.toFloat()
                    val y = paddingTop + graphHeight * (1f - fy)
                    drawLine(gridColor, Offset(paddingLeft, y), Offset(width - paddingRight, y), 1f)
                    val value = (displayMax * fy).roundToInt()
                    val fm = yPaint.fontMetrics
                    val baseline = y - (fm.ascent + fm.descent) / 2f
                    drawContext.canvas.nativeCanvas.drawText(value.toString(), paddingLeft - 8.dp.toPx(), baseline, yPaint)
                }

                val goalY = paddingTop + graphHeight * (1f - goal.toFloat() / displayMax)
                drawLine(
                    color = goalColor,
                    start = Offset(paddingLeft, goalY),
                    end = Offset(width - paddingRight, goalY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f))
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "Цель",
                    width - paddingRight + 6.dp.toPx(),
                    goalY + goalLabelPaint.textSize / 2f - 2.dp.toPx(),
                    goalLabelPaint
                )

                if (history.isNotEmpty()) {
                    val stepX = graphWidth / history.size
                    val barWidth = stepX * 0.6f
                    val corner = CornerRadius(6f, 6f)
                    history.forEachIndexed { index, entry ->
                        val centerX = paddingLeft + index * stepX + stepX / 2f
                        val barHeight = graphHeight * (entry.steps.toFloat() / displayMax)
                        val top = paddingTop + graphHeight - barHeight
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(centerX - barWidth / 2f, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = corner
                        )
                    }
                    labelIndices.forEach { idx ->
                        val x = paddingLeft + idx * stepX + stepX / 2f
                        drawLine(
                            color = gridColor,
                            start = Offset(x, height - paddingBottom),
                            end = Offset(x, height - paddingBottom + 8f),
                            strokeWidth = 1.5f
                        )
                        val label = history[idx].prettyDate
                        drawContext.canvas.nativeCanvas.drawText(label, x, height - paddingBottom / 2.8f, xPaint)
                    }
                }
            }
        }
    }
}

private fun filterHistoryByRange(history: List<StepHistoryEntry>, days: Long): List<StepHistoryEntry> {
    if (history.isEmpty() || days <= 0) return history
    val cutoff = runCatching { LocalDate.now().minusDays(days - 1) }.getOrNull() ?: return history
    return history.filter { entry ->
        runCatching { LocalDate.parse(entry.isoDate) }.getOrNull()?.let { !it.isBefore(cutoff) } ?: true
    }
}


