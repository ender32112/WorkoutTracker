package com.example.workouttracker.ui.navigation

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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.workouttracker.feature.analytics.presentation.AnalyticsScreen
import com.example.workouttracker.feature.analytics.presentation.AnalyticsViewModel
import com.example.workouttracker.feature.nutrition.presentation.NutritionScreen
import com.example.workouttracker.feature.nutrition.presentation.NutritionViewModel
import com.example.workouttracker.feature.profile.presentation.ProfileRoute
import com.example.workouttracker.feature.training.presentation.TrainingScreen
import com.example.workouttracker.feature.training.presentation.TrainingViewModel
import com.example.workouttracker.ui.theme.ThemeVariant
import com.example.workouttracker.viewmodel.AuthViewModel

@Composable
fun MainScreen(
    navController: NavController,
    currentTheme: ThemeVariant,
    onToggleTheme: () -> Unit = {}
) {
    val trainingViewModel: TrainingViewModel = hiltViewModel()
    val nutritionViewModel: NutritionViewModel = hiltViewModel()
    val analyticsViewModel: AnalyticsViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    var selectedRoute by rememberSaveable { mutableStateOf(BottomNavItem.Training.route) }
    val items = remember {
        listOf(
            BottomNavItem.Training,
            BottomNavItem.Nutrition,
            BottomNavItem.Analytics,
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
                        nutritionViewModel = nutritionViewModel,
                        analyticsViewModel = analyticsViewModel,
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
                        nutritionViewModel = nutritionViewModel,
                        analyticsViewModel = analyticsViewModel,
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
    nutritionViewModel: NutritionViewModel,
    analyticsViewModel: AnalyticsViewModel,
    authViewModel: AuthViewModel,
    navController: NavController,
    currentTheme: ThemeVariant,
    onToggleTheme: () -> Unit
) {
    AnimatedContent(
        targetState = selectedRoute,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(initialScale = 0.985f)).togetherWith(
                fadeOut(tween(160)) + scaleOut(targetScale = 0.995f)
            )
        },
        label = "main_screen_animation"
    ) { route ->
        when (route) {
            BottomNavItem.Training.route -> TrainingScreen(trainingViewModel)
            BottomNavItem.Nutrition.route -> NutritionScreen(viewModel = nutritionViewModel)
            BottomNavItem.Analytics.route -> AnalyticsScreen(
                trainingViewModel = trainingViewModel,
                nutritionViewModel = nutritionViewModel,
                analyticsViewModel = analyticsViewModel
            )
            BottomNavItem.Profile.route -> ProfileRoute(
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
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val selected = selectedRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemSelected(item.route) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
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
}

@Composable
private fun AdaptiveNavigationRail(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onItemSelected: (String) -> Unit
) {
    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
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
                label = {
                    Text(
                        text = item.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
        targetValue = if (selected) 1.12f else 1f,
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
                .size(24.dp)
                .scale(scale)
        )
        AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
