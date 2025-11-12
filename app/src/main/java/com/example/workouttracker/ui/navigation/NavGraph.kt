package com.example.workouttracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.workouttracker.ui.auth.LoginScreen
import com.example.workouttracker.ui.auth.RegistrationScreen
import com.example.workouttracker.viewmodel.AuthViewModel

@Composable
fun WorkoutNavGraph(
    navController: NavHostController = rememberNavController(),
    onToggleTheme: () -> Unit = {}   // ← добавили параметр для переключения темы
) {
    val authViewModel: AuthViewModel = viewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "main" else "login"
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
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
                authViewModel = authViewModel,
                onRegisterSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("main") {
            MainScreen(
                navController = navController,
                onToggleTheme = onToggleTheme   // ← пробрасываем дальше в профиль
            )
        }
    }
}
