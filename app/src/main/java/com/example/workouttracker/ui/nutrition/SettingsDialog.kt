package com.example.workouttracker.ui.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentNorm: Map<String, Int>,
    recommendedNorm: Norm?,
    onSave: (Map<String, Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var calories by remember { mutableStateOf(currentNorm["calories"]?.toString() ?: "2500") }
    var protein  by remember { mutableStateOf(currentNorm["protein"] ?.toString() ?: "120") }
    var carbs    by remember { mutableStateOf(currentNorm["carbs"]   ?.toString() ?: "300") }
    var fats     by remember { mutableStateOf(currentNorm["fats"]    ?.toString() ?: "80") }

    var calErr by remember { mutableStateOf<String?>(null) }
    var pErr by remember { mutableStateOf<String?>(null) }
    var cErr by remember { mutableStateOf<String?>(null) }
    var fErr by remember { mutableStateOf<String?>(null) }
    var warningMessage by remember { mutableStateOf<String?>(null) }

    // Диапазоны
    val CAL_MIN = 800;  val CAL_MAX = 6000
    val P_MIN   = 20;   val P_MAX   = 400
    val C_MIN   = 50;   val C_MAX   = 800
    val F_MIN   = 20;   val F_MAX   = 300

    fun Int.inRange(min: Int, max: Int) = this in min..max

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Нормы КБЖУ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (recommendedNorm != null) {
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Рекомендованная норма",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Калории: ${recommendedNorm.calories}")
                            Text("Белки: ${recommendedNorm.protein} г")
                            Text("Жиры: ${recommendedNorm.fats} г")
                            Text("Углеводы: ${recommendedNorm.carbs} г")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = calories,
                    onValueChange = {
                        calories = it.filter { ch -> ch.isDigit() }.take(4)
                        calErr = null
                        warningMessage = null
                    },
                    label = { Text("Калории (ккал)") },
                    isError = calErr != null,
                    supportingText = { if (calErr != null) Text(calErr!!, color = MaterialTheme.colorScheme.error) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { if (recommendedNorm != null) Text(recommendedNorm.calories.toString()) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = {
                            protein = it.filter { ch -> ch.isDigit() }.take(3)
                            pErr = null
                            warningMessage = null
                        },
                        label = { Text("Белки (г)") },
                        isError = pErr != null,
                        supportingText = { if (pErr != null) Text(pErr!!, color = MaterialTheme.colorScheme.error) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { if (recommendedNorm != null) Text(recommendedNorm.protein.toString()) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = {
                            carbs = it.filter { ch -> ch.isDigit() }.take(3)
                            cErr = null
                            warningMessage = null
                        },
                        label = { Text("Углеводы (г)") },
                        isError = cErr != null,
                        supportingText = { if (cErr != null) Text(cErr!!, color = MaterialTheme.colorScheme.error) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { if (recommendedNorm != null) Text(recommendedNorm.carbs.toString()) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fats,
                        onValueChange = {
                            fats = it.filter { ch -> ch.isDigit() }.take(3)
                            fErr = null
                            warningMessage = null
                        },
                        label = { Text("Жиры (г)") },
                        isError = fErr != null,
                        supportingText = { if (fErr != null) Text(fErr!!, color = MaterialTheme.colorScheme.error) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { if (recommendedNorm != null) Text(recommendedNorm.fats.toString()) },
                        modifier = Modifier.weight(1f)
                    )
                }

                warningMessage?.let { message ->
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    var ok = true
                    val cal = calories.toIntOrNull() ?: run { calErr = "Число"; ok = false; 0 }
                    val pr  = protein.toIntOrNull()  ?: run { pErr  = "Число"; ok = false; 0 }
                    val cr  = carbs.toIntOrNull()    ?: run { cErr  = "Число"; ok = false; 0 }
                    val ft  = fats.toIntOrNull()     ?: run { fErr  = "Число"; ok = false; 0 }

                    if (ok) {
                        if (!cal.inRange(CAL_MIN, CAL_MAX)) { calErr = "$CAL_MIN–$CAL_MAX"; ok = false }
                        if (!pr.inRange(P_MIN, P_MAX))       { pErr  = "$P_MIN–$P_MAX";     ok = false }
                        if (!cr.inRange(C_MIN, C_MAX))       { cErr  = "$C_MIN–$C_MAX";     ok = false }
                        if (!ft.inRange(F_MIN, F_MAX))       { fErr  = "$F_MIN–$F_MAX";     ok = false }
                    }
                    if (!ok) return@TextButton

                    if (recommendedNorm != null) {
                        val diffCaloriesPercent = abs(cal - recommendedNorm.calories) * 100f / recommendedNorm.calories
                        if (diffCaloriesPercent > 20f && warningMessage == null) {
                            warningMessage = "Ваша пользовательская норма сильно отличается от рекомендованной. Вы уверены?"
                            return@TextButton
                        }
                    }

                    onSave(mapOf("calories" to cal, "protein" to pr, "carbs" to cr, "fats" to ft))
                    warningMessage = null
                },
                enabled = calories.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

