package com.example.workouttracker.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workouttracker.ui.components.SectionHeader

object AppDimens {
    val screenPadding = 16.dp
    val sectionSpacing = 12.dp
    val cardCorner = 16.dp
}

@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    SectionHeader(title = title, subtitle = subtitle, actions = actions)
}

@Composable
fun PrimaryCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { content() } }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    PrimaryCard(modifier) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyState(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(AppDimens.screenPadding)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LoadingState(text: String = "Загрузка...") {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(AppDimens.screenPadding)) {
        CircularProgressIndicator()
        Text(text)
    }
}

@Composable
fun ErrorState(title: String, message: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(AppDimens.screenPadding)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Text(message)
    }
}

val DefaultScreenPadding = PaddingValues(horizontal = AppDimens.screenPadding, vertical = AppDimens.sectionSpacing)
