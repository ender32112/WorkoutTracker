package com.example.workouttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.workouttracker.ui.navigation.WorkoutNavGraph
import com.example.workouttracker.ui.theme.ThemeVariant
import com.example.workouttracker.ui.theme.WorkoutTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // текущее состояние темы — переживает конфигурационные изменения
            var themeVariant by rememberSaveable { mutableStateOf(ThemeVariant.SYSTEM) }

            WorkoutTrackerTheme(variant = themeVariant) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Колбэк переключения темы по кругу:
                    val toggleTheme: () -> Unit = {
                        themeVariant = when (themeVariant) {
                            ThemeVariant.SYSTEM  -> ThemeVariant.LIGHT
                            ThemeVariant.LIGHT   -> ThemeVariant.DARK
                            ThemeVariant.DARK    -> ThemeVariant.WARM
                            ThemeVariant.WARM    -> ThemeVariant.FUCHSIA
                            ThemeVariant.FUCHSIA -> ThemeVariant.SYSTEM
                        }
                    }

                    // >>> ВАЖНО: передайте toggleTheme внутрь графа/экрана MainScreen <<<
                    WorkoutNavGraph(
                        navController = navController,
                        onToggleTheme = toggleTheme
                    )
                }
            }
        }
    }
}
