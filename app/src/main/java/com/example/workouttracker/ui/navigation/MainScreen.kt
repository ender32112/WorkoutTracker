package com.example.workouttracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.workouttracker.ui.achievements.AchievementsScreen
import com.example.workouttracker.ui.articles.ArticlesScreen
import com.example.workouttracker.ui.nutrition.NutritionScreen
import com.example.workouttracker.ui.profile.ProfileScreen
import com.example.workouttracker.ui.training.TrainingScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import com.example.workouttracker.viewmodel.ArticleViewModel
import com.example.workouttracker.viewmodel.AuthViewModel
import com.example.workouttracker.viewmodel.TrainingViewModel




@Composable
fun MainScreen(trainingViewModel: TrainingViewModel, articleViewModel: ArticleViewModel) {
    val navController = rememberNavController()

    val authViewModel: AuthViewModel = viewModel()
        val items = listOf(
        BottomNavItem.Training,
        BottomNavItem.Nutrition,
        BottomNavItem.Articles,
        BottomNavItem.Achieve,
        BottomNavItem.Profile
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val current = navController.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Box(
                                modifier = Modifier
                                    .wrapContentSize()               // по ширине тащим
                                    .wrapContentHeight(),         // высоту — по контенту
                                contentAlignment = Alignment.BottomCenter // «прижимаем» вниз
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = { Text(item.title) },
                        selected = current == item.route,
                        onClick = {
                            if (current != item.route) {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                    restoreState    = true
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                }
                            }
                        }
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Training.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Training.route) {
                TrainingScreen(trainingViewModel)
            }
            composable(BottomNavItem.Nutrition.route) {
                NutritionScreen()
            }
            composable(BottomNavItem.Articles.route) {
                ArticlesScreen(
                    trainingViewModel = trainingViewModel,
                    articleViewModel  = articleViewModel
                )
            }
            composable(BottomNavItem.Achieve.route) {
                AchievementsScreen(
                    articleViewModel  = articleViewModel)
            }
            composable(BottomNavItem.Profile.route) {
                val authViewModel: AuthViewModel = viewModel()
                ProfileScreen(
                    authViewModel = authViewModel,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                )
        }
    }
}
}
