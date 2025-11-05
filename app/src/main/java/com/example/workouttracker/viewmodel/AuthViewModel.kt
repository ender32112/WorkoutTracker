package com.example.workouttracker.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class User(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val age: Int,
    val gender: String,
    val avatarUri: String?,
    val height: Float,
    val weight: Float,
    val shoulders: Float,
    val waist: Float,
    val hips: Float,
    val chest: Float,
    val measurementDate: String,
    val goalName: String,
    val goalDeadline: String,
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val authPrefs = application.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val _registrationState = MutableStateFlow(false)
    val registrationState: StateFlow<Boolean> get() = _registrationState

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> get() = _userState

    private val _isLoggedIn = MutableStateFlow(authPrefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        loadUser()
    }

    fun register(user: User) {
        viewModelScope.launch {
            with(prefs.edit()) {
                putString("email", user.email)
                putString("password", user.password)
                putString("firstName", user.firstName)
                putString("lastName", user.lastName)
                putInt("age", user.age)
                putString("gender", user.gender)
                putString("avatarUri", user.avatarUri)
                putFloat("height", user.height)
                putFloat("weight", user.weight)
                putFloat("shoulders", user.shoulders)
                putFloat("waist", user.waist)
                putFloat("hips", user.hips)
                putFloat("chest", user.chest)
                putString("measurementDate", user.measurementDate)
                putString("goalName", user.goalName)
                putString("goalDeadline", user.goalDeadline)
                apply()
            }
            _registrationState.value = true
            _userState.value = user
            login(user.email, user.password) // Автологин после регистрации
        }
    }

    fun login(email: String, password: String): Boolean {
        val savedEmail = prefs.getString("email", null)
        val savedPass = prefs.getString("password", null)
        return if (email == savedEmail && password == savedPass) {
            _isLoggedIn.value = true
            authPrefs.edit().putBoolean("is_logged_in", true).apply()
            loadUser() // Загружаем пользователя
            true
        } else {
            false
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _userState.value = null
        authPrefs.edit().putBoolean("is_logged_in", false).apply()
    }

    private fun loadUser() {
        val email = prefs.getString("email", null) ?: return
        val user = User(
            email = email,
            password = prefs.getString("password", "")!!,
            firstName = prefs.getString("firstName", "")!!,
            lastName = prefs.getString("lastName", "")!!,
            age = prefs.getInt("age", 0),
            gender = prefs.getString("gender", "")!!,
            avatarUri = prefs.getString("avatarUri", null),
            height = prefs.getFloat("height", 0f),
            weight = prefs.getFloat("weight", 0f),
            shoulders = prefs.getFloat("shoulders", 0f),
            waist = prefs.getFloat("waist", 0f),
            hips = prefs.getFloat("hips", 0f),
            chest = prefs.getFloat("chest", 0f),
            measurementDate = prefs.getString("measurementDate", "")!!,
            goalName = prefs.getString("goalName", "")!!,
            goalDeadline = prefs.getString("goalDeadline", "")!!
        )
        _userState.value = user
    }

    fun updateUser(user: User) = register(user)

    fun formatDate(year: Int, month: Int, day: Int): String {
        val cal = Calendar.getInstance().apply { set(year, month, day) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
}