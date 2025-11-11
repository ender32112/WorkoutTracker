package com.example.workouttracker.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ArticleViewModelFactory(
    private val trainingViewModel: TrainingViewModel,
    private val application: Application? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            // Если Application передан извне — используем его,
            // иначе берём из TrainingViewModel (он AndroidViewModel)
            val app = application ?: trainingViewModel.getApplication<Application>()
            return ArticleViewModel(trainingViewModel, app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
