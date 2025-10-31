package com.example.workouttracker.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
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
fun MainScreen(navController: NavController) {
    val trainingViewModel: TrainingViewModel = viewModel()
    val articleViewModel: ArticleViewModel = viewModel(
        factory = ArticleViewModelFactory(trainingViewModel)
    )
    val authViewModel: AuthViewModel = viewModel()

    // Состояние выбранного таба
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
                onItemSelected = { route ->
                    selectedRoute = route // ← Просто меняем состояние
                }
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
                    BottomNavItem.Achieve.route -> AchievementsScreen(articleViewModel)
                    BottomNavItem.Profile.route -> ProfileScreen(authViewModel) {
                        authViewModel.logout()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }
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
    val transition = updateTransition(selected, label = "icon_transition")

    val tint by transition.animateColor(
        transitionSpec = { tween(250) },
        label = "tint"
    ) { isSelected ->
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    }

    val scale by transition.animateFloat(
        transitionSpec = { tween(250, easing = FastOutSlowInEasing) },
        label = "scale"
    ) { isSelected -> if (isSelected) 1.18f else 1f }

    val textAlpha by transition.animateFloat(
        transitionSpec = { tween(200) },
        label = "text_alpha"
    ) { isSelected -> if (isSelected) 1f else 0f }

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
                color = tint.copy(alpha = textAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .alpha(textAlpha)
            )
        }
    }
}