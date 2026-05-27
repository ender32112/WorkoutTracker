package com.example.workouttracker.feature.nutrition.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workouttracker.ui.components.SectionHeader

@Composable
fun NutritionContent(
    groupedEntries: Map<String, List<NutritionEntry>>,
    mealTypeOrder: List<MealType>,
    todayTotal: NutritionEntry,
    dailyNorm: Map<String, Int>,
    onOpenProfile: () -> Unit,
    onOpenFridge: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSavedProducts: () -> Unit,
    onOpenMealPlan: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onEditEntry: (NutritionEntry) -> Unit,
    onDeleteEntry: (NutritionEntry) -> Unit,
    entriesByDate: (String) -> Map<MealType, List<NutritionEntry>>,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        topBar = {
            SectionHeader(
                title = "Питание",
                titleStyle = MaterialTheme.typography.headlineSmall,
                actions = {
                    IconButton(onClick = onOpenProfile) {
                        Icon(Icons.Filled.Person, contentDescription = "Профиль питания")
                    }
                    IconButton(onClick = onOpenFridge) {
                        Icon(Icons.Filled.Kitchen, contentDescription = "Холодильник")
                    }
                    IconButton(onClick = onOpenSavedProducts) {
                        Icon(Icons.Filled.Inventory2, contentDescription = "Сохранённые продукты")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Настройки питания")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 104.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { TodayCard(todayTotal, dailyNorm) }

                if (groupedEntries.isEmpty()) {
                    item {
                        EmptyNutritionCard(
                            title = "Дневник пока пуст",
                            description = "Добавьте первый приём пищи вручную, откройте план питания или выберите продукт из локальной базы."
                        )
                    }
                } else {
                    groupedEntries.forEach { (date, _) ->
                        item {
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    MaterialTheme.colorScheme.tertiaryContainer
                                                )
                                            )
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = formatDateForDisplay(date),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        val byMealType = entriesByDate(date)
                        mealTypeOrder.forEach { type ->
                            val entriesForType = byMealType[type].orEmpty()
                            if (entriesForType.isNotEmpty()) {
                                item { MealTypeHeader(type) }
                                items(entriesForType, key = { it.id }) { entry ->
                                    NutritionEntryCard(
                                        entry = entry,
                                        onEdit = { onEditEntry(entry) },
                                        onDelete = { onDeleteEntry(entry) }
                                    )
                                }
                                item { Spacer(Modifier.height(8.dp)) }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onOpenMealPlan,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "План питания")
            }

            FloatingActionButton(
                onClick = onOpenAddDialog,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить запись")
            }
        }
    }
}

@Composable
private fun EmptyNutritionCard(
    title: String,
    description: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
