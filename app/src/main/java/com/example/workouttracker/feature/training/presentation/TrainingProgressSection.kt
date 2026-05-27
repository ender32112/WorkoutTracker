package com.example.workouttracker.feature.training.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workouttracker.ui.theme.gradientPrimary
import kotlin.math.max

internal data class ExerciseProgressStat(
    val exerciseName: String,
    val sessionCount: Int,
    val totalVolume: Double,
    val bestSetVolume: Double,
    val trendPercent: Double
)

internal fun buildExerciseProgressStats(sessions: List<TrainingSession>): List<ExerciseProgressStat> {
    return sessions
        .sortedBy { it.startedAt }
        .flatMap { session -> session.exercises.map { exercise -> exercise.exerciseId to exercise } }
        .groupBy({ it.first }, { it.second })
        .map { (_, entries) ->
            val firstVolume = entries.firstOrNull()?.totalVolume ?: 0.0
            val lastVolume = entries.lastOrNull()?.totalVolume ?: 0.0
            val bestSetVolume = entries
                .flatMap { it.sets }
                .maxOfOrNull { it.weight.toDouble() * it.reps.toDouble() }
                ?: 0.0
            ExerciseProgressStat(
                exerciseName = entries.firstOrNull()?.name.orEmpty(),
                sessionCount = entries.size,
                totalVolume = entries.sumOf { it.totalVolume },
                bestSetVolume = bestSetVolume,
                trendPercent = if (firstVolume <= 0.0) 0.0 else ((lastVolume - firstVolume) / firstVolume) * 100.0
            )
        }
        .sortedByDescending { it.totalVolume }
}

@Composable
fun TrainingProgressSection(
    prs: List<ExercisePrUi>,
    weekly: List<WeeklyVolumeUi>,
    sessions: List<TrainingSession>
) {
    var period by rememberSaveable { mutableStateOf(ProgressPeriodUi.DAYS_30) }
    val now = System.currentTimeMillis()
    val filteredSessions = remember(sessions, period) {
        sessions.filter { now - it.startedAt <= period.days * 24L * 60L * 60L * 1000L }
    }
    val stats = remember(filteredSessions) { buildExerciseProgressStats(filteredSessions) }
    val totalVolume = filteredSessions.sumOf { it.totalVolume }
    val averageVolume = if (filteredSessions.isEmpty()) 0 else (totalVolume / filteredSessions.size).toInt()
    val maxWeeklyVolume = max(1, weekly.maxOfOrNull { it.volume.toInt() } ?: 1)
    val topTrend = stats.maxByOrNull { it.trendPercent }
    val topExercise = stats.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Прогресс по тренировкам",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Следите за недельным объёмом, лучшими сетами и тем, как меняются упражнения в выбранном периоде.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProgressPeriodUi.entries.forEach { item ->
                        FilterChip(
                            selected = period == item,
                            onClick = { period = item },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    listOf(
                        Triple("Сессии", filteredSessions.size.toString(), Icons.Default.FitnessCenter),
                        Triple("Общий объём", totalVolume.toInt().toString(), Icons.Default.AutoGraph),
                        Triple("Средний объём", averageVolume.toString(), Icons.Default.Schedule)
                    )
                ) { (title, value, icon) ->
                    Card(
                        modifier = Modifier.width(176.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                value,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Сводка периода",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topExercise?.let {
                            SummaryPill("Лидер по объёму", it.exerciseName)
                        }
                        topTrend?.let {
                            SummaryPill("Лучший тренд", "${it.exerciseName} • ${trendText(it.trendPercent)}")
                        }
                        if (filteredSessions.isNotEmpty()) {
                            SummaryPill("Период", period.label)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Недельный объём",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Сколько рабочей нагрузки вы набрали по неделям.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (weekly.isEmpty()) {
                        Text("Пока нет данных для графика.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        weekly.take(8).reversed().forEach { week ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(week.weekKey)
                                    Text(week.volume.toInt().toString(), fontWeight = FontWeight.SemiBold)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(999.dp)
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(
                                                (week.volume / maxWeeklyVolume.toDouble()).toFloat().coerceIn(0f, 1f)
                                            )
                                            .height(10.dp)
                                            .background(
                                                MaterialTheme.gradientPrimary,
                                                RoundedCornerShape(999.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Лучшие движения",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Лучший сет и расчётный e1RM по упражнениям, где уже есть история.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (prs.isEmpty()) {
                        Text(
                            "Сначала выполните несколько тренировок, чтобы здесь появились личные рекорды.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        prs.sortedByDescending { it.bestVolumeSet }.take(6).forEach { pr ->
                            SurfaceMetricRow(
                                title = pr.exerciseName,
                                subtitle = "Лучший сет: ${pr.bestVolumeSet.toInt()} • e1RM ${"%.1f".format(pr.bestE1rm)}",
                                meta = "Используйте этот блок как ориентир по силовому прогрессу."
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Прогресс по упражнениям",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Тренд считается по сессиям от старых к новым, а лучший сет — по реальному лучшему подходу.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (stats.isEmpty()) {
                        Text(
                            "Пока недостаточно данных для сравнения прогресса.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        stats.take(8).forEach { stat ->
                            SurfaceMetricRow(
                                title = stat.exerciseName,
                                subtitle = "Лучший сет: ${stat.bestSetVolume.toInt()} • Сессий: ${stat.sessionCount}",
                                meta = "Общий объём: ${stat.totalVolume.toInt()} • Тренд: ${trendText(stat.trendPercent)}"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(
    title: String,
    value: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SurfaceMetricRow(
    title: String,
    subtitle: String,
    meta: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun trendText(trendPercent: Double): String =
    "${if (trendPercent >= 0) "+" else ""}${"%.1f".format(trendPercent)}%"

enum class ProgressPeriodUi(val days: Int, val label: String) {
    DAYS_7(7, "7 дней"),
    DAYS_30(30, "30 дней"),
    DAYS_90(90, "90 дней"),
    DAYS_365(365, "1 год")
}
