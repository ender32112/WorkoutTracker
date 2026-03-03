package com.example.workouttracker.ui.navigation

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.workouttracker.ui.achievements.AchievementsScreen
import com.example.workouttracker.ui.analytics.AnalyticsScreen
import com.example.workouttracker.ui.articles.ArticlesScreen
import com.example.workouttracker.ui.profile.ProfileScreen
import com.example.workouttracker.ui.theme.ThemeVariant
import com.example.workouttracker.ui.training.TrainingScreen
import com.example.workouttracker.ui.nutrition.MealPlanScreen
import com.example.workouttracker.ui.nutrition.NutritionScreen
import com.example.workouttracker.viewmodel.AchievementViewModel
import com.example.workouttracker.viewmodel.AchievementViewModelFactory
import com.example.workouttracker.viewmodel.ArticleViewModel
import com.example.workouttracker.viewmodel.ArticleViewModelFactory
import com.example.workouttracker.viewmodel.AuthViewModel
import com.example.workouttracker.viewmodel.TrainingViewModel

@Composable
fun MainScreen(
    navController: NavController,
    currentTheme: ThemeVariant,
    onToggleTheme: () -> Unit = {}
) {
    val trainingViewModel: TrainingViewModel = viewModel()
    val context = LocalContext.current
    val application = context.applicationContext as Application

    val articleViewModel: ArticleViewModel = viewModel(
        factory = ArticleViewModelFactory(trainingViewModel, application)
    )
    val authViewModel: AuthViewModel = viewModel()
    val achievementViewModel: AchievementViewModel = viewModel(
        factory = AchievementViewModelFactory(trainingViewModel, articleViewModel, application)
    )

    var selectedRoute by remember { mutableStateOf(BottomNavItem.Training.route) }
    val items = remember {
        listOf(
            BottomNavItem.Training,
            BottomNavItem.Nutrition,
            BottomNavItem.MealPlan,
            BottomNavItem.Analytics,
            BottomNavItem.Articles,
            BottomNavItem.Achieve,
            BottomNavItem.Profile
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp

        if (useRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                AdaptiveNavigationRail(
                    items = items,
                    selectedRoute = selectedRoute,
                    onItemSelected = { selectedRoute = it }
                )
                Box(modifier = Modifier.weight(1f)) {
                    MainContent(
                        selectedRoute = selectedRoute,
                        trainingViewModel = trainingViewModel,
                        articleViewModel = articleViewModel,
                        achievementViewModel = achievementViewModel,
                        authViewModel = authViewModel,
                        navController = navController,
                        currentTheme = currentTheme,
                        onToggleTheme = onToggleTheme
                    )
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    SmoothNavigationBar(
                        items = items,
                        selectedRoute = selectedRoute,
                        onItemSelected = { route -> selectedRoute = route }
                    )
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    MainContent(
                        selectedRoute = selectedRoute,
                        trainingViewModel = trainingViewModel,
                        articleViewModel = articleViewModel,
                        achievementViewModel = achievementViewModel,
                        authViewModel = authViewModel,
                        navController = navController,
                        currentTheme = currentTheme,
                        onToggleTheme = onToggleTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    selectedRoute: String,
    trainingViewModel: TrainingViewModel,
    articleViewModel: ArticleViewModel,
    achievementViewModel: AchievementViewModel,
    authViewModel: AuthViewModel,
    navController: NavController,
    currentTheme: ThemeVariant,
    onToggleTheme: () -> Unit
) {
    AnimatedContent(
        targetState = selectedRoute,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(initialScale = 0.98f)).togetherWith(
                fadeOut(tween(160)) + scaleOut(targetScale = 0.99f)
            )
        },
        label = "main_screen_animation"
    ) { route ->
        when (route) {
            BottomNavItem.Training.route -> TrainingScreen(trainingViewModel)
            BottomNavItem.Nutrition.route -> NutritionScreen()
            BottomNavItem.MealPlan.route -> MealPlanScreen()
            BottomNavItem.Analytics.route -> AnalyticsScreen()
            BottomNavItem.Articles.route -> ArticlesScreen(trainingViewModel, articleViewModel)
            BottomNavItem.Achieve.route -> AchievementsScreen(achievementViewModel = achievementViewModel)
            BottomNavItem.Profile.route -> ProfileScreen(
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                },
                onToggleTheme = onToggleTheme,
                currentTheme = currentTheme
            )
        }
    }
}

@Composable
fun SmoothNavigationBar(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val selected = selectedRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelected(item.route) },
                icon = {
                    NavIconWithSmoothAnimation(
                        icon = item.icon,
                        title = item.title,
                        selected = selected
                    )
                },
                label = null,
                alwaysShowLabel = false
            )
        }
    }
}

@Composable
private fun AdaptiveNavigationRail(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(28.dp)
            )
        }
    ) {
        items.forEach { item ->
            val selected = selectedRoute == item.route
            NavigationRailItem(
                selected = selected,
                onClick = { onItemSelected(item.route) },
                icon = {
                    NavIconWithSmoothAnimation(icon = item.icon, title = item.title, selected = selected)
                },
                label = { Text(text = item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

@Composable
fun NavIconWithSmoothAnimation(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    selected: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.16f else 1f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier
                .size(26.dp)
                .scale(scale)
        )
        AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
