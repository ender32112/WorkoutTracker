package com.example.workouttracker

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.workouttracker.ui.navigation.WorkoutNavGraph
import com.example.workouttracker.ui.theme.ThemeVariant
import com.example.workouttracker.ui.theme.WorkoutTrackerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current

            // SharedPreferences для настроек
            val prefs = remember {
                context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            }

            // Текущее состояние темы — читаем из preferences
            var themeVariant by remember {
                mutableStateOf(
                    runCatching {
                        val saved = prefs.getString("theme_variant", ThemeVariant.DARK.name)
                        ThemeVariant.valueOf(saved ?: ThemeVariant.DARK.name)
                    }.getOrDefault(ThemeVariant.DARK)
                )
            }

            // Колбэк переключения темы по кругу (7 вариантов)
            val toggleTheme: () -> Unit = {
                val next = when (themeVariant) {
                    ThemeVariant.DARK        -> ThemeVariant.LIGHT
                    ThemeVariant.LIGHT       -> ThemeVariant.BROWN
                    ThemeVariant.BROWN       -> ThemeVariant.FUCHSIA
                    ThemeVariant.FUCHSIA     -> ThemeVariant.GREEN
                    ThemeVariant.GREEN       -> ThemeVariant.BLUE_PURPLE
                    ThemeVariant.BLUE_PURPLE -> ThemeVariant.AURORA
                    ThemeVariant.AURORA      -> ThemeVariant.DARK
                }
                themeVariant = next
                // сохраняем выбор
                prefs.edit().putString("theme_variant", next.name).apply()
            }

            WorkoutTrackerTheme(variant = themeVariant) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    WorkoutNavGraph(
                        navController = navController,
                        onToggleTheme = toggleTheme
                    )
                }
            }
        }
    }
}
