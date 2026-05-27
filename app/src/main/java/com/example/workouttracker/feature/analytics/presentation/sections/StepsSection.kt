package com.example.workouttracker.feature.analytics.presentation.sections

import androidx.compose.runtime.Composable
import com.example.workouttracker.feature.analytics.presentation.StepsCardPretty
import com.example.workouttracker.feature.analytics.runtime.StepDataSource

@Composable
fun StepsSection(
    steps: Long,
    goal: Int,
    stepDataSource: StepDataSource,
    hasPermission: Boolean,
    onRequest: () -> Unit,
    onLongPressEdit: () -> Unit,
    onHistoryClick: () -> Unit
) {
    StepsCardPretty(
        steps = steps,
        goal = goal,
        stepDataSource = stepDataSource,
        hasPermission = hasPermission,
        onRequest = onRequest,
        onLongPressEdit = onLongPressEdit,
        onHistoryClick = onHistoryClick
    )
}
