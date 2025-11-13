package com.example.workouttracker.ui.navigation

import android.app.Application
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
import com.example.workouttracker.viewmodel.AchievementViewModel
import com.example.workouttracker.viewmodel.AchievementViewModelFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween


import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.room.util.copy

@Composable
fun MainScreen(
    navController: NavController,
    onToggleTheme: () -> Unit = {}   // ← добавили колбэк, по умолчанию пустой
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
            SmoothNavigationBar(
                items = items,
                selectedRoute = selectedRoute,
                onItemSelected = { route -> selectedRoute = route }
            )
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedRoute,
            animationSpec = tween(250, easing = FastOutSlowInEasing),
            label = "screen_crossfade"
        ) { route ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (route) {
                    BottomNavItem.Training.route -> TrainingScreen(trainingViewModel)
                    BottomNavItem.Nutrition.route -> NutritionScreen()
                    BottomNavItem.Analytics.route -> AnalyticsScreen()
                    BottomNavItem.Articles.route -> ArticlesScreen(trainingViewModel, articleViewModel)
                    BottomNavItem.Achieve.route -> AchievementsScreen(
                        achievementViewModel = achievementViewModel
                    )
                    BottomNavItem.Profile.route -> ProfileScreen(
                        authViewModel = authViewModel,
                        onLogout = {
                            authViewModel.logout()
                            navController.navigate("login") { popUpTo(0) { inclusive = true } }
                        },
                        onToggleTheme = onToggleTheme // ← пробросили сюда
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
fun NavIconWithSmoothAnimation(
    icon: ImageVector,
    title: String,
    selected: Boolean
) {
    val cs = MaterialTheme.colorScheme

    val tint by animateColorAsState(
        targetValue = if (selected) cs.primary else cs.onSurfaceVariant,
        animationSpec = tween(durationMillis = 250),
        label = "nav_tint"
    )

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.18f else 1f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "nav_scale"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "nav_text_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier
                .size(26.dp)
                .scale(scale)
        )
        if (textAlpha > 0f) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = tint.copy(alpha = textAlpha),  // ← теперь tint — это Color, copy доступен
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .alpha(textAlpha)
            )
        }
    }
}


