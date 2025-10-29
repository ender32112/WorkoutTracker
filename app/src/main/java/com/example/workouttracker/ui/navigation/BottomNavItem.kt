package com.example.workouttracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val title: String
) {
    object Training : BottomNavItem("training", Icons.Default.FitnessCenter, "Тренировки")
    object Nutrition: BottomNavItem("nutrition", Icons.Default.Restaurant, "Питание")
    object Analytics : BottomNavItem("analytics", Icons.Default.Analytics, "Аналитика")
    object Articles : BottomNavItem("articles", Icons.Default.MenuBook, "Статьи")
    object Achieve : BottomNavItem("achievements", Icons.Default.EmojiEvents, "Достижения")
    object Profile  : BottomNavItem("profile", Icons.Default.AccountCircle, "Профиль")
}
