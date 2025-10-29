package com.example.workouttracker.ui.profile

import android.app.DatePickerDialog
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val user by authViewModel.userState.collectAsState()
    var editableUser by remember { mutableStateOf(user) }
    var fieldToEdit by remember { mutableStateOf<String?>(null) }
    var tempText by remember { mutableStateOf("") }
    val context = LocalContext.current
    var showImagePicker by remember { mutableStateOf(false) }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            editableUser = editableUser?.copy(avatarUri = it.toString())
        }
    }
    var showDatePickerFor by remember { mutableStateOf<String?>(null) }
    if (showDatePickerFor != null && editableUser != null) {
        val (year, month, day) = Calendar.getInstance().run {
            Triple(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH))
        }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                editableUser = when (showDatePickerFor) {
                    "Дата измерений" ->
                        editableUser!!.copy(measurementDate = authViewModel.formatDate(y, m, d))
                    "Дедлайн цели" ->
                        editableUser!!.copy(goalDeadline = authViewModel.formatDate(y, m, d))
                    else -> editableUser
                }
                showDatePickerFor = null
            },
            year, month, day
        ).show()
    }
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .imePadding()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Профиль", style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onLogout) {
                Text("Выйти")
            }
        }
        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            val uri = editableUser?.avatarUri?.let { Uri.parse(it) }
            if (uri != null) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Аватар",
                    modifier = Modifier.size(100.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .size(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Нет\nаватара", textAlign = TextAlign.Center)
                }
            }
            IconButton(
                onClick = { imageLauncher.launch("image/*") },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Изменить аватар")
            }
        }
        @Composable
        fun ProfileField(label: String, value: String) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                    Text(value, style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(onClick = {
                    fieldToEdit = label
                    tempText = value
                }) {
                    Text("Изменить")
                }
            }
        }
        editableUser?.let { u ->
            ProfileField("E-mail", u.email)
            ProfileField("Пароль", "••••••••")
            ProfileField("Имя", u.firstName)
            ProfileField("Фамилия", u.lastName)
            ProfileField("Возраст", u.age.toString())
            ProfileField("Пол", u.gender)
            ProfileField("Рост (см)", u.height.toString())
            ProfileField("Вес (кг)", u.weight.toString())
            ProfileField("Плечи (см)", u.shoulders.toString())
            ProfileField("Талия (см)", u.waist.toString())
            ProfileField("Бёдра (см)", u.hips.toString())
            ProfileField("Грудь (см)", u.chest.toString())
            ProfileField("Дата измерений", u.measurementDate)
            Spacer(Modifier.height(8.dp))
            ProfileField("Цель", u.goalName)
            ProfileField("Дедлайн цели", u.goalDeadline)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                editableUser?.let {
                    authViewModel.updateUser(it)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить изменения")
        }
    }
    if (fieldToEdit != null) {
        AlertDialog(
            onDismissRequest = { fieldToEdit = null },
            title = { Text("Изменить $fieldToEdit") },
            text = {
                OutlinedTextField(
                    value = tempText,
                    onValueChange = { tempText = it },
                    label = { Text(fieldToEdit!!) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editableUser = when (fieldToEdit) {
                        "E-mail"           -> editableUser!!.copy(email = tempText)
                        "Пароль"           -> editableUser!!.copy(password = tempText)
                        "Имя"              -> editableUser!!.copy(firstName = tempText)
                        "Фамилия"          -> editableUser!!.copy(lastName = tempText)
                        "Возраст"          -> editableUser!!.copy(age = tempText.toIntOrNull() ?: 0)
                        "Пол"              -> editableUser!!.copy(gender = tempText)
                        "Рост (см)"        -> editableUser!!.copy(height = tempText.toFloatOrNull() ?: 0f)
                        "Вес (кг)"         -> editableUser!!.copy(weight = tempText.toFloatOrNull() ?: 0f)
                        "Плечи (см)"       -> editableUser!!.copy(shoulders = tempText.toFloatOrNull() ?: 0f)
                        "Талия (см)"       -> editableUser!!.copy(waist = tempText.toFloatOrNull() ?: 0f)
                        "Бёдра (см)"       -> editableUser!!.copy(hips = tempText.toFloatOrNull() ?: 0f)
                        "Грудь (см)"       -> editableUser!!.copy(chest = tempText.toFloatOrNull() ?: 0f)
                        "Дата измерений"   -> editableUser!!.copy(measurementDate = tempText)
                        "Цель"            -> editableUser!!.copy(goalName     = tempText)
                        "Дедлайн цели"    -> editableUser!!.copy(goalDeadline = tempText)
                        else               -> editableUser!!
                    }
                    fieldToEdit = null
                }) {
                    Text("ОК")
                }
            },
            dismissButton = {
                TextButton(onClick = { fieldToEdit = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}
