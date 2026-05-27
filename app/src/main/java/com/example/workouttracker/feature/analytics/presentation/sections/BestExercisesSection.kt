package com.example.workouttracker.feature.analytics.presentation.sections

import androidx.compose.runtime.Composable
import com.example.workouttracker.feature.analytics.presentation.BestExercisesCardPretty
import com.example.workouttracker.feature.training.presentation.ExerciseEntry

@Composable
fun BestExercisesSection(exercises: Collection<ExerciseEntry>) {
    BestExercisesCardPretty(exercises = exercises)
}

