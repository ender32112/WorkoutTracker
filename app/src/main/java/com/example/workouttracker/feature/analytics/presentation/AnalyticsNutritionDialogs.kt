package com.example.workouttracker.feature.analytics.presentation

import com.example.workouttracker.feature.analytics.runtime.*

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.workouttracker.feature.nutrition.presentation.DailyNutritionSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

private enum class NutritionMetric(
    val label: String,
    val unit: String,
    val normKey: String,
    val color: Color,
    val extractor: (DailyNutritionSummary) -> Int
) {
    CALORIES("Калории", "ккал", "calories", Color(0xFFFF8A65), { it.calories }),
    PROTEIN("Белки", "г", "protein", Color(0xFF66BB6A), { it.protein }),
    FATS("Жиры", "г", "fats", Color(0xFFFFD54F), { it.fats }),
    CARBS("Углеводы", "г", "carbs", Color(0xFF64B5F6), { it.carbs })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionHistoryBottomSheet(
    summaries: List<DailyNutritionSummary>,
    norm: Map<String, Int>,
    onDismiss: () -> Unit
) {
    val sortedHistory = remember(summaries) { summaries.sortedBy { it.date } }

    var selectedRangeName by rememberSaveable { mutableStateOf(StepsHistoryRange.MONTH.name) }
    var selectedMetricName by rememberSaveable { mutableStateOf(NutritionMetric.CALORIES.name) }

    val selectedRange = remember(selectedRangeName) {
        runCatching { StepsHistoryRange.valueOf(selectedRangeName) }
            .getOrDefault(StepsHistoryRange.MONTH)
    }
    val selectedMetric = remember(selectedMetricName) {
        runCatching { NutritionMetric.valueOf(selectedMetricName) }
            .getOrDefault(NutritionMetric.CALORIES)
    }

    val filteredHistory = remember(sortedHistory, selectedRange) {
        filterNutritionHistory(sortedHistory, selectedRange.days)
    }
    val chartData = remember(filteredHistory, selectedMetric) {
        filteredHistory.map { isoToPrettyDate(it.date) to selectedMetric.extractor(it) }
    }
    val normValue = norm[selectedMetric.normKey]
    val bestDay = remember(filteredHistory, selectedMetric) {
        filteredHistory.maxByOrNull { selectedMetric.extractor(it) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("История питания", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Отдельное окно со статистикой калорий и КБЖУ. Меняйте диапазон и метрику, чтобы увидеть тренды.",
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

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NutritionMetric.values().forEach { metric ->
                    FilterChip(
                        selected = selectedMetric == metric,
                        onClick = { selectedMetricName = metric.name },
                        label = { Text(metric.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${selectedMetric.label} за период", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))
                    if (chartData.isEmpty()) {
                        Text(
                            text = "Пока нет данных для графика",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        NutritionHistoryChart(data = chartData, metric = selectedMetric, norm = normValue)
                    }
                }
            }

            bestDay?.let { day ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = selectedMetric.color.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Лучший день", style = MaterialTheme.typography.labelMedium)
                        Text(formatDate(day.date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${selectedMetric.extractor(day)} ${selectedMetric.unit} — максимум в выбранном диапазоне",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            HorizontalDivider()
            Text("Подробные записи", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)

            if (filteredHistory.isEmpty()) {
                Text("История пока пуста", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredHistory.sortedByDescending { it.date }.forEach { summary ->
                        NutritionHistoryItemCard(summary = summary, metric = selectedMetric, norm = normValue)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun NutritionHistoryItemCard(
    summary: DailyNutritionSummary,
    metric: NutritionMetric,
    norm: Int?
) {
    val accent = metric.color
    val value = metric.extractor(summary)
    val progress = if (norm != null && norm > 0) value.toFloat() / norm else 0f

    Surface(modifier = Modifier.fillMaxWidth(), tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(formatDate(summary.date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${summary.calories} ккал за день", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("$value ${metric.unit}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accent)
            }

            if (norm != null && norm > 0) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text("Норма ${norm} ${metric.unit}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroPill(text = "Б ${summary.protein} г", color = Color(0xFF66BB6A))
                MacroPill(text = "Ж ${summary.fats} г", color = Color(0xFFFFA726))
                MacroPill(text = "У ${summary.carbs} г", color = Color(0xFF64B5F6))
            }
        }
    }
}

@Composable
private fun NutritionHistoryChart(
    data: List<Pair<String, Int>>,
    metric: NutritionMetric,
    norm: Int?,
    modifier: Modifier = Modifier
) {
    val accent = metric.color
    val axisColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val paddingDp = 24.dp
    val density = LocalDensity.current
    val values = data.map { it.second }
    val maxVal = (values + listOfNotNull(norm)).maxOrNull()?.coerceAtLeast(0) ?: 0
    val displayMax = if (maxVal == 0) 10f else maxVal * 1.15f
    val stepsY = 4
    val labelIndices: List<Int> = when {
        data.isEmpty() -> emptyList()
        data.size == 1 -> listOf(0)
        data.size == 2 -> listOf(0, 1)
        else -> listOf(0, data.size / 2, data.lastIndex)
    }

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
        it.color = android.graphics.Color.DKGRAY
    }

    Column(modifier = modifier.fillMaxWidth().height(240.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingPx = with(density) { paddingDp.toPx() }
            val approxYTextWidth = yPaint.measureText("${displayMax.toInt()}")
            val paddingLeft = max(paddingPx, approxYTextWidth + paddingPx * 0.3f)
            val paddingRight = paddingPx * 0.6f
            val paddingTop = paddingPx * 0.6f
            val paddingBottom = paddingPx * 1.1f
            val graphWidth = (width - paddingLeft - paddingRight).coerceAtLeast(1f)
            val graphHeight = (height - paddingTop - paddingBottom).coerceAtLeast(1f)

            drawLine(axisColor, Offset(paddingLeft, paddingTop), Offset(paddingLeft, height - paddingBottom), 2f)
            drawLine(axisColor, Offset(paddingLeft, height - paddingBottom), Offset(width - paddingRight, height - paddingBottom), 2f)

            for (i in 0..stepsY) {
                val fy = i / stepsY.toFloat()
                val y = paddingTop + graphHeight * (1f - fy)
                drawLine(gridColor, Offset(paddingLeft, y), Offset(width - paddingRight, y), 1f)
                val value = displayMax * fy
                val label = "${value.toInt()}"
                val fm = yPaint.fontMetrics
                val baseline = y - (fm.ascent + fm.descent) / 2f
                drawContext.canvas.nativeCanvas.drawText(label, paddingLeft - 8f, baseline, yPaint)
            }

            norm?.let {
                if (it > 0) {
                    val y = paddingTop + graphHeight * (1f - it / displayMax)
                    drawLine(
                        color = accent.copy(alpha = 0.6f),
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                }
            }

            if (data.isNotEmpty()) {
                val stepX = graphWidth / data.size
                val barWidth = stepX * 0.55f
                data.forEachIndexed { index, (label, value) ->
                    val xCenter = paddingLeft + index * stepX + stepX / 2f
                    val normalized = (value / displayMax).coerceIn(0f, 1f)
                    val barHeight = graphHeight * normalized
                    val top = paddingTop + graphHeight - barHeight
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.6f))),
                        topLeft = Offset(xCenter - barWidth / 2f, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    if (index in labelIndices) {
                        val fm = xPaint.fontMetrics
                        val textY = height - paddingBottom + (-fm.ascent + fm.descent)
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            xCenter,
                            textY,
                            xPaint.apply { textAlign = android.graphics.Paint.Align.CENTER }
                        )
                    }
                }
            }
        }
    }
}

private val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE

private fun filterNutritionHistory(
    history: List<DailyNutritionSummary>,
    days: Long
): List<DailyNutritionSummary> {
    if (history.isEmpty()) return emptyList()

    val today = LocalDate.now()
    val start = today.minusDays(days - 1)

    return history.filter { summary ->
        runCatching { LocalDate.parse(summary.date, isoDateFormatter) }
            .getOrNull()
            ?.let { date -> date >= start }
            ?: false
    }.sortedBy { it.date }
}


