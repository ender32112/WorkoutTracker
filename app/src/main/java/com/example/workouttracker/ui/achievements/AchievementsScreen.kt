package com.example.workouttracker.ui.achievements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.viewmodel.ArticleViewModel

/**
 * Модель достижения
 */
data class Achievement(
    val id: String,
    val title: String,
    val progress: Int,  // 0..100
    val stars: Int      // 1, 2, 3
)

@Composable
fun AchievementsScreen(
    articleViewModel: ArticleViewModel = viewModel()
) {
    val achievements = listOf(
        Achievement("1", "Новичок", 100, 1),
        Achievement("2", "Воин", 75, 2),
        Achievement("3", "Мастер", 30, 3)
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Достижения", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(achievements) { achievement ->
                AchievementCard(achievement = achievement)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementCard(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(achievement.title, style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = { achievement.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .padding(top = 8.dp)
                )
                Text("${achievement.progress}%", style = MaterialTheme.typography.bodySmall)
            }

            // Звёзды
            Row {
                repeat(achievement.stars) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                repeat(3 - achievement.stars) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}