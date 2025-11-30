package com.example.workouttracker.ui.nutrition_analytic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workouttracker.ui.analytics.NutritionTodayCardPretty
import com.example.workouttracker.ui.components.SectionHeader
import com.example.workouttracker.ui.nutrition.MealType
import com.example.workouttracker.ui.nutrition.NutritionEntry

@Composable
fun NutritionAnalyticsFullScreen(
    todayTotal: NutritionEntry,
    norm: Map<String, Int>,
    todayAnalytics: DailyAnalytics?,
    weeklyAnalytics: WeeklyAnalytics?,
    onRefreshToday: () -> Unit,
    onRefreshWeekly: () -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            SectionHeader(
                title = "Аналитика питания",
                subtitle = "КБЖУ дня и выполнение плана",
                titleStyle = MaterialTheme.typography.headlineSmall,
                showDivider = false,
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть аналитику")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NutritionTodayCardPretty(total = todayTotal, norm = norm)
            AnalyticsRefreshRow(onRefreshToday = onRefreshToday, onRefreshWeekly = onRefreshWeekly)
            DailyAnalyticsCardPretty(analytics = todayAnalytics, onRefresh = onRefreshToday)
            WeeklyAnalyticsCardPretty(weekly = weeklyAnalytics)
        }
    }
}

@Composable
private fun AnalyticsRefreshRow(
    onRefreshToday: () -> Unit,
    onRefreshWeekly: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AssistChip(
            onClick = onRefreshToday,
            leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
            label = { Text("Пересчитать день") },
            colors = AssistChipDefaults.assistChipColors()
        )
        AssistChip(
            onClick = onRefreshWeekly,
            leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
            label = { Text("Пересчитать неделю") },
            colors = AssistChipDefaults.assistChipColors()
        )
    }
}

@Composable
private fun DailyAnalyticsCardPretty(
    analytics: DailyAnalytics?,
    onRefresh: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Аналитика дня",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = analytics?.let { "За ${it.date}" } ?: "Нажмите обновить, чтобы посчитать попадание",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = onRefresh,
                    leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    label = { Text("Обновить") }
                )
            }

            if (analytics == null) {
                Text(
                    text = "Нет данных за сегодня",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(onClick = onRefresh) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Рассчитать сейчас")
                }
            } else {
                MacroStatRowPretty("Калории", analytics.plannedCalories, analytics.eatenCalories, analytics.adherenceCaloriesPercent)
                MacroStatRowPretty("Белки", analytics.plannedProtein, analytics.eatenProtein, analytics.adherenceProteinPercent)
                MacroStatRowPretty("Жиры", analytics.plannedFats, analytics.eatenFats, analytics.adherenceFatsPercent)
                MacroStatRowPretty("Углеводы", analytics.plannedCarbs, analytics.eatenCarbs, analytics.adherenceCarbsPercent)

                Divider()
                Text(
                    text = "Приёмы пищи",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                MealComparisonPrettyList(analytics.mealComparisons)
            }
        }
    }
}

@Composable
private fun WeeklyAnalyticsCardPretty(weekly: WeeklyAnalytics?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Аналитика недели",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (weekly == null) {
                Text(
                    text = "Нет данных за последние дни",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Обновите неделю, чтобы собрать статистику по замерам и продуктам.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "${weekly.startDate} – ${weekly.endDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                WeeklyMetricRow("Калории", weekly.avgAdherenceCaloriesPercent)
                WeeklyMetricRow("Белки", weekly.avgAdherenceProteinPercent)
                WeeklyMetricRow("Жиры", weekly.avgAdherenceFatsPercent)
                WeeklyMetricRow("Углеводы", weekly.avgAdherenceCarbsPercent)

                if (weekly.favoriteFoods.isNotEmpty()) {
                    AnalyticsTagBlock("Часто съедаемые продукты", weekly.favoriteFoods)
                }
                if (weekly.ignoredPlannedFoods.isNotEmpty()) {
                    AnalyticsTagBlock("Из плана не доходит", weekly.ignoredPlannedFoods)
                }
                if (weekly.replacedFoods.isNotEmpty()) {
                    AnalyticsTagBlock("Стоит заменить", weekly.replacedFoods)
                }
            }
        }
    }
}

@Composable
private fun WeeklyMetricRow(label: String, percent: Int) {
    val normalized = percent.coerceIn(0, 200)
    val progress = normalized / 100f
    val color = when {
        percent in 90..110 -> MaterialTheme.colorScheme.primary
        percent < 90 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("${percent}%", color = color, style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun MacroStatRowPretty(label: String, planned: Int, fact: Int, adherence: Int) {
    val planSafe = planned.coerceAtLeast(1)
    val progress = (fact.toFloat() / planSafe).coerceIn(0f, 1f)
    val color = when {
        adherence in 95..105 -> MaterialTheme.colorScheme.primary
        fact > planned -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("${adherence}%", color = color, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = "План: $planned · Факт: $fact",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun MealComparisonPrettyList(comparisons: List<MealComparison>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        comparisons.forEach { comparison ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = comparison.mealType.displayName(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "Совпало: ${comparison.matched.size}, пропущено: ${comparison.missedFromPlan.size}, добавлено: ${comparison.extraFood.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val bullets = buildList {
                    if (comparison.extraFood.isNotEmpty()) add("Дополнительно: ${comparison.extraFood.joinToString()}")
                    if (comparison.missedFromPlan.isNotEmpty()) add("Не съедено из плана: ${comparison.missedFromPlan.joinToString()}")
                }
                bullets.forEach { line ->
                    Text(
                        text = "• $line",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTagBlock(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(
            text = items.joinToString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun NutritionAnalyticsScreen(
    todayAnalytics: DailyAnalytics?,
    weeklyAnalytics: WeeklyAnalytics?,
    onRefreshToday: () -> Unit,
    onRefreshWeekly: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedButton(
                onClick = onRefreshToday,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Обновить день")
            }
            ElevatedButton(
                onClick = onRefreshWeekly,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Обновить неделю")
            }
        }

        if (todayAnalytics == null && weeklyAnalytics == null) {
            Text(
                text = "Нет данных для аналитики",
                style = MaterialTheme.typography.bodyMedium
            )
            return
        }

        todayAnalytics?.let { analytics ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Сегодня: ${analytics.date}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MacroRow("Калории", analytics.plannedCalories, analytics.eatenCalories, analytics.adherenceCaloriesPercent)
                    MacroRow("Белки", analytics.plannedProtein, analytics.eatenProtein, analytics.adherenceProteinPercent)
                    MacroRow("Жиры", analytics.plannedFats, analytics.eatenFats, analytics.adherenceFatsPercent)
                    MacroRow("Углеводы", analytics.plannedCarbs, analytics.eatenCarbs, analytics.adherenceCarbsPercent)
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Приёмы пищи",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    MealComparisonsList(analytics.mealComparisons)
                }
            }
        }

        weeklyAnalytics?.let { weekly ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Неделя: ${weekly.startDate} – ${weekly.endDate}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text("Среднее попадание по калориям: ${weekly.avgAdherenceCaloriesPercent}%")
                    Text("Среднее попадание по белкам: ${weekly.avgAdherenceProteinPercent}%")
                    Text("Среднее попадание по жирам: ${weekly.avgAdherenceFatsPercent}%")
                    Text("Среднее попадание по углеводам: ${weekly.avgAdherenceCarbsPercent}%")

                    if (weekly.favoriteFoods.isNotEmpty()) {
                        Text("Часто съедаемые продукты:", fontWeight = FontWeight.SemiBold)
                        Text(weekly.favoriteFoods.joinToString())
                    }
                    if (weekly.ignoredPlannedFoods.isNotEmpty()) {
                        Text("Часто игнорируемые из плана:", fontWeight = FontWeight.SemiBold)
                        Text(weekly.ignoredPlannedFoods.joinToString())
                    }
                    if (weekly.replacedFoods.isNotEmpty()) {
                        Text("Возможные кандидаты на исключение:", fontWeight = FontWeight.SemiBold)
                        Text(weekly.replacedFoods.joinToString())
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroRow(label: String, planned: Int, fact: Int, adherence: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: план $planned / факт $fact",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Отклонение: $adherence%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun MealComparisonsList(comparisons: List<MealComparison>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        comparisons.forEachIndexed { index, comparison ->
            MealComparisonItem(comparison)
            if (index < comparisons.lastIndex) {
                Divider(modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun MealComparisonItem(comparison: MealComparison) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = comparison.mealType.displayName(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = "Совпало: ${comparison.matched.size}, пропущено: ${comparison.missedFromPlan.size}, добавлено: ${comparison.extraFood.size}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun MealType.displayName(): String = when (this) {
    MealType.BREAKFAST -> "Завтрак"
    MealType.LUNCH -> "Обед"
    MealType.DINNER -> "Ужин"
    MealType.SNACK -> "Перекус"
    MealType.OTHER -> "Другое"
}
