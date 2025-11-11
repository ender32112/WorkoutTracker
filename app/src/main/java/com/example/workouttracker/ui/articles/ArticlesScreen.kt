package com.example.workouttracker.ui.articles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.ui.components.SectionHeader
import com.example.workouttracker.viewmodel.ArticleViewModel
import com.example.workouttracker.viewmodel.ArticleViewModelFactory
import com.example.workouttracker.viewmodel.TrainingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticlesScreen(
    trainingViewModel: TrainingViewModel,
    articleViewModel: ArticleViewModel = viewModel(
        factory = ArticleViewModelFactory(trainingViewModel)
    )
) {
    val articles by articleViewModel.articles.collectAsState()
    val balance by articleViewModel.balance.collectAsState()

    var query by remember { mutableStateOf("") }
    var snack by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            SectionHeader(
                title = "Статьи",
                actions = {}
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BalanceCard(balance = balance)

            // Поиск
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Поиск по заголовку…") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            val filtered = remember(articles, query) {
                if (query.isBlank()) articles
                else articles.filter { it.title.contains(query, ignoreCase = true) }
            }

            LazyColumn(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filtered, key = { it.id }) { article ->
                    ArticleCard(
                        article = article,
                        balance = balance,
                        onBuy = {
                            val ok = articleViewModel.buyArticle(article.id)
                            snack = if (ok) "Статья куплена" else "Недостаточно баллов"
                        }
                    )
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    // Snackbar
    snack?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            snack = null
        }
    }
}

/* ===================== Визуальные блоки ===================== */

@Composable
private fun BalanceCard(balance: Int) {
    // мягкий градиент
    val grad = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        )
    )
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .background(grad)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Баланс очков", style = MaterialTheme.typography.titleMedium)
                Text(
                    "За тренировки и активность",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = balance.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ArticleCard(
    article: Article,
    balance: Int,
    onBuy: () -> Unit
) {
    val bannerBrush = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (article.purchased)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        // ---------- БАННЕР ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerBrush)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = article.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            if (article.purchased) {
                AssistChip(
                    onClick = { /* no-op */ },
                    label = { Text("Открыто") },
                    leadingIcon = {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                    }
                )
            } else {
                AssistChip(
                    onClick = { /* no-op */ },
                    label = { Text("${article.cost}") },
                    leadingIcon = { Text("★") }
                )
            }
        }

        // ---------- КОНТЕНТ ----------
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (article.purchased) {
                // Переключаем «превью/полный текст»
                val fullParagraphs = remember(article.content) {
                    article.content.split("\n\n").filter { it.isNotBlank() }
                }
                val preview = fullParagraphs.take(2).joinToString("\n\n")
                val rest = if (fullParagraphs.size > 2) fullParagraphs.drop(2).joinToString("\n\n") else ""

                Text(
                    text = if (!expanded) preview else (preview + if (rest.isNotBlank()) "\n\n$rest" else ""),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )

                AnimatedVisibility(
                    visible = fullParagraphs.size > 2,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "Свернуть" else "Читать дальше")
                        }
                    }
                }
            } else {
                // --- залочено (не куплено) ---
                val paragraphs = remember(article.content) {
                    article.content.split("\n\n").filter { it.isNotBlank() }
                }
                val preview = paragraphs.take(2).joinToString("\n\n")

                Text(
                    text = preview + if (paragraphs.size > 2) "\n\n… (остальное после покупки)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(12.dp))

                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val narrow = maxWidth < 360.dp   // адаптация под узкие экраны

                    if (narrow) {
                        // ==== Узкая карточка: кнопка на отдельной строке ====
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Статья доступна после покупки",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = onBuy,
                                enabled = balance >= article.cost,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 40.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                // короткие, читаемые надписи
                                Text(
                                    text = if (balance >= article.cost) "Купить • ${article.cost}" else "Мало баллов",
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        // ==== Достаточно места: всё в одну строку ====
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Статья доступна после покупки",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            Button(
                                onClick = onBuy,
                                enabled = balance >= article.cost,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 0.dp, minHeight = 40.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (balance >= article.cost) "Купить за ${article.cost}" else "Мало баллов",
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }

            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/* ============== Вспомогательные (если понадобятся) ============== */

private fun estimateReadMinutes(text: String): Int {
    val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
    // средняя скорость чтения ~200 слов/мин
    return (words / 200f).coerceAtLeast(0.5f).let { kotlin.math.ceil(it).toInt() }
}
