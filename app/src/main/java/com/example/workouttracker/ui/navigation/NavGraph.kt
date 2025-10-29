package com.example.workouttracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.workouttracker.ui.auth.LoginScreen
import com.example.workouttracker.ui.auth.RegistrationScreen
import com.example.workouttracker.viewmodel.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Главный граф навигации.
 * Запускает Login → MainScreen.
 */
@Composable
fun WorkoutNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            RegistrationScreen(
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("main") {
            MainScreen() // Без параметров!
        }
    }
}