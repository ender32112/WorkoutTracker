package com.example.workouttracker.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.workouttracker.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    authViewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit  // ДОБАВЛЕН ПАРАМЕТР
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Мужской") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    val registrationState by authViewModel.registrationState.collectAsState()

    LaunchedEffect(registrationState) {
        if (registrationState) {
            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Регистрация", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
            onValueChange = { age = it.filter { it.isDigit() } },
            label = { Text("Возраст") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Пол (выпадающий список)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                label = { Text("Пол") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("Мужской", "Женский").forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            gender = option
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = height,
            onValueChange = { height = it.filter { it.isDigit() || it == '.' } },
            label = { Text("Рост (см)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it.filter { it.isDigit() || it == '.' } },
            label = { Text("Вес (кг)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val user = com.example.workouttracker.viewmodel.User(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    age = age.toIntOrNull() ?: 0,
                    gender = gender,
                    avatarUri = null,
                    height = height.toFloatOrNull() ?: 0f,
                    weight = weight.toFloatOrNull() ?: 0f,
                    shoulders = 0f,
                    waist = 0f,
                    hips = 0f,
                    chest = 0f,
                    measurementDate = "",
                    goalName = "",
                    goalDeadline = ""
                )
                authViewModel.register(user)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = email.isNotBlank() && password.isNotBlank()
        ) {
            Text("Зарегистрироваться")
        }

        TextButton(
            onClick = onNavigateBack,  // ИСПОЛЬЗУЕМ ПАРАМЕТР
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Уже есть аккаунт? Войти")
        }
    }
}