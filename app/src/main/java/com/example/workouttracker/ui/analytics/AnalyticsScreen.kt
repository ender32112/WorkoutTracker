package com.example.workouttracker.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.viewmodel.TrainingViewModel
import kotlinx.coroutines.delay
import java.util.*
import kotlin.random.Random

@Composable
fun AnalyticsScreen(
    trainingViewModel: TrainingViewModel = viewModel()
) {
    val sessions by trainingViewModel.sessions.collectAsState()
    val steps by produceState(0) { while (true) { value = Random.nextInt(3000, 12000); delay(5000) } }
    val weather by produceState("Загрузка...") { value = fetchWeather(); delay(300000) } // 5 мин

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Шаги сегодня", style = MaterialTheme.typography.titleMedium)
                    Text("$steps", style = MaterialTheme.typography.headlineMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Погода", style = MaterialTheme.typography.titleMedium)
                    Text(weather, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Text("Прогресс по упражнениям", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
        }

        val exerciseStats = sessions.flatMap { it.exercises }
            .groupBy { it.name }
            .mapValues { entry ->
                val maxWeight = entry.value.maxOf { it.weight }
                val maxReps = entry.value.maxOf { it.reps }
                val totalVolume = entry.value.sumOf { it.sets * it.reps * it.weight.toInt() }
                ExerciseStat(entry.key, maxWeight, maxReps, totalVolume)
            }

        items(exerciseStats.values.toList()) { stat ->
            ExerciseProgressCard(stat)
        }
    }
}

data class ExerciseStat(
    val name: String,
    val maxWeight: Float,
    val maxReps: Int,
    val totalVolume: Int
)

@Composable
fun ExerciseProgressCard(stat: ExerciseStat) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stat.name, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Макс вес: ${stat.maxWeight} кг")
                Text("Макс повт: ${stat.maxReps}")
            }
            Text("Объём: ${stat.totalVolume} кг")
        }
    }
}

// Заглушка API погоды (можно заменить на реальный)
suspend fun fetchWeather(): String {
    return listOf(
        "Солнечно, +18°C",
        "Дождь, +12°C",
        "Облачно, +15°C"
    ).random()
}