package com.example.workouttracker.ui.articles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.workouttracker.viewmodel.ArticleViewModel
import com.example.workouttracker.viewmodel.TrainingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesScreen(
    trainingViewModel: TrainingViewModel,
    articleViewModel: ArticleViewModel
) {
    val articles by articleViewModel.articles.collectAsState()
    val balance  by articleViewModel.balance.collectAsState()
    var toBuy           by remember { mutableStateOf<Article?>(null) }
    var openedArticle  by remember { mutableStateOf<Article?>(null) }
    var showNotEnough  by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Баланс: $balance") })
    }) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)) {
                items(articles) { art ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        if (art.purchased) openedArticle = art
                                    }
                            ) {
                                Text(art.title, style = MaterialTheme.typography.titleMedium)
                                Text(art.date, style = MaterialTheme.typography.bodySmall)
                            }
                            if (!art.purchased) {
                                IconButton(onClick = { toBuy = art }) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = "Купить")
                                }
                            } else {
                                Icon(Icons.Default.LockOpen, contentDescription = "Открыто")
                            }
                        }
                    }
                }
            }
        }
        if (toBuy != null) {
            AlertDialog(
                onDismissRequest = { toBuy = null },
                title   = { Text("Купить «${toBuy!!.title}»?") },
                text    = { Text("Цена: ${toBuy!!.cost} баллов") },
                confirmButton = {
                    TextButton(onClick = {
                        val success = articleViewModel.buyArticle(toBuy!!.id)
                        toBuy = null
                        if (!success) showNotEnough = true
                    }) {
                        Text("Да")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { toBuy = null }) {
                        Text("Нет")
                    }
                }
            )
        }
        if (showNotEnough) {
            AlertDialog(
                onDismissRequest = { showNotEnough = false },
                title   = { Text("Ошибка") },
                text    = { Text("Недостаточно баллов для покупки этой статьи.") },
                confirmButton = {
                    TextButton(onClick = { showNotEnough = false }) {
                        Text("Ок")
                    }
                }
            )
        }
        if (openedArticle != null) {
            AlertDialog(
                onDismissRequest = { openedArticle = null },
                title = { Text(openedArticle!!.title) },
                text  = { Text(openedArticle!!.text) },
                confirmButton = {
                    TextButton(onClick = { openedArticle = null }) {
                        Text("Закрыть")
                    }
                }
            )
        }
   }
}

