package com.example.workouttracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.workouttracker.ui.auth.LoginScreen
import com.example.workouttracker.ui.auth.RegistrationScreen
import com.example.workouttracker.viewmodel.ArticleViewModel
import com.example.workouttracker.viewmodel.ArticleViewModelFactory
import com.example.workouttracker.viewmodel.AuthViewModel
import com.example.workouttracker.viewmodel.TrainingViewModel

@Composable
fun WorkoutNavGraph() {
    val navController = rememberNavController()
    val trainingViewModel: TrainingViewModel = viewModel()
    val articleViewModel: ArticleViewModel = viewModel(
        factory = ArticleViewModelFactory(trainingViewModel)
    )
    val authVm: AuthViewModel = viewModel()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                authViewModel = authVm,
                onNavigateToRegister = { navController.navigate("register") },
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("register") {
            RegistrationScreen(
                authViewModel = authVm,
                onRegisterSuccess = { navController.popBackStack() }
            )
        }
        composable("main") {
            val trainingViewModel: TrainingViewModel = viewModel()
            val articleViewModel: ArticleViewModel = viewModel(
                factory = ArticleViewModelFactory(trainingViewModel)
            )
            MainScreen(
                trainingViewModel = trainingViewModel,
                articleViewModel  = articleViewModel

            )
        }
    }
}
