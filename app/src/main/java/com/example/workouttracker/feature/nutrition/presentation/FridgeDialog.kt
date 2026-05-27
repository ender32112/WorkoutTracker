package com.example.workouttracker.feature.nutrition.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private data class SelectedFridgeItemUi(
    val selected: Boolean,
    val amountText: String
)

@Composable
fun FridgeDialog(
    fridgeItems: List<FridgeItemUiModel>,
    onConfirm: (List<FridgeProduct>) -> Unit,
    onDismiss: () -> Unit,
    onManageFridge: () -> Unit
) {
    val listState = rememberLazyListState()
    val selection = remember(fridgeItems) {
        mutableStateMapOf<Long, SelectedFridgeItemUi>().apply {
            fridgeItems.forEach { item ->
                put(item.id, SelectedFridgeItemUi(selected = true, amountText = item.amount.toString()))
            }
        }
    }
    var validationError by remember(fridgeItems) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Продукты из холодильника") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Выберите продукты из холодильника и укажите доступное количество для плана на день.",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedButton(
                    onClick = onManageFridge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Управлять холодильником")
                }

                if (fridgeItems.isEmpty()) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "Холодильник пуст. Добавьте продукты и вернитесь к генерации плана.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(fridgeItems, key = { it.id }) { item ->
                            val current = selection[item.id] ?: SelectedFridgeItemUi(
                                selected = false,
                                amountText = item.amount.toString()
                            )
                            val unitLabel = item.unitType.toShortLabel()

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = current.selected,
                                            onCheckedChange = { checked ->
                                                selection[item.id] = current.copy(selected = checked)
                                                validationError = null
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Доступно: ${item.amount} $unitLabel",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }

                                    Text(
                                        text = "На 100 г: ${formatMacro(item.calories100)} ккал, Б ${formatMacro(item.protein100)} г, Ж ${formatMacro(item.fats100)} г, У ${formatMacro(item.carbs100)} г",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    if (current.selected) {
                                        OutlinedTextField(
                                            value = current.amountText,
                                            onValueChange = { raw ->
                                                val filtered = raw.filter(Char::isDigit).take(5)
                                                selection[item.id] = current.copy(amountText = filtered)
                                                validationError = null
                                            },
                                            label = { Text("Использовать ($unitLabel)") },
                                            supportingText = { Text("Не больше ${item.amount} $unitLabel") },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                validationError?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = fridgeItems.isNotEmpty(),
                onClick = {
                    val chosen = mutableListOf<FridgeProduct>()
                    for (item in fridgeItems) {
                        val state = selection[item.id] ?: continue
                        if (!state.selected) continue

                        val amount = state.amountText.toIntOrNull()
                        if (amount == null || amount <= 0) {
                            validationError = "Укажите корректное количество для «${item.name}»."
                            return@Button
                        }
                        if (amount > item.amount) {
                            validationError = "Количество для «${item.name}» превышает доступное."
                            return@Button
                        }

                        chosen += FridgeProduct(
                            name = item.name,
                            calories100 = item.calories100,
                            protein100 = item.protein100,
                            fats100 = item.fats100,
                            carbs100 = item.carbs100,
                            availableGrams = amount,
                            unitType = item.unitType
                        )
                    }

                    if (chosen.isEmpty()) {
                        validationError = "Выберите хотя бы один продукт."
                        return@Button
                    }

                    validationError = null
                    onConfirm(chosen)
                }
            ) {
                Text("Сгенерировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

private fun QuantityUnit.toShortLabel(): String =
    if (this == QuantityUnit.GRAMS) "г" else "шт"

private fun formatMacro(value: Float): String = "%.1f".format(value)

