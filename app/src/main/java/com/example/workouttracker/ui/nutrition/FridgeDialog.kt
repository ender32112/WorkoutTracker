package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Укажите продукты и их КБЖУ на 100 г. Можно добавить доступный вес, чтобы оценить достижимость цели.",
                    style = MaterialTheme.typography.bodySmall
                )
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    itemsIndexed(products) { index, product ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Продукт ${index + 1}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (products.size > 1) {
                                    IconButton(onClick = { products.removeAt(index) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = product.name,
                                onValueChange = { products[index] = products[index].copy(name = it) },
                                label = { Text("Название") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = product.calories100,
                                    onValueChange = { products[index] = products[index].copy(calories100 = it.filter { ch -> ch.isDigit() }) },
                                    label = { Text("Ккал/100г") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = product.protein100,
                                    onValueChange = { products[index] = products[index].copy(protein100 = it.filter { ch -> ch.isDigit() }) },
                                    label = { Text("Б/100г") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = product.fats100,
                                    onValueChange = { products[index] = products[index].copy(fats100 = it.filter { ch -> ch.isDigit() }) },
                                    label = { Text("Ж/100г") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = product.carbs100,
                                    onValueChange = { products[index] = products[index].copy(carbs100 = it.filter { ch -> ch.isDigit() }) },
                                    label = { Text("У/100г") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            OutlinedTextField(
                                value = product.availableGrams,
                                onValueChange = { products[index] = products[index].copy(availableGrams = it.filter { ch -> ch.isDigit() }) },
                                label = { Text("Доступно (г)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            Divider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
                OutlinedButton(onClick = { products.add(FridgeProductUi()) }) {
                    Text("Добавить продукт")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allowExtraProducts, onCheckedChange = { allowExtraProducts = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Разрешить использование дополнительных продуктов при необходимости")
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
