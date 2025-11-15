package com.example.workouttracker.ui.profile

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.workouttracker.ui.components.SectionHeader
import com.example.workouttracker.ui.theme.ThemeVariant
import com.example.workouttracker.ui.theme.icon
import com.example.workouttracker.ui.theme.displayName
import com.example.workouttracker.viewmodel.AuthViewModel
import com.example.workouttracker.viewmodel.User
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

/* ====================== Профиль ====================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onToggleTheme: () -> Unit = {}, // ← колбэк смены темы
    currentTheme: ThemeVariant
) {
    val context = LocalContext.current
    val user by authViewModel.userState.collectAsState()
    val authError by authViewModel.authError.collectAsState()
    var editableUser by remember(user) { mutableStateOf(user) }

    // Диалог редактирования конкретного поля
    var fieldToEdit by remember { mutableStateOf<String?>(null) }
    var tempText by remember { mutableStateOf("") }
    var tempError by remember { mutableStateOf<String?>(null) }

    // Календарь — показывает школу только для дат (строго)
    var showDatePickerFor by remember { mutableStateOf<String?>(null) }

    val scroll = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authError) {
        authError?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearAuthError()
        }
    }

    /* ---------- Аватар: копирование в internal storage ---------- */
    fun saveAvatarToInternalStorage(uri: Uri): String? =
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output -> input.copyTo(output) }
            file.absolutePath
        } catch (_: Exception) { null }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val internalPath = saveAvatarToInternalStorage(uri) ?: return@rememberLauncherForActivityResult
        // удалить старый, если был
        editableUser?.avatarUri?.let { old ->
            runCatching {
                val f = File(old); if (f.exists()) f.delete()
            }
        }
        editableUser = editableUser?.copy(avatarUri = internalPath)
        editableUser?.let { authViewModel.updateUser(it) }
    }

    /* ---------- UI ---------- */
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SectionHeader(
                title = "Профиль",
                titleStyle = MaterialTheme.typography.headlineSmall,
                actions = {
                    // Смена темы
                    AssistChip(
                        onClick = onToggleTheme,
                        label = {
                            Text(
                                text = currentTheme.displayName(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.widthIn(max = 120.dp) // ограничиваем ширину, чтобы не заезжало на заголовок
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
                    // Выход
                    OutlinedButton(
                        onClick = {
                            authViewModel.logout()
                            onLogout()
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Выйти")
                    }
                }
            )
        }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                /* -------- Аватар -------- */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .border(
                                3.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                CircleShape
                            )
                            .shadow(6.dp, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val avatarPath = editableUser?.avatarUri
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
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        IconButton(
                            onClick = { imageLauncher.launch("image/*") },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 8.dp, y = 8.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Изменить",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                /* -------- Блоки полей -------- */
                editableUser?.let { u ->
                    ProfileSection(title = "Личное") {
                        ProfileFieldRow(
                            label = "E-mail",
                            value = u.email,
                            icon = Icons.Default.Email,
                            onEditClick = {
                                fieldToEdit = it; tempText = u.email; tempError = null
                            }
                        )
                        ProfileFieldRow(
                            label = "Имя",
                            value = u.firstName,
                            icon = Icons.Default.Person,
                            onEditClick = { fieldToEdit = it; tempText = u.firstName; tempError = null }
                        )
                        ProfileFieldRow(
                            label = "Фамилия",
                            value = u.lastName,
                            icon = Icons.Default.PersonOutline,
                            onEditClick = { fieldToEdit = it; tempText = u.lastName; tempError = null }
                        )
                        ProfileFieldRow(
                            label = "Возраст",
                            value = u.age.toString(),
                            icon = Icons.Default.Cake,
                            onEditClick = { fieldToEdit = it; tempText = u.age.toString(); tempError = null }
                        )
                        ProfileFieldRow(
                            label = "Пол",
                            value = u.gender,
                            icon = Icons.Default.Transgender,
                            onEditClick = { fieldToEdit = it; tempText = u.gender; tempError = null }
                        )
                    }

                    ProfileSection(title = "Фигура") {
                        ProfileFieldRow(
                            label = "Рост (см)",
                            value = u.height.toString(),
                            icon = Icons.Default.Height,
                            onEditClick = { label ->
                                fieldToEdit = label
                                tempText = u.height.toString()
                                tempError = null
                            }
                        )

                        ProfileFieldRow(
                            label = "Вес (кг)",
                            value = u.weight.toString(),
                            icon = Icons.Default.FitnessCenter,
                            onEditClick = { label ->
                                fieldToEdit = label
                                tempText = u.weight.toString()
                                tempError = null
                            }
                        )

                        ProfileFieldRow(
                            label = "Плечи (см)",
                            value = u.shoulders.toString(),
                            icon = Icons.Default.Accessibility,
                            onEditClick = { label ->
                                fieldToEdit = label
                                tempText = u.shoulders.toString()
                                tempError = null
                            }
                        )

                        ProfileFieldRow(
                            label = "Талия (см)",
                            value = u.waist.toString(),
                            icon = Icons.Default.Accessibility,
                            onEditClick = { label ->
                                fieldToEdit = label
                                tempText = u.waist.toString()
                                tempError = null
                            }
                        )

                        ProfileFieldRow(
                            label = "Бёдра (см)",
                            value = u.hips.toString(),
                            icon = Icons.Default.Accessibility,
                            onEditClick = { label ->
                                fieldToEdit = label
                                tempText = u.hips.toString()
                                tempError = null
                            }
                        )

                        ProfileFieldRow(
                            label = "Грудь (см)",
                            value = u.chest.toString(),
                            icon = Icons.Default.Accessibility,
                            onEditClick = { label ->
                                fieldToEdit = label
                                tempText = u.chest.toString()
                                tempError = null
                            }
                        )

                        ProfileFieldRow(
                            label = "Дата измерений",
                            value = u.measurementDate,
                            icon = Icons.Default.CalendarToday,
                            isDate = true,
                            onDateClick = { label ->
                                showDatePickerFor = label
                            }
                        )
                    }


                    ProfileSection(title = "Цели") {
                        ProfileFieldRow("Цель", u.goalName, Icons.Default.Flag) {
                            fieldToEdit = it; tempText = u.goalName; tempError = null
                        }
                        ProfileFieldRow(
                            label = "Дедлайн цели",
                            value = u.goalDeadline,
                            icon = Icons.Default.Event,
                            onEditClick = { /* только календарь */ },
                            isDate = true,
                            onDateClick = { showDatePickerFor = it }
                        )
                    }
                }
            }

            // Кнопка сохранения (фиксированная)
            Button(
                onClick = { editableUser?.let { authViewModel.updateUser(it) } },
                enabled = editableUser != user,
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

    /* ---------- Диалог редактирования (НЕ для дат) ---------- */
    if (fieldToEdit != null && fieldToEdit !in listOf("Дата измерений", "Дедлайн цели")) {
        val (keyboard, hint) = when (fieldToEdit) {
            "E-mail" -> KeyboardType.Email to "mail@example.com"
            "Возраст" -> KeyboardType.Number to "5–100"
            "Рост (см)", "Вес (кг)", "Плечи (см)", "Талия (см)", "Бёдра (см)", "Грудь (см)" ->
                KeyboardType.Number to "например, 172"
            else -> KeyboardType.Text to ""
        }

        AlertDialog(
            onDismissRequest = { fieldToEdit = null },
            icon = { Icon(getFieldIcon(fieldToEdit!!), contentDescription = null) },
            title = { Text("Изменить $fieldToEdit") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempText,
                        onValueChange = {
                            tempText = when (fieldToEdit) {
                                "Возраст" -> it.filter { ch -> ch.isDigit() }.take(3)
                                "Рост (см)", "Вес (кг)", "Плечи (см)", "Талия (см)", "Бёдра (см)", "Грудь (см)" ->
                                    it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' }.take(6)
                                else -> it
                            }
                            tempError = null
                        },
                        label = { Text(hint.ifBlank { fieldToEdit!! }) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboard),
                        isError = tempError != null,
                        supportingText = { if (tempError != null) Text(tempError!!) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Для поля "Пол" — подсказка допустимых значений
                    if (fieldToEdit == "Пол") {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Допустимо: Мужчина / Женщина",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Валидация перед применением
                    tempError = validateField(fieldToEdit!!, tempText)
                    if (tempError == null) {
                        editableUser = applyField(editableUser, fieldToEdit!!, tempText)
                        fieldToEdit = null
                    }
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { fieldToEdit = null }) { Text("Отмена") }
            }
        )
    }

    /* ---------- Календарь: показывается ТОЛЬКО по onDateClick ---------- */
    if (showDatePickerFor != null && editableUser != null) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            LocalContext.current,
            { _, y, m, d ->
                val date = authViewModel.formatDate(y, m, d)
                editableUser = when (showDatePickerFor) {
                    "Дата измерений" -> editableUser!!.copy(measurementDate = date)
                    "Дедлайн цели" -> editableUser!!.copy(goalDeadline = date)
                    else -> editableUser
                }
                showDatePickerFor = null
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePickerFor = null }
        }.show()
    }
}

/* ====================== Вспомогательные UI ====================== */

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ProfileFieldRow(
    label: String,
    value: String,
    icon: ImageVector,
    onEditClick: (String) -> Unit = {},   // ← добавили значение по умолчанию
    isDate: Boolean = false,
    onDateClick: (String) -> Unit = {}    // уже было с дефолтом
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        if (isDate) {
            TextButton(
                onClick = { onDateClick(label) },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Выбрать")
            }
        } else {
            TextButton(
                onClick = { onEditClick(label) },
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Изменить")
            }
        }
    }
}


/* ====================== Валидация и применение ====================== */

private fun validateField(field: String, raw: String): String? {
    fun toFloat() = raw.replace(',', '.').toFloatOrNull()
    fun toInt() = raw.toIntOrNull()

    return when (field) {
        "E-mail" -> {
            val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")
            if (!regex.matches(raw.trim())) "Некорректный e-mail" else null
        }
        "Имя", "Фамилия" -> {
            if (raw.trim().length < 2) "Слишком коротко" else null
        }
        "Пол" -> {
            val v = raw.trim().lowercase()
            if (v !in listOf("мужчина", "женщина")) "Введите: Мужчина или Женщина" else null
        }
        "Возраст" -> {
            val v = toInt() ?: return "Только целое число"
            if (v !in 5..100) "Диапазон 5–100" else null
        }
        "Рост (см)" -> {
            val v = toFloat() ?: return "Число (например, 172)"
            if (v !in 80f..250f) "Диапазон 80–250" else null
        }
        "Вес (кг)" -> {
            val v = toFloat() ?: return "Число (например, 72.4)"
            if (v !in 25f..350f) "Диапазон 25–350" else null
        }
        "Плечи (см)", "Талия (см)", "Бёдра (см)", "Грудь (см)" -> {
            val v = toFloat() ?: return "Число (например, 96.5)"
            if (v !in 20f..300f) "Нереалистичное значение" else null
        }
        "Цель" -> {
            if (raw.trim().isEmpty()) "Не может быть пусто" else null
        }
        else -> null
    }
}

private fun applyField(user: User?, field: String, raw: String): User? = user?.let {
    val f = raw.trim()
    fun fnum() = f.replace(',', '.').toFloatOrNull() ?: 0f
    fun inum() = f.toIntOrNull() ?: 0
    when (field) {
        "E-mail" -> it.copy(email = f)
        "Имя" -> it.copy(firstName = f)
        "Фамилия" -> it.copy(lastName = f)
        "Пол" -> it.copy(gender = f.replaceFirstChar { c -> c.titlecase() })
        "Возраст" -> it.copy(age = inum())
        "Рост (см)" -> it.copy(height = fnum())
        "Вес (кг)" -> it.copy(weight = fnum())
        "Плечи (см)" -> it.copy(shoulders = fnum())
        "Талия (см)" -> it.copy(waist = fnum())
        "Бёдра (см)" -> it.copy(hips = fnum())
        "Грудь (см)" -> it.copy(chest = fnum())
        "Цель" -> it.copy(goalName = f)
        else -> it
    }
}

private fun getFieldIcon(field: String): ImageVector = when (field) {
    "E-mail" -> Icons.Default.Email
    "Имя", "Фамилия" -> Icons.Default.Person
    "Пол" -> Icons.Default.Transgender
    "Возраст" -> Icons.Default.Cake
    "Рост (см)", "Вес (кг)", "Плечи (см)", "Талия (см)", "Бёдра (см)", "Грудь (см)" -> Icons.Default.Straighten
    "Дата измерений", "Дедлайн цели" -> Icons.Default.CalendarToday
    "Цель" -> Icons.Default.Flag
    else -> Icons.Default.Edit
}
