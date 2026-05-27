package com.example.workouttracker.feature.analytics.presentation.sections

import androidx.compose.runtime.Composable
import com.example.workouttracker.feature.analytics.presentation.WeightInputCardPretty

@Composable
fun WeightSection(
    input: String,
    error: String?,
    history: List<Pair<String, Float>>,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onEditClick: () -> Unit
) {
    WeightInputCardPretty(
        input = input,
        error = error,
        onInputChange = onInputChange,
        onSave = onSave,
        history = history,
        onEditClick = onEditClick
    )
}

