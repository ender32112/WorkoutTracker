package com.example.workouttracker.feature.training.presentation

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.workouttracker.data.exercise.Exercise
import com.example.workouttracker.data.exercise.ExerciseSourceFilter
import com.example.workouttracker.ui.theme.gradientPrimary

@Composable
fun ExerciseListScreen(
    exercises: List<Exercise>,
    searchQuery: String,
    selectedMuscle: String?,
    selectedBodyPart: String?,
    favoritesOnly: Boolean,
    sourceFilter: ExerciseSourceFilter,
    resultCount: Int,
    muscles: List<String>,
    bodyParts: List<String>,
    onSearchChange: (String) -> Unit,
    onSelectMuscle: (String?) -> Unit,
    onSelectBodyPart: (String?) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onSourceFilterChange: (ExerciseSourceFilter) -> Unit,
    onClearFilters: () -> Unit,
    onExerciseClick: (Exercise) -> Unit,
    onToggleFavorite: (Exercise) -> Unit,
    onAddToWorkout: (Exercise) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.gradientPrimary, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Библиотека упражнений",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f)
                        )
                        Text(
                            "Единый локальный каталог",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            "Здесь собраны базовые и пользовательские упражнения с быстрым поиском по мышцам, оборудованию и названию.",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск упражнения") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Найдено: $resultCount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = favoritesOnly,
                            onClick = onToggleFavoritesOnly,
                            label = { Text("Избранные") }
                        )
                        ExerciseSourceFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = sourceFilter == filter,
                                onClick = { onSourceFilterChange(filter) },
                                label = {
                                    Text(
                                        when (filter) {
                                            ExerciseSourceFilter.ALL -> "Все"
                                            ExerciseSourceFilter.GLOBAL -> "Каталог"
                                            ExerciseSourceFilter.CUSTOM -> "Мои"
                                        }
                                    )
                                }
                            )
                        }
                        if (selectedMuscle != null || selectedBodyPart != null || favoritesOnly || sourceFilter != ExerciseSourceFilter.ALL) {
                            TextButton(onClick = onClearFilters) {
                                Text("Сбросить фильтры")
                            }
                        }
                    }
                }
            }
        }
        item {
            FilterSection("Целевая мышца", muscles, selectedMuscle, onSelectMuscle)
        }
        item {
            FilterSection("Часть тела", bodyParts, selectedBodyPart, onSelectBodyPart)
        }
        if (exercises.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Ничего не найдено",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Сбросьте фильтры или измените запрос, чтобы снова увидеть каталог.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(exercises, key = { it.id }) { exercise ->
                ExerciseListItem(
                    exercise = exercise,
                    onClick = { onExerciseClick(exercise) },
                    onToggleFavorite = { onToggleFavorite(exercise) },
                    onAddToWorkout = { onAddToWorkout(exercise) }
                )
            }
        }
    }
}

@Composable
fun ExerciseDetailScreen(
    exercise: Exercise,
    onClose: () -> Unit,
    onAddToWorkout: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        exercise.nameRu,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (exercise.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (exercise.isFavorite) Color(0xFFE65C74) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            ExerciseMedia(
                url = exercise.gifUrl,
                localMediaUri = exercise.localMediaUri,
                contentDescription = exercise.nameRu,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(28.dp))
            )
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Профиль упражнения",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        exercise.targetMuscles.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                    }
                    if (exercise.secondaryMuscles.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            exercise.secondaryMuscles.forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                        }
                    }
                    Text("Часть тела: ${exercise.bodyPart}")
                    Text("Оборудование: ${exercise.equipment}")
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Техника выполнения",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (exercise.nameEn.isNotBlank() && !exercise.nameEn.equals(exercise.nameRu, ignoreCase = true)) {
                        Text(
                            "Оригинальное название: ${exercise.nameEn}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    exercise.instructions.forEachIndexed { index, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", color = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                step,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onAddToWorkout, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("В тренировку")
                }
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("Закрыть")
                }
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Все") })
            }
            items(options, key = { it }) { option ->
                FilterChip(
                    selected = selected.equals(option, true),
                    onClick = { onSelect(if (selected.equals(option, true)) null else option) },
                    label = { Text(option) }
                )
            }
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: Exercise,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToWorkout: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box {
                ExerciseMedia(
                    url = exercise.gifUrl,
                    localMediaUri = exercise.localMediaUri,
                    contentDescription = exercise.nameRu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                )
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.24f))
                ) {
                    Icon(
                        if (exercise.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (exercise.isFavorite) Color(0xFFFF7A93) else Color.White
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    exercise.nameRu,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exercise.targetMuscles.take(2).forEach { AssistChip(onClick = {}, label = { Text(it) }) }
                    if (exercise.equipment.isNotBlank()) {
                        AssistChip(onClick = {}, label = { Text(exercise.equipment) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onClick) {
                        Text("Подробнее")
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                    Button(onClick = onAddToWorkout, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("В сессию", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseMedia(
    url: String,
    localMediaUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context).components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }.crossfade(true).build()
    }
    val model = remember(localMediaUri, url) { localMediaUri ?: url.ifBlank { null } }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(model).crossfade(true).build(),
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text("Загрузка превью", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Нет превью",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        contentDescription,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    )
}
