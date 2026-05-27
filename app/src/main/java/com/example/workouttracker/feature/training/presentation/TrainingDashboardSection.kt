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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.workouttracker.ui.theme.gradientPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrainingDashboardSection(
    sessions: List<TrainingSession>,
    active: ActiveWorkoutUiState?,
    weekly: List<WeeklyVolumeUi>,
    templatesCount: Int,
    onStart: () -> Unit,
    onOpenSession: () -> Unit,
    onOpenLibrary: () -> Unit,
    onRepeat: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val now = System.currentTimeMillis()
    val sessionsLast30Days = sessions.count { now - it.startedAt <= 30L * 24L * 60L * 60L * 1000L }
    val weeklyVolume = weekly.firstOrNull()?.volume?.toInt() ?: 0
    val recentSessions = sessions.take(6)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TrainingHeroCard(
                active = active,
                onStart = onStart,
                onOpenSession = onOpenSession,
                onOpenLibrary = onOpenLibrary
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    listOf(
                        Triple("Сессии за 30 дней", sessionsLast30Days.toString(), Icons.Default.FitnessCenter),
                        Triple("Недельный объём", formatVolume(weeklyVolume.toDouble()), Icons.Default.AutoGraph),
                        Triple("Шаблоны", templatesCount.toString(), Icons.Default.ViewCarousel)
                    )
                ) { (title, value, icon) ->
                    SummaryMetricCard(title = title, value = value, icon = icon)
                }
            }
        }

        item {
            Text(
                "Последние тренировки",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (recentSessions.isEmpty()) {
            item {
                DashboardEmptyCard(
                    title = "История пока пустая",
                    body = "Завершите первую тренировку, и здесь появятся последние сессии, объём, ключевые упражнения и быстрые действия."
                )
            }
        } else {
            items(recentSessions, key = { it.sessionId }) { session ->
                RecentSessionCard(
                    session = session,
                    onRepeat = { onRepeat(session.sessionId) },
                    onOpenDetail = { onOpenDetail(session.sessionId) }
                )
            }
        }
    }
}

@Composable
private fun TrainingHeroCard(
    active: ActiveWorkoutUiState?,
    onStart: () -> Unit,
    onOpenSession: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.gradientPrimary, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (active == null) "Тренировочный центр" else "Активная тренировка") }
                )
                Text(
                    text = if (active == null) "Готово к новой сессии" else "Незавершённая тренировка ждёт продолжения",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = if (active == null) {
                        "Быстрый старт, библиотека упражнений и последние тренировки всегда под рукой."
                    } else {
                        "Подходы, веса и таймер отдыха уже сохранены. Можно сразу вернуться в рабочий режим."
                    },
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = if (active == null) onStart else onOpenSession) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text(if (active == null) "Начать" else "Вернуться")
                    }
                    FilledTonalButton(onClick = onOpenLibrary) {
                        Text("Библиотека")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ElevatedCard(
        modifier = Modifier
            .width(176.dp)
            .height(132.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentSessionCard(
    session: TrainingSession,
    onRepeat: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val durationMinutes = ((session.finishedAt - session.startedAt).coerceAtLeast(0L) / 60_000L).toInt()
    val keyExercises = session.exercises.take(3).joinToString(" • ") { it.name }
    val totalSets = session.exercises.sumOf { it.sets.size }

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        formatSessionDate(session.startedAt),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        formatSessionTime(session.startedAt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(formatVolume(session.totalVolume)) }
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactMetricChip("Упражнения", session.exercises.size.toString())
                CompactMetricChip("Подходы", totalSets.toString())
                CompactMetricChip("Длительность", formatDuration(durationMinutes))
            }

            Text(
                keyExercises.ifBlank { "Список упражнений появится после завершения тренировки" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRepeat, modifier = Modifier.weight(1f)) {
                    Text("Повторить")
                }
                TextButton(onClick = onOpenDetail, modifier = Modifier.weight(1f)) {
                    Text("Детали")
                }
            }
        }
    }
}

@Composable
private fun CompactMetricChip(label: String, value: String) {
    AssistChip(onClick = {}, label = { Text("$label: $value") })
}

@Composable
private fun DashboardEmptyCard(
    title: String,
    body: String
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatSessionDate(timestamp: Long): String =
    SimpleDateFormat("d MMMM", Locale("ru")).format(Date(timestamp))

private fun formatSessionTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale("ru")).format(Date(timestamp))

private fun formatDuration(minutes: Int): String =
    if (minutes <= 0) "без времени" else "$minutes мин"

private fun formatVolume(volume: Double): String =
    "%,d кг".format(Locale.US, volume.toInt()).replace(',', ' ')
