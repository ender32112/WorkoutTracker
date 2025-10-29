package com.example.workouttracker.ui.articles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.workouttracker.viewmodel.Article
import com.example.workouttracker.viewmodel.ArticleViewModel
import com.example.workouttracker.viewmodel.TrainingViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ArticlesScreen(
    trainingViewModel: TrainingViewModel,
    articleViewModel: ArticleViewModel
) {
    val articles by articleViewModel.articles.collectAsState()
    val balance by articleViewModel.balance.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        // Баланс
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Баланс", style = MaterialTheme.typography.titleMedium)
                Text("$balance баллов", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(articles) { article ->
                ArticleCard(
                    article = article,
                    balance = balance,
                    onBuy = {
                        articleViewModel.buyArticle(article.id)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleCard(
    article: Article,
    balance: Int,
    onBuy: () -> Unit
) {
    val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

    Card(
        modifier = Modifier.fillMaxWidth(),
        enabled = !article.purchased && balance >= article.cost,
        onClick = onBuy
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(article.title, style = MaterialTheme.typography.titleMedium)
                Text(date, style = MaterialTheme.typography.bodySmall)
                if (article.purchased) {
                    Text("Куплено", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("${article.cost} баллов", color = MaterialTheme.colorScheme.secondary)
                }
            }

            if (!article.purchased && balance >= article.cost) {
                Button(onClick = onBuy) {
                    Text("Купить")
                }
            } else if (article.purchased) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "Куплено",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Text("Недостаточно", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}