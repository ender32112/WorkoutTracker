package com.example.workouttracker.ui.auth

import com.example.workouttracker.viewmodel.User
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.workouttracker.viewmodel.AuthViewModel
import java.util.*

@Composable
fun RegistrationScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Мужской") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        avatarUri = uri
    }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var shoulders by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var hips by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    val context = LocalContext.current
    var measurementDate by remember {
        mutableStateOf(
            authViewModel.formatDate(
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            )
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        DatePickerDialog(
            context,
            { _, y, m, d ->
                measurementDate = authViewModel.formatDate(y, m, d)
                showDatePicker = false
            },
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    var selectedPresetGoal by remember { mutableStateOf<String?>(null) }
    var customGoalMode by remember { mutableStateOf(false) }
    var customGoalName by remember { mutableStateOf("") }
    var customGoalType by remember { mutableStateOf("") }
    var customGoalDescription by remember { mutableStateOf("") }
    var goalDeadline by remember {
        mutableStateOf(
            authViewModel.formatDate(
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            )
        )
    }
    var showGoalDatePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .statusBarsPadding()
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Фамилия") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Возраст") },
            modifier = Modifier.fillMaxWidth()
        )
        Row {
            listOf("Мужской", "Женский").forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    RadioButton(
                        selected = (gender == option),
                        onClick = { gender = option }
                    )
                    Text(option)
                }
            }
        }
        Button(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выбрать фотографию")
        }
        avatarUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .padding(vertical = 4.dp),
                contentScale = ContentScale.Crop
            )
        }
        OutlinedTextField(
            value = height,
            onValueChange = { height = it },
            label = { Text("Рост (см)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it },
            label = { Text("Вес (кг)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = shoulders,
            onValueChange = { shoulders = it },
            label = { Text("Плечи (см)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = waist,
            onValueChange = { waist = it },
            label = { Text("Талия (см)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = hips,
            onValueChange = { hips = it },
            label = { Text("Бёдра (см)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = chest,
            onValueChange = { chest = it },
            label = { Text("Грудные (см)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Дата измерений: $measurementDate")
        }
        Text("Что ваша цель?", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Похудение", "Поддержание веса", "Набор массы").forEach { goal ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedPresetGoal == goal && !customGoalMode,
                        onClick = {
                            customGoalMode = false
                            selectedPresetGoal = goal
                        }
                    )
                    Text(goal)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = customGoalMode,
                    onClick = {
                        customGoalMode = true
                        selectedPresetGoal = null
                    }
                )
                Text("Другая цель")
            }
        }
        Button(
            onClick = { showGoalDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Дедлайн выполнения: $goalDeadline")
        }
        if (showGoalDatePicker) {
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    goalDeadline = authViewModel.formatDate(y, m, d)
                    showGoalDatePicker = false
                },
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        if (customGoalMode) {
            OutlinedTextField(
                value = customGoalName,
                onValueChange = { customGoalName = it },
                label = { Text("Название цели") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = customGoalType,
                onValueChange = { customGoalType = it },
                label = { Text("Тип цели") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = customGoalDescription,
                onValueChange = { customGoalDescription = it },
                label = { Text("Описание цели") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val goalName = if (customGoalMode) customGoalName else (selectedPresetGoal ?: "")
                val user = User(
                    email               = email.trim(),
                    password            = password,
                    firstName           = firstName,
                    lastName            = lastName,
                    age                 = age.toIntOrNull() ?: 0,
                    gender              = gender,
                    avatarUri           = avatarUri?.toString(),
                    height              = height.toFloatOrNull() ?: 0f,
                    weight              = weight.toFloatOrNull() ?: 0f,
                    shoulders           = shoulders.toFloatOrNull() ?: 0f,
                    waist               = waist.toFloatOrNull() ?: 0f,
                    hips                = hips.toFloatOrNull() ?: 0f,
                    chest               = chest.toFloatOrNull() ?: 0f,
                    measurementDate     = measurementDate,
                    goalName            = goalName,
                    goalDeadline        = goalDeadline
                )
                authViewModel.register(user)
                onRegisterSuccess()
            }, modifier = Modifier.fillMaxWidth()) {
            Text("Зарегистрироваться")
        }
    }
}
