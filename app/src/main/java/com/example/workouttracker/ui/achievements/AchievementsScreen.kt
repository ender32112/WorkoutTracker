package com.example.workouttracker.ui.achievements

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.workouttracker.viewmodel.ArticleViewModel
import java.util.UUID

data class Achievement(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val exerciseName: String,
    val progress: Int,
    val stars: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    articleViewModel: ArticleViewModel
) {
    val balance by articleViewModel.balance.collectAsState()
    val achievements by articleViewModel.achievements.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Баллы: $balance") }
            )
        }
    ) { innerPadding: PaddingValues ->
        LazyColumn(
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            items(achievements) { ach ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = ach.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Сделано повторений: ${ach.progress}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            repeat(ach.stars) {
                                Icon(Icons.Filled.Star, contentDescription = null)
                            }
                            repeat(5 - ach.stars) {
                                Icon(Icons.Outlined.StarBorder, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

