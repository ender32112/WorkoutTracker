package com.example.workouttracker.ui.profile

import android.app.DatePickerDialog
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import com.example.workouttracker.viewmodel.AuthViewModel
import com.example.workouttracker.viewmodel.User
import java.io.File
import java.io.FileOutputStream
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
    val scrollState = rememberScrollState()

    // === КОПИРОВАНИЕ ФОТО В ЛОКАЛЬНОЕ ХРАНИЛИЩЕ ===
    fun saveAvatarToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val internalPath = saveAvatarToInternalStorage(selectedUri)
            internalPath?.let { path ->
                // Удаляем старое фото
                editableUser?.avatarUri?.let { oldPath ->
                    val oldFile = File(oldPath)
                    if (oldFile.exists()) oldFile.delete()
                }
                // Сохраняем новое
                editableUser = editableUser?.copy(avatarUri = path)
                authViewModel.updateUser(editableUser!!.copy(avatarUri = path))
            }
        }
    }

    var showDatePickerFor by remember { mutableStateOf<String?>(null) }
    if (showDatePickerFor != null && editableUser != null) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val date = authViewModel.formatDate(y, m, d)
                editableUser = when (showDatePickerFor) {
                    "Дата измерений" -> editableUser!!.copy(measurementDate = date)
                    "Дедлайн цели" -> editableUser!!.copy(goalDeadline = date)
                    else -> editableUser
                }
                showDatePickerFor = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === ЗАГОЛОВОК ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Профиль",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(onClick = {
                    authViewModel.logout()
                    onLogout()
                }) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Выйти")
                }
            }

            // === АВАТАР ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarPath = editableUser?.avatarUri
                    if (avatarPath != null && File(avatarPath).exists()) {
                        Image(
                            painter = rememberAsyncImagePainter(Uri.fromFile(File(avatarPath))),
                            contentDescription = "Аватар",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    }

                    IconButton(
                        onClick = { imageLauncher.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .size(50.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Изменить", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // === ПОЛЯ ===
            editableUser?.let { u ->
                ProfileSection("Личное") {
                    ProfileField("E-mail", u.email, Icons.Default.Email, fieldToEdit, { fieldToEdit = it; tempText = u.email })
                    ProfileField("Имя", u.firstName, Icons.Default.Person, fieldToEdit, { fieldToEdit = it; tempText = u.firstName })
                    ProfileField("Фамилия", u.lastName, Icons.Default.PersonOutline, fieldToEdit, { fieldToEdit = it; tempText = u.lastName })
                    ProfileField("Возраст", u.age.toString(), Icons.Default.Cake, fieldToEdit, { fieldToEdit = it; tempText = u.age.toString() })
                    ProfileField("Пол", u.gender, Icons.Default.Transgender, fieldToEdit, { fieldToEdit = it; tempText = u.gender })
                }

                ProfileSection("Фигура") {
                    ProfileField("Рост (см)", u.height.toString(), Icons.Default.Height, fieldToEdit, { fieldToEdit = it; tempText = u.height.toString() })
                    ProfileField("Вес (кг)", u.weight.toString(), Icons.Default.FitnessCenter, fieldToEdit, { fieldToEdit = it; tempText = u.weight.toString() })
                    ProfileField("Плечи (см)", u.shoulders.toString(), Icons.Default.Accessibility, fieldToEdit, { fieldToEdit = it; tempText = u.shoulders.toString() })
                    ProfileField("Талия (см)", u.waist.toString(), Icons.Default.Accessibility, fieldToEdit, { fieldToEdit = it; tempText = u.waist.toString() })
                    ProfileField("Бёдра (см)", u.hips.toString(), Icons.Default.Accessibility, fieldToEdit, { fieldToEdit = it; tempText = u.hips.toString() })
                    ProfileField("Грудь (см)", u.chest.toString(), Icons.Default.Accessibility, fieldToEdit, { fieldToEdit = it; tempText = u.chest.toString() })
                    ProfileField("Дата измерений", u.measurementDate, Icons.Default.CalendarToday, fieldToEdit, { fieldToEdit = it; tempText = u.measurementDate }, isDate = true)
                }

                ProfileSection("Цели") {
                    ProfileField("Цель", u.goalName, Icons.Default.Flag, fieldToEdit, { fieldToEdit = it; tempText = u.goalName })
                    ProfileField("Дедлайн цели", u.goalDeadline, Icons.Default.Event, fieldToEdit, { fieldToEdit = it; tempText = u.goalDeadline }, isDate = true)
                }
            }
        }

        // === КНОПКА СОХРАНЕНИЯ — ФИКСИРОВАННАЯ ВНИЗУ ===
        Button(
            onClick = {
                editableUser?.let { authViewModel.updateUser(it) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            enabled = editableUser != user
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Сохранить изменения")
        }
    }

    // === ДИАЛОГ РЕДАКТИРОВАНИЯ ===
    if (fieldToEdit != null) {
        AlertDialog(
            onDismissRequest = { fieldToEdit = null },
            icon = { Icon(getFieldIcon(fieldToEdit!!), contentDescription = null) },
            title = { Text("Изменить $fieldToEdit") },
            text = {
                OutlinedTextField(
                    value = tempText,
                    onValueChange = { tempText = it },
                    label = { Text(fieldToEdit!!) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editableUser = updateUserField(editableUser, fieldToEdit!!, tempText)
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

// === ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ===
@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    icon: ImageVector,
    fieldToEdit: String?,
    onEditClick: (String) -> Unit,
    isDate: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        TextButton(
            onClick = { onEditClick(label) },
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Icon(if (isDate) Icons.Default.CalendarToday else Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Изменить", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun getFieldIcon(field: String): ImageVector = when (field) {
    "E-mail" -> Icons.Default.Email
    "Имя", "Фамилия" -> Icons.Default.Person
    "Возраст" -> Icons.Default.Cake
    "Пол" -> Icons.Default.Transgender
    "Рост (см)", "Вес (кг)", "Плечи (см)", "Талия (см)", "Бёдра (см)", "Грудь (см)" -> Icons.Default.Straighten
    "Дата измерений", "Дедлайн цели" -> Icons.Default.CalendarToday
    "Цель" -> Icons.Default.Flag
    else -> Icons.Default.Edit
}

private fun updateUserField(user: User?, field: String, value: String): User? = user?.let {
    when (field) {
        "E-mail" -> it.copy(email = value)
        "Имя" -> it.copy(firstName = value)
        "Фамилия" -> it.copy(lastName = value)
        "Возраст" -> it.copy(age = value.toIntOrNull() ?: 0)
        "Пол" -> it.copy(gender = value)
        "Рост (см)" -> it.copy(height = value.toFloatOrNull() ?: 0f)
        "Вес (кг)" -> it.copy(weight = value.toFloatOrNull() ?: 0f)
        "Плечи (см)" -> it.copy(shoulders = value.toFloatOrNull() ?: 0f)
        "Талия (см)" -> it.copy(waist = value.toFloatOrNull() ?: 0f)
        "Бёдра (см)" -> it.copy(hips = value.toFloatOrNull() ?: 0f)
        "Грудь (см)" -> it.copy(chest = value.toFloatOrNull() ?: 0f)
        "Дата измерений" -> it.copy(measurementDate = value)
        "Цель" -> it.copy(goalName = value)
        "Дедлайн цели" -> it.copy(goalDeadline = value)
        else -> it
    }
}