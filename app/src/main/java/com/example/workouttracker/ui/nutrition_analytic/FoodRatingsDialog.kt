package com.example.workouttracker.ui.nutrition_analytic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun FoodRatingsDialog(
    ratings: List<FoodRating>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Рейтинг продуктов") },
        text = {
            if (ratings.isEmpty()) {
                Text("Недостаточно данных для рейтинга")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyColumn(
                        modifier = Modifier.height(320.dp)
                    ) {
                        items(ratings) { rating ->
                            FoodRatingRow(rating)
                            Divider(modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
private fun FoodRatingRow(rating: FoodRating) {
    val percentage = (rating.adherenceRatio * 100).roundToInt()
    val labelColor: Color
    val label: String

    when {
        rating.adherenceRatio >= 0.7 -> {
            label = "любимый"
            labelColor = MaterialTheme.colorScheme.primary
        }
        rating.adherenceRatio >= 0.4 -> {
            label = "нейтральный"
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
        else -> {
            label = "нелюбимый"
            labelColor = MaterialTheme.colorScheme.error
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = rating.nameCanonical,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "съедено ${rating.eatenCount}, пропущено ${rating.missedCount}, попадание ${percentage}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = labelColor
        )
    }
}
