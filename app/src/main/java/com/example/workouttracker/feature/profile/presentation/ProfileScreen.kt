package com.example.workouttracker.feature.profile.presentation

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Transgender
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.workouttracker.ui.designsystem.AppTopBar
import com.example.workouttracker.ui.theme.ThemeVariant
import com.example.workouttracker.ui.theme.displayName
import com.example.workouttracker.ui.theme.icon
import com.example.workouttracker.viewmodel.User
import java.io.File

private enum class EditableProfileField {
    EMAIL,
    FIRST_NAME,
    LAST_NAME,
    AGE,
    GENDER,
    HEIGHT,
    WEIGHT,
    SHOULDERS,
    WAIST,
    HIPS,
    CHEST,
    GOAL
}

private const val FIELD_EMAIL = "E-mail"
private const val FIELD_FIRST_NAME = "Имя"
private const val FIELD_LAST_NAME = "Фамилия"
private const val FIELD_AGE = "Возраст"
private const val FIELD_GENDER = "Пол"
private const val FIELD_HEIGHT = "Рост (см)"
private const val FIELD_WEIGHT = "Вес (кг)"
private const val FIELD_SHOULDERS = "Плечи (см)"
private const val FIELD_WAIST = "Талия (см)"
private const val FIELD_HIPS = "Бёдра (см)"
private const val FIELD_CHEST = "Грудь (см)"
private const val FIELD_MEASUREMENT_DATE = "Дата измерений"
private const val FIELD_GOAL = "Цель"
private const val FIELD_GOAL_DEADLINE = "Дедлайн цели"

@Composable
fun ProfileScreen(
    editableUser: User?,
    savedUser: User?,
    snackbarHostState: SnackbarHostState,
    currentTheme: ThemeVariant,
    onToggleTheme: () -> Unit = {},
    onLogout: () -> Unit,
    onSave: () -> Unit,
    onPickAvatar: () -> Unit,
    onUserChange: (User?) -> Unit,
    onRequestDatePicker: (String) -> Unit
) {
    var fieldToEdit by remember { mutableStateOf<EditableProfileField?>(null) }
    var tempText by remember { mutableStateOf("") }
    var tempError by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                title = "Профиль",
                actions = {
                    AssistChip(
                        onClick = onToggleTheme,
                        label = {
                            Text(
                                text = currentTheme.displayName(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = currentTheme.icon(),
                                contentDescription = "Текущая тема"
                            )
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onLogout,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Выйти")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileHero(
                    user = editableUser,
                    currentTheme = currentTheme,
                    onPickAvatar = onPickAvatar
                )

                editableUser?.let { user ->
                    ProfileSection(title = "Личные данные") {
                        ProfileFieldRow(FIELD_EMAIL, user.email, Icons.Default.Email, onEditClick = {
                            fieldToEdit = EditableProfileField.EMAIL
                            tempText = user.email
                            tempError = null
                        })
                        ProfileFieldRow(FIELD_FIRST_NAME, user.firstName, Icons.Default.Person, onEditClick = {
                            fieldToEdit = EditableProfileField.FIRST_NAME
                            tempText = user.firstName
                            tempError = null
                        })
                        ProfileFieldRow(FIELD_LAST_NAME, user.lastName, Icons.Default.PersonOutline, onEditClick = {
                            fieldToEdit = EditableProfileField.LAST_NAME
                            tempText = user.lastName
                            tempError = null
                        })
                        ProfileFieldRow(FIELD_AGE, user.age.toString(), Icons.Default.Cake, onEditClick = {
                            fieldToEdit = EditableProfileField.AGE
                            tempText = user.age.toString()
                            tempError = null
                        })
                        ProfileFieldRow(FIELD_GENDER, user.gender, Icons.Default.Transgender, onEditClick = {
                            fieldToEdit = EditableProfileField.GENDER
                            tempText = user.gender
                            tempError = null
                        })
                    }

                    ProfileSection(title = "Тело и замеры") {
                        ProfileFieldRow(FIELD_HEIGHT, user.height.toString(), Icons.Default.Height, onEditClick = {
                            fieldToEdit = EditableProfileField.HEIGHT
                            tempText = user.height.toString()
                            tempError = null
                        })
                        ProfileFieldRow(FIELD_WEIGHT, user.weight.toString(), Icons.Default.FitnessCenter, onEditClick = {
                            fieldToEdit = EditableProfileField.WEIGHT
                            tempText = user.weight.toString()
                            tempError = null
                        })
                        ProfileFieldRow(FIELD_SHOULDERS, user.shoulders.toString(), Icons.Default.Accessibility, onEditClick = {
                            fieldToEdit = EditableProfileField.SHOULDERS
                            tempText = user.shoulders.toString()
                            tempError = null
                        })
                        ProfileFieldRow(FIELD_WAIST, user.waist.toString(), Icons.Default.Accessibility, onEditClick = {
                            fieldToEdit = EditableProfileField.WAIST
                            tempText = user.waist.toString()
                            tempError = null
                        })
                        ProfileFieldRow(FIELD_HIPS, user.hips.toString(), Icons.Default.Accessibility, onEditClick = {
                            fieldToEdit = EditableProfileField.HIPS
                            tempText = user.hips.toString()
                            tempError = null
                        })
                        ProfileFieldRow(FIELD_CHEST, user.chest.toString(), Icons.Default.Accessibility, onEditClick = {
                            fieldToEdit = EditableProfileField.CHEST
                            tempText = user.chest.toString()
                            tempError = null
                        })
                        ProfileFieldRow(
                            label = FIELD_MEASUREMENT_DATE,
                            value = user.measurementDate,
                            icon = Icons.Default.CalendarToday,
                            isDate = true,
                            onDateClick = onRequestDatePicker
                        )
                    }

                    ProfileSection(title = "Цели") {
                        ProfileFieldRow(FIELD_GOAL, user.goalName, Icons.Default.Flag, onEditClick = {
                            fieldToEdit = EditableProfileField.GOAL
                            tempText = user.goalName
                            tempError = null
                        })
                        ProfileFieldRow(
                            label = FIELD_GOAL_DEADLINE,
                            value = user.goalDeadline,
                            icon = Icons.Default.Event,
                            isDate = true,
                            onDateClick = onRequestDatePicker
                        )
                    }
                }

                Spacer(Modifier.height(88.dp))
            }

            Button(
                onClick = onSave,
                enabled = editableUser != null && editableUser != savedUser,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Сохранить изменения")
            }
        }
    }

    if (fieldToEdit != null) {
        val (keyboardType, hint) = when (fieldToEdit) {
            EditableProfileField.EMAIL -> KeyboardType.Email to "mail@example.com"
            EditableProfileField.AGE -> KeyboardType.Number to "5-100"
            EditableProfileField.HEIGHT,
            EditableProfileField.WEIGHT,
            EditableProfileField.SHOULDERS,
            EditableProfileField.WAIST,
            EditableProfileField.HIPS,
            EditableProfileField.CHEST ->
                KeyboardType.Number to "Например, 172"
            else -> KeyboardType.Text to ""
        }

        AlertDialog(
            onDismissRequest = { fieldToEdit = null },
            icon = { Icon(iconFor(fieldToEdit), contentDescription = null) },
            title = { Text("Изменить $fieldToEdit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempText,
                        onValueChange = {
                            tempText = when (fieldToEdit) {
                                EditableProfileField.AGE -> it.filter(Char::isDigit).take(3)
                                EditableProfileField.HEIGHT,
                                EditableProfileField.WEIGHT,
                                EditableProfileField.SHOULDERS,
                                EditableProfileField.WAIST,
                                EditableProfileField.HIPS,
                                EditableProfileField.CHEST ->
                                    it.filter { symbol -> symbol.isDigit() || symbol == '.' || symbol == ',' }.take(6)
                                else -> it
                            }
                            tempError = null
                        },
                        label = { Text(hint.ifBlank { labelFor(fieldToEdit) }) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = keyboardType
                        ),
                        isError = tempError != null,
                        supportingText = {
                            tempError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (fieldToEdit == EditableProfileField.GENDER) {
                        Text(
                            "Допустимые значения: Мужчина или Женщина",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        tempError = validateEditableField(fieldToEdit, tempText)
                        if (tempError == null) {
                            onUserChange(applyEditableField(editableUser, fieldToEdit, tempText))
                            fieldToEdit = null
                        }
                    }
                ) {
                    Text("Сохранить")
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

@Composable
private fun ProfileHero(
    user: User?,
    currentTheme: ThemeVariant,
    onPickAvatar: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarPath = user?.avatarUri
                    if (!avatarPath.isNullOrBlank() && File(avatarPath).exists()) {
                        Image(
                            painter = rememberAsyncImagePainter(Uri.fromFile(File(avatarPath))),
                            contentDescription = "Аватар",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    IconButton(
                        onClick = onPickAvatar,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Изменить фото",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Text(
                text = listOf(user?.firstName.orEmpty(), user?.lastName.orEmpty())
                    .joinToString(" ")
                    .trim()
                    .ifBlank { "Ваш профиль" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = user?.email?.ifBlank { "Добавьте e-mail и базовые данные о себе" }
                    ?: "Добавьте e-mail и базовые данные о себе",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        text = "Тема: ${currentTheme.displayName()}",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = currentTheme.icon(),
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ProfileFieldRow(
    label: String,
    value: String,
    icon: ImageVector,
    onEditClick: (String) -> Unit = {},
    isDate: Boolean = false,
    onDateClick: (String) -> Unit = {}
) {
    val action = if (isDate) {
        { onDateClick(label) }
    } else {
        { onEditClick(label) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = action)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.ifBlank { "—" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        if (isDate) {
            TextButton(
                onClick = action,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Выбрать")
            }
        } else {
            TextButton(
                onClick = action,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Изменить")
            }
        }
    }
}

private fun labelFor(field: EditableProfileField?): String = when (field) {
    EditableProfileField.EMAIL -> FIELD_EMAIL
    EditableProfileField.FIRST_NAME -> FIELD_FIRST_NAME
    EditableProfileField.LAST_NAME -> FIELD_LAST_NAME
    EditableProfileField.AGE -> FIELD_AGE
    EditableProfileField.GENDER -> FIELD_GENDER
    EditableProfileField.HEIGHT -> FIELD_HEIGHT
    EditableProfileField.WEIGHT -> FIELD_WEIGHT
    EditableProfileField.SHOULDERS -> FIELD_SHOULDERS
    EditableProfileField.WAIST -> FIELD_WAIST
    EditableProfileField.HIPS -> FIELD_HIPS
    EditableProfileField.CHEST -> FIELD_CHEST
    EditableProfileField.GOAL -> FIELD_GOAL
    null -> ""
}

private fun validateEditableField(field: EditableProfileField?, raw: String): String? {
    fun toFloat() = raw.replace(',', '.').toFloatOrNull()
    fun toInt() = raw.toIntOrNull()

    return when (field) {
        EditableProfileField.EMAIL -> {
            val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            if (!regex.matches(raw.trim())) "Некорректный e-mail" else null
        }
        EditableProfileField.FIRST_NAME, EditableProfileField.LAST_NAME ->
            if (raw.trim().length < 2) "Слишком короткое значение" else null
        EditableProfileField.GENDER -> {
            val normalized = raw.trim().lowercase()
            if (normalized !in listOf("мужчина", "женщина")) "Введите: Мужчина или Женщина" else null
        }
        EditableProfileField.AGE -> {
            val value = toInt() ?: return "Нужно целое число"
            if (value !in 5..100) "Диапазон 5-100" else null
        }
        EditableProfileField.HEIGHT -> {
            val value = toFloat() ?: return "Введите число"
            if (value !in 80f..250f) "Диапазон 80-250" else null
        }
        EditableProfileField.WEIGHT -> {
            val value = toFloat() ?: return "Введите число"
            if (value !in 25f..350f) "Диапазон 25-350" else null
        }
        EditableProfileField.SHOULDERS,
        EditableProfileField.WAIST,
        EditableProfileField.HIPS,
        EditableProfileField.CHEST -> {
            val value = toFloat() ?: return "Введите число"
            if (value !in 20f..300f) "Проверьте значение" else null
        }
        EditableProfileField.GOAL -> if (raw.trim().isEmpty()) "Поле не может быть пустым" else null
        null -> null
    }
}

private fun applyEditableField(user: User?, field: EditableProfileField?, raw: String): User? = user?.let {
    val value = raw.trim()
    fun floatValue() = value.replace(',', '.').toFloatOrNull() ?: 0f
    fun intValue() = value.toIntOrNull() ?: 0

    when (field) {
        EditableProfileField.EMAIL -> it.copy(email = value)
        EditableProfileField.FIRST_NAME -> it.copy(firstName = value)
        EditableProfileField.LAST_NAME -> it.copy(lastName = value)
        EditableProfileField.GENDER -> it.copy(gender = value.replaceFirstChar { char -> char.titlecase() })
        EditableProfileField.AGE -> it.copy(age = intValue())
        EditableProfileField.HEIGHT -> it.copy(height = floatValue())
        EditableProfileField.WEIGHT -> it.copy(weight = floatValue())
        EditableProfileField.SHOULDERS -> it.copy(shoulders = floatValue())
        EditableProfileField.WAIST -> it.copy(waist = floatValue())
        EditableProfileField.HIPS -> it.copy(hips = floatValue())
        EditableProfileField.CHEST -> it.copy(chest = floatValue())
        EditableProfileField.GOAL -> it.copy(goalName = value)
        null -> it
    }
}

private fun iconFor(field: EditableProfileField?): ImageVector = when (field) {
    EditableProfileField.EMAIL -> Icons.Default.Email
    EditableProfileField.FIRST_NAME, EditableProfileField.LAST_NAME -> Icons.Default.Person
    EditableProfileField.GENDER -> Icons.Default.Transgender
    EditableProfileField.AGE -> Icons.Default.Cake
    EditableProfileField.HEIGHT,
    EditableProfileField.WEIGHT,
    EditableProfileField.SHOULDERS,
    EditableProfileField.WAIST,
    EditableProfileField.HIPS,
    EditableProfileField.CHEST -> Icons.Default.Straighten
    EditableProfileField.GOAL -> Icons.Default.Flag
    null -> Icons.Default.Edit
}

private fun validateField(field: String, raw: String): String? {
    fun toFloat() = raw.replace(',', '.').toFloatOrNull()
    fun toInt() = raw.toIntOrNull()

    return when (field) {
        FIELD_EMAIL -> {
            val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            if (!regex.matches(raw.trim())) "Некорректный e-mail" else null
        }
        FIELD_FIRST_NAME, FIELD_LAST_NAME ->
            if (raw.trim().length < 2) "Слишком короткое значение" else null
        FIELD_GENDER -> {
            val normalized = raw.trim().lowercase()
            if (normalized !in listOf("мужчина", "женщина")) "Введите: Мужчина или Женщина" else null
        }
        FIELD_AGE -> {
            val value = toInt() ?: return "Нужно целое число"
            if (value !in 5..100) "Диапазон 5-100" else null
        }
        FIELD_HEIGHT -> {
            val value = toFloat() ?: return "Введите число"
            if (value !in 80f..250f) "Диапазон 80-250" else null
        }
        FIELD_WEIGHT -> {
            val value = toFloat() ?: return "Введите число"
            if (value !in 25f..350f) "Диапазон 25-350" else null
        }
        FIELD_SHOULDERS, FIELD_WAIST, FIELD_HIPS, FIELD_CHEST -> {
            val value = toFloat() ?: return "Введите число"
            if (value !in 20f..300f) "Проверьте значение" else null
        }
        FIELD_GOAL -> if (raw.trim().isEmpty()) "Поле не может быть пустым" else null
        else -> null
    }
}

private fun applyField(user: User?, field: String, raw: String): User? = user?.let {
    val value = raw.trim()
    fun floatValue() = value.replace(',', '.').toFloatOrNull() ?: 0f
    fun intValue() = value.toIntOrNull() ?: 0

    when (field) {
        FIELD_EMAIL -> it.copy(email = value)
        FIELD_FIRST_NAME -> it.copy(firstName = value)
        FIELD_LAST_NAME -> it.copy(lastName = value)
        FIELD_GENDER -> it.copy(gender = value.replaceFirstChar { char -> char.titlecase() })
        FIELD_AGE -> it.copy(age = intValue())
        FIELD_HEIGHT -> it.copy(height = floatValue())
        FIELD_WEIGHT -> it.copy(weight = floatValue())
        FIELD_SHOULDERS -> it.copy(shoulders = floatValue())
        FIELD_WAIST -> it.copy(waist = floatValue())
        FIELD_HIPS -> it.copy(hips = floatValue())
        FIELD_CHEST -> it.copy(chest = floatValue())
        FIELD_GOAL -> it.copy(goalName = value)
        else -> it
    }
}

private fun getFieldIcon(field: String): ImageVector = when (field) {
    FIELD_EMAIL -> Icons.Default.Email
    FIELD_FIRST_NAME, FIELD_LAST_NAME -> Icons.Default.Person
    FIELD_GENDER -> Icons.Default.Transgender
    FIELD_AGE -> Icons.Default.Cake
    FIELD_HEIGHT, FIELD_WEIGHT, FIELD_SHOULDERS, FIELD_WAIST, FIELD_HIPS, FIELD_CHEST -> Icons.Default.Straighten
    FIELD_MEASUREMENT_DATE, FIELD_GOAL_DEADLINE -> Icons.Default.CalendarToday
    FIELD_GOAL -> Icons.Default.Flag
    else -> Icons.Default.Edit
}
