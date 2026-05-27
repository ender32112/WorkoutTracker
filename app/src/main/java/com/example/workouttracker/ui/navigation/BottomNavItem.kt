package com.example.workouttracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val title: String
) {
    data object Training : BottomNavItem("training", Icons.Default.FitnessCenter, "Тренировки")
    data object Nutrition : BottomNavItem("nutrition", Icons.Default.RestaurantMenu, "Питание")
    data object Analytics : BottomNavItem("analytics", Icons.Default.BarChart, "Аналитика")
    data object Profile : BottomNavItem("profile", Icons.Default.Person, "Профиль")
}
