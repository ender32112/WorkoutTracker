package com.example.workouttracker.core.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSessionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun currentUserIdOrGuest(): String = prefs.getString(KEY_CURRENT_USER_ID, null) ?: DEFAULT_USER_ID

    fun currentUserIdOrNull(): String? = prefs.getString(KEY_CURRENT_USER_ID, null)

    fun setCurrentUser(userId: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_CURRENT_USER_ID, userId)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_CURRENT_USER_ID)
            .apply()
    }

    fun loadAccountsIndex(): MutableMap<String, String> {
        val stored = prefs.getStringSet(KEY_ACCOUNTS, emptySet()) ?: emptySet()
        return buildMap {
            stored.forEach { entry ->
                val parts = entry.split('|')
                if (parts.size == 2) {
                    put(parts[0], parts[1])
                }
            }
        }.toMutableMap()
    }

    fun saveAccountsIndex(map: Map<String, String>) {
        val serialized = map.map { "${it.key}|${it.value}" }.toSet()
        prefs.edit().putStringSet(KEY_ACCOUNTS, serialized).apply()
    }

    fun markLegacyMigrated() {
        prefs.edit().putBoolean(KEY_LEGACY_MIGRATED, true).apply()
    }

    fun isLegacyMigrated(): Boolean = prefs.getBoolean(KEY_LEGACY_MIGRATED, false)

    companion object {
        const val PREFS_NAME = "auth_prefs"
        const val KEY_CURRENT_USER_ID = "current_user_id"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_ACCOUNTS = "accounts"
        const val KEY_LEGACY_MIGRATED = "legacy_migrated"
        const val DEFAULT_USER_ID = "guest"
    }
}
