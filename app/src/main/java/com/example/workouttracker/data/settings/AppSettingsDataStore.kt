package com.example.workouttracker.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class AppSettingsDataStore(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.appSettingsDataStore.data.map { prefs ->
        AppSettings(
            themeVariant = prefs[Keys.THEME_VARIANT] ?: "DARK",
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            stepGoal = prefs[Keys.STEP_GOAL] ?: 8000,
            exercisesLoaded = prefs[Keys.EXERCISES_LOADED] ?: false,
            exercisesCatalogVersion = prefs[Keys.EXERCISES_CATALOG_VERSION] ?: 0
        )
    }

    val exercisesLoadedFlow: Flow<Boolean> = context.appSettingsDataStore.data.map { prefs ->
        prefs[Keys.EXERCISES_LOADED] ?: false
    }

    suspend fun setThemeVariant(theme: String) =
        context.appSettingsDataStore.edit { it[Keys.THEME_VARIANT] = theme }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        context.appSettingsDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }

    suspend fun setStepGoal(stepGoal: Int) =
        context.appSettingsDataStore.edit { it[Keys.STEP_GOAL] = stepGoal.coerceAtLeast(1) }

    suspend fun setExercisesLoaded(loaded: Boolean) =
        context.appSettingsDataStore.edit { it[Keys.EXERCISES_LOADED] = loaded }

    suspend fun setExercisesCatalogVersion(version: Int) =
        context.appSettingsDataStore.edit { it[Keys.EXERCISES_CATALOG_VERSION] = version }

    val exercisesCatalogVersionFlow: Flow<Int> = context.appSettingsDataStore.data.map { prefs ->
        prefs[Keys.EXERCISES_CATALOG_VERSION] ?: 0
    }

    suspend fun hasCompletedMigration(token: String): Boolean =
        context.appSettingsDataStore.data.map { prefs ->
            prefs[Keys.COMPLETED_MIGRATIONS].orEmpty().contains(token)
        }.first()

    suspend fun markMigrationCompleted(token: String) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[Keys.COMPLETED_MIGRATIONS] = prefs[Keys.COMPLETED_MIGRATIONS].orEmpty() + token
        }
    }

    suspend fun updateFromLegacyPreferences(theme: String, notificationsEnabled: Boolean, stepGoal: Int) {
        context.appSettingsDataStore.edit {
            it[Keys.THEME_VARIANT] = theme
            it[Keys.NOTIFICATIONS_ENABLED] = notificationsEnabled
            it[Keys.STEP_GOAL] = stepGoal.coerceAtLeast(1)
        }
    }

    private object Keys {
        val THEME_VARIANT: Preferences.Key<String> = stringPreferencesKey("theme_variant")
        val NOTIFICATIONS_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("notify_steps_enabled")
        val STEP_GOAL: Preferences.Key<Int> = intPreferencesKey("step_goal")
        val EXERCISES_LOADED: Preferences.Key<Boolean> = booleanPreferencesKey("exercises_loaded")
        val EXERCISES_CATALOG_VERSION: Preferences.Key<Int> = intPreferencesKey("exercises_catalog_version")
        val COMPLETED_MIGRATIONS: Preferences.Key<Set<String>> = stringSetPreferencesKey("completed_migrations")
    }
}

data class AppSettings(
    val themeVariant: String,
    val notificationsEnabled: Boolean,
    val stepGoal: Int,
    val exercisesLoaded: Boolean,
    val exercisesCatalogVersion: Int
)
