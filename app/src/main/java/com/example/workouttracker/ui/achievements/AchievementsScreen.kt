package com.example.workouttracker.ui.achievements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.workouttracker.ui.components.SectionHeader
import com.example.workouttracker.viewmodel.AchievementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievementViewModel: AchievementViewModel
) {
    val achievements by achievementViewModel.achievements.collectAsState()
    val completed = achievements.count { it.isCompleted }
    val total = achievements.size

    Scaffold(
        topBar = {
            // та же шапка, что и в других разделах
            SectionHeader(
                title = "Достижения",
                titleStyle = MaterialTheme.typography.headlineSmall,
                // actions = { ... } // при необходимости добавим позже
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                )
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // сводная карточка сверху
            item {
                SummaryCard(completed = completed, total = total)
            }

            // список ачивок
            items(achievements, key = { it.id }) { achievement ->
                AchievementCard(achievement = achievement)
            }
        }
    }
}

/* -------------------------- Сводка -------------------------- */

@Composable
private fun SummaryCard(completed: Int, total: Int) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Достижения",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Выполнено: $completed / $total",
                    style = MaterialTheme.typography.bodyMedium
                )
                val pct = if (total == 0) 0 else (completed * 100 / total)
                Text(
                    "$pct%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            GradientProgressBar(
                progress = if (total == 0) 0f else completed.toFloat() / total,
                height = 10.dp
            )
        }
    }
}

/* -------------------------- Карточка достижения -------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementCard(achievement: Achievement) {
    val progressAnimated by animateFloatAsState(
        targetValue = achievement.progress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "ach_progress"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (achievement.isCompleted)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Stars(stars = achievement.stars, enabled = achievement.isCompleted)
            }

            Spacer(Modifier.height(10.dp))
            GradientProgressBar(progress = progressAnimated, height = 8.dp)

            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "${achievement.current}/${achievement.target}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${(progressAnimated * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = achievement.isCompleted) {
                ConfettiRow()
            }
        }
    }
}

/* -------------------------- Вспомогательные UI -------------------------- */

@Composable
private fun Stars(stars: Int, enabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { idx ->
            val filled = idx < stars
            Icon(
                imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(22.dp)
                    .alpha(if (enabled || filled) 1f else 0.6f)
            )
            if (idx < 2) Spacer(Modifier.width(2.dp))
        }
    }
}

/** Градиентный прогресс-бар с округлёнными краями. */
@Composable
private fun GradientProgressBar(
    progress: Float,
    height: Dp,
    corner: Dp = 10.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val gradient = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary
        )
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(bg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .clip(RoundedCornerShape(corner))
                .background(brush = gradient)
        )
    }
}

/** Мини-«конфетти»: пульсирующие точки. */
@Composable
private fun ConfettiRow() {
    val infinite = rememberInfiniteTransition(label = "confetti")
    val a1 by infinite.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "c1"
    )
    val a2 by infinite.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "c2"
    )
    val a3 by infinite.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "c3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Dot(MaterialTheme.colorScheme.primary.copy(alpha = a1))
        Dot(MaterialTheme.colorScheme.secondary.copy(alpha = a2))
        Dot(MaterialTheme.colorScheme.tertiary.copy(alpha = a3))
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}
