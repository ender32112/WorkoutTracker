package com.example.workouttracker.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
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
    private val appContext = application.applicationContext
    private val authPrefs = appContext.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE)

    private val _registrationState = MutableStateFlow(false)
    val registrationState: StateFlow<Boolean> get() = _registrationState

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> get() = _userState

    private val _isLoggedIn = MutableStateFlow(authPrefs.getBoolean(KEY_IS_LOGGED_IN, false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    init {
        maybeMigrateLegacyUser()
        if (_isLoggedIn.value) {
            loadUser()
        }
    }

    fun register(user: User) {
        viewModelScope.launch {
            _registrationState.value = false
            _authError.value = null

            val normalizedEmail = normalizeEmail(user.email)
            val accounts = loadAccountsIndex()

            if (accounts.containsKey(normalizedEmail)) {
                _authError.value = "Пользователь с таким email уже существует"
                return@launch
            }

            val userToSave = user.copy(email = user.email.trim())
            accounts[normalizedEmail] = userToSave.id
            saveAccountsIndex(accounts)
            writeUser(userToSave)
            setCurrentUser(userToSave)
            _registrationState.value = true
        }
    }

    fun clearRegistrationState() {
        _registrationState.value = false
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun login(email: String, password: String): Boolean {
        val normalizedEmail = normalizeEmail(email)
        val accounts = loadAccountsIndex()
        val userId = accounts[normalizedEmail] ?: return false
        val storedUser = readUser(userId) ?: return false

        return if (storedUser.password == password) {
            _authError.value = null
            setCurrentUser(storedUser)
            true
        } else {
            false
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _userState.value = null
        authPrefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_CURRENT_USER_ID)
            .apply()
    }

    private fun loadUser() {
        val currentUserId = authPrefs.getString(KEY_CURRENT_USER_ID, null) ?: return
        val user = readUser(currentUserId) ?: return
        _userState.value = user
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            val current = _userState.value ?: return@launch
            val trimmedUser = user.copy(email = user.email.trim())
            val accounts = loadAccountsIndex()

            val oldEmailKey = normalizeEmail(current.email)
            val newEmailKey = normalizeEmail(trimmedUser.email)

            if (oldEmailKey != newEmailKey && accounts.containsKey(newEmailKey)) {
                _authError.value = "Пользователь с таким email уже существует"
                return@launch
            }

            if (oldEmailKey != newEmailKey) {
                accounts.remove(oldEmailKey)
                accounts[newEmailKey] = current.id
                saveAccountsIndex(accounts)
            }

            writeUser(trimmedUser)
            _userState.value = trimmedUser
            _authError.value = null
        }
    }

    private fun setCurrentUser(user: User) {
        _isLoggedIn.value = true
        _userState.value = user
        authPrefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_CURRENT_USER_ID, user.id)
            .apply()
    }

    private fun readUser(userId: String): User? {
        val prefs = profilePrefs(userId)
        val email = prefs.getString("email", null) ?: return null
        return User(
            id = userId,
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
    }

    private fun writeUser(user: User) {
        val prefs = profilePrefs(user.id)
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
    }

    private fun profilePrefs(userId: String) =
        appContext.getSharedPreferences("user_profile_" + userId, Context.MODE_PRIVATE)

    private fun normalizeEmail(email: String) = email.trim().lowercase(Locale.getDefault())

    private fun loadAccountsIndex(): MutableMap<String, String> {
        val stored = authPrefs.getStringSet(KEY_ACCOUNTS, emptySet()) ?: emptySet()
        val map = mutableMapOf<String, String>()
        for (entry in stored) {
            val parts = entry.split('|')
            if (parts.size == 2) {
                map[parts[0]] = parts[1]
            }
        }
        return map
    }

    private fun saveAccountsIndex(map: Map<String, String>) {
        val serialized = map.map { "${it.key}|${it.value}" }.toSet()
        authPrefs.edit().putStringSet(KEY_ACCOUNTS, serialized).apply()
    }

    private fun maybeMigrateLegacyUser() {
        if (authPrefs.getBoolean(KEY_LEGACY_MIGRATED, false)) return

        val legacyPrefs = appContext.getSharedPreferences(LEGACY_USER_PREFS, Context.MODE_PRIVATE)
        val legacyEmail = legacyPrefs.getString("email", null)

        if (legacyEmail != null) {
            val accounts = loadAccountsIndex()
            val normalizedEmail = normalizeEmail(legacyEmail)
            val existingId = accounts[normalizedEmail]
            val userId = existingId
                ?: authPrefs.getString(KEY_CURRENT_USER_ID, null)
                ?: UUID.randomUUID().toString()

            val migratedUser = User(
                id = userId,
                email = legacyEmail,
                password = legacyPrefs.getString("password", "") ?: "",
                firstName = legacyPrefs.getString("firstName", "") ?: "",
                lastName = legacyPrefs.getString("lastName", "") ?: "",
                age = legacyPrefs.getInt("age", 0),
                gender = legacyPrefs.getString("gender", "") ?: "",
                avatarUri = legacyPrefs.getString("avatarUri", null),
                height = legacyPrefs.getFloat("height", 0f),
                weight = legacyPrefs.getFloat("weight", 0f),
                shoulders = legacyPrefs.getFloat("shoulders", 0f),
                waist = legacyPrefs.getFloat("waist", 0f),
                hips = legacyPrefs.getFloat("hips", 0f),
                chest = legacyPrefs.getFloat("chest", 0f),
                measurementDate = legacyPrefs.getString("measurementDate", "") ?: "",
                goalName = legacyPrefs.getString("goalName", "") ?: "",
                goalDeadline = legacyPrefs.getString("goalDeadline", "") ?: ""
            )

            accounts[normalizedEmail] = userId
            saveAccountsIndex(accounts)
            writeUser(migratedUser)

            migrateSharedPrefs("training_prefs", "training_prefs_${userId}")
            migrateSharedPrefs("article_prefs", "article_prefs_${userId}")
            migrateSharedPrefs("nutrition_prefs", "nutrition_prefs_${userId}")
            migrateSharedPrefs("analytics_prefs", "analytics_prefs_${userId}")

            if (_isLoggedIn.value) {
                setCurrentUser(migratedUser)
            }
        }

        authPrefs.edit().putBoolean(KEY_LEGACY_MIGRATED, true).apply()
    }

    private fun migrateSharedPrefs(legacyName: String, newName: String) {
        val legacy = appContext.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        if (legacy.all.isEmpty()) return

        val target = appContext.getSharedPreferences(newName, Context.MODE_PRIVATE)
        val editor = target.edit().clear()
        for ((key, value) in legacy.all) {
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Float -> editor.putFloat(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.apply()
    }

    fun formatDate(year: Int, month: Int, day: Int): String {
        val cal = Calendar.getInstance().apply { set(year, month, day) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    companion object {
        const val AUTH_PREFS_NAME = "auth_prefs"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_LEGACY_MIGRATED = "legacy_migrated"
        private const val LEGACY_USER_PREFS = "user_prefs"
    }
}
