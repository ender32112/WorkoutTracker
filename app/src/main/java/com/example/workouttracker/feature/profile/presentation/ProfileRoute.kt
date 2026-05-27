package com.example.workouttracker.feature.profile.presentation

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.workouttracker.ui.theme.ThemeVariant
import com.example.workouttracker.viewmodel.AuthViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

private const val FIELD_MEASUREMENT_DATE = "Дата измерений"
private const val FIELD_GOAL_DEADLINE = "Дедлайн цели"

@Composable
fun ProfileRoute(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onToggleTheme: () -> Unit,
    currentTheme: ThemeVariant
) {
    val context = LocalContext.current
    val user by authViewModel.userState.collectAsState()
    val authError by authViewModel.authError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var editableUser by remember(user) { mutableStateOf(user) }
    var requestedDateField by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authError) {
        authError?.let {
            snackbarHostState.showSnackbar(it)
            authViewModel.clearAuthError()
        }
    }

    fun saveAvatarToInternalStorage(uri: Uri): String? = try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { output -> input.copyTo(output) }
        file.absolutePath
    } catch (_: Exception) {
        null
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val internalPath = saveAvatarToInternalStorage(uri) ?: return@rememberLauncherForActivityResult
        val updatedUser = editableUser?.let { currentUser ->
            currentUser.avatarUri?.let { previous ->
                runCatching {
                    val oldFile = File(previous)
                    if (oldFile.exists()) oldFile.delete()
                }
            }
            currentUser.copy(avatarUri = internalPath)
        } ?: return@rememberLauncherForActivityResult

        editableUser = updatedUser
        authViewModel.updateUser(updatedUser)
    }

    if (requestedDateField != null && editableUser != null) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val formatted = authViewModel.formatDate(year, month, day)
                editableUser = when (requestedDateField) {
                    FIELD_MEASUREMENT_DATE -> editableUser?.copy(measurementDate = formatted)
                    FIELD_GOAL_DEADLINE -> editableUser?.copy(goalDeadline = formatted)
                    else -> editableUser
                }
                requestedDateField = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { requestedDateField = null }
        }.show()
    }

    ProfileScreen(
        editableUser = editableUser,
        savedUser = user,
        snackbarHostState = snackbarHostState,
        currentTheme = currentTheme,
        onToggleTheme = onToggleTheme,
        onLogout = {
            authViewModel.logout()
            onLogout()
        },
        onSave = { editableUser?.let(authViewModel::updateUser) },
        onPickAvatar = { imageLauncher.launch("image/*") },
        onUserChange = { editableUser = it },
        onRequestDatePicker = { requestedDateField = it }
    )
}
