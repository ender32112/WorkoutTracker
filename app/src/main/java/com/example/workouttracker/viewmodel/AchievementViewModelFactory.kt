package com.example.workouttracker.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AchievementViewModelFactory(
    private val trainingViewModel: TrainingViewModel,
    private val articleViewModel: ArticleViewModel,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AchievementViewModel::class.java)) {
            return AchievementViewModel(trainingViewModel, articleViewModel, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}