package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

private data class FridgeProductUi(
    var name: String = "",
    var calories100: String = "",
    var protein100: String = "",
    var fats100: String = "",
    var carbs100: String = "",
    var availableGrams: String = ""
)

@Composable
fun FridgeDialog(
    onConfirm: (List<FridgeProduct>, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var allowExtraProducts by remember { mutableStateOf(true) }
    val products = remember { mutableStateListOf(FridgeProductUi()) }
    val listState = rememberLazyListState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Продукты в холодильнике") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Укажите продукты и их КБЖУ на 100 г. Можно добавить доступный вес, чтобы оценить достижимость цели.",
                    style = MaterialTheme.typography.bodyMedium
                )

                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Checkbox(checked = allowExtraProducts, onCheckedChange = { allowExtraProducts = it })
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Дополнительные продукты", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Разрешить использование дополнительных продуктов при необходимости",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    itemsIndexed(products, key = { index, _ -> index }) { index, product ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = "Продукт ${index + 1}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Основные параметры",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (products.size > 1) {
                                        IconButton(onClick = { products.removeAt(index) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                                        }
                                    } else {
                                        Box(modifier = Modifier.size(24.dp))
                                    }
                                }

                                OutlinedTextField(
                                    value = product.name,
                                    onValueChange = { products[index] = products[index].copy(name = it) },
                                    label = { Text("Название") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Text(
                                    text = "КБЖУ на 100 г",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedTextField(
                                    value = product.calories100,
                                    onValueChange = { products[index] = products[index].copy(calories100 = it.filter { ch -> ch.isDigit() }) },
                                    label = { Text("Ккал / 100 г") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = product.protein100,
                                    onValueChange = { products[index] = products[index].copy(protein100 = it.filter { ch -> ch.isDigit() }) },
                                    label = { Text("Белки / 100 г") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = product.fats100,
                                    onValueChange = { products[index] = products[index].copy(fats100 = it.filter { ch -> ch.isDigit() }) },
                                    label = { Text("Жиры / 100 г") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = product.carbs100,
                                    onValueChange = { products[index] = products[index].copy(carbs100 = it.filter { ch -> ch.isDigit() }) },
                                    label = { Text("Углеводы / 100 г") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = product.availableGrams,
                                    onValueChange = { products[index] = products[index].copy(availableGrams = it.filter { ch -> ch.isDigit() }) },
                                    label = { Text("Доступно (г)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { products.add(FridgeProductUi()) }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить продукт")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val fridge = products
                    .mapNotNull { ui ->
                        val name = ui.name.trim()
                        val calories = ui.calories100.toIntOrNull()
                        val protein = ui.protein100.toIntOrNull()
                        val fats = ui.fats100.toIntOrNull()
                        val carbs = ui.carbs100.toIntOrNull()
                        if (name.isBlank() || calories == null || protein == null || fats == null || carbs == null) return@mapNotNull null
                        FridgeProduct(
                            name = name,
                            calories100 = calories,
                            protein100 = protein,
                            fats100 = fats,
                            carbs100 = carbs,
                            availableGrams = ui.availableGrams.toIntOrNull()
                        )
                    }
                onConfirm(fridge, allowExtraProducts)
            }) {
                Text("Сгенерировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
