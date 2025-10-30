package com.example.workouttracker.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.workouttracker.ui.achievements.AchievementsScreen
import com.example.workouttracker.ui.analytics.AnalyticsScreen
import com.example.workouttracker.ui.articles.ArticlesScreen
import com.example.workouttracker.ui.nutrition.NutritionScreen
import com.example.workouttracker.ui.profile.ProfileScreen
import com.example.workouttracker.ui.training.TrainingScreen
import com.example.workouttracker.viewmodel.ArticleViewModel
import com.example.workouttracker.viewmodel.ArticleViewModelFactory
import com.example.workouttracker.viewmodel.AuthViewModel
import com.example.workouttracker.viewmodel.TrainingViewModel

@Composable
fun MainScreen(navController: NavController) {  // ← ПРИНИМАЕМ!
    val localNavController = rememberNavController() // Внутренний для табов

    val trainingViewModel: TrainingViewModel = viewModel()
    val articleViewModel: ArticleViewModel = viewModel(
        factory = ArticleViewModelFactory(trainingViewModel)
    )
    val authViewModel: AuthViewModel = viewModel()

    val items = listOf(
        BottomNavItem.Training,
        BottomNavItem.Nutrition,
        BottomNavItem.Analytics,
        BottomNavItem.Articles,
        BottomNavItem.Achieve,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = localNavController.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(24.dp)) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                localNavController.navigate(item.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(localNavController.graph.startDestinationId) { saveState = true }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = localNavController,
            startDestination = BottomNavItem.Training.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Training.route) {
                TrainingScreen(trainingViewModel = trainingViewModel)
            }
            composable(BottomNavItem.Nutrition.route) {
                NutritionScreen()
            }
            composable(BottomNavItem.Analytics.route) {
                AnalyticsScreen()
            }
            composable(BottomNavItem.Articles.route) {
                ArticlesScreen(
                    trainingViewModel = trainingViewModel,
                    articleViewModel = articleViewModel
                )
            }
            composable(BottomNavItem.Achieve.route) {
                AchievementsScreen(articleViewModel = articleViewModel)
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {  // ← ВНЕШНИЙ navController!
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}