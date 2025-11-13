package com.example.workouttracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow

/* =================== Переключаемые варианты темы =================== */
enum class ThemeVariant {
    DARK,          // 1. Тёмная
    LIGHT,         // 2. Светлая
    BROWN,         // 3. Коричневая
    FUCHSIA,       // 4. Фуксия
    GREEN,         // 5. Зелёная
    BLUE_PURPLE,   // 6. Сине-фиолетовая
    AURORA         // 7. Особенная неоновая тема
}

/* =================== БАЗОВЫЕ СХЕМЫ LIGHT / DARK =================== */

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

/* =================== КОРИЧНЕВАЯ (BROWN) =================== */
// Используем твои Warm-палитры как коричневую

private val BrownLightScheme = lightColorScheme(
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

private val BrownDarkScheme = darkColorScheme(
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

/* =================== ФУКСИЯ =================== */

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

/* =================== ЗЕЛЁНАЯ =================== */

private val GreenLightScheme = lightColorScheme(
    primary = GreenLightPrimary,
    onPrimary = GreenLightOnPrimary,
    primaryContainer = GreenLightPrimaryContainer,
    onPrimaryContainer = GreenLightOnPrimaryContainer,
    secondary = GreenLightSecondary,
    onSecondary = GreenLightOnSecondary,
    background = GreenLightBackground,
    onBackground = GreenLightOnSurface,
    surface = GreenLightSurface,
    onSurface = GreenLightOnSurface,
    surfaceVariant = GreenLightSurfaceVariant,
    outline = GreenLightOutline,
    error = GreenLightError,
    onError = GreenLightOnError
)

private val GreenDarkScheme = darkColorScheme(
    primary = GreenDarkPrimary,
    onPrimary = GreenDarkOnPrimary,
    primaryContainer = GreenDarkPrimaryContainer,
    onPrimaryContainer = GreenDarkOnPrimaryContainer,
    secondary = GreenDarkSecondary,
    onSecondary = GreenDarkOnSecondary,
    background = GreenDarkBackground,
    onBackground = GreenDarkOnSurface,
    surface = GreenDarkSurface,
    onSurface = GreenDarkOnSurface,
    surfaceVariant = GreenDarkSurfaceVariant,
    outline = GreenDarkOutline,
    error = GreenDarkError,
    onError = GreenDarkOnError
)

/* =================== СИНЕ-ФИОЛЕТОВАЯ =================== */

private val BluePurpleLightScheme = lightColorScheme(
    primary = BluePurpleLightPrimary,
    onPrimary = BluePurpleLightOnPrimary,
    primaryContainer = BluePurpleLightPrimaryContainer,
    onPrimaryContainer = BluePurpleLightOnPrimaryContainer,
    secondary = BluePurpleLightSecondary,
    onSecondary = BluePurpleLightOnSecondary,
    background = BluePurpleLightBackground,
    onBackground = BluePurpleLightOnSurface,
    surface = BluePurpleLightSurface,
    onSurface = BluePurpleLightOnSurface,
    surfaceVariant = BluePurpleLightSurfaceVariant,
    outline = BluePurpleLightOutline,
    error = BluePurpleLightError,
    onError = BluePurpleLightOnError
)

private val BluePurpleDarkScheme = darkColorScheme(
    primary = BluePurpleDarkPrimary,
    onPrimary = BluePurpleDarkOnPrimary,
    primaryContainer = BluePurpleDarkPrimaryContainer,
    onPrimaryContainer = BluePurpleDarkOnPrimaryContainer,
    secondary = BluePurpleDarkSecondary,
    onSecondary = BluePurpleDarkOnSecondary,
    background = BluePurpleDarkBackground,
    onBackground = BluePurpleDarkOnSurface,
    surface = BluePurpleDarkSurface,
    onSurface = BluePurpleDarkOnSurface,
    surfaceVariant = BluePurpleDarkSurfaceVariant,
    outline = BluePurpleDarkOutline,
    error = BluePurpleDarkError,
    onError = BluePurpleDarkOnError
)

/* =================== AURORA (особенная неоновая) =================== */

private val AuroraLightScheme = lightColorScheme(
    primary = AuroraLightPrimary,
    onPrimary = AuroraLightOnPrimary,
    primaryContainer = AuroraLightPrimaryContainer,
    onPrimaryContainer = AuroraLightOnPrimaryContainer,
    secondary = AuroraLightSecondary,
    onSecondary = AuroraLightOnSecondary,
    background = AuroraLightBackground,
    onBackground = AuroraLightOnSurface,
    surface = AuroraLightSurface,
    onSurface = AuroraLightOnSurface,
    surfaceVariant = AuroraLightSurfaceVariant,
    outline = AuroraLightOutline,
    error = AuroraLightError,
    onError = AuroraLightOnError
)

private val AuroraDarkScheme = darkColorScheme(
    primary = AuroraDarkPrimary,
    onPrimary = AuroraDarkOnPrimary,
    primaryContainer = AuroraDarkPrimaryContainer,
    onPrimaryContainer = AuroraDarkOnPrimaryContainer,
    secondary = AuroraDarkSecondary,
    onSecondary = AuroraDarkOnSecondary,
    background = AuroraDarkBackground,
    onBackground = AuroraDarkOnSurface,
    surface = AuroraDarkSurface,
    onSurface = AuroraDarkOnSurface,
    surfaceVariant = AuroraDarkSurfaceVariant,
    outline = AuroraDarkOutline,
    error = AuroraDarkError,
    onError = AuroraDarkOnError
)

/* =================== Градиенты и Elevation =================== */

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
    variant: ThemeVariant = ThemeVariant.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()

    val colorScheme = when (variant) {
        ThemeVariant.DARK -> DarkColorScheme
        ThemeVariant.LIGHT -> LightColorScheme
        ThemeVariant.BROWN ->
            if (isSystemDark) BrownDarkScheme else BrownLightScheme
        ThemeVariant.FUCHSIA ->
            if (isSystemDark) FuchsiaDarkScheme else FuchsiaLightScheme
        ThemeVariant.GREEN ->
            if (isSystemDark) GreenDarkScheme else GreenLightScheme
        ThemeVariant.BLUE_PURPLE ->
            if (isSystemDark) BluePurpleDarkScheme else BluePurpleLightScheme
        ThemeVariant.AURORA ->
            if (isSystemDark) AuroraDarkScheme else AuroraLightScheme
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
