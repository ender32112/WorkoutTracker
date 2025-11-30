package com.example.workouttracker.ui.nutrition_analytic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workouttracker.ui.nutrition.MealType

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
