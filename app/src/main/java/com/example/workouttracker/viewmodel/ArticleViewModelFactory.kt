package com.example.workouttracker.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ArticleViewModelFactory(
    private val trainingViewModel: TrainingViewModel
) : ViewModelProvider.Factory {

    private val application: Application = trainingViewModel.getApplication()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            return ArticleViewModel(trainingViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}