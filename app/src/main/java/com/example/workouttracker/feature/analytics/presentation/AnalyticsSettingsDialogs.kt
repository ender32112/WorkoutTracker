package com.example.workouttracker.feature.analytics.presentation

import com.example.workouttracker.feature.analytics.runtime.*

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.workouttracker.health.HealthConnectAvailability
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsSettingsDialogPretty(
    currentCity: String,
    currentGoal: String,
    currentNotify: Boolean,
    currentStepSource: StepDataSource,
    healthAvailability: HealthConnectAvailability,
    hasHealthPermissions: Boolean,
    detectingCity: Boolean,
    onDetectCity: () -> Unit,
    onSave: (
        newCity: String,
        newGoal: String,
        notify: Boolean,
        source: StepDataSource
    ) -> Unit,
    onRefreshWeather: () -> Unit,
    onDismiss: () -> Unit
) {
    var city by remember(currentCity) { mutableStateOf(currentCity) }
    var goal by remember(currentGoal) { mutableStateOf(currentGoal) }
    var notifyEnabled by remember(currentNotify) { mutableStateOf(currentNotify) }
    var stepSource by remember(currentStepSource) { mutableStateOf(currentStepSource) }

    var goalError by remember { mutableStateOf<String?>(null) }
    fun validateGoal(s: String): String? {
        if (s.isBlank()) return "Укажите цель по шагам"
        val v = s.toIntOrNull() ?: return "Только целое число"
        if (v !in 1000..50000) return "Диапазон 1 000-50 000"
        return null
    }

    val healthStatus = when (healthAvailability) {
        HealthConnectAvailability.AVAILABLE ->
            if (hasHealthPermissions) "Разрешения Health Connect уже выданы."
            else "Для Health Connect нужно выдать разрешения."
        HealthConnectAvailability.UPDATE_REQUIRED ->
            "Health Connect нужно установить или обновить."
        HealthConnectAvailability.UNAVAILABLE ->
            "Health Connect недоступен на этом устройстве."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Tune, contentDescription = null) },
        title = { Text("Настройки аналитики") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Timeline, contentDescription = null)
                        Column {
                            Text("Персонализируйте аналитику", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Выберите источник шагов, обновите город и задайте комфортную цель.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                AnalyticsSettingsSection(
                    title = "Источник шагов",
                    subtitle = "Основной источник дневного прогресса и фонового уведомления."
                ) {
                    StepSourceOptionCard(
                        selected = stepSource == StepDataSource.DEVICE_SENSOR,
                        title = StepDataSource.DEVICE_SENSOR.uiTitle(),
                        description = StepDataSource.DEVICE_SENSOR.uiDescription(),
                        icon = Icons.Default.Smartphone,
                        onClick = { stepSource = StepDataSource.DEVICE_SENSOR }
                    )
                    Spacer(Modifier.height(10.dp))
                    StepSourceOptionCard(
                        selected = stepSource == StepDataSource.HEALTH_CONNECT,
                        title = StepDataSource.HEALTH_CONNECT.uiTitle(),
                        description = StepDataSource.HEALTH_CONNECT.uiDescription(),
                        icon = Icons.Default.Favorite,
                        onClick = { stepSource = StepDataSource.HEALTH_CONNECT }
                    )
                    if (stepSource == StepDataSource.HEALTH_CONNECT) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HealthAndSafety, contentDescription = null)
                                Text(
                                    healthStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                AnalyticsSettingsSection(
                    title = "Дневная цель",
                    subtitle = "Используется в карточке шагов и в постоянном уведомлении."
                ) {
                    OutlinedTextField(
                        value = goal,
                        onValueChange = {
                            goal = it.filter { ch -> ch.isDigit() }
                            goalError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Цель по шагам") },
                        leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                        singleLine = true,
                        isError = goalError != null,
                        supportingText = {
                            if (goalError != null) {
                                Text(goalError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Рекомендуемый диапазон: 1 000–50 000 шагов")
                            }
                        }
                    )
                }

                AnalyticsSettingsSection(
                    title = "Погода",
                    subtitle = "Город используется в карточке прогноза на экране аналитики."
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Город для погоды") },
                        leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = onDetectCity,
                            enabled = !detectingCity,
                            leadingIcon = {
                                if (detectingCity) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null)
                                }
                            },
                            label = { Text(if (detectingCity) "Определяем..." else "Определить автоматически") }
                        )
                        FilledTonalButton(onClick = onRefreshWeather) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Обновить погоду")
                        }
                    }
                }

                AnalyticsSettingsSection(
                    title = "Уведомления",
                    subtitle = "Переключатель отвечает за уведомление о достижении дневной цели."
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (notifyEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Уведомление о достижении цели", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (notifyEnabled) "Приложение сообщит, когда дневная цель будет достигнута."
                                    else "Будет только постоянное уведомление с прогрессом, без отдельного сигнала о цели.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = notifyEnabled, onCheckedChange = { notifyEnabled = it })
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = {
                val err = validateGoal(goal)
                goalError = err
                if (err == null) onSave(city, goal, notifyEnabled, stepSource)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun AnalyticsSettingsSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun StepSourceOptionCard(
    selected: Boolean,
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWeightHistoryDialogPretty(
    initial: List<Pair<String, Float>>,
    validateDate: (String) -> String?,
    validateWeight: (String) -> String?,
    onSave: (List<Pair<String, Float>>) -> Unit,
    onDismiss: () -> Unit
) {
    class RowItem(
        val id: String = UUID.randomUUID().toString(),
        date: String,
        weight: String
    ) {
        var date by mutableStateOf(date)
        var weight by mutableStateOf(weight)
    }

    val rows = remember {
        mutableStateListOf<RowItem>().apply {
            initial.forEach { add(RowItem(date = it.first.trim(), weight = it.second.toString())) }
        }
    }

    fun hasDuplicateDates(): Boolean {
        val set = HashSet<String>()
        rows.forEach { if (!set.add(it.date.trim())) return true }
        return false
    }

    val canSave by derivedStateOf {
        rows.isNotEmpty() &&
            rows.all { validateDate(it.date) == null && validateWeight(it.weight) == null } &&
            !hasDuplicateDates()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.EditCalendar, contentDescription = null) },
        title = { Text("Редактирование веса") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer
                                )
                            ),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TipsAndUpdates, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Дата — ДД.ММ (корректные значения). Вес — 30–300 кг. Без повторов дат.")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilledTonalButton(onClick = {
                        val todayShort = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())
                        rows.add(RowItem(date = todayShort, weight = ""))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Добавить")
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 380.dp)
                ) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(rows, key = { it.id }) { item ->
                            val errDate = validateDate(item.date)
                            val errWeight = validateWeight(item.weight)
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = item.date,
                                        onValueChange = { v ->
                                            val filtered = v.filter { ch -> ch.isDigit() || ch == '.' }
                                            item.date = filtered.take(5)
                                        },
                                        label = { Text("Дата (ДД.ММ)") },
                                        singleLine = true,
                                        isError = errDate != null,
                                        supportingText = { if (errDate != null) Text(errDate) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = item.weight,
                                        onValueChange = { v ->
                                            item.weight = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }
                                        },
                                        label = { Text("Вес (кг)") },
                                        singleLine = true,
                                        isError = errWeight != null,
                                        supportingText = { if (errWeight != null) Text(errWeight) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { rows.removeAll { it.id == item.id } },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                                    }
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = hasDuplicateDates(),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text("В списке есть повторяющиеся даты", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val result = rows.map { it.date.trim() to it.weight.trim().replace(',', '.').toFloat() }
                    onSave(result)
                },
                enabled = canSave
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}



