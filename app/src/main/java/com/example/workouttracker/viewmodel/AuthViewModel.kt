package com.example.workouttracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workouttracker.core.auth.AuthSessionStore
import com.example.workouttracker.data.local.LegacyDataMigrator
import com.example.workouttracker.data.local.UserEntity
import com.example.workouttracker.data.local.UserRepository
import com.example.workouttracker.data.local.WeightSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

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

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authSessionStore: AuthSessionStore,
    private val userRepository: UserRepository,
    private val migrator: LegacyDataMigrator,
    private val weightSyncRepository: WeightSyncRepository
) : ViewModel() {

    private val _registrationState = MutableStateFlow(false)
    val registrationState: StateFlow<Boolean> get() = _registrationState

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> get() = _userState

    private val _isLoggedIn = MutableStateFlow(authSessionStore.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    init {
        maybeMigrateLegacyUser()
        viewModelScope.launch {
            migrator.migrateAllKnownUsers()
            if (_isLoggedIn.value) {
                authSessionStore.currentUserIdOrNull()?.let { weightSyncRepository.harmonizeCurrentWeight(it) }
                loadUser()
            }
        }
    }

    fun register(user: User) {
        viewModelScope.launch {
            _registrationState.value = false
            _authError.value = null

            val normalizedEmail = normalizeEmail(user.email)
            val accounts = authSessionStore.loadAccountsIndex()
            if (userRepository.getUserByEmail(normalizedEmail) != null || accounts.containsKey(normalizedEmail)) {
                _authError.value = "Пользователь с таким email уже существует"
                return@launch
            }

            val userToSave = user.copy(email = user.email.trim())
            accounts[normalizedEmail] = userToSave.id
            authSessionStore.saveAccountsIndex(accounts)
            writeUser(userToSave)
            if (userToSave.weight > 0f) {
                weightSyncRepository.saveWeightMeasurement(userToSave.id, userToSave.weight)
            }
            val persistedUser = userRepository.getUserById(userToSave.id)?.toDomain() ?: userToSave
            setCurrentUser(persistedUser)
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
        val storedUser = runCatching { runBlocking { userRepository.getUserByEmail(normalizedEmail) } }
            .getOrNull()
            ?.toDomain()
            ?: return false

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
        authSessionStore.clearSession()
    }

    private suspend fun loadUser() {
        val currentUserId = authSessionStore.currentUserIdOrNull() ?: return
        val user = userRepository.getUserById(currentUserId)?.toDomain() ?: return
        _userState.value = user
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            val current = _userState.value ?: return@launch
            val trimmedUser = user.copy(email = user.email.trim())
            val accounts = authSessionStore.loadAccountsIndex()

            val oldEmailKey = normalizeEmail(current.email)
            val newEmailKey = normalizeEmail(trimmedUser.email)
            val existingUser = userRepository.getUserByEmail(newEmailKey)

            if (oldEmailKey != newEmailKey && existingUser != null && existingUser.id != current.id) {
                _authError.value = "Пользователь с таким email уже существует"
                return@launch
            }

            if (oldEmailKey != newEmailKey) {
                accounts.remove(oldEmailKey)
                accounts[newEmailKey] = current.id
                authSessionStore.saveAccountsIndex(accounts)
            }

            writeUser(trimmedUser)
            if (trimmedUser.weight > 0f && current.weight != trimmedUser.weight) {
                weightSyncRepository.saveWeightMeasurement(trimmedUser.id, trimmedUser.weight)
            } else {
                weightSyncRepository.harmonizeCurrentWeight(trimmedUser.id)
            }
            _userState.value = userRepository.getUserById(trimmedUser.id)?.toDomain() ?: trimmedUser
            _authError.value = null
        }
    }

    private fun setCurrentUser(user: User) {
        _isLoggedIn.value = true
        _userState.value = user
        authSessionStore.setCurrentUser(user.id)
    }

    private suspend fun writeUser(user: User) {
        userRepository.upsertUser(user.toEntity())
    }

    private fun normalizeEmail(email: String) = email.trim().lowercase(Locale.getDefault())

    private fun maybeMigrateLegacyUser() {
        if (authSessionStore.isLegacyMigrated()) return

        val legacyPrefs = appContext.getSharedPreferences(LEGACY_USER_PREFS, Context.MODE_PRIVATE)
        val legacyEmail = legacyPrefs.getString("email", null)

        if (legacyEmail != null) {
            val accounts = authSessionStore.loadAccountsIndex()
            val normalizedEmail = normalizeEmail(legacyEmail)
            val existingId = accounts[normalizedEmail]
            val userId = existingId
                ?: authSessionStore.currentUserIdOrNull()
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
            authSessionStore.saveAccountsIndex(accounts)
            viewModelScope.launch { writeUser(migratedUser) }

            migrateSharedPrefs("training_prefs", "training_prefs_$userId")
            migrateSharedPrefs("nutrition_prefs", "nutrition_prefs_$userId")
            migrateSharedPrefs("analytics_prefs", "analytics_prefs_$userId")

            if (_isLoggedIn.value) {
                setCurrentUser(migratedUser)
            }
        }

        authSessionStore.markLegacyMigrated()
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
        const val AUTH_PREFS_NAME = AuthSessionStore.PREFS_NAME
        const val KEY_CURRENT_USER_ID = AuthSessionStore.KEY_CURRENT_USER_ID
        const val KEY_IS_LOGGED_IN = AuthSessionStore.KEY_IS_LOGGED_IN
        const val KEY_ACCOUNTS = AuthSessionStore.KEY_ACCOUNTS
        const val KEY_LEGACY_MIGRATED = AuthSessionStore.KEY_LEGACY_MIGRATED
        const val LEGACY_USER_PREFS = "user_prefs"
    }
}

private fun UserEntity.toDomain() = User(
    id = id,
    email = email,
    password = password,
    firstName = firstName,
    lastName = lastName,
    age = age,
    gender = gender,
    avatarUri = avatarUri,
    height = height,
    weight = weight,
    shoulders = shoulders,
    waist = waist,
    hips = hips,
    chest = chest,
    measurementDate = measurementDate,
    goalName = goalName,
    goalDeadline = goalDeadline
)

private fun User.toEntity() = UserEntity(
    id = id,
    name = listOf(firstName, lastName).joinToString(" ").trim(),
    email = email.trim(),
    password = password,
    firstName = firstName,
    lastName = lastName,
    age = age,
    gender = gender,
    avatarUri = avatarUri,
    height = height,
    weight = weight,
    shoulders = shoulders,
    waist = waist,
    hips = hips,
    chest = chest,
    measurementDate = measurementDate,
    goalName = goalName,
    goalDeadline = goalDeadline
)
