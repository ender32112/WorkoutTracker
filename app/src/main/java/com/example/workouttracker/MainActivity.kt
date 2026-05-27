package com.example.workouttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.example.workouttracker.data.exercise.ExerciseSeedLoader
import com.example.workouttracker.data.local.LegacyDataMigrator
import com.example.workouttracker.data.settings.AppSettings
import com.example.workouttracker.data.settings.AppSettingsDataStore
import com.example.workouttracker.ui.navigation.WorkoutNavGraph
import com.example.workouttracker.ui.theme.ThemeVariant
import com.example.workouttracker.ui.theme.WorkoutTrackerTheme
import com.example.workouttracker.workers.StepServiceWatchdogWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var legacyDataMigrator: LegacyDataMigrator
    @Inject lateinit var exerciseSeedLoader: ExerciseSeedLoader
    @Inject lateinit var appSettingsDataStore: AppSettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StepServiceWatchdogWorker.schedule(this)
        lifecycleScope.launch {
            legacyDataMigrator.migrateAllKnownUsers()
            exerciseSeedLoader.ensureLoaded()
        }
        setContent {
            val settings by appSettingsDataStore.settingsFlow.collectAsState(
                initial = AppSettings(
                    themeVariant = ThemeVariant.DARK.name,
                    notificationsEnabled = true,
                    stepGoal = 8000,
                    exercisesLoaded = false,
                    exercisesCatalogVersion = 0
                )
            )
            val themeVariant = runCatching {
                ThemeVariant.valueOf(settings.themeVariant)
            }.getOrDefault(ThemeVariant.DARK)

            val toggleTheme: () -> Unit = {
                val next = when (themeVariant) {
                    ThemeVariant.DARK -> ThemeVariant.LIGHT
                    ThemeVariant.LIGHT -> ThemeVariant.BROWN
                    ThemeVariant.BROWN -> ThemeVariant.FUCHSIA
                    ThemeVariant.FUCHSIA -> ThemeVariant.GREEN
                    ThemeVariant.GREEN -> ThemeVariant.BLUE_PURPLE
                    ThemeVariant.BLUE_PURPLE -> ThemeVariant.AURORA
                    ThemeVariant.AURORA -> ThemeVariant.DARK
                }
                lifecycleScope.launch { appSettingsDataStore.setThemeVariant(next.name) }
            }

            WorkoutTrackerTheme(variant = themeVariant) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    WorkoutNavGraph(
                        navController = navController,
                        currentTheme = themeVariant,
                        onToggleTheme = toggleTheme
                    )
                }
            }
        }
    }
}
