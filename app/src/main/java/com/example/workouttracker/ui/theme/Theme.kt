package com.example.workouttracker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

// === СВЕТЛАЯ СХЕМА ===
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    outline = LightOutline,
    error = LightError,
    onError = LightOnError
)

// === ТЁМНАЯ СХЕМА ===
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
    onError = DarkOnError
)

// === ГРАДИЕНТЫ ===
val MaterialTheme.gradientPrimary: Brush
    @Composable
    get() = Brush.linearGradient(
        colors = if (isSystemInDarkTheme()) listOf(DarkPrimary, Color(0xFF4FC3F7))
        else listOf(PrimaryBlue, PrimaryLight)
    )

// === ГЛОБАЛЬНЫЕ ТЕНИ ===
object AppElevation {
    val card: Dp = 6.dp
    val button: Dp = 3.dp
    val fab: Dp = 8.dp
}

// === ГЛАВНАЯ ТЕМА — БЕЗ `rememberRipple()` ===
@Composable
fun WorkoutTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

// === ГЛОБАЛЬНЫЕ МОДИФИКАТОРЫ (БЕЗ ИЗМЕНЕНИЙ В UI) ===
fun Modifier.cardStyle(): Modifier = composed {
    this
        .fillMaxWidth()
        .shadow(AppElevation.card, AppShapes.medium)
        .clip(AppShapes.medium)
        .background(MaterialTheme.colorScheme.surfaceVariant)
}

fun Modifier.buttonStyle(): Modifier = composed {
    this
        .shadow(AppElevation.button, AppShapes.medium)
        .clip(AppShapes.medium)
        .background(
            brush = MaterialTheme.gradientPrimary,
            shape = AppShapes.medium
        )
}

fun Modifier.fabStyle(): Modifier = composed {
    this
        .shadow(AppElevation.fab, AppShapes.extraLarge)
        .clip(AppShapes.extraLarge)
}