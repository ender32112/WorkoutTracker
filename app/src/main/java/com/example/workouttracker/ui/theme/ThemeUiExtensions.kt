package com.example.workouttracker.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

fun ThemeVariant.displayName(): String = when (this) {
    ThemeVariant.DARK        -> "Тёмная"
    ThemeVariant.LIGHT       -> "Светлая"
    ThemeVariant.BROWN       -> "Бронза"
    ThemeVariant.FUCHSIA     -> "Фуксия"
    ThemeVariant.GREEN       -> "Зелень"
    ThemeVariant.BLUE_PURPLE -> "Закат"
    ThemeVariant.AURORA      -> "Индиго"
}

fun ThemeVariant.icon(): ImageVector = when (this) {
    ThemeVariant.DARK        -> Icons.Default.DarkMode
    ThemeVariant.LIGHT       -> Icons.Default.LightMode
    ThemeVariant.BROWN       -> Icons.Default.Coffee
    ThemeVariant.FUCHSIA     -> Icons.Default.LocalFlorist
    ThemeVariant.GREEN       -> Icons.Default.Eco
    ThemeVariant.BLUE_PURPLE -> Icons.Default.WbTwilight
    ThemeVariant.AURORA      -> Icons.Default.AutoAwesome
}
