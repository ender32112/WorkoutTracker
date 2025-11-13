package com.example.workouttracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* =================== Переключаемые варианты темы =================== */
enum class ThemeVariant {
    /** Следовать системной: light/dark */
    SYSTEM,
    LIGHT,
    DARK,
    WARM,      // тёплая (коричневые тона)
    FUCHSIA    // фуксия/фиолетовые акценты
}

/* =================== COLOR SCHEMES =================== */

/* Light / Dark (стандарт) */
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

/* Warm */
private val WarmLightScheme = lightColorScheme(
    primary = WarmLightPrimary,
    onPrimary = WarmLightOnPrimary,
    primaryContainer = WarmLightPrimaryContainer,
    onPrimaryContainer = WarmLightOnPrimaryContainer,
    secondary = WarmLightSecondary,
    onSecondary = WarmLightOnSecondary,
    background = WarmLightBackground,
    onBackground = WarmLightOnSurface,
    surface = WarmLightSurface,
    onSurface = WarmLightOnSurface,
    surfaceVariant = WarmLightSurfaceVariant,
    outline = WarmLightOutline,
    error = WarmLightError,
    onError = WarmLightOnError
)
private val WarmDarkScheme = darkColorScheme(
    primary = WarmDarkPrimary,
    onPrimary = WarmDarkOnPrimary,
    primaryContainer = WarmDarkPrimaryContainer,
    onPrimaryContainer = WarmDarkOnPrimaryContainer,
    secondary = WarmDarkSecondary,
    onSecondary = WarmDarkOnSecondary,
    background = WarmDarkBackground,
    onBackground = WarmDarkOnSurface,
    surface = WarmDarkSurface,
    onSurface = WarmDarkOnSurface,
    surfaceVariant = WarmDarkSurfaceVariant,
    outline = WarmDarkOutline,
    error = WarmDarkError,
    onError = WarmDarkOnError
)

/* Fuchsia */
private val FuchsiaLightScheme = lightColorScheme(
    primary = FuchsiaLightPrimary,
    onPrimary = FuchsiaLightOnPrimary,
    primaryContainer = FuchsiaLightPrimaryContainer,
    onPrimaryContainer = FuchsiaLightOnPrimaryContainer,
    secondary = FuchsiaLightSecondary,
    onSecondary = FuchsiaLightOnSecondary,
    background = FuchsiaLightBackground,
    onBackground = FuchsiaLightOnSurface,
    surface = FuchsiaLightSurface,
    onSurface = FuchsiaLightOnSurface,
    surfaceVariant = FuchsiaLightSurfaceVariant,
    outline = FuchsiaLightOutline,
    error = FuchsiaLightError,
    onError = FuchsiaLightOnError
)
private val FuchsiaDarkScheme = darkColorScheme(
    primary = FuchsiaDarkPrimary,
    onPrimary = FuchsiaDarkOnPrimary,
    primaryContainer = FuchsiaDarkPrimaryContainer,
    onPrimaryContainer = FuchsiaDarkOnPrimaryContainer,
    secondary = FuchsiaDarkSecondary,
    onSecondary = FuchsiaDarkOnSecondary,
    background = FuchsiaDarkBackground,
    onBackground = FuchsiaDarkOnSurface,
    surface = FuchsiaDarkSurface,
    onSurface = FuchsiaDarkOnSurface,
    surfaceVariant = FuchsiaDarkSurfaceVariant,
    outline = FuchsiaDarkOutline,
    error = FuchsiaDarkError,
    onError = FuchsiaDarkOnError
)

/* =================== Градиенты иElevation =================== */

val MaterialTheme.gradientPrimary: Brush
    @Composable
    get() {
        val cs = colorScheme
        val end = if (cs.tertiary != Color.Unspecified) cs.tertiary else cs.secondary
        return Brush.linearGradient(
            colors = listOf(cs.primary, end)
        )
    }

object AppElevation {
    val card: Dp = 6.dp
    val button: Dp = 3.dp
    val fab: Dp = 8.dp
}

/* =================== Главная тема =================== */

@Composable
fun WorkoutTrackerTheme(
    variant: ThemeVariant = ThemeVariant.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (variant) {
        ThemeVariant.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
        ThemeVariant.LIGHT  -> LightColorScheme
        ThemeVariant.DARK   -> DarkColorScheme
        ThemeVariant.WARM   -> if (isSystemDark) WarmDarkScheme else WarmLightScheme
        ThemeVariant.FUCHSIA-> if (isSystemDark) FuchsiaDarkScheme else FuchsiaLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

/* =================== Глобальные модификаторы =================== */
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

