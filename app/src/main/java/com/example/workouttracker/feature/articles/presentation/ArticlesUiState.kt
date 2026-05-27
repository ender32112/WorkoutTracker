package com.example.workouttracker.feature.articles.presentation

import com.example.workouttracker.core.presentation.FeatureUiState

data class ArticlesUiState(
    val articles: List<Article> = emptyList(),
    val balance: Int = 0
) : FeatureUiState
